package com.mparticle.modernization.event_logging

import com.mparticle.BaseEvent
import com.mparticle.modernization.MParticleComponent
import java.math.BigDecimal

interface MParticleEventLogging : MParticleComponent {
    //All common, commerce, screen, pushRegistration, notification, notificationOpened and NetworkPerformance events should be logged with the same function
    fun logEvent(event: BaseEvent)
    fun leaveBreadcrumb(breadcrumb: String)
    fun logError(message: String, params: Map<String, String>? = null)
    fun logLtvIncrease(valueIncreased: BigDecimal, eventName: String? = null, params: Map<String, String>? = null)
    fun logException(exception: Exception, message: String? = null, params: Map<String, String>? = null)
}