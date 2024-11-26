package com.mparticle.internal.messages

import com.mparticle.MParticle
import com.mparticle.identity.AliasRequest
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants.MessageKey
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

class MPAliasMessage : JSONObject {
    constructor(jsonString: String) : super(jsonString)

    @Throws(JSONException::class)
    constructor(request: AliasRequest, deviceApplicationStamp: String, apiKey: String) {
        val environment = getStringValue(ConfigManager.getEnvironment())
        val requestId = UUID.randomUUID().toString()

        val dataJson = JSONObject()
            .put(MessageKey.SOURCE_MPID, request.sourceMpid)
            .put(MessageKey.DESTINATION_MPID, request.destinationMpid)
            .put(MessageKey.DEVICE_APPLICATION_STAMP_ALIAS, deviceApplicationStamp)

        if (request.startTime != 0L) {
            dataJson.put(MessageKey.START_TIME, request.startTime)
        }
        if (request.endTime != 0L) {
            dataJson.put(MessageKey.END_TIME, request.endTime)
        }

        put(MessageKey.DATA, dataJson)
        put(MessageKey.REQUEST_TYPE, MessageKey.ALIAS_REQUEST_TYPE)
        put(MessageKey.REQUEST_ID, requestId)
        put(MessageKey.ENVIRONMENT_ALIAS, environment)
        put(MessageKey.API_KEY, apiKey)
    }

    @get:Throws(JSONException::class)
    val aliasRequest: AliasRequest
        get() {
            val data = getJSONObject(MessageKey.DATA)
            return AliasRequest.builder()
                .destinationMpid(data.getLong(MessageKey.DESTINATION_MPID))
                .sourceMpid(data.getLong(MessageKey.SOURCE_MPID))
                .endTime(data.optLong(MessageKey.END_TIME, 0))
                .startTime(data.optLong(MessageKey.START_TIME, 0))
                .build()
        }

    @get:Throws(JSONException::class)
    val requestId: String
        get() = getString(MessageKey.REQUEST_ID)


    protected fun getStringValue(environment: MParticle.Environment): String {
        return when (environment) {
            MParticle.Environment.Development -> "development"
            MParticle.Environment.Production -> "production"
            else -> ""
        }
    }
}