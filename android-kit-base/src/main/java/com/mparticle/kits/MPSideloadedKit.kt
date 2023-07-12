package com.mparticle.kits

import com.mparticle.internal.SideloadedKit
import org.json.JSONObject

abstract class MPSideloadedKit : KitIntegration(), SideloadedKit {

    init {
        configuration = KitConfiguration.createKitConfiguration(JSONObject())
    }

    fun addFilters(filter: MPSideloadedFilters): MPSideloadedKit {
        configuration = configuration.applyFilters(filter)
        return this
    }

    override fun getJsonConfig(): JSONObject = super.getJsonConfig()

    private fun KitConfiguration.applyFilters(filters: MPSideloadedFilters): KitConfiguration {
        return configuration.applySideloadedKits(filters)
    }
}
