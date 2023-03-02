package com.mparticle.modernization.core

import com.mparticle.MParticleOptions
import com.mparticle.modernization.eventlogging.MParticleEventLogging
import com.mparticle.modernization.identity.MParticleIdentity
import com.mparticle.modernization.kit.MParticleKitManager
import com.mparticle.modernization.uploading.MParticleDataUploader

class MParticle private constructor(private val options: MParticleOptions) {
    private var mediator: MParticleMediator = MParticleMediator()

    init {
        mediator.configure(options)
    }

    companion object {
        private var _instance: MParticle? = null

        @Throws(MParticleInitializationException::class)
        fun getInstance(): MParticle = _instance ?: throw MParticleInitializationException()

        fun start(options: MParticleOptions) {
            _instance = MParticle(options)
        }
    }

    fun KitManager(): MParticleKitManager? = mediator.kitManager as MParticleKitManager?
    fun Identity(): MParticleIdentity? = mediator.identity as MParticleIdentity?
    fun EventLogging(): MParticleEventLogging? = mediator.eventLogging
    fun DataUploading(): MParticleDataUploader? = mediator.dataUploader
}
