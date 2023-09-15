package com.mparticle.kits

import com.mparticle.Configuration

open class KitOptions(initializer: KitOptions.() -> Unit = {}) : Configuration<KitManagerImpl> {
    val kits: MutableMap<Int, Class<out KitIntegration>> = mutableMapOf()

    init {
        this.initializer()
    }

    open fun addKit(kitId: Int, type: Class<out KitIntegration>): KitOptions {
        kits[kitId] = type
        return this
    }

    override fun configures(): Class<KitManagerImpl> {
        return KitManagerImpl::class.java
    }

    override fun apply(kitManager: KitManagerImpl) {
        kitManager.setKitOptions(this)
    }
}
