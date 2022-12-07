package com.mparticle.kits.testkits

import android.content.Context
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.ReportingMessage

open class BaseTestKit : KitIntegration() {

    override fun onKitCreate(
        settings: Map<String, String>,
        context: Context
    ): List<ReportingMessage> = listOf()

    open override fun setOptOut(optedOut: Boolean): List<ReportingMessage> {
        // do nothing
        return listOf()
    }

    open override fun getName(): String {
        return this::class.java.simpleName
    }

    override fun <T> getInstance(): T? {
        return super.getInstance() as T?
    }
}
