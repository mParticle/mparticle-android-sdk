package com.mparticle.modernization.kit

import com.mparticle.BaseEvent
import com.mparticle.modernization.MParticleMediator
import java.math.BigDecimal

class XKit(private val mediator: MParticleMediator) : MParticleKitInternal{
    override fun logEvent(event: BaseEvent) {
        TODO("Not yet implemented")
    }

    override fun leaveBreadcrumb(breadcrumb: String) {
        TODO("Not yet implemented")
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