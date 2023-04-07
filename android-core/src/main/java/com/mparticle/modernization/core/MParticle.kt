package com.mparticle.modernization.core

import com.mparticle.MParticleOptions
import com.mparticle.modernization.eventlogging.MParticleEventLogging
import com.mparticle.modernization.identity.MParticleIdentity
import com.mparticle.modernization.kit.MParticleKitManager
import com.mparticle.modernization.uploading.MParticleDataUploader

internal class MParticle private constructor(private val options: MParticleOptions) {
    private var mediator: MParticleMediator = MParticleMediator()

    init {
        mediator.configure(options)
    }

    companion object {
        private var _instance: MParticle? = null

        @Throws(Exception::class)
        fun getInstance(): MParticle = _instance ?: throw Exception("MParticle must be started before getting the instance")

        fun start(options: MParticleOptions) {
            _instance = MParticle(options)
        }
    }

    fun KitManager(): MParticleKitManager? = mediator.kitManager as MParticleKitManager?
    fun Identity(): MParticleIdentity? = mediator.identity as MParticleIdentity?
    fun EventLogging(): MParticleEventLogging? = mediator.eventLogging
    fun DataUploading(): MParticleDataUploader? = mediator.dataUploader
}
