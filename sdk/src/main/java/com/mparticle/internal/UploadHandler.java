package com.mparticle.internal;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.Constants.PrefKeys;
import com.mparticle.internal.Constants.Status;
import com.mparticle.internal.MParticleDatabase.CommandTable;
import com.mparticle.internal.MParticleDatabase.MessageTable;
import com.mparticle.internal.MParticleDatabase.UploadTable;
import com.mparticle.segmentation.SegmentListener;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

import javax.net.ssl.SSLHandshakeException;

/**
 * Primary queue handler which is responsible for querying, packaging, and uploading data.
 */
public final class UploadHandler extends Handler {

    private final Context mContext;
    private final MParticleDatabase mDbHelper;
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
     * The following get*Query methods were once static fields, but in order to save on app startup time, they're
     * now created as needed.
     */

    /**
     * The beginning of the delete query used to clear the uploads table after a successful upload.
     */
    private static String getDeletableMessagesQuery() {
        return String.format(
                "(%s='NO-SESSION')",
                MessageTable.SESSION_ID);
    }

    /**
     * Query to determine all of the generated uploads that are ready for the wire.
     */
    private static String getUploadableMessagesQuery() {
        return String.format(
                "((%s='NO-SESSION') or ((%s>=?) and (%s!=%d)))",
                MessageTable.SESSION_ID,
                MessageTable.STATUS,
                MessageTable.STATUS,
                Status.UPLOADED);
    }

    /**
     * Query to determine all the session history batches that are ready for the wire
     */
    private static String getSessionHistoryBatchesQuery() {
        return String.format(
                "((%s!='NO-SESSION') and ((%s>=?) and (%s=%d) and (%s != ?)))",
                MessageTable.SESSION_ID,
                MessageTable.STATUS,
                MessageTable.STATUS,
                Status.UPLOADED,
                MessageTable.SESSION_ID);
    }

    /**
     * Query used to clear session history uploads after a successful upload to the SDK server.
     */
    private static String getSqlFinishedHistoryMessagesQuery() {
        return String.format(
                "((%s='NO-SESSION') or ((%s>=?) and (%s=%d) and (%s=?)))",
                MessageTable.SESSION_ID,
                MessageTable.STATUS,
                MessageTable.STATUS,
                Status.UPLOADED,
                MessageTable.SESSION_ID);
    }

    /**
     * Boolean used to determine if we're currently connected to the network. If we're not connected to the network,
     * don't even try to query or upload, just shut down to save on battery life.
     */
    private volatile boolean isNetworkConnected = true;

    /**
     * Maintain a reference to these two objects as they primary do not change over the course of an app execution, so we
     * shouldn't be recreating them every time we need them.
     */
    private JSONObject deviceInfo;
    private JSONObject appInfo;

    public UploadHandler(Context context, Looper looper, ConfigManager configManager, MParticleDatabase database) {
        super(looper);
        mConfigManager = configManager;
        mContext = context;
        mApiKey = mConfigManager.getApiKey();

        audienceDB = new SegmentDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mDbHelper = database;
        try {
            setApiClient(new MParticleApiClient(configManager, mPreferences, context));
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        }
    }

    private JSONObject getDeviceInfo(){
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

    private JSONObject getAppInfo(){
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

    public void setConnected(boolean connected){
        isNetworkConnected = connected;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (db == null){
            try {
                db = mDbHelper.getWritableDatabase();
            }catch (Exception e){
                return;
            }
        }
        switch (msg.what) {
            case UPDATE_CONFIG:
                try {
                    mApiClient.fetchConfig();
                } catch (SSLHandshakeException ssle){
                        ConfigManager.log(MParticle.LogLevel.DEBUG, "SSL handshake failed while update configuration - possible MITM attack detected.");
                } catch (IOException ioe) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed to update configuration: ", ioe.toString());
                } catch (MParticleApiClient.MPThrottleException e) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, e.getMessage());
                } catch (MParticleApiClient.MPConfigException e) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed to update configuration: ", e.toString());
                } catch (Exception e){

                }
                break;
            case UPLOAD_MESSAGES:
            case UPLOAD_TRIGGER_MESSAGES:
                try {
                    long uploadInterval = mConfigManager.getUploadInterval();
                    if (isNetworkConnected && !mApiClient.isThrottled()) {
                        if (uploadInterval > 0 || msg.arg1 == 1) {
                            prepareUploads(false);
                            boolean needsHistory = processUploads(false);
                            if (needsHistory) {
                                this.sendEmptyMessage(UPLOAD_HISTORY);
                            }
                        }

                    }
                    if (MParticle.getInstance().internal().isSessionActive() && uploadInterval > 0 && msg.arg1 == 0) {
                        this.sendEmptyMessageDelayed(UPLOAD_MESSAGES, uploadInterval);
                    }
                }catch (Exception e){

                }
                break;
            case UPLOAD_HISTORY:
                Cursor cursor = null;
                try {
                    // if the uploads table is empty (no old uploads)
                    // and the messages table has messages that are not from the current session,
                    // or there is no current session
                    // then create a history upload and send it
                    cursor = db.rawQuery("select * from " + UploadTable.TABLE_NAME, null);
                    if ((cursor == null) || (cursor.getCount() == 0)) {
                        this.removeMessages(UPLOAD_HISTORY);
                        // execute all the upload steps
                        prepareUploads(true);
                        if (isNetworkConnected) {
                            processUploads(true);
                        }
                    }

                }catch (Exception e){

                }finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
                break;
            case INIT_CONFIG:
                try {
                    mConfigManager.delayedStart();
                }catch (Exception e){
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed to init configuration: ", e.toString());
                }
                break;
        }
    }



    String prepareOrderBy = MessageTable.CREATED_AT + ", " + MessageTable.SESSION_ID + " , _id asc";
    String[] prepareSelection = new String[]{"_id", MessageTable.MESSAGE, MessageTable.CREATED_AT, MessageTable.STATUS, MessageTable.SESSION_ID};
    String[] defaultSelectionArgs = new String[]{Integer.toString(Status.READY)};

    /**
     * This method is responsible for looking for messages that have been logged, and assembling them into batches to be uploaded.
     * It does not trigger network comms.
     */
    private void prepareUploads(boolean history) {
        Cursor readyMessagesCursor = null;
        try {
            // select messages ready to upload
            String selection;
            String[] selectionArgs;
            if (history) {
                selection = getSessionHistoryBatchesQuery();
                selectionArgs = new String[]{Integer.toString(Status.READY), MParticle.getInstance().internal().getSessionId()};
            } else {
                selection = getUploadableMessagesQuery();
                selectionArgs = defaultSelectionArgs;
            }

            readyMessagesCursor = db.query(
                    MessageTable.TABLE_NAME,
                    prepareSelection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    prepareOrderBy);

            if (readyMessagesCursor.getCount() > 0) {
                mApiClient.fetchConfig();
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Preparing " + readyMessagesCursor.getCount() + " events for upload");

                if (history) {
                    String currentSessionId;
                    int sessionIndex = readyMessagesCursor.getColumnIndex(MessageTable.SESSION_ID);
                    JSONArray messagesArray = new JSONArray();
                    boolean sessionEndFound = false;
                    while (readyMessagesCursor.moveToNext()) {
                        currentSessionId = readyMessagesCursor.getString(sessionIndex);
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));

                        if (msgObject.getString(MessageKey.TYPE).equals(MessageType.SESSION_END)) {
                            sessionEndFound = true;
                        }
                        messagesArray.put(msgObject);

                        if (readyMessagesCursor.isLast()) {
                            JSONObject uploadMessage = createUploadMessage(messagesArray, history);
                            // store in uploads table
                            if (sessionEndFound) {
                                dbInsertUpload(uploadMessage);
                                dbDeleteProcessedMessages(currentSessionId);
                            }
                        } else {
                            if (readyMessagesCursor.moveToNext() && !readyMessagesCursor.getString(sessionIndex).equals(currentSessionId)) {
                                JSONObject uploadMessage = createUploadMessage(messagesArray, history);
                                // store in uploads table
                                if (uploadMessage != null) {
                                    dbInsertUpload(uploadMessage);
                                    dbDeleteProcessedMessages(currentSessionId);
                                }
                                messagesArray = new JSONArray();
                            }
                            readyMessagesCursor.moveToPrevious();
                        }
                    }
                } else {
                    JSONArray messagesArray = new JSONArray();
                    int lastMessageId = 0;
                    while (readyMessagesCursor.moveToNext()) {
                        JSONObject msgObject = new JSONObject(readyMessagesCursor.getString(1));
                        messagesArray.put(msgObject);
                        lastMessageId = readyMessagesCursor.getInt(0);
                    }
                    JSONObject uploadMessage = createUploadMessage(messagesArray, history);
                    // store in uploads table
                    dbInsertUpload(uploadMessage);
                    dbMarkAsUploadedMessage(lastMessageId);

                }
            }
        } catch (SSLHandshakeException ssle){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "SSL handshake failed while fetching configuration during upload preparation - possible MITM attack detected.");
        } catch (MParticleApiClient.MPThrottleException e) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, e.getMessage());
        }  catch (MParticleApiClient.MPConfigException e) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, e.getMessage());
        } catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, "Error preparing batch upload in mParticle DB: " + e.getMessage());
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
    private boolean processUploads(boolean history) {
        boolean processingSessionEnd = false;
        Cursor readyUploadsCursor = null;
        try {
            readyUploadsCursor = db.query(UploadTable.TABLE_NAME, uploadColumns,
                    null, null, null, null, UploadTable.CREATED_AT);

            while (readyUploadsCursor.moveToNext()) {
                int id = readyUploadsCursor.getInt(0);
                String message = readyUploadsCursor.getString(1);
                if (!history) {
                    // if message is the MessageType.SESSION_END, then remember so the session history can be triggered
                    if (message.contains(containsClause)) {
                        processingSessionEnd = true;
                    }
                }

                HttpURLConnection connection = mApiClient.sendMessageBatch(message);

                if (connection != null && shouldDelete(connection.getResponseCode())) {
                    dbDeleteUpload(id);
                    try {
                        JSONObject jsonObject = MParticleApiClient.getJsonResponse(connection);
                        if (jsonObject != null &&
                                jsonObject.has(MessageKey.MESSAGES)) {
                            JSONArray responseCommands = jsonObject.getJSONArray(MessageKey.MESSAGES);
                            for (int i = 0; i < responseCommands.length(); i++) {
                                JSONObject commandObject = responseCommands.getJSONObject(i);
                                dbInsertCommand(commandObject);
                            }
                        }

                    } catch (JSONException e) {
                        // ignore problems parsing response commands
                    }
                } else {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Upload failed and will be retried.");
                }
            }
        } catch (MParticleApiClient.MPRampException e){
        } catch (MParticleApiClient.MPThrottleException e) {
            //ConfigManager.log(MParticle.LogLevel.DEBUG, e.getMessage());
        } catch (SSLHandshakeException ssle){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "SSL handshake failed while preparing uploads - possible MITM attack detected.");
        } catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error processing batch uploads in mParticle DB");
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
        return HttpStatus.SC_ACCEPTED == statusCode ||
                (statusCode >= HttpStatus.SC_BAD_REQUEST && statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Primarily deprecated functionality that lets the server tell the SDK to make requests to 3rd parties.
     * Once upon a time this was required for Google Analytics, but is no longer used for any service provider.
     */
    private void processCommands() {
        try {

            String[] selectionColumns = new String[]{"_id", CommandTable.URL, CommandTable.METHOD,
                    CommandTable.POST_DATA, CommandTable.HEADERS};
            Cursor commandsCursor = db.query(CommandTable.TABLE_NAME, selectionColumns,
                    null, null, null, null, "_id");
            while (commandsCursor.moveToNext()) {
                int id = commandsCursor.getInt(0);
                String commandUrl = commandsCursor.getString(1);
                String method = commandsCursor.getString(2);
                String postData = commandsCursor.getString(3);
                String headers = commandsCursor.getString(4);

                HttpURLConnection connection = null;
                int responseCode = -1;
                try {
                    connection = mApiClient.sendCommand(commandUrl, method, postData, headers);
                    responseCode = connection.getResponseCode();
                } catch (Throwable t) {
                    // fail silently. a message will be logged if debug mode is enabled
                } finally {
                    if (connection != null && responseCode > -1) {
                        dbDeleteCommand(id);
                    } else {
                        ConfigManager.log(MParticle.LogLevel.DEBUG, "Provider command processing failed and will be retried.");
                    }
                }
            }
            commandsCursor.close();
        } catch (SQLiteException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, e, "Error processing provider command uploads in mParticle DB");
        } finally {

        }
    }

    /**
     * Method that is responsible for building an upload message to be sent over the wire.
     */
    private JSONObject createUploadMessage(JSONArray messagesArray, boolean history) throws JSONException {
        JSONObject batchMessage = MessageBatch.create(mContext,
                messagesArray,
                history,
                getAppInfo(),
                getDeviceInfo(),
                mConfigManager,
                mPreferences);
        addGCMHistory(batchMessage);
        return batchMessage;
    }

    String gcmDeleteWhere = MParticleDatabase.GcmMessageTable.EXPIRATION + " < ? and " + MParticleDatabase.GcmMessageTable.DISPLAYED_AT + " > 0";
    String[] gcmColumns = {MParticleDatabase.GcmMessageTable.CONTENT_ID, MParticleDatabase.GcmMessageTable.CAMPAIGN_ID, MParticleDatabase.GcmMessageTable.EXPIRATION, MParticleDatabase.GcmMessageTable.DISPLAYED_AT};

    /**
     * If the customer is using our GCM solution, query and append all of the history used for attribution.
     *
     */
    private void addGCMHistory(JSONObject uploadMessage) {
        //first remove expired
        Cursor gcmHistory = null;
        try {
            String[] deleteWhereArgs = {Long.toString(System.currentTimeMillis())};
            db.delete(MParticleDatabase.GcmMessageTable.TABLE_NAME, gcmDeleteWhere, deleteWhereArgs);

            gcmHistory = db.query(MParticleDatabase.GcmMessageTable.TABLE_NAME,
                    gcmColumns,
                    null,
                    null,
                    null,
                    null,
                    MParticleDatabase.GcmMessageTable.EXPIRATION + " desc"); //this order to necessary so that we only append the latest push messages
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

    private void dbInsertUpload(JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTable.API_KEY, mApiKey);
        contentValues.put(UploadTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(UploadTable.MESSAGE, message.toString());
        db.insert(UploadTable.TABLE_NAME, null, contentValues);
    }

    private void dbDeleteProcessedMessages(String sessionId) {
        String[] whereArgs = new String[]{Integer.toString(Status.UPLOADED), sessionId};
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, UploadHandler.getSqlFinishedHistoryMessagesQuery(), whereArgs);
    }

    private void dbMarkAsUploadedMessage(int lastMessageId) {
        //non-session messages can be deleted, they're not part of session history
        String[] whereArgs = new String[]{Long.toString(lastMessageId)};
        String whereClause = getDeletableMessagesQuery() + " and (_id<=?)";
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, whereClause, whereArgs);

        whereArgs = new String[]{Integer.toString(Status.READY), Long.toString(lastMessageId)};
        whereClause = getUploadableMessagesQuery() + " and (_id<=?)";
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.STATUS, Status.UPLOADED);
        int rowsupdated = db.update(MessageTable.TABLE_NAME, contentValues, whereClause, whereArgs);
    }

    private void dbDeleteUpload(int id) {
        String[] whereArgs = {Long.toString(id)};
        int rowsdeleted = db.delete(UploadTable.TABLE_NAME, "_id=?", whereArgs);
    }

    private void dbDeleteCommand(int id) {
        String[] whereArgs = {Long.toString(id)};
        db.delete(CommandTable.TABLE_NAME, "_id=?", whereArgs);
    }

    private void dbInsertCommand(JSONObject command) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CommandTable.URL, command.getString(MessageKey.URL));
        contentValues.put(CommandTable.METHOD, command.getString(MessageKey.METHOD));
        contentValues.put(CommandTable.POST_DATA, command.optString(MessageKey.POST));
        contentValues.put(CommandTable.HEADERS, command.optString(MessageKey.HEADERS));
        contentValues.put(CommandTable.CREATED_AT, System.currentTimeMillis());
        contentValues.put(CommandTable.API_KEY, mApiKey);
        db.insert(CommandTable.TABLE_NAME, null, contentValues);
    }

    /**
     * Used by the test suite for mocking
     */
    public void setApiClient(IMPApiClient apiClient) {
        mApiClient = apiClient;
    }

    public void fetchSegments(long timeout, String endpointId, SegmentListener listener) {
        new SegmentRetriever(audienceDB, mApiClient).fetchSegments(timeout, endpointId, listener);
    }
}
