package com.mparticle.internal;

import static com.mparticle.networking.NetworkConnection.HTTP_TOO_MANY_REQUESTS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.mparticle.MParticle;
import com.mparticle.audience.AudienceResponse;
import com.mparticle.audience.AudienceTask;
import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.AliasResponse;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.internal.messages.MPAliasMessage;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

/**
 * Primary queue handler which is responsible for querying, packaging, and uploading data.
 */
public class UploadHandler extends BaseHandler {
    private final Context mContext;
    MParticleDBManager mParticleDBManager;
    private final AppStateManager mAppStateManager;
    private final MessageManager mMessageManager;
    private ConfigManager mConfigManager;
    private KitFrameworkWrapper mKitFrameworkWrapper;
    /**
     * Message used to trigger the primary upload logic - will upload all non-history batches that are ready to go.
     */
    public static final int UPLOAD_MESSAGES = 1;
    /**
     * Trigger a configuration update out of band from a typical upload.
     */
    public static final int UPDATE_CONFIG = 4;
    /**
     * Some messages need to be uploaded immediately for accurate attribution and segmentation/audience behavior, this message will be trigger after
     * one of these messages has been detected.
     */
    public static final int UPLOAD_TRIGGER_MESSAGES = 5;

    /**
     * Used on app startup to defer a few tasks to provide a quicker launch time.
     */
    public static final int INIT_CONFIG = 6;

    /**
     * Default retention window for persisted events, batches, and sessions when
     * {@link com.mparticle.MParticleOptions.Builder#persistenceMaxAgeSeconds(int)} is not set.
     * Matches the iOS SDK's 90-day default.
     */
    @VisibleForTesting
    static final long DEFAULT_PERSISTENCE_MAX_AGE_MILLIS = 90L * 24L * 60L * 60L * 1000L;

    /**
     * Minimum interval between age-based persistence sweeps, matching the iOS SDK's 24 hour
     * throttle on {@code cleanUp}.
     */
    @VisibleForTesting
    static final long PERSISTENCE_CLEANUP_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L;

    /**
     * Unix-epoch millisecond timestamp of the last successful age-based sweep. Zero means
     * "never run in this process".
     */
    private long mLastPersistenceCleanupMillis = 0L;

    private final SharedPreferences mPreferences;
    private final SegmentDatabase audienceDB;

    /**
     * API client interface reference, useful for the unit test suite project.
     */
    MParticleApiClient mApiClient;

    /**
     * Boolean used to determine if we're currently connected to the network. If we're not connected to the network,
     * don't even try to query or upload, just shut down to save on battery life.
     */
    volatile boolean isNetworkConnected = true;

    /**
     * Only used for unit testing.
     */
    UploadHandler(Context context, ConfigManager configManager, AppStateManager appStateManager, MessageManager messageManager, MParticleDBManager mparticleDBManager, @Nullable KitFrameworkWrapper kitFrameworkWrapper) {
        super();
        mConfigManager = configManager;
        mContext = context;
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mParticleDBManager = mparticleDBManager;
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mMessageManager = messageManager;
        mKitFrameworkWrapper = kitFrameworkWrapper;
        try {
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //This should never happen - the URLs are created by constants.
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing.");
        }
    }


    public UploadHandler(Context context, Looper looper, ConfigManager configManager, AppStateManager appStateManager, MessageManager messageManager, MParticleDBManager mparticleDBManager, @Nullable KitFrameworkWrapper kitFrameworkWrapper) {
        super(looper);
        mConfigManager = configManager;
        mContext = context;
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mParticleDBManager = mparticleDBManager;
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mMessageManager = messageManager;
        mKitFrameworkWrapper = kitFrameworkWrapper;
        try {
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //This should never happen - the URLs are created by constants.
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing.");
        }
    }

    @Override
    public void handleMessageImpl(Message msg) {
        try {
            mParticleDBManager.getDatabase();
            switch (msg.what) {
                case UPDATE_CONFIG:
                    MParticle instance = MParticle.getInstance();
                    if (instance != null) {
                        instance.Internal().getKitManager().loadKitLibrary();
                    }
                    mApiClient.fetchConfig(true);
                    break;
                case INIT_CONFIG:
                    mConfigManager.delayedStart();
                    break;
                case UPLOAD_MESSAGES:
                case UPLOAD_TRIGGER_MESSAGES:
                    long uploadInterval = mConfigManager.getUploadInterval();
                    if (isNetworkConnected) {
                        if (uploadInterval > 0 || msg.arg1 == 1) {
                            while (mParticleDBManager.hasMessagesForUpload()) {
                                prepareMessageUploads(mConfigManager.getUploadSettings());
                            }
                            upload();
                        }
                    }
                    if (mAppStateManager.getSession().isActive() && uploadInterval > 0 && msg.arg1 == 0) {
                        this.sendEmptyDelayed(UPLOAD_MESSAGES, uploadInterval);
                    }
                    break;
            }
        } catch (MParticleApiClientImpl.MPConfigException e) {
            Logger.error("Bad API request - is the correct API key and secret configured?");
        } catch (Exception e) {
            Logger.verbose("UploadHandler Exception while handling message: " + e.toString());
        } catch (VerifyError ve) {
            Logger.verbose("UploadHandler VerifyError while handling message" + ve.toString());
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
     */
    public void prepareMessageUploads(UploadSettings uploadSettings) throws Exception {
        String currentSessionId = mAppStateManager.getSession().mSessionID;
        long remainingHeap = MPUtility.getRemainingHeapInBytes();
        if (remainingHeap < Constants.LIMIT_MAX_UPLOAD_SIZE) {
            throw new Exception("Low remaining heap space, deferring uploads.");
        }
        MParticle instance = MParticle.getInstance();
        //abort if MParticle singleton has been dereferenced, there is no way to evaluate session history
        if (instance == null) {
            return;
        }
        try {
            mParticleDBManager.cleanupMessages();
            mParticleDBManager.createMessagesForUploadMessage(mConfigManager, mMessageManager.getDeviceAttributes(), currentSessionId, uploadSettings);
        } catch (Exception e) {
            Logger.verbose("Error preparing batch upload in mParticle DB: " + e.getMessage());
        }
    }

    String containsClause = "\"" + Constants.MessageKey.TYPE + "\":\"" + Constants.MessageType.SESSION_END + "\"";

    /**
     * This method is responsible for looking for batches that are ready to be uploaded, and uploading them.
     */
    protected void upload() {
        maybePrunePersistedRecords(System.currentTimeMillis());
        mParticleDBManager.cleanupUploadMessages();
        try {
            List<MParticleDBManager.ReadyUpload> readyUploads = mParticleDBManager.getReadyUploads();
            if (readyUploads.size() > 0) {
                mApiClient.fetchConfig();
            }
            for (MParticleDBManager.ReadyUpload readyUpload : readyUploads) {
                String message = readyUpload.getMessage();
                InternalListenerManager.getListener().onCompositeObjects(readyUpload, message);
                if (readyUpload.isAliasRequest()) {
                    uploadAliasRequest(readyUpload.getId(), message, readyUpload.getUploadSettings());
                } else {
                    uploadMessage(readyUpload.getId(), message, readyUpload.getUploadSettings());
                }
            }
        } catch (MParticleApiClientImpl.MPThrottleException e) {
        } catch (SSLHandshakeException ssle) {
            Logger.debug("SSL handshake failed while preparing uploads - possible MITM attack detected.");
        } catch (MParticleApiClientImpl.MPConfigException e) {
            Logger.error("Bad API request - is the correct API key and secret configured?");
        } catch (Exception e) {
            Logger.error(e, "Error processing batch uploads in mParticle DB.");
        }
    }

    /**
     * Run an age-based retention sweep across persisted events, batches, and sessions at most
     * once every {@link #PERSISTENCE_CLEANUP_INTERVAL_MILLIS}. When the consumer has not
     * configured {@link com.mparticle.MParticleOptions.Builder#persistenceMaxAgeSeconds(int)},
     * the default 90-day window is used. The throttle timestamp is only advanced on a
     * successful sweep so that transient failures (for example a locked database) can be
     * retried on the next upload cycle rather than deferred for 24 hours.
     *
     * @param nowMillis current time in unix-epoch milliseconds
     */
    @VisibleForTesting
    void maybePrunePersistedRecords(long nowMillis) {
        if (nowMillis - mLastPersistenceCleanupMillis < PERSISTENCE_CLEANUP_INTERVAL_MILLIS) {
            return;
        }
        if (mParticleDBManager == null) {
            return;
        }
        try {
            Integer configured = mConfigManager == null ? null : mConfigManager.getPersistenceMaxAgeSeconds();
            long maxAgeMillis = (configured == null)
                    ? DEFAULT_PERSISTENCE_MAX_AGE_MILLIS
                    : configured.longValue() * 1000L;
            long cutoffMillis = nowMillis - maxAgeMillis;
            mParticleDBManager.deleteRecordsOlderThan(cutoffMillis);
            mLastPersistenceCleanupMillis = nowMillis;
        } catch (Exception e) {
            Logger.warning(e, "Failed to prune persisted records by age.");
        }
    }

    void uploadMessage(int id, String message, UploadSettings uploadSettings) throws IOException, MParticleApiClientImpl.MPThrottleException {
        int responseCode = -1;
        boolean sampling = false;
        try {
            responseCode = mApiClient.sendMessageBatch(message, uploadSettings);
        } catch (MParticleApiClientImpl.MPRampException e) {
            sampling = true;
            Logger.debug("This device is being sampled.");
        } catch (AssertionError e) {
            //Some devices do not have MD5, and therefore cannot process SSL certificates,
            //and will throw an AssertionError containing an NoSuchAlgorithmException
            //there's not much to do in that case except catch the error and discard the data.
            Logger.error("API request failed " + e.toString());
            sampling = true;
        }

        if (sampling || shouldDelete(responseCode)) {
            forwardBatchToKits(message);
            mParticleDBManager.deleteUpload(id);
        } else {
            Logger.warning("Upload failed and will be retried.");
        }
    }

    void uploadAliasRequest(int id, String aliasRequestMessage, UploadSettings uploadSettings) throws IOException, MParticleApiClientImpl.MPThrottleException {
        MParticleApiClient.AliasNetworkResponse response = new MParticleApiClientImpl.AliasNetworkResponse(-1);
        boolean sampling = false;

        try {
            response = mApiClient.sendAliasRequest(aliasRequestMessage, uploadSettings);
        } catch (JSONException e) {
            response.setErrorMessage("Unable to deserialize Alias Request");
            Logger.error(response.getErrorMessage());
        } catch (MParticleApiClientImpl.MPRampException e) {
            sampling = true;
            response.setErrorMessage("This device is being sampled.");
            Logger.debug(response.getErrorMessage());
        }

        boolean shouldDelete = sampling || shouldDelete(response.getResponseCode());
        if (shouldDelete) {
            mParticleDBManager.deleteUpload(id);
        } else {
            Logger.warning("Alias Request will be retried");
        }
        try {
            MPAliasMessage mpAliasMessage = new MPAliasMessage(aliasRequestMessage);
            AliasRequest aliasRequest = mpAliasMessage.getAliasRequest();
            String requestId = mpAliasMessage.getRequestId();
            AliasResponse aliasResponse = new AliasResponse(response, aliasRequest, requestId, !shouldDelete);
            InternalListenerManager.getListener().onAliasRequestFinished(aliasResponse);
        } catch (JSONException ignore) {
            Logger.warning("Unable to deserialize AliasRequest, SdkListener.onAliasRequestFinished will not be called");
        }
    }

    public boolean shouldDelete(int statusCode) {
        return statusCode != HTTP_TOO_MANY_REQUESTS && (200 == statusCode || 202 == statusCode ||
                (statusCode >= 400 && statusCode < 500));
    }

    /*
     * Used by the test suite for mocking.
     */
    void setApiClient(MParticleApiClient apiClient) {
        mApiClient = apiClient;
    }

    public void setConnected(boolean connected) {

        try {
            MParticle instance = MParticle.getInstance();
            if (instance != null && !isNetworkConnected && connected && mConfigManager.isPushEnabled()) {
                instance.Messaging().enablePushNotifications(mConfigManager.getPushSenderId());
            }
        } catch (Exception e) {

        }
        isNetworkConnected = connected;
    }


    public AudienceTask<AudienceResponse> fetchUserAudiences(long mpId) {
        return new UserAudiencesRetriever(mApiClient).fetchAudiences(mpId,mConfigManager.isAudienceFeatureFlagEnabled());
    }

    //added so unit tests can subclass
    protected void sendEmpty(int what) {
        sendEmptyMessage(what);
    }

    //added so unit tests can subclass
    protected void sendEmptyDelayed(int what, long delay) {
        sendEmptyMessageDelayed(what, delay);
    }

    protected void forwardBatchToKits(final String message) {
        //super messy, but we've hit constructor purgatory and can't guarantee mKitFrameworkWrapper isn't null
        if (mKitFrameworkWrapper == null) {
            MParticle instance = MParticle.getInstance();
            if (instance != null) {
                mKitFrameworkWrapper = instance.Internal().getKitManager();
            }
        }
        if (mKitFrameworkWrapper != null) {
            mKitFrameworkWrapper.logBatch(message);
        } else {
            Logger.warning("Unable to forward batch to Kits, KitManager has been closed");
        }
    }
}
