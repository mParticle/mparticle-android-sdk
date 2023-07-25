package com.mparticle.tooling

import com.mparticle.tooling.Utils.executeCLI
import java.io.File
import java.io.IOException

enum class DataPlanError(val message: String) {
    VersionInvalid("Error: Data Plan Version is Invalid");
}

class DataPlanningNodeApp(val config: Config) {
    var path = System.getenv("PATH")

    init {
        val nodePath = config.internalConfig.nodePath
        if (nodePath?.isEmpty() == false) {
            val currentPath = System.getenv()["PATH"]

            if (currentPath?.contains(nodePath) == false && !nodePath.isEmpty()) {
                path = currentPath + ":" + nodePath.substring(0, nodePath.lastIndexOf("/node"))
            }
        }
    }

    fun validate(
        dataplan: String,
        message: String,
        version: String?
    ): NodeAppResult<List<ValidationResult>> {
        try {
            val mpCommand = config.internalConfig.mpPath ?: "mp"
            val args = if (version == null) {
                mutableListOf(
                    mpCommand,
                    "planning:events:validate",
                    "--dataPlanVersion",
                    dataplan,
                    "--translateEvents",
                    "--event",
                    message
                )
            } else {
                mutableListOf(
                    mpCommand,
                    "planning:events:validate",
                    "--dataPlan",
                    dataplan,
                    "--translateEvents",
                    "--event",
                    message,
                    "--versionNumber",
                    version
                )
            }
            val results = args.toTypedArray()
                .executeCLI(path, workingDirectory = config.credentialsFilePath ?: ".")
            val error = DataPlanError.values().firstOrNull { results.contains(it.message) }
            if (error != null) {
                return NodeAppResult(listOf(ValidationResult(error = error, arguments = args)))
            }
            return NodeAppResult(response = ValidationResult.from(results, arguments = args))
        } catch (ioe: Exception) {
            return NodeAppResult(
                ValidationResult.from(
                    "",
                    listOf("${ioe.message}\n${ioe.stackTrace.joinToString("\n")}")
                )
            )
        }
    }

    fun fetchDataPlan(accountId: String, planId: String, version: String?): NodeAppResult<String?> {
        try {
            val arguments = mutableListOf(
                "mp",
                "data-plan:fetch",
                "--accountId",
                accountId,
                "--dataPlanId",
                planId
            )
            if (version != null) {
                arguments.add("--version")
                arguments.add(version)
            }
            return NodeAppResult(response = arguments.toTypedArray().executeCLI(path))
        } catch (ioe: IOException) {
            return NodeAppResult()
        }
    }

    fun install(version: String? = null): NodeAppResult<String> {
        if (checkMPInstalled()) {
            println("MParticle CLI tools already installed")
        }
        val packageString = "@mparticle/cli" + if (version != null) "@$version" else ""
        try {
            val result = arrayOf("npm", "install", "-g", packageString).executeCLI(path)
            return NodeAppResult(result)
        } catch (ios: IOException) {
            return NodeAppResult(ios.message)
        }
    }

    fun uninstall(): NodeAppResult<String> {
        try {
            val result = arrayOf("npm", "uninstall", "-g", "@mparticle/cli").executeCLI(path)
            return NodeAppResult(result)
        } catch (ios: IOException) {
            return NodeAppResult(ios.message)
        }
    }

    fun checkMPInstalled(): Boolean {
        try {
            val mpPath = config.internalConfig.mpPath ?: "mp"
            val result = arrayOf(mpPath).executeCLI(path)
            return !(result.contains("No such file") || result.contains("command not found"))
        } catch (ioe: IOException) {
            return false
        }
    }

    fun checkNodeInstalled(): Boolean {
        try {
            val result = arrayOf("node", "-v").executeCLI(path)
            return !(result.contains("No such file") || result.contains("command not found"))
        } catch (ioe: IOException) {
            return false
        }
    }

    fun checkNPMInstalled(): Boolean {
        try {
            val result = arrayOf("npm").executeCLI(path)
            return !(result.contains("No such file") || result.contains("command not found"))
        } catch (ioe: IOException) {
            return false
        }
    }

    companion object {
        private val tempNodeFileName = "mparticle-data-planning.js"

        fun fromFile(filePath: String): DataPlanningNodeApp? {
            val nodeFile = File(filePath)
            return if (nodeFile.exists()) {
                DataPlanningNodeApp(Config())
            } else {
                null
            }
        }

        fun fromJS(jsFileBlob: String): DataPlanningNodeApp? {
            return Utils.getFileLocation(tempNodeFileName)?.let { nodeFile ->
                val nodeFile = File(nodeFile)
                nodeFile.createNewFile()
                nodeFile.writeText(jsFileBlob)
                DataPlanningNodeApp(Config())
            }
        }
    }

    class NodeAppResult<T>(val response: T? = null)
}
