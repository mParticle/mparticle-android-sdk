package com.mparticle.modernization.kit

import com.mparticle.BaseEvent
import java.math.BigDecimal

abstract class MParticleKit {
    open fun logEvent(event: BaseEvent) {}
   open fun leaveBreadcrumb(breadcrumb: String) {}
    open fun logError(message: String, params: Map<String, String>? = null) {}
   open fun logLtvIncrease(
        valueIncreased: BigDecimal,
        eventName: String? = null,
        params: Map<String, String>? = null
    ) {
    }

    open fun logException(
        exception: Exception,
        message: String? = null,
        params: Map<String, String>? = null
    ) {
    }
}

 abstract class MParticleKitInternal : MParticleKit()