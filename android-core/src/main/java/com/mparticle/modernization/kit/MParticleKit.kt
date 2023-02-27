package com.mparticle.modernization.kit

import com.mparticle.BaseEvent
import java.math.BigDecimal

interface MParticleKit {
    fun logEvent(event: BaseEvent)
    fun leaveBreadcrumb(breadcrumb: String)
    fun logError(message: String, params: Map<String, String>? = null)
    fun logLtvIncrease(valueIncreased: BigDecimal, eventName: String? = null, params: Map<String, String>? = null)
    fun logException(exception: Exception, message: String? = null, params: Map<String, String>? = null)
}

interface MParticleKitInternal : MParticleKit