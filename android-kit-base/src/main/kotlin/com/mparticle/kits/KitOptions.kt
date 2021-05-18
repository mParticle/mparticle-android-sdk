package com.mparticle.kits

import android.app.Activity
import android.content.Context
import com.mparticle.Configuration
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.BaseIdentityTask
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.identity.MParticleUser
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

class KitOptions(initializer: KitOptions.() -> Unit = {}): Configuration<KitManagerImpl> {
    val kits: MutableMap<Int, Class<out KitIntegration>> = mutableMapOf()

    init {
        this.initializer()
    }

    fun addKit(kitId: Int, type: Class<out KitIntegration>): KitOptions {
        kits[kitId] = type
        return this;
    }

    override fun configures(): Class<KitManagerImpl> {
        return KitManagerImpl::class.java
    }

    override fun apply(kitManager: KitManagerImpl) {
        kitManager.setKitOptions(this);
    }
}