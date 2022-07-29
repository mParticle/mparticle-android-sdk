package com.mparticle.kits

import com.mparticle.messages.KitConfigMessage

class ConfiguredKitOptions : KitOptions() {
    val initialConfig = mutableMapOf<Int, KitConfigMessage>()

    fun addKit(type: Class<out KitIntegration>, kitId: Int): ConfiguredKitOptions {
        return addKit(type, KitConfigMessage(id = kitId))
    }

    override fun addKit(kitId: Int, type: Class<out KitIntegration>): KitOptions {
        return addKit(type, kitId)
    }

    fun addKit(type: Class<out KitIntegration>, kitId: Int, includeInConfig: Boolean): ConfiguredKitOptions {
        return addKit(type, KitConfigMessage(id = kitId), includeInConfig)
    }

    fun addKit(type: Class<out KitIntegration>, config: KitConfigMessage, includeInConfig: Boolean = true): ConfiguredKitOptions {
        if (includeInConfig) {
            initialConfig[config.id] = config
        }
        super.addKit(config.id, type)
        return this
    }
}

fun ConfiguredKitOptions(configuredKitOptions: ConfiguredKitOptions.() -> Unit): ConfiguredKitOptions {
    return ConfiguredKitOptions().apply(configuredKitOptions)
}
