package com.mparticle.kits.testkits

import com.mparticle.MPEvent
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage

class EventTestKit : ListenerTestKit(), KitIntegration.EventListener {
    var onLogEvent: (MPEvent) -> List<ReportingMessage> = { emptyList() }

    override fun logEvent(baseEvent: MPEvent): List<ReportingMessage> {
        return onLogEvent(baseEvent)
    }
    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> {
        TODO("Not yet implemented")
    }

    override fun logError(
        message: String,
        errorAttributes: Map<String, String?>?
    ): List<ReportingMessage> {
        TODO("Not yet implemented")
    }

    override fun logException(
        exception: Exception,
        exceptionAttributes: Map<String, String?>?,
        message: String?
    ): List<ReportingMessage> {
        TODO("Not yet implemented")
    }

    override fun logScreen(
        screenName: String,
        screenAttributes: Map<String, String?>?
    ): List<ReportingMessage> {
        TODO("Not yet implemented")
    }
}
