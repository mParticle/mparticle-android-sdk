package com.mparticle.kits

import org.json.JSONObject

class ConfiguredKitOptions : KitOptions() {
    val configurations = mutableMapOf<Int, JSONObject>()

    override fun addKit(kitId: Int, type: Class<out KitIntegration>): ConfiguredKitOptions {
        return addKit(kitId, type, JSONObject().put("id", kitId))
    }

    fun addKit(kitId: Int, type: Class<out KitIntegration>, config: JSONObject?): ConfiguredKitOptions {
        configurations[kitId] = config?.put("id", kitId) ?: JSONObject()
        super.addKit(kitId, type)
        return this
    }
}

fun ConfiguredKitOptions(configuredKitOptions: ConfiguredKitOptions.() -> Unit): ConfiguredKitOptions {
    return ConfiguredKitOptions().apply(configuredKitOptions)
}
