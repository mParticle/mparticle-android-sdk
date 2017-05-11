package com.mparticle.internal;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.Constants.Status;
import com.mparticle.internal.MParticleDatabase.MessageTable;
import com.mparticle.internal.MParticleDatabase.UploadTable;
import com.mparticle.segmentation.SegmentListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
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
     *
     * Only used for unit testing
     */
    UploadHandler(Context context, ConfigManager configManager, MParticleDatabase database, AppStateManager appStateManager, MessageManager messageManager) {
        mConfigManager = configManager;
        mContext = context;
        mApiKey = mConfigManager.getApiKey();
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDbHelper = database;
        mMessageManager = messageManager;
        try {
            setApiClient(new MParticleApiClientImpl(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        } catch (MParticleApiClientImpl.MPNoConfigException e) {
            Logger.error("Unable to process uploads, API key and/or API Secret are missing");
        }
    }


    public UploadHandler(Context context, Looper looper, ConfigManager configManager, MParticleDatabase database, AppStateManager appStateManager, MessageManager messageManager) {
        super(looper);
        mConfigManager = configManager;
        mContext = context;
        mApiKey = mConfigManager.getApiKey();
        mAppStateManager = appStateManager;
        audienceDB = new SegmentDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDbHelper = database;
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
            if (db == null){
                db = mDbHelper.getWritableDatabase();
            }
            switch (msg.what) {
                case UPDATE_CONFIG:
                    MParticle.getInstance().getKitManager().loadKitLibrary();
                    mApiClient.fetchConfig(true);
                    break;
                case INIT_CONFIG:
                    mConfigManager.delayedStart();
                    break;
                case UPLOAD_MESSAGES:
                case UPLOAD_TRIGGER_MESSAGES:
                    long uploadInterval = mConfigManager.getUploadInterval();
                    if (isNetworkConnected && !mApiClient.isThrottled()) {
                        if (uploadInterval > 0 || msg.arg1 == 1) {
                            prepareMessageUploads(false);
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
                    prepareMessageUploads(true);
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
    private void prepareMessageUploads(boolean history) throws Exception {
        Cursor readyMessagesCursor = null, reportingMessageCursor = null, sessionCursor = null;
        String currentSessionId = mAppStateManager.getSession().mSessionID;
        long remainingHeap = MPUtility.getRemainingHeapInBytes();
        if (remainingHeap < Constants.LIMIT_MAX_UPLOAD_SIZE) {
            throw new Exception("Low remaining heap space, deferring uploads.");
        }
        final boolean sessionHistoryEnabled = MParticle.getInstance().getConfigManager().getIncludeSessionHistory();
        try {
            MParticleDatabase.cleanupMessages(db);
            db.beginTransaction();
            if (history && !sessionHistoryEnabled){
                MParticleDatabase.deleteOldMessages(db, currentSessionId);
                MParticleDatabase.deleteOldSessions(db, currentSessionId);
                db.setTransactionSuccessful();
                return;
            }

            if (history) {
                readyMessagesCursor = MParticleDatabase.getSessionHistory(db, currentSessionId);
            } else {
                readyMessagesCursor = MParticleDatabase.getMessagesForUpload(db);
            }
            if (readyMessagesCursor.getCount() <= 0) {
                return;
            }
            int messageIdIndex = readyMessagesCursor.getColumnIndex(MessageTable._ID);
            int messageIndex = readyMessagesCursor.getColumnIndex(MessageTable.MESSAGE);
            int sessionIdIndex = readyMessagesCursor.getColumnIndex(MessageTable.SESSION_ID);
            HashMap<String, MessageBatch> uploadMessagesBySession = new HashMap<String, MessageBatch>(2);
            int highestUploadedMessageId = 0;
            while (readyMessagesCursor.moveToNext()) {
                String sessionId = readyMessagesCursor.getString(sessionIdIndex);
                int messageId = readyMessagesCursor.getInt(messageIdIndex);
                MessageBatch uploadMessage = uploadMessagesBySession.get(sessionId);
                if (uploadMessage == null) {
                    uploadMessage = createUploadMessage(history);
                    uploadMessagesBySession.put(sessionId, uploadMessage);
                }
                String message = readyMessagesCursor.getString(messageIndex);
                int messageLength = message.length();
                JSONObject msgObject = new JSONObject(message);
                if (messageLength + uploadMessage.getMessageLengthBytes() > Constants.LIMIT_MAX_UPLOAD_SIZE) {
                    break;
                }

                if (history) {
                    uploadMessage.addSessionHistoryMessage(msgObject);
                } else {
                    uploadMessage.addMessage(msgObject);
                }
                uploadMessage.incrementMessageLengthBytes(messageLength);
                highestUploadedMessageId = messageId;
            }
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }

            if (!sessionHistoryEnabled || history) {
                //if this is a session-less message, or if session history is disabled, just delete it
                int deleted = dbDeleteMessages(highestUploadedMessageId);
            } else {
                //else mark the messages as uploaded, so next time around it'll be included in session history
                dbMarkMessagesAsUploaded(highestUploadedMessageId);
            }

            //add reporting information for this session
            if (!history) {
                reportingMessageCursor = MParticleDatabase.getReportingMessagesForUpload(db);
                int reportingMessageIdIndex = reportingMessageCursor.getColumnIndex(MParticleDatabase.ReportingTable._ID);
                while (reportingMessageCursor.moveToNext()) {
                    JSONObject msgObject = new JSONObject(
                            reportingMessageCursor.getString(
                                    reportingMessageCursor.getColumnIndex(MParticleDatabase.ReportingTable.MESSAGE)
                            )
                    );
                    String sessionId = reportingMessageCursor.getString(
                            reportingMessageCursor.getColumnIndex(MParticleDatabase.ReportingTable.SESSION_ID)
                    );
                    int reportingMessageId = reportingMessageCursor.getInt(reportingMessageIdIndex);
                    MessageBatch batch = uploadMessagesBySession.get(sessionId);
                    if (batch == null) {
                        //if there's no matching session id then just use the first batch object
                        batch = uploadMessagesBySession.values().iterator().next();
                    }
                    if (batch != null) {
                        batch.addReportingMessage(msgObject);
                    }
                    dbMarkAsUploadedReportingMessage(reportingMessageId);
                }
            }

            sessionCursor = MParticleDatabase.getSessions(db);
            while(sessionCursor.moveToNext()) {
                String sessionId = sessionCursor.getString(sessionCursor.getColumnIndex(MParticleDatabase.SessionTable.SESSION_ID));
                MessageBatch batch = uploadMessagesBySession.get(sessionId);
                if (batch != null) {
                    try {
                        String appInfo = sessionCursor.getString(sessionCursor.getColumnIndex(MParticleDatabase.SessionTable.APP_INFO));
                        JSONObject appInfoJson = new JSONObject(appInfo);
                        batch.setAppInfo(appInfoJson);
                        String deviceInfo = sessionCursor.getString(sessionCursor.getColumnIndex(MParticleDatabase.SessionTable.DEVICE_INFO));
                        JSONObject deviceInfoJson = new JSONObject(deviceInfo);
                        mMessageManager.getDeviceAttributes().updateDeviceInfo(mContext, deviceInfoJson);
                        batch.setDeviceInfo(deviceInfoJson);
                    }catch (Exception e) {

                    }
                }
            }

            for (Map.Entry<String, MessageBatch> session : uploadMessagesBySession.entrySet()) {
                MessageBatch uploadMessage = session.getValue();
                if (uploadMessage != null) {
                    String sessionId = session.getKey();
                    //for upgrade scenarios, there may be no device or app info associated with the session, so create it now.
                    if (uploadMessage.getAppInfo() == null) {
                        uploadMessage.setAppInfo(mMessageManager.getDeviceAttributes().getAppInfo(mContext));
                    }
                    if (uploadMessage.getDeviceInfo() == null || sessionId.equals(currentSessionId)) {
                        uploadMessage.setDeviceInfo(mMessageManager.getDeviceAttributes().getDeviceInfo(mContext));
                    }
                    JSONArray messages = history ? uploadMessage.getSessionHistoryMessages() : uploadMessage.getMessages();
                    JSONArray identities = findIdentityState(messages);
                    uploadMessage.setIdentities(identities);
                    JSONObject userAttributes = findUserAttributeState(messages);
                    uploadMessage.setUserAttributes(userAttributes);
                    dbInsertUpload(uploadMessage);
                    //if this was to process session history, or
                    //if we're never going to process history AND
                    //this batch contains a previous session, then delete the session
                    if (history || (!sessionHistoryEnabled && !sessionId.equals(currentSessionId))) {
                        db.delete(MParticleDatabase.SessionTable.TABLE_NAME, MParticleDatabase.SessionTable.SESSION_ID + "=?", new String[]{sessionId});
                    }
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e){
            Logger.verbose("Error preparing batch upload in mParticle DB: " + e.getMessage());
        } finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }
            if (reportingMessageCursor != null && !reportingMessageCursor.isClosed()){
                reportingMessageCursor.close();
            }
            if (sessionCursor != null && !sessionCursor.isClosed()){
                sessionCursor.close();
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

    String[] uploadColumns = new String[]{"_id", UploadTable.MESSAGE};
    String containsClause = "\"" + MessageKey.TYPE + "\":\"" + MessageType.SESSION_END + "\"";

    /**
     * This method is responsible for looking for batches that are ready to be uploaded, and uploading them.
     */
    boolean upload(boolean history) {
        MParticleDatabase.cleanupUploadMessages(db);
        boolean processingSessionEnd = false;
        Cursor readyUploadsCursor = null;
        try {
            readyUploadsCursor = db.query(UploadTable.TABLE_NAME, uploadColumns,
                    null, null, null, null, UploadTable.CREATED_AT);
            int messageIdIndex = readyUploadsCursor.getColumnIndex(UploadTable._ID);
            int messageIndex = readyUploadsCursor.getColumnIndex(UploadTable.MESSAGE);
            if (readyUploadsCursor.getCount() > 0) {
                mApiClient.fetchConfig();
            }
            final boolean includeSessionHistory = mConfigManager.getIncludeSessionHistory();
            while (readyUploadsCursor.moveToNext()) {
                int id = readyUploadsCursor.getInt(messageIdIndex);
                //this case actually shouldn't be needed anymore except for upgrade scenarios.
                //as of version 4.9.0, upload batches for session history shouldn't even be created.
                if (history && !includeSessionHistory){
                    deleteUpload(id);
                } else {
                    String message = readyUploadsCursor.getString(messageIndex);
                    if (!history) {
                        // if message is the MessageType.SESSION_END, then remember so the session history can be triggered
                        if (!processingSessionEnd && message.contains(containsClause)) {
                            processingSessionEnd = true;
                        }
                    }
                    uploadMessage(id, message);
                }
            }
        } catch (MParticleApiClientImpl.MPThrottleException e) {
        } catch (SSLHandshakeException ssle){
            Logger.debug("SSL handshake failed while preparing uploads - possible MITM attack detected.");
        } catch (MParticleApiClientImpl.MPConfigException e) {
            Logger.error("Bad API request - is the correct API key and secret configured?");
        } catch (Exception e){
            Logger.error(e, "Error processing batch uploads in mParticle DB");
        } finally {
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
            deleteUpload(id);
        } else {
            Logger.warning("Upload failed and will be retried.");
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
     * Look for the last UAC message to find the end-state of user attributes
     */
    private JSONObject findUserAttributeState(JSONArray messages) {
        JSONObject userAttributes = null;
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                try {
                    if (messages.getJSONObject(i).get(Constants.MessageKey.TYPE).equals(MessageType.USER_ATTRIBUTE_CHANGE)) {
                        userAttributes = messages.getJSONObject(i).getJSONObject(MessageKey.USER_ATTRIBUTES);
                        messages.getJSONObject(i).remove(MessageKey.USER_ATTRIBUTES);
                    }
                }catch (JSONException jse) {

                }catch (NullPointerException npe) {

                }
            }
        }
        if (userAttributes == null) {
            return getAllUserAttributes();
        } else {
            return userAttributes;
        }
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
            Logger.warning(e, "Error while building GCM campaign history");
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
     * Delete a message that has been uploaded in session history
     */
    int dbDeleteMessages(int messageId) {
        String[] whereArgs = new String[]{Integer.toString(messageId)};
        return db.delete(MessageTable.TABLE_NAME, MessageTable._ID + " <= ?", whereArgs);
    }

    void dbMarkMessagesAsUploaded(int messageId) {
        String[] whereArgs = new String[]{Integer.toString(messageId)};
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.STATUS, Status.UPLOADED);
        int rowsupdated = db.update(MessageTable.TABLE_NAME, contentValues, MessageTable._ID + " <= ?", whereArgs);
    }

    /**
     * Delete reporting messages after they've been included in an upload message.
     *
     */
    void dbMarkAsUploadedReportingMessage(int lastMessageId) {
        String messageId = Long.toString(lastMessageId);
        String[] whereArgs = new String[]{messageId};
        String whereClause = "_id =?";
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
