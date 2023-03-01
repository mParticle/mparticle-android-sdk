package com.mparticle.modernization.event_logging

import com.mparticle.BaseEvent
import com.mparticle.modernization.core.MParticleComponent
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.math.BigDecimal

interface MParticleEventLogging : MParticleComponent {
    //All common, commerce, screen, pushRegistration, notification, notificationOpened and NetworkPerformance events should be logged with the same function
    /**
     * Log an event - TODO review EventBuilder to be able to build all type of events
     * @param event to log
     */
    fun logEvent(@NotNull event: BaseEvent)

    /**
     * Leave breadcrumb
     * @param breadcrumb
     */
    fun leaveBreadcrumb(@NotNull breadcrumb: String)

    /**
     * Log an error
     * @param message
     * @param params optional by default null
     */
    fun logError(@NotNull message: String, @Nullable params: Map<String, String>? = null)

    /**
     * Log lifetime value increase
     * @param valueIncreased
     * @param eventName  optional by default null
     * @param params optional by default null
     */
    fun logLtvIncrease(
        @NotNull valueIncreased: BigDecimal,
        @Nullable eventName: String? = null,
        @Nullable params: Map<String, String>? = null
    )

    /**
     * Log exception
     * @param exception
     * @param message optional by default null
     * @param params optional by default null
     */
    fun logException(
        @NotNull exception: Exception,
        @Nullable message: String? = null,
        @Nullable params: Map<String, String>? = null
    )
}