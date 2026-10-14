package com.mparticle

import com.mparticle.tooling.CONFIG_FILE_NAME
import com.mparticle.tooling.Config
import com.mparticle.tooling.Utils
import groovy.util.GroovyTestCase.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

class MParticlePluginTest {
    @Test
    fun testConfigSerialization() {
        val config =
            Config().apply {
                this.credentialsFilePath = "credentialsFilePath"
                this.dataPlanVersionFile = "dataplanfile"
                this.dataPlanId = "dataplanid"
                this.dataPlanVersion = "dataplanversion"
                this.debugReportServerMessage = true
                this.resultsFile = "resultsfilelocation"
                this.verbose = null
                this.workspaceId = "workspaceId"
                this.internalConfig =
                    Config.InternalConfig(
                        "path/to/node",
                        "path/to/mp",
                    )
            }
        val json = config.toJson()
        assertEquals(config, Config.from(json))
    }

    @Test
    fun testConfigFileIsWrittenToAPrivateDirectoryInTheUserHome() {
        val home = Files.createTempDirectory("home").toFile()
        val originalHome = System.getProperty("user.home")
        System.setProperty("user.home", home.path)
        try {
            val stateDirectory = File(home, ".mparticle")
            assertNull(Utils.getConfigFile())
            assertFalse(stateDirectory.exists())

            val config =
                Config().apply {
                    this.internalConfig = Config.InternalConfig("path/to/node", "path/to/mp")
                }
            Utils.setConfigFile(config)

            assertEquals(stateDirectory, File(Utils.getFileLocation(CONFIG_FILE_NAME)).parentFile)
            assertEquals(config, Utils.getConfigFile())
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                assertEquals("rwx------", PosixFilePermissions.toString(Files.getPosixFilePermissions(stateDirectory.toPath())))
            }
        } finally {
            System.setProperty("user.home", originalHome)
            home.deleteRecursively()
        }
    }
}
