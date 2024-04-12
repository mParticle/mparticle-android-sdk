package com.mparticle.internal;

import static com.mparticle.networking.NetworkConnection.HTTP_TOO_MANY_REQUESTS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.AliasResponse;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.internal.messages.MPAliasMessage;
import com.mparticle.segmentation.SegmentListener;

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
     * Message that triggers much of the same logic as above, but is specifically for session-history. Typically, the SDK will upload all messages
     * in a given batch that are ready for upload. But, some service-providers such as Flurry need to be sent all of the session information at once
     * With *history*, the SDK will package all of the messages that occur in a particular session.
     */
    public static final int UPLOAD_HISTORY = 3;
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
                                prepareMessageUploads(false);
                            }
                            boolean needsHistory = upload(false);
                            if (needsHistory) {
                                this.sendEmpty(UPLOAD_HISTORY);
                            }
                        }
                    }
                    if (mAppStateManager.getSession().isActive() && uploadInterval > 0 && msg.arg1 == 0) {
                        this.sendEmptyDelayed(UPLOAD_MESSAGES, uploadInterval);
                    }
                    break;
                case UPLOAD_HISTORY:
                    removeMessage(UPLOAD_HISTORY);
                    prepareMessageUploads(true);
                    if (isNetworkConnected) {
                        upload(true);
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
    protected void prepareMessageUploads(boolean history) throws Exception {
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
        final boolean sessionHistoryEnabled = instance.Internal().getConfigManager().getIncludeSessionHistory();
        try {
            mParticleDBManager.cleanupMessages();
            if (history && !sessionHistoryEnabled) {
                mParticleDBManager.deleteMessagesAndSessions(currentSessionId);
                return;
            }
            if (history) {
                mParticleDBManager.createSessionHistoryUploadMessage(mConfigManager, mMessageManager.getDeviceAttributes(), currentSessionId);
            } else {
                mParticleDBManager.createMessagesForUploadMessage(mConfigManager, mMessageManager.getDeviceAttributes(), currentSessionId, sessionHistoryEnabled);
            }
        } catch (Exception e) {
            Logger.verbose("Error preparing batch upload in mParticle DB: " + e.getMessage());
        }
    }

    String containsClause = "\"" + Constants.MessageKey.TYPE + "\":\"" + Constants.MessageType.SESSION_END + "\"";

    /**
     * This method is responsible for looking for batches that are ready to be uploaded, and uploading them.
     */
    protected boolean upload(boolean history) {
        mParticleDBManager.cleanupUploadMessages();
        boolean processingSessionEnd = false;
        try {
            List<MParticleDBManager.ReadyUpload> readyUploads = mParticleDBManager.getReadyUploads();
            if (readyUploads.size() > 0) {
                mApiClient.fetchConfig();
            }
            final boolean includeSessionHistory = mConfigManager.getIncludeSessionHistory();
            for (MParticleDBManager.ReadyUpload readyUpload : readyUploads) {
                //This case actually shouldn't be needed anymore except for upgrade scenarios.
                //As of version 4.9.0, upload batches for session history shouldn't even be created.
                if (history && !includeSessionHistory) {
                    mParticleDBManager.deleteUpload(readyUpload.getId());
                } else {
                    if (!history) {
                        // If message is the MessageType.SESSION_END, then remember so the session history can be triggered.
                        if (!processingSessionEnd && readyUpload.getMessage().contains(containsClause)) {
                            processingSessionEnd = true;
                        }
                    }
                    String message = readyUpload.getMessage();
                    InternalListenerManager.getListener().onCompositeObjects(readyUpload, message);
                    if (readyUpload.isAliasRequest()) {
                        uploadAliasRequest(readyUpload.getId(), message);
                    } else {
                        uploadMessage(readyUpload.getId(), message);
                    }
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
        return processingSessionEnd;
    }

    void uploadMessage(int id, String message) throws IOException, MParticleApiClientImpl.MPThrottleException {
        int responseCode = -1;
        boolean sampling = false;
        try {
            responseCode = mApiClient.sendMessageBatch(message);
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

    void uploadAliasRequest(int id, String aliasRequestMessage) throws IOException, MParticleApiClientImpl.MPThrottleException {
        MParticleApiClient.AliasNetworkResponse response = new MParticleApiClientImpl.AliasNetworkResponse(-1);
        boolean sampling = false;

        try {
            response = mApiClient.sendAliasRequest(aliasRequestMessage);
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


    public void fetchUserAudiences() {
        new UserAudiencesRetriever(mApiClient).fetchAudience();
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
