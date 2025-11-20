package com.mparticle.modernization.kit

import com.mparticle.BaseEvent
import com.mparticle.modernization.eventlogging.MParticleEventLogging
import java.math.BigDecimal

internal abstract class MParticleKit : MParticleEventLogging {

    override fun logEvent(event: BaseEvent) {
    }

    override fun leaveBreadcrumb(breadcrumb: String) {
    }

    override fun logError(message: String, params: Map<String, String>?) {
    }

    override fun logException(
        exception: Exception,
        message: String?,
        params: Map<String, String>?
    ) {
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        eventName: String?,
        params: Map<String, String>?
    ) {
    }
}

internal abstract class MParticleKitInternal : MParticleKit()
