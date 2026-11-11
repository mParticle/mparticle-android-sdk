package com.mparticle.internal

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.os.Message
import androidx.annotation.Nullable
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.mparticle.MParticle
import com.mparticle.audience.AudienceResponse
import com.mparticle.audience.AudienceTask
import com.mparticle.identity.AliasResponse
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.database.services.MParticleDBManager
import com.mparticle.internal.listeners.InternalListenerManager
import com.mparticle.internal.messages.MPAliasMessage
import com.mparticle.networking.NetworkConnection.Companion.HTTP_TOO_MANY_REQUESTS
import org.json.JSONException
import java.io.IOException
import java.net.MalformedURLException
import javax.net.ssl.SSLHandshakeException

/**
 * Primary queue handler which is responsible for querying, packaging, and uploading data.
 */
open class UploadHandler : BaseHandler {
    private val mContext: Context?

    internal var mParticleDBManager: MParticleDBManager?
    private val mAppStateManager: AppStateManager?
    private val mMessageManager: MessageManager?
    private var mConfigManager: ConfigManager?
    private var mKitFrameworkWrapper: KitFrameworkWrapper?

    /**
     * Unix-epoch millisecond timestamp of the last successful age-based sweep. Zero means
     * "never run in this process".
     */
    private var mLastPersistenceCleanupMillis = 0L

    private val mPreferences: SharedPreferences?
    private val audienceDB: SegmentDatabase

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmField
    var mApiClient: MParticleApiClient? = null

    /**
     * Boolean used to determine if we're currently connected to the network. If we're not connected to the network,
     * don't even try to query or upload, just shut down to save on battery life.
     */
    @Volatile
    internal var isNetworkConnected = true

    /**
     * Only used for unit testing.
     */
    internal constructor(
        context: Context?,
        configManager: ConfigManager?,
        appStateManager: AppStateManager?,
        messageManager: MessageManager?,
        mparticleDBManager: MParticleDBManager?,
        @Nullable kitFrameworkWrapper: KitFrameworkWrapper?,
    ) : super() {
        mConfigManager = configManager
        mContext = context
        mAppStateManager = appStateManager
        audienceDB = SegmentDatabase(mContext)
        mParticleDBManager = mparticleDBManager
        mPreferences = mContext!!.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        mMessageManager = messageManager
        mKitFrameworkWrapper = kitFrameworkWrapper
        try {
            setApiClient(MParticleApiClientImpl(configManager, mPreferences, context))
        } catch (e: MalformedURLException) {
            // This should never happen - the URLs are created by constants.
        } catch (e: MParticleApiClientImpl.MPNoConfigException) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing.")
        }
    }

    constructor(
        context: Context?,
        looper: Looper?,
        configManager: ConfigManager?,
        appStateManager: AppStateManager?,
        messageManager: MessageManager?,
        mparticleDBManager: MParticleDBManager?,
        @Nullable kitFrameworkWrapper: KitFrameworkWrapper?,
    ) : super(looper) {
        mConfigManager = configManager
        mContext = context
        mAppStateManager = appStateManager
        audienceDB = SegmentDatabase(mContext)
        mParticleDBManager = mparticleDBManager
        mPreferences = mContext!!.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        mMessageManager = messageManager
        mKitFrameworkWrapper = kitFrameworkWrapper
        try {
            setApiClient(MParticleApiClientImpl(configManager, mPreferences, context))
        } catch (e: MalformedURLException) {
            // This should never happen - the URLs are created by constants.
        } catch (e: MParticleApiClientImpl.MPNoConfigException) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing.")
        }
    }

    override fun handleMessageImpl(msg: Message?) {
        try {
            mParticleDBManager!!.getDatabase()
            when (msg!!.what) {
                UPDATE_CONFIG -> {
                    val instance = MParticle.getInstance()
                    if (instance != null) {
                        instance.Internal().kitManager.loadKitLibrary()
                    }
                    mApiClient!!.fetchConfig(true)
                }
                INIT_CONFIG -> mConfigManager!!.delayedStart()
                UPLOAD_MESSAGES, UPLOAD_TRIGGER_MESSAGES -> {
                    val uploadInterval = mConfigManager!!.uploadInterval
                    if (isNetworkConnected) {
                        if (uploadInterval > 0 || msg.arg1 == 1) {
                            while (mParticleDBManager!!.hasMessagesForUpload()) {
                                if (!prepareMessageUploads(mConfigManager!!.uploadSettings)) {
                                    break
                                }
                            }
                            upload()
                        }
                    }
                    if (mAppStateManager!!.session.isActive && uploadInterval > 0 && msg.arg1 == 0) {
                        this.sendEmptyDelayed(UPLOAD_MESSAGES, uploadInterval)
                    }
                }
            }
        } catch (e: MParticleApiClientImpl.MPConfigException) {
            Logger.error("Bad API request - is the correct API key and secret configured?")
        } catch (e: Exception) {
            Logger.verbose("UploadHandler Exception while handling message: $e")
        } catch (ve: VerifyError) {
            Logger.verbose("UploadHandler VerifyError while handling message$ve")
        }
    }

    /**
     * This is the first processing step:
     * - query messages that have been persisted but not marked as uploaded
     * - group them into upload batches objects, one per session
     * - query reporting messages and add them to their respective batches
     * - query app and device customAttributes, and add them to their respective batches
     * - persist all of the resulting upload batch objects
     * - mark the messages as having been uploaded.
     *
     * @return false when nothing was packaged, so the pending messages are still there
     */
    @Throws(Exception::class)
    open fun prepareMessageUploads(uploadSettings: UploadSettings?): Boolean {
        val currentSessionId = mAppStateManager!!.session.mSessionID
        val remainingHeap = MPUtility.getRemainingHeapInBytes()
        if (remainingHeap < Constants.LIMIT_MAX_UPLOAD_SIZE) {
            throw Exception("Low remaining heap space, deferring uploads.")
        }
        val instance = MParticle.getInstance()
        // abort if MParticle singleton has been dereferenced, there is no way to evaluate session history
        if (instance == null) {
            return false
        }
        try {
            mParticleDBManager!!.cleanupMessages()
            mParticleDBManager!!.createMessagesForUploadMessage(
                mConfigManager,
                mMessageManager!!.deviceAttributes,
                currentSessionId,
                uploadSettings,
            )
            return true
        } catch (e: Exception) {
            Logger.verbose("Error preparing batch upload in mParticle DB: ${e.message}")
            return false
        }
    }

    internal var containsClause = "\"${Constants.MessageKey.TYPE}\":\"${Constants.MessageType.SESSION_END}\""

    /**
     * This method is responsible for looking for batches that are ready to be uploaded, and uploading them.
     */
    protected open fun upload() {
        maybePrunePersistedRecords(System.currentTimeMillis())
        mParticleDBManager!!.cleanupUploadMessages()
        try {
            val readyUploads = mParticleDBManager!!.getReadyUploads()
            if (readyUploads.size > 0) {
                mApiClient!!.fetchConfig()
            }
            for (readyUpload in readyUploads) {
                val message = readyUpload.message
                InternalListenerManager.listener.onCompositeObjects(readyUpload, message)
                if (readyUpload.isAliasRequest) {
                    uploadAliasRequest(readyUpload.id, message, readyUpload.uploadSettings)
                } else {
                    uploadMessage(readyUpload.id, message, readyUpload.uploadSettings)
                }
            }
        } catch (e: MParticleApiClientImpl.MPThrottleException) {
        } catch (ssle: SSLHandshakeException) {
            Logger.debug("SSL handshake failed while preparing uploads - possible MITM attack detected.")
        } catch (e: MParticleApiClientImpl.MPConfigException) {
            Logger.error("Bad API request - is the correct API key and secret configured?")
        } catch (e: Exception) {
            Logger.error(e, "Error processing batch uploads in mParticle DB.")
        }
    }

    /**
     * Run an age-based retention sweep across persisted events, batches, and sessions at most
     * once every [PERSISTENCE_CLEANUP_INTERVAL_MILLIS]. When the consumer has not
     * configured [com.mparticle.MParticleOptions.Builder.persistenceMaxAgeSeconds],
     * the default 90-day window is used. The throttle timestamp is only advanced on a
     * successful sweep so that transient failures (for example a locked database) can be
     * retried on the next upload cycle rather than deferred for 24 hours.
     *
     * @param nowMillis current time in unix-epoch milliseconds
     */
    @VisibleForTesting
    internal open fun maybePrunePersistedRecords(nowMillis: Long) {
        if (nowMillis - mLastPersistenceCleanupMillis < PERSISTENCE_CLEANUP_INTERVAL_MILLIS) {
            return
        }
        if (mParticleDBManager == null) {
            return
        }
        val configured: Int? = if (mConfigManager == null) null else mConfigManager!!.persistenceMaxAgeSeconds
        val maxAgeMillis =
            if (configured == null) {
                DEFAULT_PERSISTENCE_MAX_AGE_MILLIS
            } else {
                configured.toLong() * 1000L
            }
        val cutoffMillis = nowMillis - maxAgeMillis
        if (mParticleDBManager!!.deleteRecordsOlderThan(cutoffMillis)) {
            mLastPersistenceCleanupMillis = nowMillis
        }
    }

    @Throws(IOException::class, MParticleApiClientImpl.MPThrottleException::class)
    internal open fun uploadMessage(id: Int, message: String?, uploadSettings: UploadSettings?) {
        var responseCode = -1
        var sampling = false
        try {
            responseCode = mApiClient!!.sendMessageBatch(message, uploadSettings)
        } catch (e: MParticleApiClientImpl.MPRampException) {
            sampling = true
            Logger.debug("This device is being sampled.")
        } catch (e: AssertionError) {
            // Some devices do not have MD5, and therefore cannot process SSL certificates,
            // and will throw an AssertionError containing an NoSuchAlgorithmException
            // there's not much to do in that case except catch the error and discard the data.
            Logger.error("API request failed $e")
            sampling = true
        }

        if (sampling || shouldDelete(responseCode)) {
            forwardBatchToKits(message)
            mParticleDBManager!!.deleteUpload(id)
        } else {
            Logger.warning("Upload failed and will be retried.")
        }
    }

    @Throws(IOException::class, MParticleApiClientImpl.MPThrottleException::class)
    internal open fun uploadAliasRequest(id: Int, aliasRequestMessage: String?, uploadSettings: UploadSettings?) {
        var response: MParticleApiClient.AliasNetworkResponse = MParticleApiClient.AliasNetworkResponse(-1)
        var sampling = false

        try {
            response = mApiClient!!.sendAliasRequest(aliasRequestMessage, uploadSettings)
        } catch (e: JSONException) {
            response.setErrorMessage("Unable to deserialize Alias Request")
            Logger.error(response.getErrorMessage())
        } catch (e: MParticleApiClientImpl.MPRampException) {
            sampling = true
            response.setErrorMessage("This device is being sampled.")
            Logger.debug(response.getErrorMessage())
        }

        val shouldDelete = sampling || shouldDelete(response.getResponseCode())
        if (shouldDelete) {
            mParticleDBManager!!.deleteUpload(id)
        } else {
            Logger.warning("Alias Request will be retried")
        }
        try {
            val mpAliasMessage = MPAliasMessage(aliasRequestMessage)
            val aliasRequest = mpAliasMessage.aliasRequest
            val requestId = mpAliasMessage.requestId
            val aliasResponse = AliasResponse(response, aliasRequest, requestId, !shouldDelete)
            InternalListenerManager.listener.onAliasRequestFinished(aliasResponse)
        } catch (ignore: JSONException) {
            Logger.warning("Unable to deserialize AliasRequest, SdkListener.onAliasRequestFinished will not be called")
        }
    }

    open fun shouldDelete(statusCode: Int): Boolean =
        statusCode != HTTP_TOO_MANY_REQUESTS && (200 == statusCode || 202 == statusCode || (statusCode >= 400 && statusCode < 500))

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open fun setApiClient(apiClient: MParticleApiClient?) {
        mApiClient = apiClient
    }

    open fun setConnected(connected: Boolean) {
        try {
            val instance = MParticle.getInstance()
            if (instance != null && !isNetworkConnected && connected && mConfigManager!!.isPushEnabled) {
                instance.Messaging().enablePushNotifications(mConfigManager!!.pushSenderId)
            }
        } catch (e: Exception) {
        }
        isNetworkConnected = connected
    }

    open fun fetchUserAudiences(mpId: Long): AudienceTask<AudienceResponse> =
        UserAudiencesRetriever(mApiClient!!).fetchAudiences(mpId, mConfigManager!!.isAudienceFeatureFlagEnabled)

    // added so unit tests can subclass
    protected open fun sendEmpty(what: Int) {
        sendEmptyMessage(what)
    }

    // added so unit tests can subclass
    protected open fun sendEmptyDelayed(what: Int, delay: Long) {
        sendEmptyMessageDelayed(what, delay)
    }

    protected open fun forwardBatchToKits(message: String?) {
        // super messy, but we've hit constructor purgatory and can't guarantee mKitFrameworkWrapper isn't null
        if (mKitFrameworkWrapper == null) {
            val instance = MParticle.getInstance()
            if (instance != null) {
                mKitFrameworkWrapper = instance.Internal().kitManager
            }
        }
        if (mKitFrameworkWrapper != null) {
            mKitFrameworkWrapper!!.logBatch(message)
        } else {
            Logger.warning("Unable to forward batch to Kits, KitManager has been closed")
        }
    }

    companion object {
        /**
         * Message used to trigger the primary upload logic - will upload all non-history batches that are ready to go.
         */
        const val UPLOAD_MESSAGES = 1

        /**
         * Trigger a configuration update out of band from a typical upload.
         */
        const val UPDATE_CONFIG = 4

        /**
         * Some messages need to be uploaded immediately for accurate attribution and segmentation/audience behavior,
         * this message will be trigger after one of these messages has been detected.
         */
        const val UPLOAD_TRIGGER_MESSAGES = 5

        /**
         * Used on app startup to defer a few tasks to provide a quicker launch time.
         */
        const val INIT_CONFIG = 6

        /**
         * Default retention window for persisted events, batches, and sessions when
         * [com.mparticle.MParticleOptions.Builder.persistenceMaxAgeSeconds] is not set.
         * Matches the iOS SDK's 90-day default.
         */
        @VisibleForTesting
        internal const val DEFAULT_PERSISTENCE_MAX_AGE_MILLIS = 90L * 24L * 60L * 60L * 1000L

        /**
         * Minimum interval between age-based persistence sweeps, matching the iOS SDK's 24 hour
         * throttle on `cleanUp`.
         */
        @VisibleForTesting
        internal const val PERSISTENCE_CLEANUP_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
