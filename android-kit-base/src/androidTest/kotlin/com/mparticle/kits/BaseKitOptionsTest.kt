package com.mparticle.kits

import com.mparticle.AccessUtils
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.json.JSONArray
import org.json.JSONObject

open class BaseKitOptionsTest : BaseCleanInstallEachTest() {

    override fun startMParticle(optionsBuilder: MParticleOptions.Builder) {
        AccessUtils.setCredentialsIfEmpty(optionsBuilder)

        val options: MParticleOptions = optionsBuilder.build()
        val kitOptions = options.getConfiguration(KitOptions::class.java) ?: KitOptions()
        val configuredKitOptions =
            options.getConfiguration(ConfiguredKitOptions::class.java) ?: ConfiguredKitOptions()

        val kitConfigurations: MutableMap<Int, JSONObject?> =
            mutableMapOf()
        (kitOptions.kits + configuredKitOptions.kits).forEach { id, clazz ->
            kitConfigurations[id] = JSONObject().put("id", id)
        }
        configuredKitOptions.configurations.forEach { id, config ->
            kitConfigurations[id] = config
        }
        kitConfigurations.values
            .fold(JSONArray()) { init, next -> init.apply { put(next) } }
            .let {
                JSONObject().put("eks", it)
            }
            .let {
                mServer.setupConfigResponse(it.toString())
            }
        super.startMParticle(optionsBuilder)
    }
}
