package com.mparticle.internal.database

import com.mparticle.internal.Logger
import com.mparticle.networking.NetworkOptions
import org.json.JSONException
import org.json.JSONObject

open class UploadSettings(apiKey: String, secret: String, networkOptions: NetworkOptions, activeKits: String, supportedKits: String) {
    // toJson() reads these fields rather than the open getters, so a subclass that overrides a getter
    // does not change the stored JSON.
    private val mApiKey: String = apiKey
    private val mSecret: String = secret
    private val mNetworkOptions: NetworkOptions = networkOptions
    private val mActiveKits: String = activeKits
    private val mSupportedKits: String = supportedKits

    open val apiKey: String
        get() = mApiKey

    open val secret: String
        get() = mSecret

    open val networkOptions: NetworkOptions
        get() = mNetworkOptions

    open val activeKits: String
        get() = mActiveKits

    open val supportedKits: String
        get() = mSupportedKits

    open fun toJson(): String? {
        val uploadSettingsJson = JSONObject()
        try {
            uploadSettingsJson.put("apiKey", mApiKey)
            uploadSettingsJson.put("secret", mSecret)
            uploadSettingsJson.put("networkOptions", mNetworkOptions.toJson())
            uploadSettingsJson.put("activeKits", mActiveKits)
            uploadSettingsJson.put("supportedKits", mSupportedKits)
        } catch (jse: JSONException) {
            Logger.error(jse)
        }
        return uploadSettingsJson.toString()
    }

    companion object {
        @JvmStatic
        fun withJson(jsonString: String?): UploadSettings? {
            try {
                val jsonObject = JSONObject(jsonString!!)
                val apiKey = jsonObject.getString("apiKey")
                val secret = jsonObject.getString("secret")
                val networkOptionsJsonObject = jsonObject.getJSONObject("networkOptions")
                val networkOptions = NetworkOptions.withNetworkOptions(networkOptionsJsonObject.toString())
                val activeKits = jsonObject.getString("activeKits")
                val supportedKits = jsonObject.getString("supportedKits")
                if (networkOptions != null) {
                    return UploadSettings(apiKey, secret, networkOptions, activeKits, supportedKits)
                }
            } catch (jse: JSONException) {
                Logger.error(jse)
            }
            return null
        }
    }
}
