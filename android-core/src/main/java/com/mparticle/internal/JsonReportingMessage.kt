package com.mparticle.internal

import org.json.JSONObject

interface JsonReportingMessage {
    fun setDevMode(development: Boolean)

    val timestamp: Long

    val moduleId: Int

    fun toJson(): JSONObject

    var sessionId: String
}
