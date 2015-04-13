package com.mparticle.internal;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.AppStateManager;
import com.mparticle.BuildConfig;
import com.mparticle.ConfigManager;
import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.Constants.PrefKeys;
import com.mparticle.internal.Constants.Status;
import com.mparticle.internal.MParticleDatabase.MessageTable;
import com.mparticle.internal.MParticleDatabase.UploadTable;
import com.mparticle.segmentation.SegmentListener;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;

import javax.net.ssl.SSLHandshakeException;

/**
 * Primary queue handler which is responsible for querying, packaging, and uploading data.
 */
public class UploadHandler extends Handler {

    private final Context mContext;
    private final MParticleDatabase mDbHelper;
    private final AppStateManager mAppStateManager;
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

    /**
     * Reference to the primary database where messages, uploads, and sessions are stored.
     */
    private SQLiteDatabase db;
    private final SharedPreferences mPreferences;

    private final String mApiKey;
    private final SegmentDatabase audienceDB;

    /**
     * API client interface reference, useful for the unit test suite project.
     */
    private IMPApiClient mApiClient;

    /**
     * Boolean used to determine if we're currently connected to the network. If we're not connected to the network,
     * don't even try to query or upload, just shut down to save on battery life.
     */
    volatile boolean isNetworkConnected = true;

    /**
     * Maintain a reference to these two objects as they primary do not change over the course of an app execution, so we
     * shouldn't be recreating them every time we need them.
     */
    private JSONObject deviceInfo;
    private JSONObject appInfo;

    /**
     *
     * Only used for unit testing
     */
    UploadHandler(Context context, ConfigManager configManager, MParticleDatabase database, AppStateManager appStateManager) {
        mConfigManager = configManager;
        mContext = context;
        mApiKey = mConfigManager.getApiKey();
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDbHelper = database;
        try {
            setApiClient(new MParticleApiClient(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        }
    }


    public UploadHandler(Context context, Looper looper, ConfigManager configManager, MParticleDatabase database, AppStateManager appStateManager) {
        super(looper);
        mConfigManager = configManager;
        mContext = context;
        mApiKey = mConfigManager.getApiKey();
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDbHelper = database;
        try {
            setApiClient(new MParticleApiClient(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            if (db == null){
                db = mDbHelper.getWritableDatabase();
            }
            switch (msg.what) {
                case UPDATE_CONFIG:
                    mApiClient.fetchConfig();
                    break;
                case INIT_CONFIG:
                    mConfigManager.delayedStart();
                    break;
                case UPLOAD_MESSAGES:
                case UPLOAD_TRIGGER_MESSAGES:
                    long uploadInterval = mConfigManager.getUploadInterval();
                    if (isNetworkConnected && !mApiClient.isThrottled()) {
                        if (uploadInterval > 0 || msg.arg1 == 1) {
                            prepareMessageUploads();
                            boolean needsHistory = upload(false);
                            if (needsHistory) {
                                this.sendEmptyMessage(UPLOAD_HISTORY);
                            }
                        }
                    }
                    if (mAppStateManager.getSession().isActive() && uploadInterval > 0 && msg.arg1 == 0) {
                        this.sendEmptyMessageDelayed(UPLOAD_MESSAGES, uploadInterval);
                    }
                    break;
                case UPLOAD_HISTORY:
                    removeMessages(UPLOAD_HISTORY);
                    prepareHistoryUploads();
                    if (isNetworkConnected) {
                        upload(true);
                    }
                    break;
            }

        }catch (Exception e){
            if (BuildConfig.MP_DEBUG){
                ConfigManager.log(MParticle.LogLevel.DEBUG, "UploadHandler Exception while handling message: " + msg.what + "\n" + e.toString());
            }
        }
    }

    /**
     * This is the first processing step:
     * - query messages that have been logged but not marked as uploaded
     * - group them into batches, generate a JSON batch message, insert it as an upload
     * - mark the messages as having been uploaded.
     */
    private void prepareMessageUploads() {
        Cursor readyMessagesCursor = null;
        try {
            readyMessagesCursor = MParticleDatabase.getMessagesForUpload(db);
            if (readyMessagesCursor.getCount() > 0) {
                mApiClient.fetchConfig();
                int messageIdIndex = readyMessagesCursor.getColumnIndex(MessageTable._ID);
                int messageIndex = readyMessagesCursor.getColumnIndex(MessageTable.MESSAGE);
                while (!readyMessagesCursor.isAfterLast()) {
                    int highestMessageId = 0;

                    JSONArray messagesArray = new JSONArray();
                    while (messagesArray.length() <= Constants.BATCH_LIMIT && readyMessagesCursor.moveToNext()) {
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(messageIndex));
                        messagesArray.put(msgObject);
                        highestMessageId = readyMessagesCursor.getInt(messageIdIndex);
                    }
                    MessageBatch uploadMessage = createUploadMessage(messagesArray, false);
                    dbInsertUpload(uploadMessage);
                    dbMarkAsUploadedMessage(highestMessageId);
                }
            }
        } catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Error preparing batch upload in mParticle DB: " + e.getMessage());
        } finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }
        }
    }

    /**
     * - Query all messages that have been uploaded that are not for the current session
     * - Group each message by session and create a JSON session-history message, insert as an upload
     * - Delete all the original messages.
     */
    void prepareHistoryUploads(){
        Cursor readyMessagesCursor = null;
        try {
            readyMessagesCursor = MParticleDatabase.getSessionHistory(db, mAppStateManager.getSession().mSessionID);
            if (readyMessagesCursor.getCount() > 0) {
                mApiClient.fetchConfig();
                int sessionIndex = readyMessagesCursor.getColumnIndex(MessageTable.SESSION_ID);
                int messageIndex = readyMessagesCursor.getColumnIndex(MessageTable.MESSAGE);
                JSONArray messagesArray = new JSONArray();
                String lastSessionId = null;
                while (readyMessagesCursor.moveToNext()) {
                    String currentSessionId = readyMessagesCursor.getString(sessionIndex);
                    MPMessage message = new MPMessage(readyMessagesCursor.getString(messageIndex));
                    if (lastSessionId == null || lastSessionId.equals(currentSessionId)){
                        messagesArray.put(message);
                    }else {
                        MessageBatch uploadMessage = createUploadMessage(messagesArray, true);
                        if (uploadMessage != null) {
                            dbInsertUpload(uploadMessage);
                            dbDeleteProcessedMessages(lastSessionId);
                        }
                        messagesArray = new JSONArray();
                        messagesArray.put(message);
                    }
                    lastSessionId = currentSessionId;
                }

            }
        } catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Error preparing batch upload in mParticle DB: " + e.getMessage());
        } finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }
        }
    }

    String[] uploadColumns = new String[]{"_id", UploadTable.MESSAGE};
    String containsClause = "\"" + MessageKey.TYPE + "\":\"" + MessageType.SESSION_END + "\"";

    /**
     * This method is responsible for looking for batches that are ready to be uploaded, and uploading them.
     */
    boolean upload(boolean history) {
        boolean processingSessionEnd = false;
        Cursor readyUploadsCursor = null;
        try {
            readyUploadsCursor = db.query(UploadTable.TABLE_NAME, uploadColumns,
                    null, null, null, null, UploadTable.CREATED_AT);
            int messageIdIndex = readyUploadsCursor.getColumnIndex(UploadTable._ID);
            int messageIndex = readyUploadsCursor.getColumnIndex(UploadTable.MESSAGE);
            while (readyUploadsCursor.moveToNext()) {
                int id = readyUploadsCursor.getInt(messageIdIndex);
                String message = readyUploadsCursor.getString(messageIndex);
                if (!history) {
                    // if message is the MessageType.SESSION_END, then remember so the session history can be triggered
                    if (!processingSessionEnd && message.contains(containsClause)) {
                        processingSessionEnd = true;
                    }
                }

                int responseCode = mApiClient.sendMessageBatch(message);

                if (shouldDelete(responseCode)) {
                    deleteUpload(id);
                } else {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Upload failed and will be retried.");
                }
            }
        } catch (MParticleApiClient.MPRampException e){
        } catch (MParticleApiClient.MPThrottleException e) {
        } catch (SSLHandshakeException ssle){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "SSL handshake failed while preparing uploads - possible MITM attack detected.");
        } catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Error processing batch uploads in mParticle DB");
        }finally {
            if (readyUploadsCursor != null && !readyUploadsCursor.isClosed()){
                readyUploadsCursor.close();
            }

        }
        return processingSessionEnd;
    }

    /**
     * Delete messages if they're accepted (202), or 4xx, which means we're sending bad data.
     */
    boolean shouldDelete(int statusCode) {
        return statusCode != MParticleApiClient.HTTP_TOO_MANY_REQUESTS && (HttpStatus.SC_ACCEPTED == statusCode ||
                (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR));
    }

    /**
     * Method that is responsible for building an upload message to be sent over the wire.
     */
    MessageBatch createUploadMessage(JSONArray messagesArray, boolean history) throws JSONException {
        MessageBatch batchMessage = MessageBatch.create(mContext,
                messagesArray,
                history,
                getAppInfo(),
                getDeviceInfo(),
                mConfigManager,
                mPreferences,
                mApiClient.getCookies());
        addGCMHistory(batchMessage);
        return batchMessage;
    }

    /**
     * If the customer is using our GCM solution, query and append all of the history used for attribution.
     *
     */
    void addGCMHistory(MessageBatch uploadMessage) {
        Cursor gcmHistory = null;
        try {
            MParticleDatabase.deleteExpiredGcmMessages(db);
            gcmHistory = MParticleDatabase.getGcmHistory(db);
            if (gcmHistory.getCount() > 0) {
                JSONObject historyObject = new JSONObject();
                while (gcmHistory.moveToNext()) {
                    int contentId = gcmHistory.getInt(gcmHistory.getColumnIndex(MParticleDatabase.GcmMessageTable.CONTENT_ID));
                    if (contentId != MParticleDatabase.GcmMessageTable.PROVIDER_CONTENT_ID) {
                        int campaignId = gcmHistory.getInt(gcmHistory.getColumnIndex(MParticleDatabase.GcmMessageTable.CAMPAIGN_ID));
                        String campaignIdString = Integer.toString(campaignId);
                        long displayedDate = gcmHistory.getLong(gcmHistory.getColumnIndex(MParticleDatabase.GcmMessageTable.DISPLAYED_AT));
                        JSONObject campaignObject = historyObject.optJSONObject(campaignIdString);
                        //only append the latest pushes
                        if (campaignObject == null) {
                            campaignObject = new JSONObject();
                            campaignObject.put(Constants.MessageKey.PUSH_CONTENT_ID, contentId);
                            campaignObject.put(MessageKey.PUSH_CAMPAIGN_HISTORY_TIMESTAMP, displayedDate);
                            historyObject.put(campaignIdString, campaignObject);
                        }
                    }
                }
                uploadMessage.put(Constants.MessageKey.PUSH_CAMPAIGN_HISTORY, historyObject);
            }
        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.WARNING, e, "Error while building GCM campaign history");
        }finally {
            if (gcmHistory != null && !gcmHistory.isClosed()){
                gcmHistory.close();
            }
        }
    }

    /**
     * Generic method to insert a new upload,
     * either a regular message batch, or a session history.
     *
     * @param message
     */
    void dbInsertUpload(MessageBatch message) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.API_KEY, mApiKey);
        contentValues.put(UploadTable.CREATED_AT, message.optLong(MessageKey.TIMESTAMP, System.currentTimeMillis()));
        contentValues.put(UploadTable.MESSAGE, message.toString());
        db.insert(UploadTable.TABLE_NAME, null, contentValues);
    }

    /**
     * After a session history has been uploaded:
     * - delete all of the messages associated with that session.
     *
     * @param sessionId
     */
    int dbDeleteProcessedMessages(String sessionId) {
        String[] whereArgs = new String[]{sessionId};
        return db.delete(MessageTable.TABLE_NAME, MessageTable.SESSION_ID + " = ?", whereArgs);
    }

    /**
     * After an upload record has been created:
     * - if the message doesn't belong to a session, just removed it.
     * - otherwise mark the message as having been uploaded, thereby
     *   making it ready to be included in session history.
     *
     * @param lastMessageId
     */
    void dbMarkAsUploadedMessage(int lastMessageId) {
        //non-session messages can be deleted, they're not part of session history
        String messageId = Long.toString(lastMessageId);
        String[] whereArgs = new String[]{messageId};
        String whereClause = MParticleDatabase.getDeletableMessagesQuery() + " and (_id<=?)";
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, whereClause, whereArgs);

        whereArgs = new String[]{messageId};
        whereClause = "(_id<=?)";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.STATUS, Status.UPLOADED);
        int rowsupdated = db.update(MessageTable.TABLE_NAME, contentValues, whereClause, whereArgs);
    }

    /**
     * After an actually successful upload over the wire.
     *
     * @param id
     * @return number of rows deleted (should be 1)
     */
    int deleteUpload(int id) {
        String[] whereArgs = {Long.toString(id)};
        return db.delete(UploadTable.TABLE_NAME, "_id=?", whereArgs);
    }

    /**
     * Used by the test suite for mocking
     */
    void setApiClient(IMPApiClient apiClient) {
        mApiClient = apiClient;
    }

    public void setConnected(boolean connected){
        isNetworkConnected = connected;
    }

    public void fetchSegments(long timeout, String endpointId, SegmentListener listener) {
        new SegmentRetriever(audienceDB, mApiClient).fetchSegments(timeout, endpointId, listener);
    }

    JSONObject getDeviceInfo(){
        if (deviceInfo == null){
            deviceInfo = DeviceAttributes.collectDeviceInfo(mContext);
        }
        if (MPUtility.isGmsAdIdAvailable()) {
            try {
                com.google.android.gms.ads.identifier.AdvertisingIdClient.Info adInfo = com.google.android.gms.ads.identifier.AdvertisingIdClient.getAdvertisingIdInfo(mContext);
                if (!adInfo.isLimitAdTrackingEnabled()) {
                    deviceInfo.put(MessageKey.GOOGLE_ADV_ID, adInfo.getId());
                }
                deviceInfo.put(MessageKey.LIMIT_AD_TRACKING, adInfo.isLimitAdTrackingEnabled());
            }catch (Exception e){
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed while building device-info object: ", e.toString());
            }

        }
        return deviceInfo;
    }

    JSONObject getAppInfo(){
        if (appInfo == null){
            appInfo = DeviceAttributes.collectAppInfo(mContext);
        }
        try {
            appInfo.put(MessageKey.ENVIRONMENT, mConfigManager.getEnvironment().getValue());
            appInfo.put(MessageKey.INSTALL_REFERRER, mPreferences.getString(PrefKeys.INSTALL_REFERRER, null));
        }catch (JSONException e){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed while building app-info object: ", e.toString());
        }
        return appInfo;
    }
}
