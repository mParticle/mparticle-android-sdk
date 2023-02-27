package com.mparticle.modernization

import com.mparticle.MParticleOptions
import com.mparticle.modernization.event_logging.MParticleEventLogging
import com.mparticle.modernization.event_logging.MParticleFakeEventLoggingImpl
import com.mparticle.modernization.identity.MParticleFakeIdentityImpl
import com.mparticle.modernization.kit.KitManagerInternal
import com.mparticle.modernization.kit.MParticleKit
import com.mparticle.modernization.kit.MParticleKitManagerImpl
import com.mparticle.modernization.kit.MpKit

class MParticleMediator {
    var eventLogging: MParticleEventLogging? = null
    internal var identity: InternalIdentity? = null
    var kitManager: KitManagerInternal? = null

    private var kits: MutableList<MParticleKit> = mutableListOf()

    fun configure(options: MParticleOptions) {
        kits = registerKits(options)
        registerComponent(MParticleKitManagerImpl(kits))
        registerComponent(MParticleFakeIdentityImpl(this))
        registerComponent(MParticleFakeEventLoggingImpl(this))
    }

    private fun registerKits(options: MParticleOptions): MutableList<MParticleKit> =
        mutableListOf(MpKit(this))

    private fun registerComponent(component: MParticleComponent) {
        when (component) {
            is InternalIdentity -> identity = component
            is KitManagerInternal -> kitManager = component
            is MParticleEventLogging -> eventLogging = component
        }
    }
}