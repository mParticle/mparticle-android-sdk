package com.mparticle;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.mparticle.MessageDatabase.MessageTable;
import com.mparticle.MessageDatabase.SessionTable;

import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.UploadStatus;

public class MessageManager {

    private final static String TAG = "MParticle";

    private static MessageManager sMessageManager;
    private static HandlerThread sMessageHandlerThread;

    private Context mContext;

    private MessageHandler mMessageHandler;

    private MessageManager(Context context) {
        mContext = context.getApplicationContext();
        mMessageHandler = new MessageHandler(mContext, sMessageHandlerThread.getLooper());
        mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
    }

    public static MessageManager getInstance(Context context) {
        if (null == MessageManager.sMessageManager) {
            sMessageHandlerThread = new HandlerThread("mParticleMessageHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sMessageHandlerThread.start();
            MessageManager.sMessageManager = new MessageManager(context);
        }
        return MessageManager.sMessageManager;
    }

    /* package-private */ static JSONObject createMessage(String messageType, String sessionId, long time, String name, JSONObject attributes) throws JSONException {
            JSONObject message = new JSONObject();
            message.put(MessageKey.TYPE, messageType);
            message.put(MessageKey.TIMESTAMP, time);
            if (MessageType.SESSION_START==messageType) {
                message.put(MessageKey.ID, sessionId);
            } else {
                message.put(MessageKey.SESSION_ID, sessionId);
                message.put(MessageKey.ID, UUID.randomUUID().toString());
            }
            if (null != name) {
                message.put(MessageKey.NAME, name);
            }
            if (null != attributes) {
                message.put(MessageKey.ATTRIBUTES, attributes);
            }
            return message;
    }

    public void startSession(String sessionId, long time) {
        try {
            JSONObject message = createMessage(MessageType.SESSION_START, sessionId, time, null, null);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, UploadStatus.PENDING, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start session message");
        }
    }
    public void stopSession(String sessionId, long time) {
        // TODO: this should pass the time value
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, sessionId));
    }
    public void endSession(String sessionId, long time) {
        stopSession(sessionId, time);
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
    }
    public void logCustomEvent(String sessionId, long time, String eventName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.CUSTOM_EVENT, sessionId, time, eventName, attributes);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, UploadStatus.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start event message");
        }
    }
    public void logScreenView(String sessionId, long time, String screenName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.SCREEN_VIEW, sessionId, time, screenName, attributes);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, UploadStatus.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle screen view message");
        }
    }

    public static final class MessageHandler extends Handler {
        private MessageDatabase mDB;
        private Context mContext;

        public static final int STORE_MESSAGE = 0;
        public static final int UPDATE_SESSION_END = 1;
        public static final int CREATE_SESSION_END_MESSAGE = 2;
        public static final int END_ORPHAN_SESSIONS = 3;

        public MessageHandler(Context context, Looper looper) {
            super(looper);
            mContext = context;
            mDB = new MessageDatabase(mContext);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
            case STORE_MESSAGE:
                try {
                    JSONObject message = (JSONObject) msg.obj;
                    int messageStatus = msg.arg1;
                    String messageType = message.getString(MessageKey.TYPE);
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    // handle the special case of session-start by creating the session record first
                    if (MessageType.SESSION_START==messageType) {
                        insertSession(db, message);
                    }
                    insertMessage(db, message, messageStatus);

                    if (MessageType.SESSION_START!=messageType) {
                        ContentValues sessionValues = new ContentValues();
                        sessionValues.put(SessionTable.END_TIME, message.getLong(MessageKey.TIMESTAMP));
                        String[] whereArgs = {getMessageSessionId(message),
                                message.getString(MessageKey.TIMESTAMP) };
                        db.update("sessions", sessionValues, SessionTable.SESSION_ID + "=? and " +
                                                                    SessionTable.END_TIME+"<?", whereArgs);
                    }
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error saving event to mParticle DB", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error with JSON object", e);
                } finally {
                    mDB.close();
                }
                break;
            case UPDATE_SESSION_END:
                // TODO: consider verifying that the time > time-in-table
                // TODO: use the requested time rather than the system time
                try {
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(SessionTable.END_TIME, System.currentTimeMillis());
                    String[] whereArgs = new String[]{(String) msg.obj};
                    db.update("sessions", values, SessionTable.SESSION_ID+"=?", whereArgs);
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error updating session end time in mParticle DB", e);
                } finally {
                    mDB.close();
                }
                break;
            case CREATE_SESSION_END_MESSAGE:
                // TODO: this is possibly an upload handler message
                try {
                    String sessionId = (String) msg.obj;
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    // select the session and get the start/end times
                    String[] selectionArgs = new String[]{sessionId};
                    String[] sessionColumns = new String[]{SessionTable.START_TIME, SessionTable.END_TIME, SessionTable.ATTRIBUTES};
                    Cursor selectCursor = db.query("sessions", sessionColumns, SessionTable.SESSION_ID+"=?", selectionArgs, null, null, null);
                    selectCursor.moveToFirst();
                    long start = selectCursor.getLong(0);
                    long end = selectCursor.getLong(1);
                    // NOTE: not currently using session attributes
                    // String sessionAttributes = selectCursor.getString(2);

                    // create a session-end message and add the calculated duration
                    JSONObject endMessage = MessageManager.createMessage(MessageType.SESSION_END, sessionId, end, null, null);
                    endMessage.put(MessageKey.SESSION_LENGTH, (end-start)/1000);

                    // insert the record into messages with duration
                    insertMessage(db, endMessage, UploadStatus.READY);

                    // update session status
                    ContentValues sessionValues = new ContentValues();
                    sessionValues.put(SessionTable.UPLOAD_STATUS, UploadStatus.ENDED);
                    db.update("sessions", sessionValues, SessionTable.SESSION_ID + "=?", new String[]{sessionId});

                } catch (SQLiteException e) {
                    Log.e(TAG, "Error creating session end message in mParticle DB", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating session end message in mParticle DB", e);
                } finally {
                    mDB.close();
                }
                break;
            case END_ORPHAN_SESSIONS:
                try {
                    // find sessions without session-end message and create them
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    String[] sessionColumns = new String[]{SessionTable.SESSION_ID};
                    Cursor selectCursor = db.query("sessions", sessionColumns,
                            SessionTable.UPLOAD_STATUS+"!="+UploadStatus.ENDED, null, null, null, null);
                    // NOTE: there should be at most one orphan. but process any that are found
                    while (selectCursor.moveToNext()) {
                        String sessionId = selectCursor.getString(0);
                        sendMessage(obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
                    }
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error processing initialization in mParticle DB", e);
                } finally {
                    mDB.close();
                }
                break;
            default:
                break;
            }
        }

        private void insertSession(SQLiteDatabase db, JSONObject message) throws JSONException {
            ContentValues values = new ContentValues();
            values.put(SessionTable.SESSION_ID,  message.getString(MessageKey.ID));
            long sessionStartTime =  message.getLong(MessageKey.TIMESTAMP);
            values.put(SessionTable.START_TIME, sessionStartTime);
            values.put(SessionTable.END_TIME, sessionStartTime);
            values.put(SessionTable.UPLOAD_STATUS, UploadStatus.PENDING);
            db.insert("sessions", null, values);
        }

        private void insertMessage(SQLiteDatabase db, JSONObject message, int status) throws JSONException {
            String messageType = message.getString(MessageKey.TYPE);
            ContentValues contentValues = new ContentValues();
            contentValues.put(MessageTable.MESSAGE_TYPE, messageType);
            contentValues.put(MessageTable.MESSAGE_TIME, message.getLong(MessageKey.TIMESTAMP));
            contentValues.put(MessageTable.UUID, message.getString(MessageKey.ID));
            contentValues.put(MessageTable.SESSION_ID, getMessageSessionId(message));
            contentValues.put(MessageTable.MESSAGE, message.toString());
            contentValues.put(MessageTable.UPLOAD_STATUS, status);
            db.insert("messages", null, contentValues);
        }

        // helper method for getting a session id out of a message since session-start messages use the id field
        private String getMessageSessionId(JSONObject message) throws JSONException {
            String sessionId;
            if (MessageType.SESSION_START==message.getString(MessageKey.TYPE)) {
                sessionId= message.getString(MessageKey.ID);
            } else {
                sessionId= message.getString(MessageKey.SESSION_ID);
            }
            return sessionId;

        }
    }
}
