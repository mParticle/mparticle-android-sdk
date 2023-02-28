package com.mparticle.modernization.event_logging

import com.mparticle.BaseEvent
import com.mparticle.MParticleOptions
import com.mparticle.internal.KitManager
import com.mparticle.modernization.MParticleMediator
import com.mparticle.modernization.identity.IdentityCallback
import com.mparticle.modernization.kit.MParticleKit
import java.math.BigDecimal

internal class MParticleFakeEventLoggingImpl(private val mediator: MParticleMediator ) : MParticleEventLogging {

    override fun logEvent(event: BaseEvent) {
        TODO("Not yet implemented")
    }

    override fun leaveBreadcrumb(breadcrumb: String) {
      mediator.kitManager?.leaveBreadcrumb(breadcrumb)
    }

    override fun logError(message: String, params: Map<String, String>?) {
        TODO("Not yet implemented")
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        eventName: String?,
        params: Map<String, String>?
    ) {
        TODO("Not yet implemented")
    }

    override fun logException(
        exception: Exception,
        message: String?,
        params: Map<String, String>?
    ) {
        TODO("Not yet implemented")
    }
}