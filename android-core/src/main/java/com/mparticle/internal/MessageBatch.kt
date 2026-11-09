package com.mparticle.internal

import com.mparticle.BuildConfig
import com.mparticle.MParticle
import com.mparticle.consent.ConsentInstance
import com.mparticle.consent.ConsentState
import com.mparticle.internal.Constants.MessageKey.Companion.DATA_PLAN_CONTEXT
import com.mparticle.internal.Constants.MessageKey.Companion.DATA_PLAN_ID
import com.mparticle.internal.Constants.MessageKey.Companion.DATA_PLAN_KEY
import com.mparticle.internal.Constants.MessageKey.Companion.DATA_PLAN_VERSION
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

open class MessageBatch protected constructor() : JSONObject() {
    private var mMessageLengthBytes: Long = 0

    open fun addConsentState(consentState: ConsentState?) {
        if (consentState != null) {
            try {
                val state = JSONObject()
                this.put(Constants.MessageKey.CONSENT_STATE, state)

                val gdprState = consentState.getGDPRConsentState()
                if (gdprState != null) {
                    val gdpr = JSONObject()
                    state.put(Constants.MessageKey.CONSENT_STATE_GDPR, gdpr)
                    for (entry in gdprState.entries) {
                        val consent: ConsentInstance? = entry.value
                        if (consent != null) {
                            addConsentStateJSON(gdpr, entry.key, entry.value)
                        }
                    }
                }
                val ccpaConsent = consentState.getCCPAConsentState()
                if (ccpaConsent != null) {
                    val ccpa = JSONObject()
                    state.put(Constants.MessageKey.CONSENT_STATE_CCPA, ccpa)
                    addConsentStateJSON(ccpa, Constants.MessageKey.CCPA_CONSENT_KEY, ccpaConsent)
                }
            } catch (ignored: JSONException) {
            }
        }
    }

    @Throws(JSONException::class)
    open fun addDataplanContext(dataplanId: String?, dataplanVersion: Int?) {
        if (dataplanId != null) {
            val dataplan = JSONObject()
            dataplan.put(DATA_PLAN_ID, dataplanId)
            if (dataplanVersion != null) {
                dataplan.put(DATA_PLAN_VERSION, dataplanVersion)
            }

            put(DATA_PLAN_CONTEXT, JSONObject().put(DATA_PLAN_KEY, dataplan))
        }
    }

    open fun addMessage(message: JSONObject?) {
        try {
            if (!has(Constants.MessageKey.MESSAGES)) {
                put(Constants.MessageKey.MESSAGES, JSONArray())
            }
            getJSONArray(Constants.MessageKey.MESSAGES).put(message)
        } catch (ignored: JSONException) {
        }
    }

    open fun addReportingMessage(reportingMessage: JSONObject?) {
        try {
            if (!has(Constants.MessageKey.REPORTING)) {
                put(Constants.MessageKey.REPORTING, JSONArray())
            }
            getJSONArray(Constants.MessageKey.REPORTING).put(reportingMessage)
        } catch (ignored: JSONException) {
        }
    }

    open fun setAppInfo(appInfo: JSONObject?) {
        try {
            put(Constants.MessageKey.APP_INFO, appInfo)
        } catch (ignored: JSONException) {
        }
    }

    open fun setDeviceInfo(deviceInfo: JSONObject?) {
        try {
            put(Constants.MessageKey.DEVICE_INFO, deviceInfo)
        } catch (ignored: JSONException) {
        }
    }

    open val appInfo: JSONObject?
        get() {
            try {
                return getJSONObject(Constants.MessageKey.APP_INFO)
            } catch (e: JSONException) {
                return null
            }
        }

    open val deviceInfo: JSONObject?
        get() {
            try {
                return getJSONObject(Constants.MessageKey.DEVICE_INFO)
            } catch (e: JSONException) {
                return null
            }
        }

    open val messages: JSONArray?
        get() {
            try {
                return getJSONArray(Constants.MessageKey.MESSAGES)
            } catch (e: JSONException) {
                return null
            }
        }

    open fun setIdentities(identities: JSONArray?) {
        try {
            put(Constants.MessageKey.USER_IDENTITIES, identities)
        } catch (ignored: JSONException) {
        }
    }

    open fun setUserAttributes(userAttributes: JSONObject?) {
        try {
            put(Constants.MessageKey.USER_ATTRIBUTES, userAttributes)
        } catch (ignored: JSONException) {
        }
    }

    open val messageLengthBytes: Long
        get() = mMessageLengthBytes

    open fun incrementMessageLengthBytes(bytes: Long) {
        mMessageLengthBytes = mMessageLengthBytes + bytes
    }

    @Throws(JSONException::class)
    private fun addConsentStateJSON(parentJSON: JSONObject, key: String?, consentInstance: ConsentInstance) {
        val consentInstanceJSON = JSONObject()
        parentJSON.put(key, consentInstanceJSON)
        consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_CONSENTED, consentInstance.isConsented)
        if (consentInstance.document != null) {
            consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_DOCUMENT, consentInstance.document)
        }
        // Typed Any? so that the Object overload stores the boxed Long, and a null removes the key.
        val timestamp: Any? = consentInstance.timestamp
        consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_TIMESTAMP, timestamp)
        if (consentInstance.location != null) {
            consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_LOCATION, consentInstance.location)
        }
        if (consentInstance.hardwareId != null) {
            consentInstanceJSON.put(Constants.MessageKey.CONSENT_STATE_HARDWARE_ID, consentInstance.hardwareId)
        }
    }

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun create(history: Boolean, configManager: ConfigManager?, cookies: JSONObject?, batchId: BatchId?): MessageBatch {
            val uploadMessage = MessageBatch()
            if (BuildConfig.MP_DEBUG) {
                uploadMessage.put(Constants.MessageKey.ECHO, true)
            }
            uploadMessage.put(Constants.MessageKey.TYPE, Constants.MessageType.REQUEST_HEADER)
            uploadMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString())
            uploadMessage.put(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis())
            uploadMessage.put(Constants.MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION)
            uploadMessage.put(Constants.MessageKey.OPT_OUT_HEADER, configManager!!.optedOut)
            uploadMessage.put(Constants.MessageKey.CONFIG_UPLOAD_INTERVAL, configManager.uploadInterval / 1000)
            uploadMessage.put(Constants.MessageKey.MPARTICLE_CONFIG_VERSION, configManager.etag)
            uploadMessage.put(Constants.MessageKey.CONFIG_SESSION_TIMEOUT, configManager.sessionTimeout / 1000)
            uploadMessage.put(Constants.MessageKey.MPID, batchId!!.mpid.toString())
            uploadMessage.put(Constants.MessageKey.SANDBOX, ConfigManager.getEnvironment() == MParticle.Environment.Development)
            uploadMessage.put(Constants.MessageKey.DEVICE_APPLICATION_STAMP, configManager.deviceApplicationStamp)

            if (history) {
                val deletedAttr = configManager.getUserStorage(batchId.mpid).deletedUserAttributes
                if (deletedAttr != null) {
                    uploadMessage.put(Constants.MessageKey.DELETED_USER_ATTRIBUTES, JSONArray(deletedAttr))
                    configManager.userStorage.deleteDeletedUserAttributes()
                }
            }

            uploadMessage.put(Constants.MessageKey.COOKIES, cookies)
            uploadMessage.put(Constants.MessageKey.PROVIDER_PERSISTENCE, configManager.providerPersistence)
            uploadMessage.put(Constants.MessageKey.INTEGRATION_ATTRIBUTES, configManager.integrationAttributes)
            uploadMessage.addConsentState(configManager.getEffectiveConsentState(batchId.mpid))
            uploadMessage.addDataplanContext(batchId.dataplanId, batchId.dataplanVersion)
            return uploadMessage
        }
    }
}
