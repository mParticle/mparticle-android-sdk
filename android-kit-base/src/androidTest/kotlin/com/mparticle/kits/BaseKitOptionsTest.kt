package com.mparticle.kits

import com.mparticle.AccessUtils
import com.mparticle.Configuration
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.networking.MockServer
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.json.JSONException
import org.json.JSONObject

open class BaseKitOptionsTest: BaseCleanInstallEachTest() {
    fun startMParticle(optionsBuilder: MParticleOptions.Builder, server: MockServer) {
        AccessUtils.setCredentialsIfEmpty(optionsBuilder)
        val options: MParticleOptions = optionsBuilder.build()
        val kitOptions = options.getConfiguration(KitOptions::class.java)
        var config: JSONObject? = null
        if (kitOptions != null) {
            val kitConfigurations: MutableMap<Class<out KitIntegration>?, JSONObject?> = mutableMapOf()
            val kitIntegrations: Map<Int, Class<out KitIntegration>> = kitOptions.kits
            for ((key, value) in kitIntegrations) {
                try {
                    val jsonObject = JSONObject()
                    jsonObject.put("id", key)
                    kitConfigurations[value] = jsonObject
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            config = setupConfigMessageForKits(kitConfigurations)
        }
        if (config != null) {
            optionsBuilder.configuration(ConfigManagerConfiguration(config))
        }
        startMParticle(optionsBuilder)
    }


    internal class ConfigManagerConfiguration(var initialConfig: JSONObject) : Configuration<ConfigManager> {
        override fun configures(): Class<ConfigManager> {
            return ConfigManager::class.java
        }

        override fun apply(configManager: ConfigManager) {
            try {
                configManager.updateConfig(initialConfig)
            } catch (e: JSONException) {
                throw RuntimeException("Unable to set initial configuration: $initialConfig")
            }
        }

    }
}