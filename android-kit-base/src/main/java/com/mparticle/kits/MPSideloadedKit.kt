package com.mparticle.kits

import android.content.Context
import com.mparticle.internal.SideloadedKit
import org.json.JSONObject

abstract class MPSideloadedKit(val kitId: Int) : KitIntegration(), SideloadedKit {

    companion object {
        const val MIN_SIDELOADED_KIT = 1000000
    }

    init {
        configuration = KitConfiguration.createKitConfiguration(
            JSONObject().put(
                KitConfiguration.KEY_ID,
                kitId
            )
        )
    }

    override fun kitId(): Int = kitId

    override fun getName(): String = this::class.java.name.split(".").last().orEmpty()

    override fun onKitCreate(
        settings: MutableMap<String, String>?,
        context: Context?
    ): MutableList<ReportingMessage> = mutableListOf<ReportingMessage>()

    override fun setOptOut(optedOut: Boolean): MutableList<ReportingMessage> = mutableListOf()

    fun addFilters(filter: MPSideloadedFilters): MPSideloadedKit {
        configuration = configuration.applyFilters(filter)
        return this
    }

    override fun getJsonConfig(): JSONObject? = super.getJsonConfig()

    private fun KitConfiguration.applyFilters(filters: MPSideloadedFilters): KitConfiguration? {
        return configuration?.applySideloadedKits(filters)
    }
}
