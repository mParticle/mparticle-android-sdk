package com.mparticle.kits.testkits

import android.content.Context
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage

open class BaseTestKit : KitIntegration() {

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context
    ): List<ReportingMessage> = listOf()

    override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        // do nothing
        return listOf()
    }

    override val name: String
        get() = this::class.java.simpleName

    override val instance: Any?
        get() = this
}
