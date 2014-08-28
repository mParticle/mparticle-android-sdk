package com.mparticle;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.Status;
import com.mparticle.MParticleDatabase.BreadcrumbTable;
import com.mparticle.MParticleDatabase.MessageTable;
import com.mparticle.MParticleDatabase.SessionTable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* package-private */final class MessageHandler extends Handler {

    private static final String TAG = Constants.LOG_TAG;

    private final SQLiteDatabase db;
    private final String mApiKey;

    public static final int STORE_MESSAGE = 0;
    public static final int UPDATE_SESSION_ATTRIBUTES = 1;
    public static final int UPDATE_SESSION_END = 2;
    public static final int CREATE_SESSION_END_MESSAGE = 3;
    public static final int END_ORPHAN_SESSIONS = 4;
    public static final int STORE_BREADCRUMB = 5;

    // boolean flag used in unit tests to wait until processing is finished.
    // this is not used in the normal execution.
    /* package-private */ boolean mIsProcessingMessage = false;

    public MessageHandler(Looper looper, String apiKey, SQLiteDatabase database) {
        super(looper);
        db = database;
        mApiKey = apiKey;
    }

    @Override
    public void handleMessage(Message msg) {
        mIsProcessingMessage = true;

        switch (msg.what) {
            case STORE_MESSAGE:
                try {
                    JSONObject message = (JSONObject) msg.obj;
                    message.put(MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
                    String messageType = message.getString(MessageKey.TYPE);
                    // handle the special case of session-start by creating the
                    // session record first
                    if (MessageType.SESSION_START == messageType) {
                        dbInsertSession(message);
                    }
                    if (MessageType.ERROR == messageType){
                        appendBreadcrumbs(message);
                    }
                    dbInsertMessage(message);

                    if (MessageType.SESSION_START != messageType) {
                        dbUpdateSessionEndTime(getMessageSessionId(message), message.getLong(MessageKey.TIMESTAMP), 0);
                    }
                } catch (SQLiteException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error saving event to mParticle DB");
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error with JSON object");
                } finally {

                }
                break;
            case UPDATE_SESSION_ATTRIBUTES:
                try {
                    JSONObject sessionAttributes = (JSONObject) msg.obj;
                    String sessionId = sessionAttributes.getString(MessageKey.SESSION_ID);
                    String attributes = sessionAttributes.getString(MessageKey.ATTRIBUTES);
                    dbUpdateSessionAttributes(sessionId, attributes);
                } catch (SQLiteException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error updating session attributes in mParticle DB");
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error with JSON object");
                } finally {

                }
                break;
            case UPDATE_SESSION_END:
                try {
                    JSONObject sessionTiming = (JSONObject) msg.obj;
                    String sessionId = sessionTiming.getString(MessageKey.SESSION_ID);
                    long time = sessionTiming.getLong(MessageKey.TIMESTAMP);
                    long sessionLength = sessionTiming.getLong(MessageKey.SESSION_LENGTH);


                    dbUpdateSessionEndTime(sessionId, time, sessionLength);
                } catch (SQLiteException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error updating session end time in mParticle DB");
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error with JSON object");
                } finally {

                }
                break;
            case CREATE_SESSION_END_MESSAGE:
                try {
                    String sessionId = (String) msg.obj;
                    String[] selectionArgs = new String[]{sessionId};
                    String[] sessionColumns = new String[]{SessionTable.START_TIME, SessionTable.END_TIME,
                            SessionTable.SESSION_LENGTH, SessionTable.ATTRIBUTES};
                    Cursor selectCursor = db.query(SessionTable.TABLE_NAME, sessionColumns, SessionTable.SESSION_ID + "=?",
                            selectionArgs, null, null, null);
                    if (selectCursor.moveToFirst()) {
                        long start = selectCursor.getLong(0);
                        long end = selectCursor.getLong(1);
                        long length = selectCursor.getLong(2);
                        String attributes = selectCursor.getString(3);
                        JSONObject sessionAttributes = null;
                        if (null != attributes) {
                            sessionAttributes = new JSONObject(attributes);
                        }

                        // create a session-end message
                        JSONObject endMessage = MessageManager.createMessageSessionEnd(sessionId, start, end, length,
                                sessionAttributes);

                        // insert the record into messages with duration
                        dbInsertMessage(endMessage);

                        // delete the processed session record
                        db.delete(SessionTable.TABLE_NAME, SessionTable.SESSION_ID + "=?", new String[]{sessionId});
                    } else {
                        ConfigManager.log(MParticle.LogLevel.ERROR, "Error creating session, no entry for sessionId in mParticle DB");
                    }
                    selectCursor.close();

                } catch (SQLiteException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error creating session end message in mParticle DB");
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error with JSON object");
                } finally {

                }
                break;
            case END_ORPHAN_SESSIONS:
                try {
                    // find left-over sessions that exist during startup and end them
                    String[] selectionArgs = new String[]{mApiKey};
                    String[] sessionColumns = new String[]{SessionTable.SESSION_ID};
                    Cursor selectCursor = db.query(SessionTable.TABLE_NAME, sessionColumns,
                            SessionTable.API_KEY + "=?",
                            selectionArgs, null, null, null);
                    // NOTE: there should be at most one orphan per api key - but
                    // process any that are found
                    while (selectCursor.moveToNext()) {
                        String sessionId = selectCursor.getString(0);
                        sendMessage(obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
                    }
                    selectCursor.close();
                } catch (SQLiteException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error processing initialization in mParticle DB");
                } finally {

                }
                break;
            case STORE_BREADCRUMB:
                try {
                    JSONObject message = (JSONObject) msg.obj;
                    dbInsertBreadcrumb(message);
                } catch (SQLiteException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error saving breadcrumb to mParticle DB");
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, e, "Error with JSON object");
                } finally {

                }
        }
        mIsProcessingMessage = false;
    }

    private static final String[] breadcrumbColumns = {
            BreadcrumbTable.CREATED_AT,
            BreadcrumbTable.MESSAGE
    };

    private void appendBreadcrumbs(JSONObject message) throws JSONException {
        Cursor breadcrumbCursor = db.query(BreadcrumbTable.TABLE_NAME,
                breadcrumbColumns,
                null,
                null,
                null,
                null,
                BreadcrumbTable.CREATED_AT + " desc limit " + MParticle.getInstance().mConfigManager.getBreadcrumbLimit());

        if (breadcrumbCursor.getCount() > 0){
            JSONArray breadcrumbs = new JSONArray();
            int breadcrumbIndex = breadcrumbCursor.getColumnIndex(BreadcrumbTable.MESSAGE);
            while (breadcrumbCursor.moveToNext()){
                JSONObject breadcrumbObject = new JSONObject(breadcrumbCursor.getString(breadcrumbIndex));
                breadcrumbs.put(breadcrumbObject);
            }
            message.put(MessageType.BREADCRUMB, breadcrumbs);
        }
    }

    private static final String[] idColumns = {"_id"};

    private void dbInsertBreadcrumb(JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MParticleDatabase.BreadcrumbTable.API_KEY, mApiKey);
        contentValues.put(MParticleDatabase.BreadcrumbTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(MParticleDatabase.BreadcrumbTable.SESSION_ID, getMessageSessionId(message));
        contentValues.put(MParticleDatabase.BreadcrumbTable.MESSAGE, message.toString());


        db.insert(BreadcrumbTable.TABLE_NAME, null, contentValues);
        Cursor cursor = db.query(BreadcrumbTable.TABLE_NAME, idColumns, null, null, null, null, " _id desc limit 1");
        if (cursor.moveToFirst()){
            int maxId = cursor.getInt(0);
            if (maxId > MParticle.getInstance().mConfigManager.getBreadcrumbLimit()){
                String[] limit = {Integer.toString(maxId - MParticle.getInstance().mConfigManager.getBreadcrumbLimit())};
                db.delete(BreadcrumbTable.TABLE_NAME, " _id < ?", limit);
            }
        }
    }

    private void dbInsertSession(JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SessionTable.API_KEY, mApiKey);
        contentValues.put(SessionTable.SESSION_ID, message.getString(MessageKey.ID));
        contentValues.put(SessionTable.START_TIME, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(SessionTable.END_TIME, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(SessionTable.SESSION_LENGTH, 0);
        db.insert(SessionTable.TABLE_NAME, null, contentValues);
    }

    private void dbInsertMessage(JSONObject message) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTable.API_KEY, mApiKey);
        contentValues.put(MessageTable.CREATED_AT, message.getLong(MessageKey.TIMESTAMP));
        contentValues.put(MessageTable.SESSION_ID, getMessageSessionId(message));
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
            sessionValues.put(SessionTable.SESSION_LENGTH, sessionLength);
        }
        String[] whereArgs = {sessionId};
        db.update(SessionTable.TABLE_NAME, sessionValues, SessionTable.SESSION_ID + "=?", whereArgs);
    }

    // helper method for getting a session id out of a message since
    // session-start messages use the id field
    private String getMessageSessionId(JSONObject message) throws JSONException {
        String sessionId;
        if (MessageType.SESSION_START.equals(message.getString(MessageKey.TYPE))) {
            sessionId = message.getString(MessageKey.ID);
        } else {
            sessionId = message.optString(MessageKey.SESSION_ID, "NO-SESSION");
        }
        return sessionId;
    }
}
