package com.mparticle.kits.testkits

import com.mparticle.commerce.CommerceEvent
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage
import java.math.BigDecimal

class CommerceListenerTestKit : ListenerTestKit(), KitIntegration.CommerceListener {
    var logEvent: ((CommerceEvent?) -> List<ReportingMessage>)? = null
    var logLtvIncrease: ((BigDecimal?, BigDecimal?, String?, Map<String, String>?) -> List<ReportingMessage>)? =
        null

    override fun logEvent(event: CommerceEvent?): List<ReportingMessage> {
        return logEvent?.invoke(event) ?: listOf()
    }

    override fun logLtvIncrease(
        valueIncreased: BigDecimal?,
        valueTotal: BigDecimal?,
        eventName: String?,
        contextInfo: Map<String, String>?
    ): List<ReportingMessage> {
        return logLtvIncrease?.invoke(valueIncreased, valueTotal, eventName, contextInfo)
            ?: listOf()
    }
}
