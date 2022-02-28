package com.mparticle

import com.mparticle.tooling.Config
import groovy.util.GroovyTestCase.assertEquals
import org.junit.Test

class MParticlePluginTest {

    @Test
    fun testConfigSerialization() {
        val config = Config().apply {
            this.credentialsFilePath = "credentialsFilePath"
            this.dataPlanVersionFile = "dataplanfile"
            this.dataPlanId = "dataplanid"
            this.dataPlanVersion = "dataplanversion"
            this.debugReportServerMessage = true
            this.resultsFile = "resultsfilelocation"
            this.verbose = null
            this.workspaceId = "workspaceId"
            this.internalConfig = Config.InternalConfig(
                "path/to/node",
                "path/to/mp"
            )
        }
        val json = config.toJson()
        assertEquals(config, Config.from(json))
    }
}
