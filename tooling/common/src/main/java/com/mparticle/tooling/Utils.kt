package com.mparticle.tooling

import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

object Utils {
    fun getCurrentFileLocation(fileName: String): String = javaClass
        .getResource(fileName)
        .path
        .replace(
            "file:",
            "",
        ).split("lint.jar")[0]

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
            }",
        )
        var error = ""
        var result = ""
        val processBuilder = ProcessBuilder()
        if (path != null) {
            val envMap = mapOf("PATH" to path)
            processBuilder.environment().putAll(envMap)
        }
        val p =
            processBuilder
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
        val file = File(getFileLocation(CONFIG_FILE_NAME))
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
        val file = File(getFileLocation(CONFIG_FILE_NAME))
        if (file.exists()) {
            return file.lastModified()
        } else {
            return 0
        }
    }

    fun setConfigFile(config: Config) {
        stateFile(CONFIG_FILE_NAME)
            .writeText(config.toJson().toString())
    }

    fun removeConfigFile() {
        val file = File(getFileLocation(CONFIG_FILE_NAME))
        if (file.exists()) {
            file.delete()
        }
    }

    fun getLocalDataplan(): String? {
        val file = File(getFileLocation(DATAPLAN_FILE_NAME))
        if (file.exists()) {
            return file.readText()
        } else {
            return null
        }
    }

    fun removeLocalDataplan() {
        val file = File(getFileLocation(DATAPLAN_FILE_NAME))
        if (file.exists()) {
            file.delete()
        }
    }

    fun setLocalDataplan(dataplan: String) {
        stateFile(DATAPLAN_FILE_NAME)
            .writeText(dataplan)
    }

    fun getFileLocation(fileName: String): String = File(stateDirectory(), fileName).path

    private fun stateDirectory(): File = File(System.getProperty("user.home"), STATE_DIRECTORY_NAME)

    // Creates the state directory, private to the current user, on first write.
    private fun stateFile(fileName: String): File {
        val directory = stateDirectory().toPath()
        if (!Files.isDirectory(directory)) {
            if (directory.fileSystem.supportedFileAttributeViews().contains("posix")) {
                Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
            } else {
                Files.createDirectories(directory)
            }
        }
        return directory.resolve(fileName).toFile()
    }

    private const val STATE_DIRECTORY_NAME = ".mparticle"
}

const val CONFIG_FILE_NAME = "mparticle-config"
const val DATAPLAN_FILE_NAME = "mparticle-dataplan"
