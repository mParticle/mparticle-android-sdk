package com.mparticle.kits

import org.json.JSONObject

abstract class MPSideloadedKit : KitIntegration() {

    init {
        configuration = KitConfiguration.createKitConfiguration(JSONObject())
    }

    fun addFilters(filters: MPSideloadedFilters): MPSideloadedKit {
        return this
    }

}