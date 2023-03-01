package com.mparticle.modernization.core

import com.mparticle.MParticleOptions
import com.mparticle.modernization.data_uploader.MParticleDataUploader
import com.mparticle.modernization.data_uploader.MParticleDataUploaderImpl
import com.mparticle.modernization.data_uploader.MParticleUploadingStrategy
import com.mparticle.modernization.event_logging.MParticleEventLogging
import com.mparticle.modernization.event_logging.example_impl.MParticleEventLoggingImpl
import com.mparticle.modernization.identity.InternalIdentity
import com.mparticle.modernization.identity.example_impl.MParticleIdentityImpl
import com.mparticle.modernization.kit.KitManagerInternal
import com.mparticle.modernization.kit.MParticleKit
import com.mparticle.modernization.kit.MParticleKitManagerImpl
import com.mparticle.modernization.kit.example_impl.MpKit
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class MParticleMediator {
    internal var eventLogging: MParticleEventLogging? = null
    internal var identity: InternalIdentity? = null
    internal var kitManager: KitManagerInternal? = null
    internal var dataUploader: MParticleDataUploader? = null

    private var kits: MutableList<MParticleKit> = mutableListOf()
    private var mParticleUploadingStrategies: List<MParticleUploadingStrategy> = listOf()

    internal lateinit var coroutineScope: CoroutineScope
    internal lateinit var coroutineDispatcher: CloseableCoroutineDispatcher

    fun configure(options: MParticleOptions) {
        coroutineScope = CoroutineScope(SupervisorJob())
        coroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
        kits = registerKits(options)
        registerComponent(MParticleKitManagerImpl(kits))
        registerComponent(MParticleIdentityImpl(this))
        registerComponent(MParticleEventLoggingImpl(this))
        registerComponent(MParticleDataUploaderImpl(this, mParticleUploadingStrategies, null))
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