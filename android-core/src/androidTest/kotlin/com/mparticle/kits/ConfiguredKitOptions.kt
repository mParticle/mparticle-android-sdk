package com.mparticle.kits

import org.json.JSONObject

class ConfiguredKitOptions : KitOptions() {
    val testingConfiguration = mutableMapOf<Int, JSONObject?>()

    override fun addKit(kitId: Int, type: Class<out KitIntegration>): ConfiguredKitOptions {
        return addKit(kitId, type, JSONObject().put("id", kitId))
    }

    fun addKit(
        kitId: Int,
        type: Class<out KitIntegration>,
        config: JSONObject?
    ): ConfiguredKitOptions {
        testingConfiguration[kitId] = config?.put("id", kitId)
        super.addKit(kitId, type)
        return this
    }
}

fun ConfiguredKitOptions(configuredKitOptions: ConfiguredKitOptions.() -> Unit): ConfiguredKitOptions {
    return ConfiguredKitOptions().apply(configuredKitOptions)
}
