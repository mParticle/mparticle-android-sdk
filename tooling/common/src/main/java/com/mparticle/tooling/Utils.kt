package com.mparticle.tooling

import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object Utils {
    fun getCurrentFileLocation(fileName: String): String {
        return javaClass.getResource(fileName).path.replace(
            "file:",
            ""
        ).split("lint.jar")[0]
    }

    fun Array<String>.executeCLI(path: String? = null, workingDirectory: String = "."): String {
        Logger.verbose(
            "command line operation: ${
            joinToString(" ") {
                if (it.contains(" ")) {
                    "\"$it\""
                } else {
                    it
                }
            }
            }"
        )
        var error = ""
        var result = ""
        val processBuilder = ProcessBuilder()
        if (path != null) {
            val envMap = mapOf("PATH" to path)
            processBuilder.environment().putAll(envMap)
        }
        val p = processBuilder
            .directory(File(workingDirectory))
            .command(*this)
            .start()
        result = BufferedReader(InputStreamReader(p.inputStream)).readText()
        error = BufferedReader(InputStreamReader(p.errorStream)).readText()
        Logger.verbose("result: $result")
        return if (result.isEmpty()) {
            "error $error"
        } else {
            result
        }
    }

    fun getConfigFile(): Config? {
        val file = File(getFileLocation(configFileName))
        if (file.exists()) {
            val contents = file.readText()
            try {
                return Config.from(JSONObject(contents))
            } catch (e: JSONException) {
                return null
            }
        }
        return null
    }

    fun getConfigFileLastModified(): Long {
        val file = File(getFileLocation(configFileName))
        if (file.exists()) {
            return file.lastModified()
        } else {
            return 0
        }
    }

    fun setConfigFile(config: Config) {
        File(getFileLocation(configFileName))
            .writeText(config.toJson().toString())
    }

    fun removeConfigFile() {
        val file = File(getFileLocation(configFileName))
        if (file.exists()) {
            file.delete()
        }
    }

    fun getLocalDataplan(): String? {
        val file = File(getFileLocation(dataplanFileName))
        if (file.exists()) {
            return file.readText()
        } else {
            return null
        }
    }

    fun removeLocalDataplan() {
        val file = File(getFileLocation(dataplanFileName))
        if (file.exists()) {
            file.delete()
        }
    }

    fun setLocalDataplan(dataplan: String) {
        File(getFileLocation(dataplanFileName))
            .writeText(dataplan)
    }

    fun getFileLocation(fileName: String): String {
        val env = System.getenv()
        var tempDirectory = env["TMPDIR"]
        if (tempDirectory?.endsWith("/") == false) {
            tempDirectory += "/"
        }
        return tempDirectory + fileName
    }
}

const val configFileName = "mparticle-config"
const val dataplanFileName = "mparticle-dataplan"
