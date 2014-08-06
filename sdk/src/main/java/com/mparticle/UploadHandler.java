package com.mparticle;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.PrefKeys;
import com.mparticle.Constants.Status;
import com.mparticle.MParticleDatabase.CommandTable;
import com.mparticle.MParticleDatabase.MessageTable;
import com.mparticle.MParticleDatabase.SessionTable;
import com.mparticle.MParticleDatabase.UploadTable;
import com.mparticle.segmentation.Segment;
import com.mparticle.segmentation.SegmentListener;
import com.mparticle.segmentation.SegmentMembership;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* package-private */final class UploadHandler extends Handler {

    public static final int UPLOAD_MESSAGES = 1;
    public static final int CLEANUP = 2;
    public static final int UPLOAD_HISTORY = 3;
    public static final int UPDATE_CONFIG = 4;
    private static final String TAG = Constants.LOG_TAG;
    private final SQLiteDatabase db;
    private final SharedPreferences mPreferences;
    private final Context mContext;
    private final String mApiKey;
    private final SegmentDatabase audienceDB;
    private MParticleApiClient mApiClient;

    private ConfigManager mConfigManager;

    public static final String SQL_DELETABLE_MESSAGES = String.format(
            "(%s='NO-SESSION')",
            MessageTable.SESSION_ID);
    public static final String SQL_UPLOADABLE_MESSAGES = String.format(
            "((%s='NO-SESSION') or ((%s>=?) and (%s!=%d)))",
            MessageTable.SESSION_ID,
            MessageTable.STATUS,
            MessageTable.STATUS,
            Status.UPLOADED);
    public static final String SQL_HISTORY_MESSAGES = String.format(
            "((%s='NO-SESSION') or ((%s>=?) and (%s=%d) and (%s != ?)))",
            MessageTable.SESSION_ID,
            MessageTable.STATUS,
            MessageTable.STATUS,
            Status.UPLOADED,
            MessageTable.SESSION_ID);
    public static final String SQL_FINISHED_HISTORY_MESSAGES = String.format(
            "((%s='NO-SESSION') or ((%s>=?) and (%s=%d) and (%s=?)))",
            MessageTable.SESSION_ID,
            MessageTable.STATUS,
            MessageTable.STATUS,
            Status.UPLOADED,
            MessageTable.SESSION_ID);
    private volatile boolean isNetworkConnected = true;
    private JSONObject deviceInfo;
    private JSONObject appInfo;

    public UploadHandler(Context context, Looper looper, ConfigManager configManager, SQLiteDatabase database) {
        super(looper);
        mConfigManager = configManager;

        mContext = context.getApplicationContext();
        mApiKey = mConfigManager.getApiKey();

        db = database;
        audienceDB = new SegmentDatabase(mContext);
        mPreferences = mContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);

        try {
            mApiClient = new MParticleApiClient(configManager, mApiKey, mConfigManager.getApiSecret(), mPreferences);
        } catch (MalformedURLException e) {
            //this should never happen - the URLs are created by constants.
        }
    }

    private JSONObject getDeviceInfo(){
        if (deviceInfo == null){
            deviceInfo = DeviceAttributes.collectDeviceInfo(mContext);
        }
        return deviceInfo;
    }

    private JSONObject getAppInfo(){
        if (appInfo == null){
            appInfo = DeviceAttributes.collectAppInfo(mContext);
        }
        try {
            appInfo.put(MessageKey.ENVIRONMENT, mConfigManager.getEnvironment().getValue());
        }catch (JSONException e){

        }
        return appInfo;
    }

    public void setConnected(boolean connected){
        isNetworkConnected = connected;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
            case UPDATE_CONFIG:
                try {
                    mApiClient.fetchConfig();
                } catch (IOException ioe) {

                } catch (MParticleApiClient.MPThrottleException e) {
                    mConfigManager.debugLog(e.getMessage());
                }
                break;
            case UPLOAD_MESSAGES:
                boolean needsHistory;
                // execute all the upload steps
                long uploadInterval = mConfigManager.getUploadInterval();
                if (uploadInterval > 0 || msg.arg1 == 1) {
                    prepareUploads(false);
                    if (isNetworkConnected) {
                        needsHistory = processUploads(false);
                        processCommands();
                        if (needsHistory) {
                            this.sendEmptyMessage(UPLOAD_HISTORY);
                        }
                    }
                }
                // trigger another upload check unless configured for manual uploads
                if (uploadInterval > 0 && msg.arg1 == 0) {
                    this.sendEmptyMessageDelayed(UPLOAD_MESSAGES, uploadInterval);
                }
                break;
            case UPLOAD_HISTORY:
                mConfigManager.debugLog("Performing history upload.");

                // if the uploads table is empty (no old uploads)
                //  and the messages table has messages that are not from the current session,
                //  or there is no current session
                //  then create a history upload and send it

                Cursor isempty = db.rawQuery("select * from " + UploadTable.TABLE_NAME, null);
                if ((isempty == null) || (isempty.getCount() == 0)) {

                    this.removeMessages(UPLOAD_HISTORY);
                    // execute all the upload steps
                    prepareUploads(true);
                    if (isNetworkConnected) {
                        processUploads(true);
                    }
                } else {
                    // the previous upload is not done, try again in 30 seconds
                    this.sendEmptyMessageDelayed(UPLOAD_HISTORY, 30 * 1000);
                }
                if (isempty != null && !isempty.isClosed()){
                    isempty.close();
                }
                break;
            case CLEANUP:
                // delete stale commands, uploads, messages, and sessions
                //cleanupDatabase(Constants.DB_CLEANUP_EXPIRATION);
                //this.sendEmptyMessageDelayed(CLEANUP, Constants.DB_CLEANUP_INTERVAL);
                break;
        }
    }


    /* package-private */void prepareUploads(boolean history) {
        Cursor readyMessagesCursor = null;
        try {
            // select messages ready to upload


            String selection;
            String[] selectionArgs;
            if (history) {
                selection = SQL_HISTORY_MESSAGES;
                selectionArgs = new String[]{Integer.toString(Status.READY), MParticle.getInstance().mSessionID};
            } else {
                selection = SQL_UPLOADABLE_MESSAGES;
                selectionArgs = new String[]{Integer.toString(Status.READY)};
            }
            String[] selectionColumns = new String[]{"_id", MessageTable.MESSAGE, MessageTable.CREATED_AT, MessageTable.STATUS, MessageTable.SESSION_ID};

            readyMessagesCursor = db.query(
                    MessageTable.TABLE_NAME,
                    selectionColumns,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    MessageTable.CREATED_AT + ", " + MessageTable.SESSION_ID + " , _id asc");

            if (readyMessagesCursor.getCount() > 0) {
                mApiClient.fetchConfig();
                mConfigManager.debugLog("Preparing " + readyMessagesCursor.getCount() + " events for upload");

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
        } catch (SQLiteException e) {
            mConfigManager.debugLog("Error preparing batch upload in mParticle DB: " + e.getMessage());
        } catch (IOException e) {
            mConfigManager.debugLog("Error preparing batch upload in mParticle DB: " + e.getMessage());
        } catch (JSONException e) {
            mConfigManager.debugLog("Error preparing batch upload in mParticle DB: " + e.getMessage());
        } catch (MParticleApiClient.MPThrottleException e) {
            mConfigManager.debugLog(e.getMessage());
        } finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()){
                readyMessagesCursor.close();
            }
        }
    }

    private boolean processUploads(boolean history) {
        boolean processingSessionEnd = false;
        Cursor readyUploadsCursor = null;
        try {
            // read batches ready to upload

            String[] selectionColumns = new String[]{"_id", UploadTable.MESSAGE};
            readyUploadsCursor = db.query(UploadTable.TABLE_NAME, selectionColumns,
                    null, null, null, null, UploadTable.CREATED_AT);

            while (readyUploadsCursor.moveToNext()) {
                int id = readyUploadsCursor.getInt(0);
                String message = readyUploadsCursor.getString(1);
                if (!history) {
                    // if message is the MessageType.SESSION_END, then remember so the session history can be triggered
                    if (message.contains("\"" + MessageKey.TYPE + "\":\"" + MessageType.SESSION_END + "\"")) {
                        processingSessionEnd = true;
                    }
                }

                MParticleApiClient.ApiResponse response = mApiClient.sendMessageBatch(message);

                if (response.shouldDelete()) {
                    dbDeleteUpload(id);
                    try {
                        if (response.getJsonResponse() != null &&
                                response.getJsonResponse().has(MessageKey.MESSAGES)) {
                            JSONArray responseCommands = response.getJsonResponse().getJSONArray(MessageKey.MESSAGES);
                            for (int i = 0; i < responseCommands.length(); i++) {
                                JSONObject commandObject = responseCommands.getJSONObject(i);
                                dbInsertCommand(commandObject);
                            }
                        }

                    } catch (JSONException e) {
                        // ignore problems parsing response commands
                    }
                } else {
                    mConfigManager.debugLog("Upload failed and will be retried.");
                }
            }
        } catch (MParticleApiClient.MPThrottleException e) {
            mConfigManager.debugLog(e.getMessage());
        } catch (SQLiteException e) {
            Log.d(TAG, "Error processing batch uploads in mParticle DB", e);
        } catch (IOException ioe) {
            Log.d(TAG, "Error processing batch uploads in mParticle DB", ioe);
        } finally {
            if (readyUploadsCursor != null && !readyUploadsCursor.isClosed()){
                readyUploadsCursor.close();
            }

        }
        return processingSessionEnd;
    }


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

                MParticleApiClient.ApiResponse response = null;
                try {
                    response = mApiClient.sendCommand(commandUrl, method, postData, headers);
                } catch (Throwable t) {
                    // fail silently. a message will be logged if debug mode is enabled
                } finally {
                    if (response != null && response.getResponseCode() > -1) {
                        dbDeleteCommand(id);
                    } else {
                        mConfigManager.debugLog("Provider command processing failed and will be retried.");
                    }
                }
            }
            commandsCursor.close();
        } catch (SQLiteException e) {
            Log.e(TAG, "Error processing provider command uploads in mParticle DB", e);
        } finally {

        }
    }

    private JSONObject createUploadMessage(JSONArray messagesArray, boolean history) throws JSONException {
        JSONObject uploadMessage = new JSONObject();

        uploadMessage.put(MessageKey.TYPE, MessageType.REQUEST_HEADER);
        uploadMessage.put(MessageKey.ID, UUID.randomUUID().toString());
        uploadMessage.put(MessageKey.TIMESTAMP, System.currentTimeMillis());
        uploadMessage.put(MessageKey.MPARTICLE_VERSION, Constants.MPARTICLE_VERSION);
        uploadMessage.put(MessageKey.OPT_OUT_HEADER, mConfigManager.getOptedOut());
        uploadMessage.put(MessageKey.CONFIG_UPLOAD_INTERVAL, mConfigManager.getUploadInterval()/1000);
        uploadMessage.put(MessageKey.CONFIG_SESSION_TIMEOUT, mConfigManager.getSessionTimeout()/1000);


        uploadMessage.put(MessageKey.APP_INFO, getAppInfo());
        // if there is notification key then include it
        String regId = PushRegistrationHelper.getRegistrationId(mContext);
        if ((regId != null) && (regId.length() > 0)) {
            getDeviceInfo().put(MessageKey.PUSH_TOKEN, regId);
        } else {
            getDeviceInfo().remove(MessageKey.PUSH_TOKEN);
        }

        getDeviceInfo().put(MessageKey.PUSH_SOUND_ENABLED, mConfigManager.isPushSoundEnabled());
        getDeviceInfo().put(MessageKey.PUSH_VIBRATION_ENABLED, mConfigManager.isPushVibrationEnabled());

        String payload = mConfigManager.getAdtruth().lastPayload;
        if (payload != null) {
            getDeviceInfo().put(MessageKey.ADTRUTH_ID, mConfigManager.getAdtruth().lastPayload);
        }

        uploadMessage.put(MessageKey.DEVICE_INFO, getDeviceInfo());
        uploadMessage.put(MessageKey.SANDBOX, mConfigManager.getEnvironment().equals(MParticle.Environment.Development));

        uploadMessage.put(MessageKey.LTV, new BigDecimal(mPreferences.getString(PrefKeys.LTV, "0")));

        String userAttrs = mPreferences.getString(PrefKeys.USER_ATTRS + mApiKey, null);
        if (null != userAttrs) {
            uploadMessage.put(MessageKey.USER_ATTRIBUTES, new JSONObject(userAttrs));
        }

        if (history) {
            String deletedAttr = mPreferences.getString(PrefKeys.DELETED_USER_ATTRS + mApiKey, null);
            if (null != deletedAttr) {
                uploadMessage.put(MessageKey.DELETED_USER_ATTRIBUTES, new JSONArray(userAttrs));
                mPreferences.edit().remove(PrefKeys.DELETED_USER_ATTRS + mApiKey).commit();
            }
        }

        String userIds = mPreferences.getString(PrefKeys.USER_IDENTITIES + mApiKey, null);
        if (null != userIds) {
            JSONArray identities = new JSONArray(userIds);
            boolean changeMade = false;
            for (int i = 0; i < identities.length(); i++) {
                if (identities.getJSONObject(i).getBoolean(MessageKey.IDENTITY_FIRST_SEEN)){
                    identities.getJSONObject(i).put(MessageKey.IDENTITY_FIRST_SEEN, false);
                    changeMade = true;
                }
            }
            if (changeMade) {
                uploadMessage.put(MessageKey.USER_IDENTITIES, new JSONArray(userIds));
                mPreferences.edit().putString(PrefKeys.USER_IDENTITIES + mApiKey, identities.toString()).commit();
            }else{
                uploadMessage.put(MessageKey.USER_IDENTITIES, identities);
            }
        }

        uploadMessage.put(history ? MessageKey.HISTORY : MessageKey.MESSAGES, messagesArray);

        MParticleApiClient.addCookies(uploadMessage, mConfigManager);

        uploadMessage.put(MessageKey.PROVIDER_PERSISTENCE, mConfigManager.getProviderPersistence());

        return uploadMessage;
    }

    private void cleanupDatabase(int expirationPeriod) {
        String[] whereArgs = {Long.toString(System.currentTimeMillis() - expirationPeriod)};
        db.delete(CommandTable.TABLE_NAME, CommandTable.CREATED_AT + "<?", whereArgs);
        db.delete(UploadTable.TABLE_NAME, UploadTable.CREATED_AT + "<?", whereArgs);
        db.delete(MessageTable.TABLE_NAME, MessageTable.CREATED_AT + "<?", whereArgs);
        db.delete(SessionTable.TABLE_NAME, SessionTable.END_TIME + "<?", whereArgs);
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
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, SQL_FINISHED_HISTORY_MESSAGES, whereArgs);
    }

    private void dbMarkAsUploadedMessage(int lastMessageId) {
        //non-session messages can be deleted, they're not part of session history
        String[] whereArgs = new String[]{Long.toString(lastMessageId)};
        String whereClause = SQL_DELETABLE_MESSAGES + " and (_id<=?)";
        int rowsdeleted = db.delete(MessageTable.TABLE_NAME, whereClause, whereArgs);

        whereArgs = new String[]{Integer.toString(Status.READY), Long.toString(lastMessageId)};
        whereClause = SQL_UPLOADABLE_MESSAGES + " and (_id<=?)";
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

    public void fetchSegments(long timeout, final String endpointId, final SegmentListener listener) {
        new SegmentTask(timeout, endpointId, listener).execute();
    }

    private SegmentMembership queryAudiences(String endpointId) {
        SQLiteDatabase db = audienceDB.getReadableDatabase();

        String selection = null;
        String[] args = null;
        if (endpointId != null && endpointId.length() > 0){
            selection = SegmentDatabase.SegmentTable.ENDPOINTS + " like ?";
            args = new String[1];
            args[0] = "%\"" + endpointId + "\"%";
        }

        Cursor audienceCursor = db.query(SegmentDatabase.SegmentTable.TABLE_NAME,
                                        null,
                                        selection,
                                        args,
                                        null,
                                        null,
                                        AUDIENCE_QUERY);
        SparseArray<Segment> audiences = new SparseArray<Segment>();

        StringBuilder keys = new StringBuilder("(");
        while (audienceCursor.moveToNext()){
            int id = audienceCursor.getInt(audienceCursor.getColumnIndex(SegmentDatabase.SegmentTable.SEGMENT_ID));

            Segment segment = new Segment(id,
                                            audienceCursor.getString(audienceCursor.getColumnIndex(SegmentDatabase.SegmentTable.NAME)),
                                            audienceCursor.getString(audienceCursor.getColumnIndex(SegmentDatabase.SegmentTable.ENDPOINTS)));
            audiences.put(id, segment);
            keys.append(id);
            keys.append(", ");
        }
        audienceCursor.close();
        keys.delete(keys.length()-2, keys.length());
        keys.append(")");

        long currentTime = System.currentTimeMillis();
        Cursor membershipCursor = db.query(false,
                                    SegmentDatabase.SegmentMembershipTable.TABLE_NAME,
                                    MEMBERSHIP_QUERY_COLUMNS,
                                    String.format(MEMBERSHIP_QUERY_SELECTION,
                                                    keys.toString(),
                                                    currentTime),
                                    null,
                                    null,
                                    null,
                                    MEMBERSHIP_QUERY_ORDER,
                                    null);


        ArrayList<Segment> finalSegments = new ArrayList<Segment>();
        int currentId = -1;
        while (membershipCursor.moveToNext()){
            int id = membershipCursor.getInt(1);
            if (id != currentId) {
                currentId = id;
                String action = membershipCursor.getString(2);
                if (action.equals(Constants.Audience.ACTION_ADD)){
                    finalSegments.add(audiences.get(currentId));
                }
            }
        }
        membershipCursor.close();

        db.close();
        return new SegmentMembership(finalSegments);
    }

    private final static String AUDIENCE_QUERY = SegmentDatabase.SegmentTable.SEGMENT_ID + " desc";
    private final static String MEMBERSHIP_QUERY_ORDER = SegmentDatabase.SegmentMembershipTable.SEGMENT_ID + " desc, " + SegmentDatabase.SegmentMembershipTable.TIMESTAMP + " desc";
    private final static String[] MEMBERSHIP_QUERY_COLUMNS = new String[]
                                                                {
                                                                    SegmentDatabase.SegmentMembershipTable.ID,
                                                                    SegmentDatabase.SegmentMembershipTable.SEGMENT_ID,
                                                                    SegmentDatabase.SegmentMembershipTable.MEMBERSHIP_ACTION
                                                                };
    private final static String MEMBERSHIP_QUERY_SELECTION = "audience_id in %s and " + SegmentDatabase.SegmentMembershipTable.TIMESTAMP + " < %d";

    private void insertAudiences(JSONObject audiences) throws JSONException {
        SQLiteDatabase db = audienceDB.getWritableDatabase();
        JSONArray audienceList = audiences.getJSONArray(Constants.Audience.API_AUDIENCE_LIST);
        db.beginTransaction();
        boolean success = false;
        try {
            db.delete(SegmentDatabase.SegmentMembershipTable.TABLE_NAME, null, null);
            db.delete(SegmentDatabase.SegmentTable.TABLE_NAME, null, null);
            for (int i = 0; i < audienceList.length(); i++) {
                ContentValues audienceRow = new ContentValues();
                JSONObject audience = audienceList.getJSONObject(i);
                int id = audience.getInt(Constants.Audience.API_AUDIENCE_ID);
                String name = audience.getString(Constants.Audience.API_AUDIENCE_NAME);
                String endPointIds = audience.getJSONArray(Constants.Audience.API_AUDIENCE_ENDPOINTS).toString();
                audienceRow.put(SegmentDatabase.SegmentTable.SEGMENT_ID, id);
                audienceRow.put(SegmentDatabase.SegmentTable.NAME, name);
                audienceRow.put(SegmentDatabase.SegmentTable.ENDPOINTS, endPointIds);
                db.insert(SegmentDatabase.SegmentTable.TABLE_NAME, null, audienceRow);
                JSONArray memberships = audience.getJSONArray(Constants.Audience.API_AUDIENCE_MEMBERSHIPS);
                for (int j = 0; j < memberships.length(); j++) {
                    ContentValues membershipRow = new ContentValues();
                    membershipRow.put(SegmentDatabase.SegmentMembershipTable.SEGMENT_ID, id);
                    membershipRow.put(SegmentDatabase.SegmentMembershipTable.MEMBERSHIP_ACTION, memberships.getJSONObject(j).getString(Constants.Audience.API_AUDIENCE_ACTION));
                    membershipRow.put(SegmentDatabase.SegmentMembershipTable.TIMESTAMP, memberships.getJSONObject(j).optLong(Constants.Audience.API_AUDIENCE_MEMBERSHIP_TIMESTAMP, 0));
                    db.insert(SegmentDatabase.SegmentMembershipTable.TABLE_NAME, null, membershipRow);
                }
            }
            success = true;
        }catch (Exception e){
            Log.d(Constants.LOG_TAG, "Failed to insert audiences: " + e.getMessage());
        }finally {
            if (success){
                db.setTransactionSuccessful();
            }
            db.endTransaction();
            db.close();
        }

    }

    class SegmentTask extends AsyncTask<Void, Void, SegmentMembership> {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        String endpointId;
        SegmentListener listener;
        long timeout;
        SegmentTask(long timeout, String endpointId, SegmentListener listener){
            this.timeout = timeout;
            this.endpointId = endpointId;
            this.listener = listener;
        }
        @Override
        protected SegmentMembership doInBackground(Void... params) {
            FutureTask<Boolean> futureTask1 = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    JSONObject audiences = mApiClient.fetchAudiences();
                    if (audiences != null){
                        insertAudiences(audiences);
                    }
                    return audiences != null;
                }
            });

            executor.execute(futureTask1);
            try {
                futureTask1.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            executor.shutdown();
            return queryAudiences(endpointId);
        }

        @Override
        protected void onPostExecute(SegmentMembership segmentMembership) {
            listener.onSegmentsRetrieved(segmentMembership);
        }
    }
}
