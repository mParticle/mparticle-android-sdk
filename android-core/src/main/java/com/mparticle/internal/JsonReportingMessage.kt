package com.mparticle.internal

import org.json.JSONObject

interface JsonReportingMessage {
    fun setDevMode(development: Boolean)

    fun getTimestamp(): Long

    fun getModuleId(): Int

    fun toJson(): JSONObject

    fun getSessionId(): String

    fun setSessionId(sessionId: String)
}
