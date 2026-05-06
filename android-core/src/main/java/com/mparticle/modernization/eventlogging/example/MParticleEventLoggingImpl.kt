package com.mparticle.modernization.eventlogging.example

import com.mparticle.BaseEvent
import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.eventlogging.MParticleEventLogging
import com.mparticle.modernization.kit.MParticleKit

internal class MParticleEventLoggingImpl(private val mediator: MParticleMediator) :
    MParticleEventLogging {

    /**
     * The component is incharge of event logging itself, by delegation its responsability to the
     * different kits (by using the kitManager component from the mediator).
     * Because each kit migth have a different configuration to follow in order to "log an event",
     * specifically us - we filter, create and handle projectsion, apply rules etc, this class should
     * be only incharge of cross kit pre/post processing.
     */
    override fun logEvent(event: BaseEvent) {
        //pre-process stuff
        mediator.kitManager?.logEvent(event)
        //post-process stuff
    }

    override fun logError(message: String, params: Map<String, String>?, exception: Exception?) {
        TODO("Not yet implemented")
    }

}
