package com.mparticle.modernization.kit.example

import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.kit.CommerceListener
import com.mparticle.modernization.kit.EventListener
import com.mparticle.modernization.kit.MParticleKitInternal
import com.mparticle.modernization.launch
import com.mparticle.modernization.datahandler.MParticleDataHandler
import com.mparticle.modernization.kit.KitConfiguration

/**
 * MParticle business related logic. The component should delegate to the kitManager, and the kit manager
 * delegate to each kit (being MParticle a specific use case).
 * While a third-party kit wrapper like OneTrust will operate against their SDK, this one will trigger
 * their internal managers and structure to manage data pre-processing,storing/retrieval,mapping,parsing,upload.
 */
internal class MpKit(
    private val mediator: MParticleMediator,
    private val dataHandler: MParticleDataHandler
) : MParticleKitInternal(),
    CommerceListener, EventListener {

    override fun commerceEventLogged(event: CommerceEvent) {
        //TODO Filter, handle projections and save event
        mediator.launch { dataHandler.saveData(event, true) }
    }

    override fun eventLogged(event: MPEvent) {
        mediator.launch { dataHandler.saveData(event) }
    }

    override fun errorLogged(message: String, params: Map<String, String>?, exception: Exception?) {
        TODO("Not yet implemented")
    }

    override fun getConfiguration(): KitConfiguration {
        TODO("Not yet implemented")
    }
}
