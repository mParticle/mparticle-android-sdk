package com.mparticle.modernization

import com.mparticle.MParticleOptions
import com.mparticle.internal.KitManager
import com.mparticle.modernization.event_logging.MParticleEventLogging

class MParticle private constructor(private val options: MParticleOptions) {
    private var mediator: MParticleMediator = MParticleMediator()

    init {
        mediator.configure(options)
    }

    companion object {
        private var _instance: MParticle? = null
        fun getInstance(): MParticle? = _instance

        fun start(options: MParticleOptions) {
            _instance = MParticle(options)
        }
    }

    fun KitManager(): KitManager? = mediator.kitManager as KitManager?
    fun Identity(): MParticleIdentity? = mediator.identity as MParticleIdentity?
    fun EventLogging(): MParticleEventLogging? = mediator.eventLogging
}