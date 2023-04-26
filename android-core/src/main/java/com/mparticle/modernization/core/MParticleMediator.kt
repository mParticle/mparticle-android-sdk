package com.mparticle.modernization.core

import com.mparticle.MParticleOptions
import com.mparticle.modernization.BatchManager
import com.mparticle.modernization.MpApiClientImpl
import com.mparticle.modernization.data.MParticleDataRepository
import com.mparticle.modernization.eventlogging.MParticleEventLogging
import com.mparticle.modernization.eventlogging.example.MParticleEventLoggingImpl
import com.mparticle.modernization.identity.InternalIdentity
import com.mparticle.modernization.identity.example.MParticleIdentityImpl
import com.mparticle.modernization.kit.KitManagerInternal
import com.mparticle.modernization.kit.MParticleKit
import com.mparticle.modernization.kit.MParticleKitManagerImpl
import com.mparticle.modernization.kit.example.MpKit
import com.mparticle.modernization.datahandler.MParticleDataStrategyManagerImpl
import com.mparticle.modernization.datahandler.MParticleDataHandlerStrategy
import com.mparticle.modernization.datahandler.example.MParticleCommerceHandler
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

internal class MParticleMediator(private val dataRepository: MParticleDataRepository) {
    internal var eventLogging: MParticleEventLogging? = null
    internal var identity: InternalIdentity? = null
    internal var kitManager: KitManagerInternal? = null
    internal var batchManager : BatchManager = BatchManager(MpApiClientImpl())

    private var mParticleUploadingStrategies: List<MParticleDataHandlerStrategy<*,*>> = listOf(
        MParticleCommerceHandler(dataRepository, batchManager)
    )

    internal lateinit var coroutineScope: CoroutineScope
    internal lateinit var coroutineDispatcher: CloseableCoroutineDispatcher

    /**
     * Mediator register kits and components, acting as a "common layer" for the components internally,
     * and also providing a single instance of the available and visible components to the MParticle
     * facade. This will help also controlling which this we want to make accesible and which one we doesn't.
     */
    fun configure(options: MParticleOptions) {
        /**
         * Creation of auto-cancellable thread-pool using coroutines.
         * Using a utility, and due to the fact that the relationship between mediator-mPaticle instance
         * is 1:1, we would be able to launch async operation managed by the thread pool of each mparticle
         * instance
         */
        coroutineScope = CoroutineScope(SupervisorJob())
        coroutineDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

        var kits: MutableList<MParticleKit> = registerKits(options)
        /**
         * Mediator has a reference to each component. The components mostly are instanciated with a
         * dependency to the mediator, meaning that, if component A (running action 1) needs an action 3 from component B
         * to be executed to complete action 1; component A can use the mediator to access component B
         */
        registerComponent(MParticleKitManagerImpl(kits))
        registerComponent(MParticleIdentityImpl(this))
        registerComponent(MParticleEventLoggingImpl(this))
    }

    private fun registerKits(options: MParticleOptions): MutableList<MParticleKit> =
        mutableListOf(MpKit(this, MParticleDataStrategyManagerImpl(mParticleUploadingStrategies)))

    private fun registerComponent(component: MParticleComponent) {
        when (component) {
            is InternalIdentity -> identity = component
            is KitManagerInternal -> kitManager = component
            is MParticleEventLogging -> eventLogging = component
        }
    }
}
