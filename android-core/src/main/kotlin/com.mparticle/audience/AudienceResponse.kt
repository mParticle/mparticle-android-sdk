package com.mparticle.audience

import com.mparticle.internal.Logger
import org.json.JSONObject

class AudienceResponse {
    private var code: Int = 0
    private var error: String? = null
    private var result: JSONObject? = null
    private var audienceList = ArrayList<Audience>()

    constructor(httpCode: Int, errorMsg: String) {
        code = httpCode
        error = errorMsg

    }

    constructor(httpCode: Int, jsonObject: JSONObject) {
        code = httpCode
        result = jsonObject
        parseJsonObject(jsonObject)
    }

    fun getAudienceResult(): ArrayList<Audience> {
        return audienceList
    }

    fun getError(): String? {
        return error
    }

    private fun parseJsonObject(jsonObject: JSONObject) {
        try {
            val audienceMemberships = jsonObject.getJSONArray("audience_memberships")
            for (i in 0 until audienceMemberships.length()) {
                val audience = audienceMemberships[i] as JSONObject
                val audienceID = audience["audience_id"]
                audienceList.add(Audience(audienceID.toString()))
            }
        } catch (e: Exception) {
            Logger.error("Exception while parsing audience response $e")
        }
    }
}