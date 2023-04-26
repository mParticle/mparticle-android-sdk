package com.mparticle.modernization.eventlogging

import com.mparticle.BaseEvent
import com.mparticle.modernization.core.MParticleComponent
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.math.BigDecimal

internal interface MParticleEventLogging : MParticleComponent {
    // All common, commerce, screen, pushRegistration, ltvIncrease, breadcrumb, exception, notification, notificationOpened and NetworkPerformance events should be logged with the same function
    /**
     * Log an event - TODO review EventBuilder to be able to build all type of events
     * @param event to log
     */
    fun logEvent(@NotNull event: BaseEvent)


    /**
     * Log an error
     * @param message
     * @param params optional by default null
     * @param exception
     */
    fun logError(@NotNull message: String, @Nullable params: Map<String, String>? = null, @Nullable exception : Exception?)

}
