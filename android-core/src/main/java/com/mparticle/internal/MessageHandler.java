package com.mparticle.internal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.Constants.Status;
import com.mparticle.internal.MParticleDatabase.BreadcrumbTable;
import com.mparticle.internal.MParticleDatabase.UserAttributesTable;
import com.mparticle.internal.MParticleDatabase.MessageTable;
import com.mparticle.internal.MParticleDatabase.SessionTable;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.MPCloudNotificationMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/* package-private */ class MessageHandler extends Handler {

    private final SQLiteOpenHelper mDbHelper;
    private final Context mContext;

    private SQLiteDatabase db;

    public static final int STORE_MESSAGE = 0;
    public static final int UPDATE_SESSION_ATTRIBUTES = 1;
    public static final int UPDATE_SESSION_END = 2;
    public static final int CREATE_SESSION_END_MESSAGE = 3;
    public static final int END_ORPHAN_SESSIONS = 4;
    public static final int STORE_BREADCRUMB = 5;
    public static final int STORE_GCM_MESSAGE = 6;
    public static final int MARK_INFLUENCE_OPEN_GCM = 7;
    public static final int CLEAR_PROVIDER_GCM = 8;
    public static final int STORE_REPORTING_MESSAGE_LIST = 9;
    public static final int REMOVE_USER_ATTRIBUTE = 10;
    public static final int SET_USER_ATTRIBUTE = 11;
    public static final int INCREMENT_USER_ATTRIBUTE = 12;

    private final MessageManagerCallbacks mMessageManagerCallbacks;

    public MessageHandler(Looper looper, MessageManagerCallbacks messageManager, SQLiteOpenHelper dbHelper, Context context) {
        super(looper);
        mMessageManagerCallbacks = messageManager;
        mDbHelper = dbHelper;
        mContext = context;
    }


    private boolean prepareDatabase() {
        if (db == null){
            try {
                db = mDbHelper.getWritableDatabase();
            }catch (Exception e){
                //if we failed to create the database, there's not much we can do, so just bail out.
                return false;
            }
        }
        return true;
    }
    @Override
    public void handleMessage(Message msg) {
        if (!prepareDatabase()){
            return;
        }
        mMessageManagerCallbacks.delayedStart();
        switch (msg.what) {
            case STORE_MESSAGE:
                try {

                    MPMessage message = (MPMessage) msg.obj;
                    message.put(MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
                    String messageType = message.getString(MessageKey.TYPE);
                    // handle the special case of session-start by creating the
                    // session record first
                    if (MessageType.SESSION_START.equals(messageType)) {
                        dbInsertSession(message);
                    }else{
                        dbUpdateSessionEndTime(message.getSessionId(), message.getLong(MessageKey.TIMESTAMP), 0);
                        message.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
                    }
                    if (MessageType.ERROR.equals(messageType)){
                        appendBreadcrumbs(message);
                    }
                    if (MessageType.APP_STATE_TRANSITION.equals(messageType)){
                        appendLatestPushNotification(message);
                    }
                    if (MessageType.PUSH_RECEIVED.equals(messageType) &&
                            message.has(MessageKey.PUSH_BEHAVIOR) &&
                            !validateBehaviorFlags(message)){
                        return;
                    }
                    dbInsertMessage(message);

                    mMessageManagerCallbacks.checkForTrigger(message);

                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error saving message to mParticle DB.");
                }
                break;
            case UPDATE_SESSION_ATTRIBUTES:
                try {
                    JSONObject sessionAttributes = (JSONObject) msg.obj;
                    String sessionId = sessionAttributes.getString(MessageKey.SESSION_ID);
                    String attributes = sessionAttributes.getString(MessageKey.ATTRIBUTES);
                    dbUpdateSessionAttributes(sessionId, attributes);
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error updating session attributes in mParticle DB.");
                }
                break;
            case UPDATE_SESSION_END:
                try {
                    Session session = (Session) msg.obj;
                    dbUpdateSessionEndTime(session.mSessionID, session.mLastEventTime, session.getForegroundTime());
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error updating session end time in mParticle DB");
                }
                break;
            case CREATE_SESSION_END_MESSAGE:
                try {
                    String sessionId = (String) msg.obj;
                    String[] selectionArgs = new String[]{sessionId};
                    String[] sessionColumns = new String[]{SessionTable.START_TIME, SessionTable.END_TIME,
                            SessionTable.SESSION_FOREGROUND_LENGTH, SessionTable.ATTRIBUTES};
                    Cursor selectCursor = db.query(SessionTable.TABLE_NAME, sessionColumns, SessionTable.SESSION_ID + "=?",
                            selectionArgs, null, null, null);
                    if (selectCursor.moveToFirst()) {
                        long start = selectCursor.getLong(0);
                        long end = selectCursor.getLong(1);
                        long foregroundLength = selectCursor.getLong(2);
                        String attributes = selectCursor.getString(3);
                        JSONObject sessionAttributes = null;
                        if (null != attributes) {
                            sessionAttributes = new JSONObject(attributes);
                        }

                        // create a session-end message
                        try {
                            MPMessage endMessage = mMessageManagerCallbacks.createMessageSessionEnd(sessionId, start, end, foregroundLength,
                                    sessionAttributes);
                            endMessage.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
                            // insert the record into messages with duration
                            dbInsertMessage(endMessage);
                        }catch (JSONException jse){
                            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle session end message");
                        }

                        // delete the processed session record
                        db.delete(SessionTable.TABLE_NAME, SessionTable.SESSION_ID + "=?", new String[]{sessionId});
                    } else {
                        ConfigManager.log(MParticle.LogLevel.ERROR, "Error creating session end, no entry for sessionId in mParticle DB");
                    }
                    selectCursor.close();
                    //1 means this came from ending the session
                    if (msg.arg1 == 1){
                        mMessageManagerCallbacks.endUploadLoop();
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error creating session end message in mParticle DB");
                }finally {

                }
                break;
            case END_ORPHAN_SESSIONS:
                try {
                    // find left-over sessions that exist during startup and end them
                    String[] selectionArgs = new String[]{mMessageManagerCallbacks.getApiKey()};
                    String[] sessionColumns = new String[]{SessionTable.SESSION_ID};
                    Cursor selectCursor = db.query(SessionTable.TABLE_NAME, sessionColumns,
                            SessionTable.API_KEY + "=?",
                            selectionArgs, null, null, null);
                    // NOTE: there should be at most one orphan per api key - but
                    // process any that are found
                    while (selectCursor.moveToNext()) {
                        String sessionId = selectCursor.getString(0);
                        sendMessage(obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, 0, 0, sessionId));
                    }
                    selectCursor.close();
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error processing initialization in mParticle DB");
                }
                break;
            case STORE_BREADCRUMB:
                try {
                    MPMessage message = (MPMessage) msg.obj;
                    message.put(Constants.MessageKey.ID, UUID.randomUUID().toString());
                    dbInsertBreadcrumb(message);
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error saving breadcrumb to mParticle DB");
                }
                break;
            case STORE_GCM_MESSAGE:
                try {
                    AbstractCloudMessage message = (AbstractCloudMessage) msg.obj;
                    dbInsertGcmMessage(message, msg.getData().getString(MParticleDatabase.GcmMessageTable.APPSTATE));
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error saving GCM message to mParticle DB", e.toString());
                }
                break;
            case MARK_INFLUENCE_OPEN_GCM:
                MessageManager.InfluenceOpenMessage message = (MessageManager.InfluenceOpenMessage) msg.obj;
                logInfluenceOpenGcmMessages(message);
                break;
            case CLEAR_PROVIDER_GCM:
                try {
                    clearOldProviderGcm();
                }catch (Exception e){
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while clearing provider GCM messages: ", e.toString());
                }
                break;
            case STORE_REPORTING_MESSAGE_LIST:
                try{
                    List<JsonReportingMessage> reportingMessages = (List<JsonReportingMessage>)msg.obj;
                    dbInsertReportingMessages(reportingMessages);
                }catch (Exception e) {
                    if (BuildConfig.MP_DEBUG) {
                        ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while inserting reporting messages: ", e.toString());
                    }
                }
                break;
            case REMOVE_USER_ATTRIBUTE:
                try {
                    removeUserAttribute((MessageManager.UserAttributeRemoval)msg.obj, mMessageManagerCallbacks);
                }catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while removing user attribute: ", e.toString());
                }
                break;
            case SET_USER_ATTRIBUTE:
                try {
                    setUserAttribute((MessageManager.UserAttributeResponse) msg.obj);
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while setting user attribute: ", e.toString());
                }
                break;
            case INCREMENT_USER_ATTRIBUTE:
                try {
                    incrementUserAttribute((String)msg.obj, msg.arg1);
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while incrementing user attribute: ", e.toString());
                }
        }
    }

    private void incrementUserAttribute(String key, int incrementValue) {
        TreeMap<String, String> userAttributes = getUserAttributeSingles();

        if (!userAttributes.containsKey(key)) {
            TreeMap<String, List<String>> userAttributeList = getUserAttributeLists();
            if (userAttributeList.containsKey(key)) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "Error while attempting to increment user attribute - existing attribute is a list, which can't be incremented.");
                return;
            }
        }
        String newValue = null;
        String currentValue = userAttributes.get(key);
        if (currentValue == null) {
            newValue = Integer.toString(incrementValue);
        } else {
            try {
                newValue = Integer.toString(Integer.parseInt(currentValue) + incrementValue);
            }catch (NumberFormatException nfe) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "Error while attempting to increment user attribute - existing attribute is not a number.");
                return;
            }
        }
        MessageManager.UserAttributeResponse wrapper = new MessageManager.UserAttributeResponse();
        wrapper.attributeSingles = new HashMap<String, String>(1);
        wrapper.attributeSingles.put(key, newValue);
        setUserAttribute(wrapper);
        MParticle.getInstance().getKitManager().setUserAttribute(key, newValue);
    }

    private void removeUserAttribute(MessageManager.UserAttributeRemoval container, MessageManagerCallbacks callbacks) {
        Map<String, Object> currentValues = MParticle.getInstance().getAllUserAttributes();
        String[] deleteWhereArgs = {container.key};
        try {
            db.beginTransaction();
            int deleted = db.delete(UserAttributesTable.TABLE_NAME, UserAttributesTable.ATTRIBUTE_KEY + " = ?", deleteWhereArgs);
            if (callbacks != null && deleted > 0) {
                callbacks.attributeRemoved(container.key);
                callbacks.logUserAttributeChangeMessage(container.key, null, currentValues.get(container.key), true, false, container.time);
            }
            db.setTransactionSuccessful();
        }catch (Exception e) {

        } finally {
            db.endTransaction();
        }
    }

    private void clearOldProviderGcm() {
        String[] deleteWhereArgs = {Integer.toString(MParticleDatabase.GcmMessageTable.PROVIDER_CONTENT_ID)};
        db.delete(MParticleDatabase.GcmMessageTable.TABLE_NAME, MParticleDatabase.GcmMessageTable.CONTENT_ID + " = ?", deleteWhereArgs);
    }

    private boolean validateBehaviorFlags(MPMessage message) {
        Cursor gcmCursor = null;
        boolean shouldInsert = true;
        int newBehavior = message.optInt(MessageKey.PUSH_BEHAVIOR);
        try {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Validating GCM behaviors...");
            String[] args = {Integer.toString(message.getInt(MParticleDatabase.GcmMessageTable.CONTENT_ID))};
            gcmCursor = db.query(MParticleDatabase.GcmMessageTable.TABLE_NAME,
                    null,
                    MParticleDatabase.GcmMessageTable.CONTENT_ID + " =?",
                    args,
                    null,
                    null,
                    null);
            long timestamp = 0;
            if (gcmCursor.moveToFirst()) {
                int currentBehaviors = gcmCursor.getInt(gcmCursor.getColumnIndex(MParticleDatabase.GcmMessageTable.BEHAVIOR));
                gcmCursor.close();

                //if we're trying to log a direct open, but the push has already been marked influence open, remove direct open from the new behavior
                if (((newBehavior & AbstractCloudMessage.FLAG_DIRECT_OPEN) == AbstractCloudMessage.FLAG_DIRECT_OPEN) &&
                        ((currentBehaviors & AbstractCloudMessage.FLAG_INFLUENCE_OPEN) == AbstractCloudMessage.FLAG_INFLUENCE_OPEN)) {
                    return false;
                }//if we're trying to log an influence open, but the push has already been marked direct open, remove influence open from the new behavior
                else if (((newBehavior & AbstractCloudMessage.FLAG_INFLUENCE_OPEN) == AbstractCloudMessage.FLAG_INFLUENCE_OPEN) &&
                        ((currentBehaviors & AbstractCloudMessage.FLAG_DIRECT_OPEN) == AbstractCloudMessage.FLAG_DIRECT_OPEN)) {
                    return false;
                }
                if ((currentBehaviors & AbstractCloudMessage.FLAG_RECEIVED) == AbstractCloudMessage.FLAG_RECEIVED ){
                    newBehavior &= ~AbstractCloudMessage.FLAG_RECEIVED;
                }
                if ((currentBehaviors & AbstractCloudMessage.FLAG_DISPLAYED) == AbstractCloudMessage.FLAG_DISPLAYED ){
                    newBehavior &= ~AbstractCloudMessage.FLAG_DISPLAYED;
                }

                if ((newBehavior & AbstractCloudMessage.FLAG_DISPLAYED) == AbstractCloudMessage.FLAG_DISPLAYED){
                    timestamp = message.getTimestamp();
                }
                message.put(MessageKey.PUSH_BEHAVIOR, newBehavior);

                if (newBehavior != currentBehaviors) {
                    ContentValues values = new ContentValues();
                    values.put(MParticleDatabase.GcmMessageTable.BEHAVIOR, newBehavior);
                    if (timestamp > 0){
                        values.put(MParticleDatabase.GcmMessageTable.DISPLAYED_AT, timestamp);
                    }
                    int updated = db.update(MParticleDatabase.GcmMessageTable.TABLE_NAME, values, MParticleDatabase.GcmMessageTable.CONTENT_ID + " =?", args);
                    if (updated > 0) {
                        ConfigManager.log(MParticle.LogLevel.DEBUG, "Updated GCM with content ID: " + message.getInt(MParticleDatabase.GcmMessageTable.CONTENT_ID) + " and behavior(s): " + getBehaviorString(newBehavior));
                    }
                }else{
                    shouldInsert = false;
                }

            }
        } catch (Exception e) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, e, "Failed to update GCM message.");
        }finally {
            if (gcmCursor != null && !gcmCursor.isClosed()){
                gcmCursor.close();
            }
        }
        return shouldInsert;
    }

    private String getBehaviorString(int newBehavior){
        String behavior = "";
        if ((newBehavior & AbstractCloudMessage.FLAG_DIRECT_OPEN) == AbstractCloudMessage.FLAG_DIRECT_OPEN){
            behavior += "direct-open, ";
        }else if ((newBehavior & AbstractCloudMessage.FLAG_INFLUENCE_OPEN) == AbstractCloudMessage.FLAG_INFLUENCE_OPEN){
            behavior += "influence-open, ";
        }else if ((newBehavior & AbstractCloudMessage.FLAG_RECEIVED) == AbstractCloudMessage.FLAG_RECEIVED){
            behavior += "received, ";
        }else if ((newBehavior & AbstractCloudMessage.FLAG_DISPLAYED) == AbstractCloudMessage.FLAG_DISPLAYED){
            behavior += "displayed, ";
        }
        return behavior;
    }

    private void logInfluenceOpenGcmMessages(MessageManager.InfluenceOpenMessage message) {
        Cursor gcmCursor = null;
        try{

            gcmCursor = db.query(MParticleDatabase.GcmMessageTable.TABLE_NAME,
                    null,MParticleDatabase.GcmMessageTable.CONTENT_ID + " != " + MParticleDatabase.GcmMessageTable.PROVIDER_CONTENT_ID + " and " +
                    MParticleDatabase.GcmMessageTable.DISPLAYED_AT +
                            " > 0 and " +
                            MParticleDatabase.GcmMessageTable.DISPLAYED_AT +
                            " > " + (message.mTimeStamp - message.mTimeout) +
                            " and ((" + MParticleDatabase.GcmMessageTable.BEHAVIOR + " & " + AbstractCloudMessage.FLAG_INFLUENCE_OPEN + "" + ") != " + AbstractCloudMessage.FLAG_INFLUENCE_OPEN + ")",
                    null,
                    null,
                    null,
                    null);
            while (gcmCursor.moveToNext()){
                mMessageManagerCallbacks.logNotification(gcmCursor.getInt(gcmCursor.getColumnIndex(MParticleDatabase.GcmMessageTable.CONTENT_ID)),
                        gcmCursor.getString(gcmCursor.getColumnIndex(MParticleDatabase.GcmMessageTable.PAYLOAD)),
                        null,
                        gcmCursor.getString(gcmCursor.getColumnIndex(MParticleDatabase.GcmMessageTable.APPSTATE)),
                        AbstractCloudMessage.FLAG_INFLUENCE_OPEN
                );
            }
        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error logging influence-open message to mParticle DB ", e.toString());
        }finally {
            if (gcmCursor != null && !gcmCursor.isClosed()){
                gcmCursor.close();
            }
        }
    }

    private void appendLatestPushNotification(MPMessage message) {
        Cursor pushCursor = null;
        try {
            pushCursor = db.query(MParticleDatabase.GcmMessageTable.TABLE_NAME,
                    null,
                    MParticleDatabase.GcmMessageTable.DISPLAYED_AT + " > 0",
                    null,
                    null,
                    null,
                    MParticleDatabase.GcmMessageTable.DISPLAYED_AT + " desc limit 1");
            if (pushCursor.moveToFirst()) {
                String payload = pushCursor.getString(pushCursor.getColumnIndex(MParticleDatabase.GcmMessageTable.PAYLOAD));
                message.put(MessageKey.PAYLOAD, payload);
            }
        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed to append latest push notification payload: " + e.toString());
        }finally {
            if (pushCursor != null && !pushCursor.isClosed()){
                pushCursor.close();
            }
        }
    }

    private static final String[] breadcrumbColumns = {
            BreadcrumbTable.CREATED_AT,
            BreadcrumbTable.MESSAGE
    };

    private void appendBreadcrumbs(JSONObject message) throws JSONException {
        Cursor breadcrumbCursor = null;
        try {
            breadcrumbCursor = db.query(BreadcrumbTable.TABLE_NAME,
                    breadcrumbColumns,
                    null,
                    null,
                    null,
                    null,
                    BreadcrumbTable.CREATED_AT + " desc limit " + ConfigManager.getBreadcrumbLimit());

            if (breadcrumbCursor.getCount() > 0) {
                JSONArray breadcrumbs = new JSONArray();
                int breadcrumbIndex = breadcrumbCursor.getColumnIndex(BreadcrumbTable.MESSAGE);
                while (breadcrumbCursor.moveToNext()) {
                    JSONObject breadcrumbObject = new JSONObject(breadcrumbCursor.getString(breadcrumbIndex));
                    breadcrumbs.put(breadcrumbObject);
                }
                message.put(MessageType.BREADCRUMB, breadcrumbs);
            }
        }catch (Exception e) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Error while appending breadcrumbs: " + e.toString());
        } finally {
            if (breadcrumbCursor != null && !breadcrumbCursor.isClosed()) {
                breadcrumbCursor.close();
            }
        }
    }

    private static final String[] idColumns = {"_id"};

    private void dbInsertBreadcrumb(MPMessage message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MParticleDatabase.BreadcrumbTable.API_KEY, mMessageManagerCallbacks.getApiKey());
        contentValues.put(MParticleDatabase.BreadcrumbTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(MParticleDatabase.BreadcrumbTable.SESSION_ID, message.getSessionId());
        contentValues.put(MParticleDatabase.BreadcrumbTable.MESSAGE, message.toString());


        db.insert(BreadcrumbTable.TABLE_NAME, null, contentValues);
        Cursor cursor = db.query(BreadcrumbTable.TABLE_NAME, idColumns, null, null, null, null, " _id desc limit 1");
        if (cursor.moveToFirst()){
            int maxId = cursor.getInt(0);
            if (maxId > ConfigManager.getBreadcrumbLimit()){
                String[] limit = {Integer.toString(maxId - ConfigManager.getBreadcrumbLimit())};
                db.delete(BreadcrumbTable.TABLE_NAME, " _id < ?", limit);
            }
        }
    }

    private void dbInsertGcmMessage(AbstractCloudMessage message, String appState) throws JSONException {
        ContentValues contentValues = new ContentValues();
        if (message instanceof MPCloudNotificationMessage) {
            contentValues.put(MParticleDatabase.GcmMessageTable.CONTENT_ID, ((MPCloudNotificationMessage)message).getContentId());
            contentValues.put(MParticleDatabase.GcmMessageTable.CAMPAIGN_ID, ((MPCloudNotificationMessage)message).getCampaignId());
            contentValues.put(MParticleDatabase.GcmMessageTable.EXPIRATION, ((MPCloudNotificationMessage)message).getExpiration());
            contentValues.put(MParticleDatabase.GcmMessageTable.DISPLAYED_AT, message.getActualDeliveryTime());
        }else{
            contentValues.put(MParticleDatabase.GcmMessageTable.CONTENT_ID, MParticleDatabase.GcmMessageTable.PROVIDER_CONTENT_ID);
            contentValues.put(MParticleDatabase.GcmMessageTable.CAMPAIGN_ID, 0);
            contentValues.put(MParticleDatabase.GcmMessageTable.EXPIRATION, System.currentTimeMillis() + (24 * 60 * 60 * 1000));
            contentValues.put(MParticleDatabase.GcmMessageTable.DISPLAYED_AT, System.currentTimeMillis());
        }
        contentValues.put(MParticleDatabase.GcmMessageTable.PAYLOAD, message.getRedactedJsonPayload().toString());
        contentValues.put(MParticleDatabase.GcmMessageTable.BEHAVIOR, 0);
        contentValues.put(MParticleDatabase.GcmMessageTable.CREATED_AT, System.currentTimeMillis());

        contentValues.put(MParticleDatabase.GcmMessageTable.APPSTATE, appState);

        db.replace(MParticleDatabase.GcmMessageTable.TABLE_NAME, null, contentValues);
    }

    private void dbInsertSession(MPMessage message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SessionTable.API_KEY, mMessageManagerCallbacks.getApiKey());
        contentValues.put(SessionTable.SESSION_ID, message.getSessionId());
        contentValues.put(SessionTable.START_TIME, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(SessionTable.END_TIME, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(SessionTable.SESSION_FOREGROUND_LENGTH, 0);
        contentValues.put(SessionTable.APP_INFO, DeviceAttributes.collectAppInfo(mContext).toString());
        contentValues.put(SessionTable.DEVICE_INFO, DeviceAttributes.collectDeviceInfo(mContext).toString());
        db.insert(SessionTable.TABLE_NAME, null, contentValues);
    }

    private void dbInsertReportingMessages(List<JsonReportingMessage> messages) throws JSONException {
        try {
            db.beginTransaction();
            for (int i = 0; i < messages.size(); i++) {
                JsonReportingMessage message = messages.get(i);
                ContentValues values = new ContentValues();
                values.put(MParticleDatabase.ReportingTable.CREATED_AT, message.getTimestamp());
                values.put(MParticleDatabase.ReportingTable.MODULE_ID, message.getModuleId());
                values.put(MParticleDatabase.ReportingTable.MESSAGE, message.toJson().toString());
                values.put(MParticleDatabase.ReportingTable.SESSION_ID, message.getSessionId());
                db.insert(MParticleDatabase.ReportingTable.TABLE_NAME, null, values);
            }

            db.setTransactionSuccessful();
        }catch (Exception e) {
            if (BuildConfig.MP_DEBUG) {
                ConfigManager.log(MParticle.LogLevel.ERROR, "Error inserting reporting message: " + e.toString());
            }
        }finally {
            db.endTransaction();
        }
    }

    private void dbInsertMessage(MPMessage message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.API_KEY, mMessageManagerCallbacks.getApiKey());
        contentValues.put(MessageTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        String sessionID = message.getSessionId();
        contentValues.put(MessageTable.SESSION_ID, sessionID);
        if (Constants.NO_SESSION_ID.equals(sessionID)){
            message.remove(Constants.MessageKey.SESSION_ID);
        }
        contentValues.put(MessageTable.MESSAGE, message.toString());

        if (message.getString(MessageKey.TYPE) == MessageType.FIRST_RUN) {
            // Force the first run message to be parsed immediately
            contentValues.put(MessageTable.STATUS, Status.BATCH_READY);
        } else {
            contentValues.put(MessageTable.STATUS, Status.READY);
        }

        db.insert(MessageTable.TABLE_NAME, null, contentValues);
    }

    private void dbUpdateMessageStatus(String sessionId, long status) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.STATUS, status);
        String[] whereArgs = {sessionId};
        db.update(MessageTable.TABLE_NAME, contentValues, MessageTable.SESSION_ID + "=?", whereArgs);
    }

    private void dbUpdateSessionAttributes(String sessionId, String attributes) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(SessionTable.ATTRIBUTES, attributes);
        String[] whereArgs = {sessionId};
        db.update(SessionTable.TABLE_NAME, sessionValues, SessionTable.SESSION_ID + "=?", whereArgs);
    }

    private void dbUpdateSessionEndTime(String sessionId, long endTime, long sessionLength) {
        ContentValues sessionValues = new ContentValues();
        sessionValues.put(SessionTable.END_TIME, endTime);
        if (sessionLength > 0) {
            sessionValues.put(SessionTable.SESSION_FOREGROUND_LENGTH, sessionLength);
        }
        String[] whereArgs = {sessionId};
        db.update(SessionTable.TABLE_NAME, sessionValues, SessionTable.SESSION_ID + "=?", whereArgs);
    }


    public TreeMap<String,String> getUserAttributeSingles() {
        if (!prepareDatabase()){
            return null;
        }
        TreeMap<String, String> attributes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            String[] args =  {"1"};

            cursor = db.query(UserAttributesTable.TABLE_NAME, null, UserAttributesTable.IS_LIST + " != ?", args, null, null, UserAttributesTable.ATTRIBUTE_KEY + ", "+ MParticleDatabase.UserAttributesTable.CREATED_AT +" desc");
            int keyIndex = cursor.getColumnIndex(UserAttributesTable.ATTRIBUTE_KEY);
            int valueIndex = cursor.getColumnIndex(UserAttributesTable.ATTRIBUTE_VALUE);
            while (cursor.moveToNext()) {
                attributes.put(cursor.getString(keyIndex), cursor.getString(valueIndex));
            }
        }catch (Exception e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while querying user attributes: ", e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return attributes;
    }

    public TreeMap<String,List<String>> getUserAttributeLists() {
        if (!prepareDatabase()){
            return null;
        }
        TreeMap<String, List<String>> attributes = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        Cursor cursor = null;
        try {
            String[] args =  {"1"};
            cursor = db.query(UserAttributesTable.TABLE_NAME, null, UserAttributesTable.IS_LIST + " = ?", args, null, null, UserAttributesTable.ATTRIBUTE_KEY + ", "+ MParticleDatabase.UserAttributesTable.CREATED_AT +" desc");
            int keyIndex = cursor.getColumnIndex(UserAttributesTable.ATTRIBUTE_KEY);
            int valueIndex = cursor.getColumnIndex(UserAttributesTable.ATTRIBUTE_VALUE);
            String previousKey = null;
            List<String> currentList = null;
            while (cursor.moveToNext()) {
                String currentKey = cursor.getString(keyIndex);
                if (!currentKey.equals(previousKey)){
                    previousKey = currentKey;
                    currentList = new ArrayList<String>();
                    attributes.put(currentKey, currentList);
                }
                attributes.get(currentKey).add(cursor.getString(valueIndex));
            }
        }catch (Exception e) {
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while querying user attribute lists: ", e.toString());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return attributes;
    }


    public void setUserAttribute(MessageManager.UserAttributeResponse userAttributes) {
        if (!prepareDatabase()){
            return;
        }
        Map<String, Object> currentValues = MParticle.getInstance().getAllUserAttributes();
        db.beginTransaction();
        try {
            long time = System.currentTimeMillis();
            if (userAttributes.attributeLists != null) {
                for (Map.Entry<String, List<String>> entry : userAttributes.attributeLists.entrySet()) {
                    String key = entry.getKey();
                    List<String> attributeValues = entry.getValue();
                    String[] deleteWhereArgs = {key};
                    int deleted = db.delete(UserAttributesTable.TABLE_NAME, UserAttributesTable.ATTRIBUTE_KEY + " = ?", deleteWhereArgs);
                    boolean isNewAttribute = deleted == 0;
                    ContentValues values = new ContentValues();
                    for (String attributeValue : attributeValues) {
                        values.put(UserAttributesTable.ATTRIBUTE_KEY, key);
                        values.put(UserAttributesTable.ATTRIBUTE_VALUE, attributeValue);
                        values.put(UserAttributesTable.IS_LIST, true);
                        values.put(UserAttributesTable.CREATED_AT, time);
                        db.insert(UserAttributesTable.TABLE_NAME, null, values);
                    }
                    mMessageManagerCallbacks.logUserAttributeChangeMessage(key, attributeValues, currentValues.get(key), false, isNewAttribute, userAttributes.time);
                }
            }
            if (userAttributes.attributeSingles != null) {
                for (Map.Entry<String, String> entry : userAttributes.attributeSingles.entrySet()) {
                    String key = entry.getKey();
                    String attributeValue = entry.getValue();
                    String[] deleteWhereArgs = {key};
                    int deleted = db.delete(UserAttributesTable.TABLE_NAME, UserAttributesTable.ATTRIBUTE_KEY + " = ?", deleteWhereArgs);
                    boolean isNewAttribute = deleted == 0;
                    ContentValues values = new ContentValues();
                    values.put(UserAttributesTable.ATTRIBUTE_KEY, key);
                    values.put(UserAttributesTable.ATTRIBUTE_VALUE, attributeValue);
                    values.put(UserAttributesTable.IS_LIST, false);
                    values.put(UserAttributesTable.CREATED_AT, time);
                    db.insert(UserAttributesTable.TABLE_NAME, null, values);
                    mMessageManagerCallbacks.logUserAttributeChangeMessage(key, attributeValue, currentValues.get(key), false, isNewAttribute, userAttributes.time);
                }
            }
            db.setTransactionSuccessful();
        }catch (Exception e){
            ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error while adding user attributes: ", e.toString());
        } finally {
            db.endTransaction();
        }
    }
}
