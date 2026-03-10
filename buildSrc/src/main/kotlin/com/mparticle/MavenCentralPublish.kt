package com.mparticle

import com.mparticle.publish.MParticleMavenPublishExtension
import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal
import org.gradle.jvm.tasks.Jar

fun Project.configureMavenPublishing(mparticleMavenPublish: MParticleMavenPublishExtension) {
    val versionFromProperty = project.findProperty("VERSION")?.toString()
        .takeIf { !it.isNullOrBlank() } ?: "0.0.0"
    val shouldSign = !project.findProperty("signingInMemoryKey")?.toString().isNullOrBlank()
    // Capture before entering configure block where `this` changes to MavenPublishBaseExtension
    val isAndroidLibrary = pluginManager.hasPlugin("com.android.library")

    extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
        // Dokka (bundled with AGP 8.3.2) uses ASM8, which cannot handle PermittedSubclasses
        // attributes present in sealed classes compiled with JVM 17 (e.g. RoktEvent in
        // android-core). Prevent AGP's withJavadocJar() from registering the failing
        // javaDocReleaseGeneration task; we attach a stub empty javadoc jar in afterEvaluate.
        if (isAndroidLibrary) {
            configure(AndroidSingleVariantLibrary(sourcesJar = true, publishJavadocJar = false))
        }

        val publicationName = "maven"

        afterEvaluate {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
            coordinates(
                groupId = mparticleMavenPublish.groupId.getOrElse("com.mparticle"),
                artifactId = mparticleMavenPublish.artifactId.getOrElse(project.name),
                version = versionFromProperty,
            )
            if (shouldSign) {
                signAllPublications()
            } else {
                logger.lifecycle("Skipping signAllPublications for ${project.name} (no signingInMemoryKey)")
            }
            pom {
                name.set(mparticleMavenPublish.description.getOrElse(project.name))
                description.set(mparticleMavenPublish.description.getOrElse(project.name))
                url.set("https://github.com/mparticle/mparticle-android-sdk")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("mParticle")
                        name.set("mParticle Inc.")
                        email.set("developers@mparticle.com")
                    }
                }
                scm {
                    url.set("https://github.com/mparticle/mparticle-android-sdk")
                    connection.set("scm:git:https://github.com/mparticle/mparticle-android-sdk")
                    developerConnection.set("scm:git:git@github.com:mparticle/mparticle-android-sdk.git")
                }
            }

            // Attach a stub empty javadoc jar to satisfy Maven Central requirements.
            // The javaDocReleaseGeneration Dokka task was intentionally bypassed above.
            if (isAndroidLibrary) {
                val emptyJavadocJar = tasks.register("emptyJavadocJar", Jar::class.java)
                emptyJavadocJar.configure { archiveClassifier.set("javadoc") }
                project.extensions.getByType(PublishingExtension::class.java)
                    .publications
                    .withType(MavenPublication::class.java)
                    .matching { it.name == publicationName }
                    .configureEach { artifact(emptyJavadocJar) }
            }
        }

        val validateTaskName =
            "validatePomFor${publicationName.replaceFirstChar { it.uppercaseChar() }}Publication"

        tasks.register(validateTaskName, ValidatePomTask::class.java) {
            description = "Validates the generated POM file for the '$publicationName' publication."
            group = "verification"
            pomFile.set(project.layout.buildDirectory.file("publications/$publicationName/pom-default.xml"))
            dependsOn("generatePomFileFor${publicationName.replaceFirstChar { it.uppercaseChar() }}Publication")
        }

        tasks.withType(PublishToMavenLocal::class.java).configureEach {
            if (name.contains(publicationName, ignoreCase = true)) {
                dependsOn(validateTaskName)
            }
        }
    }
}
