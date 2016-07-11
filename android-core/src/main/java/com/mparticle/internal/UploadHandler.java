package com.mparticle.internal;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.Constants.PrefKeys;
import com.mparticle.internal.Constants.Status;
import com.mparticle.internal.MParticleDatabase.MessageTable;
import com.mparticle.internal.MParticleDatabase.UploadTable;
import com.mparticle.segmentation.SegmentListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

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
    private MParticleApiClient mApiClient;

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
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
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
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
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
                    MParticle.getInstance().getKitManager().loadKitLibrary();
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

        } catch (Exception e){
            if (BuildConfig.MP_DEBUG){
                ConfigManager.log(MParticle.LogLevel.DEBUG, "UploadHandler Exception while handling message: " + e.toString());
            }
        } catch (VerifyError ve) {
            if (BuildConfig.MP_DEBUG){
                ConfigManager.log(MParticle.LogLevel.DEBUG, "UploadHandler VerifyError while handling message" + ve.toString());
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
        Cursor readyMessagesCursor = null, reportingMessageCursor = null;
        try {
            db.beginTransaction();
            readyMessagesCursor = MParticleDatabase.getMessagesForUpload(db);
            reportingMessageCursor = MParticleDatabase.getReportingMessagesForUpload(db);
            if (readyMessagesCursor.getCount() > 0 || reportingMessageCursor.getCount() > 0) {
                mApiClient.fetchConfig();
                int messageIdIndex = readyMessagesCursor.getColumnIndex(MessageTable._ID);
                int messageIndex = readyMessagesCursor.getColumnIndex(MessageTable.MESSAGE);
                int reportingMessageIndex = reportingMessageCursor.getColumnIndex(MParticleDatabase.ReportingTable.MESSAGE);
                int reportingIdMessageIndex = reportingMessageCursor.getColumnIndex(MParticleDatabase.ReportingTable._ID);
                JSONObject userAttributesJson = getAllUserAttributes();
                while (!readyMessagesCursor.isAfterLast() || !reportingMessageCursor.isAfterLast()) {
                    int highestMessageId = 0;
                    int highestReportingMessageId = 0;
                    JSONArray messagesArray = new JSONArray();
                    while (messagesArray.length() <= Constants.BATCH_LIMIT && readyMessagesCursor.moveToNext()) {
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(messageIndex));
                        messagesArray.put(msgObject);
                        highestMessageId = readyMessagesCursor.getInt(messageIdIndex);
                    }
                    JSONArray reportingMessagesArray = new JSONArray();
                    while (reportingMessagesArray.length() <= Constants.BATCH_LIMIT && reportingMessageCursor.moveToNext()) {
                        JSONObject msgObject = new JSONObject(reportingMessageCursor.getString(reportingMessageIndex));
                        reportingMessagesArray.put(msgObject);
                        highestReportingMessageId = reportingMessageCursor.getInt(reportingIdMessageIndex);
                    }
                    MessageBatch uploadMessage = createUploadMessage(messagesArray, reportingMessagesArray, false, userAttributesJson);
                    dbInsertUpload(uploadMessage);
                    if (highestMessageId > 0) {
                        dbMarkAsUploadedMessage(highestMessageId);
                    }
                    if (highestReportingMessageId > 0) {
                        dbMarkAsUploadedReportingMessage(highestReportingMessageId);
                    }
                }
                db.setTransactionSuccessful();
            }
        } catch (Exception e){
            if (BuildConfig.MP_DEBUG) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Error preparing batch upload in mParticle DB: " + e.getMessage());
            }
        } finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }
            if (reportingMessageCursor != null && !reportingMessageCursor.isClosed()){
                reportingMessageCursor.close();
            }
            db.endTransaction();
        }
    }

    static JSONObject getAllUserAttributes()  {
        Map<String, Object> attributes = MParticle.getInstance().getAllUserAttributes();
        JSONObject jsonAttributes = new JSONObject();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Object value = entry.getValue();
            if (entry.getValue() instanceof List) {
                List<String> attributeList = (List<String>)value;
                JSONArray jsonArray = new JSONArray();
                for (String attribute : attributeList) {
                    jsonArray.put(attribute);
                }
                try {
                    jsonAttributes.put(entry.getKey(), jsonArray);
                } catch (JSONException e) {

                }
            }else {
                try {
                    Object entryValue = entry.getValue();
                    if (entryValue == null) {
                        entryValue = JSONObject.NULL;
                    }
                    jsonAttributes.put(entry.getKey(), entryValue);
                } catch (JSONException e) {

                }
            }
        }
        return jsonAttributes;
    }

    /**
     * - Query all messages that have been uploaded that are not for the current session
     * - Group each message by session and create a JSON session-history message, insert as an upload
     * - Delete all the original messages.
     */
    void prepareHistoryUploads(){
        Cursor readyMessagesCursor = null;
        try {
            db.beginTransaction();
            readyMessagesCursor = MParticleDatabase.getSessionHistory(db, mAppStateManager.getSession().mSessionID);
            if (readyMessagesCursor.getCount() > 0) {
                mApiClient.fetchConfig();
                int sessionIndex = readyMessagesCursor.getColumnIndex(MessageTable.SESSION_ID);
                int messageIndex = readyMessagesCursor.getColumnIndex(MessageTable.MESSAGE);
                JSONArray messagesArray = new JSONArray();
                String lastSessionId = null;
                JSONObject attributes = getAllUserAttributes();
                while (readyMessagesCursor.moveToNext()) {
                    String currentSessionId = readyMessagesCursor.getString(sessionIndex);
                    MPMessage message = new MPMessage(readyMessagesCursor.getString(messageIndex));
                    boolean sameSession = lastSessionId == null || lastSessionId.equals(currentSessionId);
                    if (sameSession){
                        messagesArray.put(message);
                    }
                    if (!sameSession || readyMessagesCursor.isLast()) {
                        MessageBatch uploadMessage = createUploadMessage(messagesArray, null, true, attributes);
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
            db.setTransactionSuccessful();
        } catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Error preparing batch upload in mParticle DB: " + e.getMessage());
        } finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }
            db.endTransaction();
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
                uploadMessage(id, message);
            }
        } catch (MParticleApiClientImpl.MPThrottleException e) {
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

    void uploadMessage(int id, String message) throws IOException, MParticleApiClientImpl.MPThrottleException {
        int responseCode = -1;
        boolean sampling = false;
        try {
            responseCode = mApiClient.sendMessageBatch(message);
        }catch (MParticleApiClientImpl.MPRampException e) {
            sampling = true;
            ConfigManager.log(MParticle.LogLevel.DEBUG, "This device is being sampled.");
        }

        if (sampling || shouldDelete(responseCode)) {
            deleteUpload(id);
        } else {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Upload failed and will be retried.");
        }
    }

    /**
     * Delete messages if they're accepted (202), or 4xx, which means we're sending bad data.
     */
    boolean shouldDelete(int statusCode) {
        return statusCode != MParticleApiClientImpl.HTTP_TOO_MANY_REQUESTS && (202 == statusCode ||
                (statusCode >= 400 && statusCode < 500));
    }

    /**
     * Method that is responsible for building an upload message to be sent over the wire.
     */
    MessageBatch createUploadMessage(JSONArray messagesArray, JSONArray reportingMessagesArray, boolean history, JSONObject userAttributes) throws JSONException {
        MessageBatch batchMessage = MessageBatch.create(mContext,
                messagesArray,
                reportingMessagesArray,
                history,
                getAppInfo(),
                getDeviceInfo(),
                mConfigManager,
                mPreferences,
                mApiClient.getCookies(), userAttributes);
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
     * Delete reporting messages after they've been included in an upload message.
     *
     */
    void dbMarkAsUploadedReportingMessage(int lastMessageId) {
        String messageId = Long.toString(lastMessageId);
        String[] whereArgs = new String[]{messageId};
        String whereClause = "_id<=?";
        int rowsdeleted = db.delete(MParticleDatabase.ReportingTable.TABLE_NAME, whereClause, whereArgs);
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
    void setApiClient(MParticleApiClient apiClient) {
        mApiClient = apiClient;
    }

    public void setConnected(boolean connected){
        isNetworkConnected = connected;
        try {
            if (isNetworkConnected && mConfigManager.isPushEnabled() && PushRegistrationHelper.getLatestPushRegistration(mContext) == null) {
                MParticle.getInstance().Messaging().enablePushNotifications(mConfigManager.getPushSenderId());
            }
        }catch (Exception e) {

        }
    }

    public void fetchSegments(long timeout, String endpointId, SegmentListener listener) {
        new SegmentRetriever(audienceDB, mApiClient).fetchSegments(timeout, endpointId, listener);
    }

    JSONObject getDeviceInfo(){
        if (deviceInfo == null){
            deviceInfo = DeviceAttributes.collectDeviceInfo(mContext);
        }
        return deviceInfo;
    }

    JSONObject getAppInfo() {
        //keep the appInfo object in memory as its values will not change
        boolean firstUpload = false;
        if (appInfo == null) {
            firstUpload = true;
            appInfo = DeviceAttributes.collectAppInfo(mContext);
        }
        //the following are collected at the time of batch creation as they may change
        try {
            appInfo.put(MessageKey.ENVIRONMENT, mConfigManager.getEnvironment().getValue());
            appInfo.put(MessageKey.INSTALL_REFERRER, mPreferences.getString(PrefKeys.INSTALL_REFERRER, null));
            appInfo.remove(MessageKey.LIMIT_AD_TRACKING);
            appInfo.remove(MessageKey.GOOGLE_ADV_ID);
            MPUtility.AndroidAdIdInfo adIdInfo = MPUtility.getGoogleAdIdInfo(mContext);
            String message = "Failed to collect Google Play Advertising ID, be sure to add Google Play services or com.google.android.gms:play-services-ads to your app's dependencies.";
            if (adIdInfo != null) {
                appInfo.put(MessageKey.LIMIT_AD_TRACKING, adIdInfo.isLimitAdTrackingEnabled);
                if (adIdInfo.isLimitAdTrackingEnabled && mConfigManager.getRestrictAAIDBasedOnLAT()) {
                    message = "Google Play Advertising ID available but ad tracking is disabled on this device.";
                } else {
                    appInfo.put(MessageKey.GOOGLE_ADV_ID, adIdInfo.id);
                    message = "Successfully collected Google Play Advertising ID.";
                }
            }
            if (firstUpload) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, message);
            }
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed while building app-info object: ", e.toString());
        }
        return appInfo;
    }
}
