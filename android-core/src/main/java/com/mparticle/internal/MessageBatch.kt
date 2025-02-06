package com.mparticle.internal

import com.mparticle.BuildConfig
import com.mparticle.MParticle
import com.mparticle.consent.ConsentInstance
import com.mparticle.consent.ConsentState
import com.mparticle.internal.Constants.MessageKey
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

open class MessageBatch protected constructor() : JSONObject() {
    var messageLengthBytes: Long = 0
        private set

    fun addConsentState(consentState: ConsentState?) {
        consentState?.let {
            try {
                val state = JSONObject()
                this.put(MessageKey.CONSENT_STATE, state)

                val gdprState = consentState.gdprConsentState
                gdprState?.let {
                    val gdpr = JSONObject()
                    state.put(MessageKey.CONSENT_STATE_GDPR, gdpr)
                    for ((key, consent) in gdprState) {
                        consent?.let {
                            addConsentStateJSON(gdpr, key, consent)
                        }
                    }
                }
                val ccpaConsent = consentState.ccpaConsentState
                ccpaConsent ?.let {
                    val ccpa = JSONObject()
                    state.put(MessageKey.CONSENT_STATE_CCPA, ccpa)
                    addConsentStateJSON(ccpa, MessageKey.CCPA_CONSENT_KEY, ccpaConsent)
                }
            } catch (ignored: JSONException) {
            }
        }
    }

    @Throws(JSONException::class)
    fun addDataplanContext(dataplanId: String?, dataplanVersion: Int?) {
        dataplanId?.let {
            val dataplan = JSONObject()
            dataplan.put(MessageKey.DATA_PLAN_ID, dataplanId)
            dataplanVersion?.let {
                dataplan.put(MessageKey.DATA_PLAN_VERSION, dataplanVersion)
            }

            put(MessageKey.DATA_PLAN_CONTEXT, JSONObject().put(MessageKey.DATA_PLAN_KEY, dataplan))
        }
    }

    fun addMessage(message: JSONObject?) {
        try {
            if (!has(MessageKey.MESSAGES)) {
                put(MessageKey.MESSAGES, JSONArray())
            }
            getJSONArray(MessageKey.MESSAGES).put(message)
        } catch (ignored: JSONException) {
        }
    }

    fun addReportingMessage(reportingMessage: JSONObject?) {
        try {
            if (!has(MessageKey.REPORTING)) {
                put(MessageKey.REPORTING, JSONArray())
            }
            getJSONArray(MessageKey.REPORTING).put(reportingMessage)
        } catch (ignored: JSONException) {
        }
    }

    var appInfo: JSONObject?
        get() = try {
            getJSONObject(MessageKey.APP_INFO)
        } catch (e: JSONException) {
            null
        }
        set(appInfo) {
            try {
                put(MessageKey.APP_INFO, appInfo)
            } catch (ignored: JSONException) {
            }
        }

    var deviceInfo: JSONObject?
        get() {
            return try {
                getJSONObject(MessageKey.DEVICE_INFO)
            } catch (e: JSONException) {
                null
            }
        }
        set(deviceInfo) {
            try {
                put(MessageKey.DEVICE_INFO, deviceInfo)
            } catch (ignored: JSONException) {
            }
        }

    val messages: JSONArray?
        get() {
            return try {
                getJSONArray(MessageKey.MESSAGES)
            } catch (e: JSONException) {
                null
            }
        }

    fun setIdentities(identities: JSONArray?) {
        try {
            put(MessageKey.USER_IDENTITIES, identities)
        } catch (ignored: JSONException) {
        }
    }

    fun setUserAttributes(userAttributes: JSONObject?) {
        try {
            put(MessageKey.USER_ATTRIBUTES, userAttributes)
        } catch (ignored: JSONException) {
        }
    }

    fun incrementMessageLengthBytes(bytes: Long) {
        messageLengthBytes = messageLengthBytes + bytes
    }

    @Throws(JSONException::class)
    private fun addConsentStateJSON(parentJSON: JSONObject, key: String, consentInstance: ConsentInstance) {
        val consentInstanceJSON = JSONObject()
        parentJSON.put(key, consentInstanceJSON)
        consentInstanceJSON.put(MessageKey.CONSENT_STATE_CONSENTED, consentInstance.isConsented)
        consentInstance.document?.let {
            consentInstanceJSON.put(MessageKey.CONSENT_STATE_DOCUMENT, consentInstance.document)
        }
        consentInstanceJSON.put(MessageKey.CONSENT_STATE_TIMESTAMP, consentInstance.timestamp)
        consentInstance.location ?.let {
            consentInstanceJSON.put(MessageKey.CONSENT_STATE_LOCATION, consentInstance.location)
        }
        consentInstance.hardwareId?.let {
            consentInstanceJSON.put(MessageKey.CONSENT_STATE_HARDWARE_ID, consentInstance.hardwareId)
        }
    }

    companion object {
        @JvmStatic
        @Throws(JSONException::class)
        fun create(history: Boolean, configManager: ConfigManager, cookies: JSONObject?, batchId: BatchId): MessageBatch {
            val uploadMessage = MessageBatch()
            if (BuildConfig.MP_DEBUG) {
                uploadMessage.put(MessageKey.ECHO, true)
            }
            uploadMessage.put(MessageKey.TYPE, Constants.MessageType.REQUEST_HEADER)
            uploadMessage.put(MessageKey.ID, UUID.randomUUID().toString())
            uploadMessage.put(MessageKey.TIMESTAMP, System.currentTimeMillis())
            uploadMessage.put(MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION)
            uploadMessage.put(MessageKey.OPT_OUT_HEADER, configManager.optedOut)
            uploadMessage.put(MessageKey.CONFIG_UPLOAD_INTERVAL, configManager.uploadInterval / 1000)
            uploadMessage.put(MessageKey.MPARTICLE_CONFIG_VERSION, configManager.etag)
            uploadMessage.put(MessageKey.CONFIG_SESSION_TIMEOUT, configManager.sessionTimeout / 1000)
            uploadMessage.put(MessageKey.MPID, batchId.mpid.toString())
            uploadMessage.put(MessageKey.SANDBOX, ConfigManager.getEnvironment() == MParticle.Environment.Development)
            uploadMessage.put(MessageKey.DEVICE_APPLICATION_STAMP, configManager.deviceApplicationStamp)

            if (history) {
                val deletedAttr = configManager.getUserStorage(batchId.mpid).deletedUserAttributes
                deletedAttr ?.let {
                    uploadMessage.put(MessageKey.DELETED_USER_ATTRIBUTES, JSONArray(deletedAttr))
                    configManager.userStorage.deleteDeletedUserAttributes()
                }
            }

            uploadMessage.put(MessageKey.COOKIES, cookies)
            uploadMessage.put(MessageKey.PROVIDER_PERSISTENCE, configManager.providerPersistence)
            uploadMessage.put(MessageKey.INTEGRATION_ATTRIBUTES, configManager.integrationAttributes)
            uploadMessage.addConsentState(configManager.getConsentState(batchId.mpid))
            uploadMessage.addDataplanContext(batchId.dataplanId, batchId.dataplanVersion)
            return uploadMessage
        }
    }
}
