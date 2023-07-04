package com.mparticle.kits

import org.json.JSONObject

abstract class MPSideloadedKit : KitIntegration() {

    init {
        configuration = KitConfiguration.createKitConfiguration(JSONObject())
    }

    fun addFilters(filters: MPSideloadedFilters): MPSideloadedKit {
        configuration = configuration.applyFilters(filters)
        return this
    }

    private fun KitConfiguration.applyFilters(filters: MPSideloadedFilters): KitConfiguration {
        return this
    }
}
