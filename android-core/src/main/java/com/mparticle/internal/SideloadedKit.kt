package com.mparticle.internal

import org.json.JSONObject

interface SideloadedKit {

    fun getJsonConfig(): JSONObject?

    fun kitId(): Int

    fun getName(): String
}
