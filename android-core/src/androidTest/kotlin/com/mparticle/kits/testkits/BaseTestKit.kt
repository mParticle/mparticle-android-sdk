package com.mparticle.kits.testkits

import android.content.Context
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage

open class BaseTestKit : KitIntegration() {
    open override fun onKitCreate(
        settings: Map<String, String>?,
        context: Context
    ): List<ReportingMessage> {
        return listOf()
    }

    open override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        // do nothing
        return listOf()
    }

    open override fun getName(): String {
        return this::class.java.simpleName
    }

    open override fun getInstance() = this
}
