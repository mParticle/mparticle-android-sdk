package com.mparticle

import com.mparticle.tooling.Config
import com.mparticle.tooling.DataPlanningNodeApp
import com.mparticle.tooling.Logger
import com.mparticle.tooling.Utils
import com.mparticle.tooling.Utils.executeCLI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class MParticlePlugin : Plugin<Project> {

    companion object {
        val MParticleCliVersion: String? = null
    }

    var isInstallTask = false
    var isUninstallTask = false

    override fun apply(target: Project) {
        val mpInstallTaskName = "mpInstall"
        val mpUninstallTaskName = "mpUninstall"
        target.extensions.create("mparticle", MParticleExtension::class.java)

        target.gradle.startParameter.taskNames?.apply {
            isInstallTask = any { it == mpInstallTaskName }
            isUninstallTask = any { it == mpUninstallTaskName }
        }

        target.afterEvaluate {
            updateConfig(it)
        }
        target.tasks.register(mpInstallTaskName, installTask)
        target.tasks.register(mpUninstallTaskName, uninstallTask)
    }

    private val installTask: (Task) -> Unit = {
        if (isInstallTask) {
            println("installing MParticle ClI")
            val dataPlanningNodeApp = getDataplanningApp()
            if (dataPlanningNodeApp != null) {
                if (!dataPlanningNodeApp.checkNPMInstalled()) {
                    println("MParticle requires NPM be installed. Please install NPM")
                } else if (dataPlanningNodeApp.checkMPInstalled()) {
                    println("MParticle CLI tools aready installed")
                } else {
                    val result = dataPlanningNodeApp.install()
                    println("MParticle CLI result: \n${result.response}")
                    if (result.response == null || result.response?.contains("npm ERR") == true) {
                        println("MParticle unable to install CLI tools.)")
                    } else {
                        println("MParticle CLI installed")
                    }
                }
            }
            updateConfig(it.project)
        }
    }

    private val uninstallTask: (Task) -> Unit = {
        if (isUninstallTask) {
            println("uninstalling MParticle CLI")
            val dataPlanningNodeApp = getDataplanningApp()
            if (dataPlanningNodeApp != null) {
                val result = dataPlanningNodeApp.uninstall()
                println("MParticle CLI result: \n${result.response}")
                if (result.response == null || result.response?.contains("npm ERR") ?: false) {
                    println("MParticle unable to uninstall CLI tools. To do so manually, run \"npm uninstall @mparticle/cli\"")
                } else {
                    println("MParticle CLI uninstalled")
                }
            }
            updateConfig(it.project)
        }
    }

    private fun updateConfig(project: Project) {
        val extension = project.extensions.getByType(MParticleExtension::class.java)
        clearConfig()
        var config = readConfig(extension)
        updateArtifactPaths(config)
        Utils.setConfigFile(config)
        storeDataPlanInLocalStorage(config)
    }

    private fun getDataplanningApp(noConfig: Boolean = false): DataPlanningNodeApp? {
        val config = Utils.getConfigFile() ?: if (noConfig) Config() else null
        if (config != null) {
            return DataPlanningNodeApp(config)
        } else {
            return null
        }
    }

    private fun clearConfig() {
        Utils.apply {
            removeLocalDataplan()
            removeConfigFile()
        }
    }

    private fun updateArtifactPaths(config: Config) {
        val dataPlanningNodeApp = getDataplanningApp(true)
        if (dataPlanningNodeApp?.checkNodeInstalled() == false) {
            println("MParticle Node not installed. Please install to use Dataplanning features")
        } else {
            val nodeLocation = arrayOf("type", "-p", "node").executeCLI().trim()
            Logger.verbose("node location = $nodeLocation")
            config.internalConfig.nodePath = nodeLocation
        }
        if (dataPlanningNodeApp?.checkNPMInstalled() == false) {
            println("MParticle NPM not installed. Please install to use Dataplanning features")
        }
        if (dataPlanningNodeApp?.checkMPInstalled() == false && !isInstallTask && !isUninstallTask) {
            println("MParticle CLI tools not installed. run \"./gradlew mpInstall\" to install")
        } else {
            val location = arrayOf("type", "-p", "mp").executeCLI().trim()
            Logger.verbose("mp location = $location")
            config.internalConfig.mpPath = location
        }
        Utils.setConfigFile(config)
    }

    private fun readConfig(extension: MParticleExtension): Config {
        val staticConfigFile = File("./mp.config.json")
        var config = if (staticConfigFile.exists()) {
            Logger.verbose("Config File found")
            val text = staticConfigFile.readText()
            try {
                val json = JSONObject(text)
                Config.from(json)
            } catch (jse: JSONException) {
                Logger.warning("Error reading mp.config.json: ${jse.message}")
                Config()
            }
        } else {
            Logger.verbose("Config File Not Found, using extension values")
            Config()
        }
        var credentialsFile = File("mp.config.json")
        if (credentialsFile.exists()) {
            config.credentialsFilePath = credentialsFile.absolutePath.let {
                it.substring(0, it.length - "/mp.config.json".length)
            }
        } else {
            Logger.verbose("Credentials File Not Found")
            Config()
        }

        extension.apply {
            if (resultsFile != null) {
                config.resultsFile = resultsFile
            }
            if (verbose != null) {
                config.verbose = verbose
            }
            if (debugReportServerMessage != null) {
                config.debugReportServerMessage = debugReportServerMessage
            }
            if (dataPlanVersionFile != null) {
                config.dataPlanVersionFile = dataPlanVersionFile
            }
            if (disabled != null) {
                config.disabled = disabled
            }
        }
        return config
    }

    private fun storeDataPlanInLocalStorage(config: Config) {
        Utils.removeLocalDataplan()
        if (config.dataPlanVersionFile != null) {
            if (File(config.dataPlanVersionFile).exists()) {
                Utils.setLocalDataplan(File(config.dataPlanVersionFile).readText())
            }
        }
    }
}
