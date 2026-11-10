package com.mparticle.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.NonNull
import com.mparticle.SdkListener
import com.mparticle.audience.AudienceResponse
import com.mparticle.audience.BaseAudienceTask
import com.mparticle.identity.IdentityApi
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.listeners.InternalListenerManager
import com.mparticle.networking.MPConnection
import com.mparticle.networking.MPUrl
import com.mparticle.networking.MParticleBaseClientImpl
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Objects

/**
 * Class responsible for all network communication to the mParticle Events and Configuration APIs.
 */
open class MParticleApiClientImpl :
    MParticleBaseClientImpl,
    MParticleApiClient {
    private val mConfigManager: ConfigManager
    private val mApiSecret: String?
    private var mConfigUrl: MPUrl? = null
    private val mUserAgent: String
    private val mPreferences: SharedPreferences?
    private val mContext: Context?

    @JvmField
    internal var mDeviceRampNumber: Int? = null
    private var mCurrentCookies: JSONObject? = null
    private var mConfigLastFetched: Long = -1
    private var alreadyWarned = false

    @Throws(MalformedURLException::class, MPNoConfigException::class)
    constructor(configManager: ConfigManager?, sharedPreferences: SharedPreferences?, context: Context?) : super(context, configManager) {
        mContext = context
        mConfigManager = configManager!!
        mApiSecret = configManager.apiSecret
        mPreferences = sharedPreferences
        mUserAgent = "mParticle Android SDK/" + Constants.MPARTICLE_VERSION
        if (MPUtility.isEmpty(mApiKey) || MPUtility.isEmpty(mApiSecret)) {
            throw MPNoConfigException()
        }
    }

    /**
     * Only used for unit testing.
     */
    internal open fun setConfigUrl(configUrl: MPUrl?) {
        mConfigUrl = configUrl
    }

    @Throws(IOException::class, MPConfigException::class)
    override fun fetchConfig() {
        fetchConfig(false)
    }

    /**
     * Fetches a remote configuration. Minimum time constraint based on MIN_CONFIG_REQUEST_INTERVAL
     * if not forced, configuration request will not take place if minimum time has not elapsed
     *
     * @param force: if true, minimum elpsed time criteria will be ignored, and configuration
     *               request will take place regardless of elapsed time
     */
    @Throws(IOException::class, MPConfigException::class)
    override fun fetchConfig(force: Boolean) {
        if (!force) {
            if (System.currentTimeMillis() - mConfigLastFetched > MIN_CONFIG_REQUEST_INTERVAL) {
                mConfigLastFetched = System.currentTimeMillis()
            } else {
                Logger.verbose("Config request deferred, not enough time has elapsed since last request.")
                return
            }
        }
        try {
            if (mConfigUrl == null) {
                mConfigUrl = getUrl(Endpoint.CONFIG)
            }
            val connection = mConfigUrl!!.openConnection()
            connection.setConnectTimeout(mConfigManager.connectionTimeout)
            connection.setReadTimeout(mConfigManager.connectionTimeout)
            connection.setRequestProperty(
                HEADER_ENVIRONMENT,
                ConfigManager.getEnvironment().value.toString(),
            )

            val supportedKits = mConfigManager.supportedKitString
            if (!MPUtility.isEmpty(supportedKits)) {
                connection.setRequestProperty(HEADER_KITS, supportedKits)
            }

            connection.setRequestProperty("User-Agent", mUserAgent)
            val etag = mConfigManager.etag
            if (etag != null) {
                connection.setRequestProperty("If-None-Match", etag)
            }
            val modified = mConfigManager.ifModified
            if (modified != null) {
                connection.setRequestProperty("If-Modified-Since", modified)
            }

            addMessageSignature(connection, null)

            Logger.verbose(
                "Config request attempt:\n" +
                    "URL- " + mConfigUrl.toString(),
            )

            if (InternalListenerManager.isEnabled) {
                InternalListenerManager.listener.onNetworkRequestStarted(
                    SdkListener.Endpoint.CONFIG,
                    connection.url.toString(),
                    JSONObject(),
                )
            }

            makeUrlRequest(Endpoint.CONFIG, connection, true)

            var response: JSONObject? = JSONObject()
            val responseCode = connection.responseCode
            try {
                response = MPUtility.getJsonResponse(connection)
                InternalListenerManager.listener.onNetworkRequestFinished(
                    SdkListener.Endpoint.CONFIG,
                    connection.url.toString(),
                    response,
                    responseCode,
                )
            } catch (ex: Exception) {
            }
            if (responseCode in 200..299) {
                parseCookies(response)

                Logger.verbose(
                    "Config result: \n " +
                        connection.responseCode + ": " +
                        connection.responseMessage + "\n" +
                        "response:\n" + response,
                )

                val newEtag = connection.getHeaderField("ETag")
                val newModified = connection.getHeaderField("Last-Modified")

                mConfigManager.updateConfig(response, newEtag, newModified)
            } else if (connection.responseCode == 400) {
                throw MPConfigException()
            } else if (connection.responseCode == 304) {
                mConfigManager.configUpToDate()
                Logger.verbose("Config request deferred, configuration already up-to-date.")
            } else {
                Logger.error(
                    "Config request failed- " + connection.responseCode + ": " + connection.responseMessage,
                )
            }
        } catch (e: MalformedURLException) {
            Logger.error("Error constructing config service URL.")
        } catch (e: JSONException) {
            Logger.error("Config request failed to process response message JSON.")
        } catch (e: AssertionError) {
            // some devices do not have MD5, and therefore cannot process SSL certificates
            // there's not much to do in that case except catch the error
            Logger.error("Config request failed $e")
        }
    }

    override fun fetchUserAudience(task: BaseAudienceTask?, mpId: Long) {
        var jsonResponse: JSONObject? = null
        try {
            val connection = getUrl(Endpoint.AUDIENCE, mpId).openConnection()
            Logger.verbose("Audience API request: \n" + connection.url.toString())
            connection.setRequestProperty("User-Agent", mUserAgent)
            addMessageSignature(connection, null)
            connection.setRequestProperty("x-mp-key", mApiKey)
            makeUrlRequest(Endpoint.AUDIENCE, connection, true)
            if (connection.responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                Logger.error("Your workspace is not enabled to retrieve user audiences")
            }
            jsonResponse = MPUtility.getJsonResponse(connection)
            Logger.verbose(
                "Audience API response: \n Status code: " + connection.responseCode + "  JSON response: " + jsonResponse,
            )
            if (jsonResponse != null && connection.responseCode == 200) {
                task!!.setSuccessful(AudienceResponse(connection.responseCode, jsonResponse))
            } else {
                task!!.setFailed(AudienceResponse(connection.responseCode, "mParticle Audience API failed."))
            }
        } catch (e: Exception) {
            Logger.error("mParticle Audience API failed. $e")
            task!!.setFailed(
                AudienceResponse(IdentityApi.UNKNOWN_ERROR, Objects.requireNonNull(e.message)!!),
            )
        }
    }

    @Throws(IOException::class, MPThrottleException::class, MPRampException::class)
    override fun sendMessageBatch(message: String?, uploadSettings: UploadSettings?): Int {
        checkThrottleTime(Endpoint.EVENTS)
        checkRampValue()
        val eventUrl = getUrl(Endpoint.EVENTS, null, null, uploadSettings)
        val connection = eventUrl.openConnection()
        connection.setConnectTimeout(mConfigManager.connectionTimeout)
        connection.setReadTimeout(mConfigManager.connectionTimeout)
        connection.setDoOutput(true)
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Content-Encoding", "gzip")
        connection.setRequestProperty("User-Agent", mUserAgent)

        val activeKits = uploadSettings!!.activeKits
        if (!MPUtility.isEmpty(activeKits)) {
            connection.setRequestProperty(HEADER_KITS, activeKits)
        }
        val supportedKits = uploadSettings.supportedKits
        if (!MPUtility.isEmpty(supportedKits)) {
            connection.setRequestProperty(HEADER_BUNDLED_KITS, supportedKits)
        }

        addMessageSignature(connection, message)

        logUpload(message)

        try {
            InternalListenerManager.listener.onNetworkRequestStarted(
                SdkListener.Endpoint.EVENTS,
                connection.url.toString(),
                JSONObject(message!!),
                message,
            )
        } catch (e: Exception) {
        }

        makeUrlRequest(Endpoint.EVENTS, connection, message, true)

        Logger.verbose(
            "Upload request attempt:\n" +
                "URL- " + eventUrl.toString(),
        )

        Logger.verbose(message)

        val responseCode = connection.responseCode

        if (responseCode in 200..299) {
            val response = MPUtility.getJsonResponse(connection)
            if (InternalListenerManager.isEnabled) {
                InternalListenerManager.listener.onNetworkRequestFinished(
                    SdkListener.Endpoint.EVENTS,
                    connection.url.toString(),
                    response,
                    responseCode,
                )
            }

            Logger.verbose(
                "Upload result response: \n" +
                    connection.responseCode + ": " +
                    connection.responseMessage + "\n" +
                    "response:\n" + response,
            )
            parseCookies(response)
        } else {
            Logger.error("Upload request failed- " + responseCode + ": " + connection.responseMessage)
            try {
                InternalListenerManager.listener.onNetworkRequestFinished(
                    SdkListener.Endpoint.EVENTS,
                    connection.url.file,
                    JSONObject().put(SdkListener.ERROR_MESSAGE, connection.responseMessage),
                    responseCode,
                )
            } catch (e: Exception) {
            }
        }
        return connection.responseCode
    }

    @NonNull
    @Throws(IOException::class, MPThrottleException::class, MPRampException::class)
    override fun sendAliasRequest(message: String?, uploadSettings: UploadSettings?): MParticleApiClient.AliasNetworkResponse {
        checkThrottleTime(Endpoint.ALIAS)
        Logger.verbose("Identity alias request:\n$message")

        val aliasUrl = getUrl(Endpoint.ALIAS, null, null, uploadSettings)
        var connection = aliasUrl.openConnection()
        connection.setConnectTimeout(mConfigManager.connectionTimeout)
        connection.setReadTimeout(mConfigManager.connectionTimeout)
        connection.setDoOutput(true)
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Content-Encoding", "gzip")
        connection.setRequestProperty("User-Agent", mUserAgent)
        addMessageSignature(connection, message)

        var url = ""
        try {
            url = connection.url.toString()
            InternalListenerManager.listener.onNetworkRequestStarted(
                SdkListener.Endpoint.EVENTS,
                url,
                JSONObject(message!!),
                message,
            )
        } catch (ignore: Exception) {
        }

        connection = makeUrlRequest(Endpoint.ALIAS, connection, message, false)!!
        val responseCode = connection.responseCode
        var error = ""
        var response: JSONObject? = JSONObject()
        if (responseCode in 200..299) {
            Logger.verbose(
                "Alias Request response: \n " +
                    connection.responseCode + ": " +
                    connection.responseMessage,
            )
        } else {
            response = MPUtility.getJsonResponse(connection)
            if (response != null) {
                error = response.optString(ALIAS_ERROR_MESSAGE)
            }
            Logger.error("Alias Request failed- $responseCode: $error")
        }
        InternalListenerManager.listener.onNetworkRequestFinished(
            SdkListener.Endpoint.EVENTS,
            url,
            response,
            responseCode,
        )
        return MParticleApiClient.AliasNetworkResponse(responseCode, error)
    }

    private fun logUpload(message: String?) {
        try {
            val messageJson = JSONObject(message!!)
            if (messageJson.has(Constants.MessageKey.MESSAGES)) {
                val messages = messageJson.getJSONArray(Constants.MessageKey.MESSAGES)
                Logger.verbose("Uploading message batch...")
                for (i in 0 until messages.length()) {
                    Logger.verbose(
                        "Message type: " + (messages.get(i) as JSONObject).getString(Constants.MessageKey.TYPE),
                    )
                }
            }
        } catch (jse: JSONException) {
        }
    }

    internal open fun addMessageSignature(request: MPConnection?, message: String?) {
        try {
            val date = getHeaderDateString()
            request!!.setRequestProperty("Date", date)
            request.setRequestProperty(
                HEADER_SIGNATURE,
                getHeaderHashString(request, date, message, mApiSecret),
            )
        } catch (e: InvalidKeyException) {
            Logger.error("Error signing message.")
        } catch (e: NoSuchAlgorithmException) {
            Logger.error("Error signing message.")
        } catch (e: UnsupportedEncodingException) {
            Logger.error("Error signing message.")
        }
    }

    private fun parseCookies(jsonResponse: JSONObject?) {
        // A success response may have no JSON body; throwing here would abort the upload after the send.
        if (jsonResponse == null) {
            return
        }
        try {
            if (jsonResponse.has(CONSUMER_INFO)) {
                val consumerInfo = jsonResponse.getJSONObject(CONSUMER_INFO)
                setCookies(consumerInfo.optJSONObject(Constants.MessageKey.COOKIES))
            }
        } catch (ignored: JSONException) {
        }
    }

    inner class MPThrottleException internal constructor() : Exception("mParticle servers are busy, API connections have been throttled.")

    inner class MPConfigException internal constructor() : Exception("mParticle configuration request failed.")

    class MPRampException internal constructor() : Exception("This device is being sampled.")

    class MPNoConfigException internal constructor() : Exception("No API key and/or API secret.")

    @Throws(MPThrottleException::class)
    internal open fun checkThrottleTime(endpoint: Endpoint?) {
        if (System.currentTimeMillis() < getNextRequestTime(endpoint)) {
            throw MPThrottleException()
        }
    }

    @Throws(MPRampException::class)
    private fun checkRampValue() {
        if (mDeviceRampNumber == null) {
            mDeviceRampNumber =
                MPUtility
                    .hashFnv1A(MPUtility.getRampUdid(mContext).toByteArray())
                    .mod(BigInteger.valueOf(100))
                    .toInt()
        }
        val currentRamp = mConfigManager.currentRampValue
        if (currentRamp in 1..99 && mDeviceRampNumber!! > mConfigManager.currentRampValue) {
            throw MPRampException()
        }
    }

    open fun setCookies(serverCookies: JSONObject?) {
        if (serverCookies != null) {
            try {
                val localCookies = getCookies()
                val keys = serverCookies.keys()

                while (keys.hasNext()) {
                    val key = keys.next() as String
                    localCookies!!.put(key, serverCookies.getJSONObject(key))
                }
                mCurrentCookies = localCookies
                mConfigManager.userStorage.cookies = mCurrentCookies.toString()
            } catch (jse: JSONException) {
            }
        }
    }

    override fun getCookies(): JSONObject? {
        if (mCurrentCookies == null) {
            val currentCookies = mConfigManager.userStorage.cookies
            if (MPUtility.isEmpty(currentCookies)) {
                mCurrentCookies = JSONObject()
                mConfigManager.userStorage.cookies = mCurrentCookies.toString()
                return mCurrentCookies
            } else {
                try {
                    mCurrentCookies = JSONObject(currentCookies)
                } catch (e: JSONException) {
                    mCurrentCookies = JSONObject()
                }
            }
            val nowCalendar = Calendar.getInstance()
            nowCalendar.set(Calendar.YEAR, 1990)
            val oldDate = nowCalendar.time
            val parser = SimpleDateFormat("yyyy")
            val keys = mCurrentCookies!!.keys()
            val keysToRemove = ArrayList<String>()
            while (keys.hasNext()) {
                try {
                    val key = keys.next() as String
                    if (mCurrentCookies!!.get(key) is JSONObject) {
                        val expiration = (mCurrentCookies!!.get(key) as JSONObject).getString("e")
                        try {
                            val date = parser.parse(expiration)
                            if (date.before(oldDate)) {
                                keysToRemove.add(key)
                            }
                        } catch (dpe: ParseException) {
                        }
                    }
                } catch (jse: JSONException) {
                }
            }
            for (key in keysToRemove) {
                mCurrentCookies!!.remove(key)
            }
            if (keysToRemove.size > 0) {
                mConfigManager.userStorage.cookies = mCurrentCookies.toString()
            }
            return mCurrentCookies
        } else {
            return mCurrentCookies
        }
    }

    internal companion object {
        /**
         * Signature header used for authentication with the client key/secret
         */
        private const val HEADER_SIGNATURE = "x-mp-signature"

        /**
         * Environment header used to tell the SDK server if this is a development or production request
         */
        private const val HEADER_ENVIRONMENT = "x-mp-env"

        /**
         * Embedded kit header used to tell both the supported EKs (/config), and the currently active EKs (/events)
         */
        private const val HEADER_KITS = "x-mp-kits"
        private const val HEADER_BUNDLED_KITS = "x-mp-bundled-kits"

        /**
         * Wrapper around cookies, MPID, and other server-response customAttributes that requires parsing.
         */
        private const val CONSUMER_INFO = "ci"

        /**
         * Human readable error message field for "alias" responses
         */
        private const val ALIAS_ERROR_MESSAGE = "message"

        /**
         * Default throttle time - in the worst case scenario if the server is busy, the soonest
         * the SDK will attempt to contact the server again will be after this 2 hour window.
         */
        internal const val DEFAULT_THROTTLE_MILLIS: Long = 1000L * 60 * 60 * 2
        internal const val MAX_THROTTLE_MILLIS: Long = 1000L * 60 * 60 * 24

        /**
         * Minimum time between passive Config requests, 10 minutes
         */
        private const val MIN_CONFIG_REQUEST_INTERVAL = 10 * 60 * 1000
    }
}
