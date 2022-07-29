package com.mparticle.kits

import com.mparticle.messages.KitConfigMessage

class ConfiguredKitOptions : KitOptions() {
    val initialConfig = mutableMapOf<Int, KitConfigMessage>()

    override fun addKit(type: Class<out KitIntegration>, kitId: Int): ConfiguredKitOptions {
        return addKit(type, KitConfigMessage(id = kitId))
    }

    fun addKit(type: Class<out KitIntegration>, kitId: Int, includeInConfig: Boolean): ConfiguredKitOptions {
        return addKit(type, KitConfigMessage(id = kitId), includeInConfig)
    }

    fun addKit(type: Class<out KitIntegration>, config: KitConfigMessage, includeInConfig: Boolean = true): ConfiguredKitOptions {
        if (includeInConfig) {
            initialConfig[config.id] = config
        }
        super.addKit(type, config.id)
        return this
    }
}

fun ConfiguredKitOptions(configuredKitOptions: ConfiguredKitOptions.() -> Unit): ConfiguredKitOptions {
    return ConfiguredKitOptions().apply(configuredKitOptions)
}
