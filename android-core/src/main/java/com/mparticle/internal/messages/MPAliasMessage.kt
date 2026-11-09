package com.mparticle.internal.messages

import com.mparticle.MParticle
import com.mparticle.identity.AliasRequest
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants.MessageKey.Companion.ALIAS_REQUEST_TYPE
import com.mparticle.internal.Constants.MessageKey.Companion.API_KEY
import com.mparticle.internal.Constants.MessageKey.Companion.DATA
import com.mparticle.internal.Constants.MessageKey.Companion.DESTINATION_MPID
import com.mparticle.internal.Constants.MessageKey.Companion.DEVICE_APPLICATION_STAMP_ALIAS
import com.mparticle.internal.Constants.MessageKey.Companion.END_TIME
import com.mparticle.internal.Constants.MessageKey.Companion.ENVIRONMENT_ALIAS
import com.mparticle.internal.Constants.MessageKey.Companion.REQUEST_ID
import com.mparticle.internal.Constants.MessageKey.Companion.REQUEST_TYPE
import com.mparticle.internal.Constants.MessageKey.Companion.SOURCE_MPID
import com.mparticle.internal.Constants.MessageKey.Companion.START_TIME
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

open class MPAliasMessage : JSONObject {
    @Throws(JSONException::class)
    constructor(jsonString: String?) : super(jsonString)

    @Throws(JSONException::class)
    constructor(request: AliasRequest?, deviceApplicationStamp: String?, apiKey: String?) : super() {
        val environment = getStringValue(ConfigManager.getEnvironment())
        val requestId = UUID.randomUUID().toString()

        val dataJson =
            JSONObject()
                .put(SOURCE_MPID, request!!.sourceMpid)
                .put(DESTINATION_MPID, request.destinationMpid)
                .put(DEVICE_APPLICATION_STAMP_ALIAS, deviceApplicationStamp)

        if (request.startTime != 0L) {
            dataJson.put(START_TIME, request.startTime)
        }
        if (request.endTime != 0L) {
            dataJson.put(END_TIME, request.endTime)
        }

        put(DATA, dataJson)
        put(REQUEST_TYPE, ALIAS_REQUEST_TYPE)
        put(REQUEST_ID, requestId)
        put(ENVIRONMENT_ALIAS, environment)
        put(API_KEY, apiKey)
    }

    open val aliasRequest: AliasRequest
        @Throws(JSONException::class)
        get() {
            val data = getJSONObject(DATA)
            return AliasRequest
                .builder()
                .destinationMpid(data.getLong(DESTINATION_MPID))
                .sourceMpid(data.getLong(SOURCE_MPID))
                .endTime(data.optLong(END_TIME, 0))
                .startTime(data.optLong(START_TIME, 0))
                .build()
        }

    open val requestId: String
        @Throws(JSONException::class)
        get() = getString(REQUEST_ID)

    protected open fun getStringValue(environment: MParticle.Environment?): String {
        when (environment!!) {
            MParticle.Environment.Development -> return "development"
            MParticle.Environment.Production -> return "production"
            else -> return ""
        }
    }
}
