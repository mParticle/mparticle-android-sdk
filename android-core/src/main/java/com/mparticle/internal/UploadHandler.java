package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.dto.ReadyUpload;
import com.mparticle.segmentation.SegmentListener;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;


import javax.net.ssl.SSLHandshakeException;

/**
 * Primary queue handler which is responsible for querying, packaging, and uploading data.
 */
public class UploadHandler extends Handler {

    private final Context mContext;
    MParticleDBManager mParticleDBManager;
    private final AppStateManager mAppStateManager;
    private final MessageManager mMessageManager;
    private ConfigManager mConfigManager;
    /**
     * Message used to trigger the primary upload logic - will upload all non-history batches that are ready to go.
     */
    public static final int UPLOAD_MESSAGES = 1;
    /**
     * Message that triggers much of the same logic as above, but is specifically for session-history. Typically the SDK will upload all messages
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
    private MParticleApiClient mApiClient;

    /**
     * Boolean used to determine if we're currently connected to the network. If we're not connected to the network,
     * don't even try to query or upload, just shut down to save on battery life.
     */
    volatile boolean isNetworkConnected = true;

    /**
     *
     * Only used for unit testing
     */
    UploadHandler(Context context, ConfigManager configManager, AppStateManager appStateManager, MessageManager messageManager) {
        mConfigManager = configManager;
        mContext = context;
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mParticleDBManager = new MParticleDBManager(context, DatabaseTables.getInstance(context));
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mMessageManager = messageManager;
        try {
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing");
        }
    }


    public UploadHandler(Context context, Looper looper, ConfigManager configManager, AppStateManager appStateManager, MessageManager messageManager) {
        super(looper);
        mConfigManager = configManager;
        mContext = context;
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mParticleDBManager = new MParticleDBManager(context, DatabaseTables.getInstance(context));
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mMessageManager = messageManager;
        try {
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing");
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (mParticleDBManager == null) {
                mParticleDBManager = new MParticleDBManager(mContext, DatabaseTables.getInstance(mContext));
            }
            switch (msg.what) {
                case UPDATE_CONFIG:
                    MParticle.getInstance().getKitManager().loadKitLibrary();
                    mApiClient.fetchConfig();
                    break;
                case INIT_CONFIG:
                    mConfigManager.delayedStart();
                    break;
                case UPLOAD_MESSAGES:
                case UPLOAD_TRIGGER_MESSAGES:
                    long uploadInterval = mConfigManager.getUploadInterval();
                    long mpid = (Long)msg.obj;
                    if (isNetworkConnected && !mApiClient.isThrottled()) {
                        if (uploadInterval > 0 || msg.arg1 == 1) {
                            prepareMessageUploads(false, mpid);
                            boolean needsHistory = upload(false);
                            if (needsHistory) {
                                this.sendMessage(obtainMessage(UPLOAD_HISTORY, mConfigManager.getMpid()));
                            }
                        }
                    }
                    if (mAppStateManager.getSession().isActive() && uploadInterval > 0 && msg.arg1 == 0) {
                        this.sendMessageDelayed(obtainMessage(UPLOAD_MESSAGES, mConfigManager.getMpid()), uploadInterval);
                    }
                    break;
                case UPLOAD_HISTORY:
                    mpid = (Long)msg.obj;
                    removeMessages(UPLOAD_HISTORY, mpid);
                    prepareMessageUploads(true, mpid);
                    if (isNetworkConnected) {
                        upload(true);
                    }
                    break;
            }
        } catch (MParticleApiClientImpl.MPConfigException e) {
            Logger.error("Bad API request - is the correct API key and secret configured?");
        } catch (Exception e){
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
     * - query app and device info, and add them to their respective batches
     * - persist all of the resulting upload batch objects
     * - mark the messages as having been uploaded.
     */
    private void prepareMessageUploads(boolean history, long mpId) throws Exception {
        String currentSessionId = mAppStateManager.getSession().mSessionID;
        long remainingHeap = MPUtility.getRemainingHeapInBytes();
        if (remainingHeap < Constants.LIMIT_MAX_UPLOAD_SIZE) {
            throw new Exception("Low remaining heap space, deferring uploads.");
        }
        final boolean sessionHistoryEnabled = MParticle.getInstance().getConfigManager().getIncludeSessionHistory();
        try {
            mParticleDBManager.cleanupMessages(mpId);
            if (history && !sessionHistoryEnabled) {
                mParticleDBManager.deleteMessagesAndSessions(currentSessionId, mpId);
                return;
            }

            if (history) {
                mParticleDBManager.createSessionHistoryUploadMessage(mConfigManager, mMessageManager.getDeviceAttributes(), currentSessionId, mpId);
            } else {
                mParticleDBManager.createMessagesForUploadMessage(mConfigManager, mMessageManager.getDeviceAttributes(), currentSessionId, sessionHistoryEnabled, mpId);
            }
        } catch (Exception e) {
            Logger.verbose("Error preparing batch upload in mParticle DB: " + e.getMessage());
        }
    }

    String containsClause = "\"" + Constants.MessageKey.TYPE + "\":\"" + Constants.MessageType.SESSION_END + "\"";

    /**
     * This method is responsible for looking for batches that are ready to be uploaded, and uploading them.
     */
    boolean upload(boolean history) {
        mParticleDBManager.cleanupUploadMessages();
        boolean processingSessionEnd = false;
        try {
            List<ReadyUpload> readyUploads = mParticleDBManager.getReadyUploads();
            if (readyUploads.size() > 0) {
                mApiClient.fetchConfig();
            }
            final boolean includeSessionHistory = mConfigManager.getIncludeSessionHistory();
            for (ReadyUpload readyUpload : readyUploads) {
                //this case actually shouldn't be needed anymore except for upgrade scenarios.
                //as of version 4.9.0, upload batches for session history shouldn't even be created.
                if (history && !includeSessionHistory) {
                    mParticleDBManager.deleteUpload(readyUpload.getId());
                } else {
                    if (!history) {
                        // if message is the MessageType.SESSION_END, then remember so the session history can be triggered
                        if (!processingSessionEnd && readyUpload.getMessage().contains(containsClause)) {
                            processingSessionEnd = true;
                        }
                    }
                    uploadMessage(readyUpload.getId(), readyUpload.getMessage());
                }
            }
        } catch (MParticleApiClientImpl.MPThrottleException e) {
        } catch (SSLHandshakeException ssle) {
            Logger.debug("SSL handshake failed while preparing uploads - possible MITM attack detected.");
        } catch (MParticleApiClientImpl.MPConfigException e) {
            Logger.error("Bad API request - is the correct API key and secret configured?");
        } catch (Exception e) {
            Logger.error(e, "Error processing batch uploads in mParticle DB");
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
            //some devices do not have MD5, and therefore cannot process SSL certificates,
            //and will throw an AssertionError containing an NoSuchAlgorithmException
            //there's not much to do in that case except catch the error and discard the data.
            Logger.error("API request failed " + e.toString());
            sampling = true;
        }

        if (sampling || shouldDelete(responseCode)) {
            mParticleDBManager.deleteUpload(id);
        } else {
            Logger.warning("Upload failed and will be retried.");
        }
    }

    public boolean shouldDelete(int statusCode) {
        return statusCode != MParticleApiClientImpl.HTTP_TOO_MANY_REQUESTS && (202 == statusCode ||
                (statusCode >= 400 && statusCode < 500));
    }

    /**
     * Method that is responsible for building an upload message to be sent over the wire.
     */
    MessageBatch createUploadMessage(boolean history) throws JSONException {
        MessageBatch batchMessage = MessageBatch.create(
                history,
                mConfigManager,
                mPreferences,
                mApiClient.getCookies());
        addGCMHistory(batchMessage);
        return batchMessage;
    }

    /**
     * Look for the last UIC message to find the end-state of user identities
     */
    private JSONArray findIdentityState(JSONArray messages) {
        JSONArray identities = null;
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                try {
                    if (messages.getJSONObject(i).get(Constants.MessageKey.TYPE).equals(MessageType.USER_IDENTITY_CHANGE)) {
                        identities = messages.getJSONObject(i).getJSONArray(MessageKey.USER_IDENTITIES);
                        messages.getJSONObject(i).remove(MessageKey.USER_IDENTITIES);
                    }
                }catch (JSONException jse) {

                }catch (NullPointerException npe) {

                }
            }
        }
        if (identities == null) {
            return mMessageManager.getUserIdentityJson();
        } else {
            return identities;
        }
    }

    /**
     * If the customer is using our GCM solution, query and append all of the history used for attribution.
     *
     */
    void addGCMHistory(MessageBatch uploadMessage) {
        try {
            mParticleDBManager.deleteExpiredGcmMessages();
            JSONObject historyObject = mParticleDBManager.getGcmHistory();
            if (historyObject != null) {
                uploadMessage.put(Constants.MessageKey.PUSH_CAMPAIGN_HISTORY, historyObject);
            }
        } catch (Exception e) {
            Logger.warning(e, "Error while building GCM campaign history");
        }
    }

    /*
     * Used by the test suite for mocking
     */
    void setApiClient(MParticleApiClient apiClient) {
        mApiClient = apiClient;
    }

    public void setConnected(boolean connected){

        try {
            if (!isNetworkConnected && connected && mConfigManager.isPushEnabled() && PushRegistrationHelper.getLatestPushRegistration(mContext) == null) {
                MParticle.getInstance().Messaging().enablePushNotifications(mConfigManager.getPushSenderId());
            }
        }catch (Exception e) {

        }
        isNetworkConnected = connected;
    }

    public void fetchSegments(long timeout, String endpointId, SegmentListener listener) {
        new SegmentRetriever(audienceDB, mApiClient).fetchSegments(timeout, endpointId, listener);
    }
}
