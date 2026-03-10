package com.mparticle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

abstract class ValidatePomTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val pomFile: RegularFileProperty

    @TaskAction
    fun execute() {
        val pom = pomFile.get().asFile
        if (!pom.exists()) {
            throw GradleException("POM file not found: ${pom.absolutePath}")
        }

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc: Document = builder.parse(pom)
        doc.documentElement.normalize()

        val errors = mutableListOf<String>()
        val projectElement = doc.documentElement // Root <project> element

        // Helper function to get text content of the first element by tag name
        fun getFirstElementText(parent: Element, tagName: String): String? {
            val nodeList = parent.getElementsByTagName(tagName)
            return if (nodeList.length > 0) nodeList.item(0).textContent?.trim() else null
        }

        // Helper function to check if text content is null or empty
        fun checkNotEmpty(parent: Element, tagName: String, errorMessage: String) {
            if (getFirstElementText(parent, tagName).isNullOrEmpty()) {
                errors.add(errorMessage)
            }
        }

        // --- Basic Structure Checks ---
        checkNotEmpty(projectElement, "groupId", "Missing or empty <groupId>")
        checkNotEmpty(projectElement, "artifactId", "Missing or empty <artifactId>")
        checkNotEmpty(projectElement, "version", "Missing or empty <version>")
        checkNotEmpty(projectElement, "name", "Missing or empty <name>")
        checkNotEmpty(projectElement, "description", "Missing or empty <description>")
        checkNotEmpty(projectElement, "url", "Missing or empty <url>")

        // --- License Check ---
        val licensesNode = projectElement.getElementsByTagName("licenses").item(0) as? Element
        val licenseNodes = licensesNode?.getElementsByTagName("license")
        if (licenseNodes == null || licenseNodes.length == 0) {
            errors.add("Missing <licenses> section or <license> entry")
        } else {
            for (i in 0 until licenseNodes.length) {
                val lic = licenseNodes.item(i) as Element
                checkNotEmpty(lic, "name", "Missing <name> in license")
                checkNotEmpty(lic, "url", "Missing <url> in license")
            }
        }

        // --- Developer Check ---
        val developersNode = projectElement.getElementsByTagName("developers").item(0) as? Element
        val developerNodes = developersNode?.getElementsByTagName("developer")
        if (developerNodes == null || developerNodes.length == 0) {
            errors.add("Missing <developers> section or <developer> entry")
        } else {
            for (i in 0 until developerNodes.length) {
                val dev = developerNodes.item(i) as Element
                val devId = getFirstElementText(dev, "id")
                val devName = getFirstElementText(dev, "name")
                val devEmail = getFirstElementText(dev, "email")
                if (devId.isNullOrEmpty() && devName.isNullOrEmpty() && devEmail.isNullOrEmpty()) {
                    errors.add("Developer entry needs at least <id>, <name>, or <email>")
                }
            }
        }

        // --- SCM Check ---
        val scmNode = projectElement.getElementsByTagName("scm").item(0) as? Element
        if (scmNode == null) {
            errors.add("Missing <scm> section")
        } else {
            checkNotEmpty(scmNode, "connection", "Missing <scm><connection>")
            checkNotEmpty(scmNode, "developerConnection", "Missing <scm><developerConnection>")
            checkNotEmpty(scmNode, "url", "Missing <scm><url>")
        }

        // --- Dependency Version Check ---
        val dependenciesNode = projectElement.getElementsByTagName("dependencies").item(0) as? Element
        val dependencyNodes: NodeList? = dependenciesNode?.getElementsByTagName("dependency")
        if (dependencyNodes != null) {
            for (i in 0 until dependencyNodes.length) {
                val dep = dependencyNodes.item(i) as Element
                val groupId = getFirstElementText(dep, "groupId")
                val artifactId = getFirstElementText(dep, "artifactId")
                val version = getFirstElementText(dep, "version")
                val scope = getFirstElementText(dep, "scope")
                val depId = "${groupId ?: "MISSING_GROUP"}:${artifactId ?: "MISSING_ARTIFACT"}"

                if (groupId.isNullOrEmpty()) {
                    errors.add("Dependency entry is missing <groupId>.")
                }
                if (artifactId.isNullOrEmpty()) {
                    errors.add("Dependency entry is missing <artifactId>.")
                }

                // Check specifically for missing <version> tag within a <dependency>
                // Ignore dependencies with <scope>import</scope> (BOMs)
                if (version.isNullOrEmpty() && scope != "import") {
                    errors.add("Dependency $depId is missing required <version> tag.")
                }
            }
        }

        // --- Report Errors ---
        if (errors.isNotEmpty()) {
            throw GradleException("POM validation failed for ${pom.name}:\n - ${errors.joinToString("\n - ")}")
        } else {
            logger.lifecycle("POM validation successful for ${pom.name}")
        }
    }
}
