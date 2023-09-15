package com.mparticle.kits.testkits

import com.mparticle.MPEvent
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage

class EventTestKit : ListenerTestKit(), KitIntegration.EventListener {
    var onLogEvent: (MPEvent) -> MutableList<ReportingMessage>? = { null }

    override fun logEvent(baseEvent: MPEvent): MutableList<ReportingMessage>? {
        return onLogEvent(baseEvent)
    }

    override fun leaveBreadcrumb(breadcrumb: String?): MutableList<ReportingMessage> {
        TODO("Not yet implemented")
    }

    override fun logError(
        message: String?,
        errorAttributes: MutableMap<String, String>?
    ): MutableList<ReportingMessage> {
        TODO("Not yet implemented")
    }

    override fun logException(
        exception: Exception?,
        exceptionAttributes: MutableMap<String, String>?,
        message: String?
    ): MutableList<ReportingMessage> {
        TODO("Not yet implemented")
    }

    override fun logScreen(
        screenName: String?,
        screenAttributes: MutableMap<String, String>?
    ): MutableList<ReportingMessage> {
        TODO("Not yet implemented")
    }
}
