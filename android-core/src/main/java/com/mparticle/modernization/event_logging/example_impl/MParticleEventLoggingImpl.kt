package com.mparticle.modernization.event_logging.example_impl

import com.mparticle.BaseEvent
import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.event_logging.MParticleEventLogging
import java.math.BigDecimal

internal class MParticleEventLoggingImpl(private val mediator: MParticleMediator) :
    MParticleEventLogging {

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