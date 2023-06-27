package com.mparticle.internal

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.json.JSONObject

interface SideloadedKit {
//    @Nullable
//    fun getKitConfiguration() : JSONObject?

    fun setJSONConfiguration(configuration: JSONObject)
}