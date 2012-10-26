package com.mparticle;

import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
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

    private static final String TAG = "mParticleAPI";

    private static MessageManager sMessageManager;
    private static HandlerThread sMessageHandlerThread;
    private MessageHandler mMessageHandler;
    private static HandlerThread sUploadHandlerThread;
    private UploadHandler mUploadHandler;

    private Context mContext;
    private static Location sLocation;

    private MessageManager(Context context) {
        mContext = context.getApplicationContext();
        mMessageHandler = new MessageHandler(mContext, sMessageHandlerThread.getLooper());
        mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
        mUploadHandler = new UploadHandler(mContext, sUploadHandlerThread.getLooper());
    }

    public static MessageManager getInstance(Context context) {
        if (null == MessageManager.sMessageManager) {
            sMessageHandlerThread = new HandlerThread("mParticleMessageHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sMessageHandlerThread.start();
            sUploadHandlerThread = new HandlerThread("mParticleUploadHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sUploadHandlerThread.start();
            MessageManager.sMessageManager = new MessageManager(context);
        }
        return MessageManager.sMessageManager;
    }

    /* package-private */ static JSONObject createMessage(String messageType, String sessionId, long sessionStart, long time, String name, JSONObject attributes, boolean includeLocation) throws JSONException {
            JSONObject message = new JSONObject();
            message.put(MessageKey.TYPE, messageType);
            message.put(MessageKey.TIMESTAMP, time/1000);
            if (MessageType.SESSION_START==messageType) {
                message.put(MessageKey.ID, sessionId);
            } else {
                message.put(MessageKey.SESSION_ID, sessionId);
                message.put(MessageKey.ID, UUID.randomUUID().toString());
                if (sessionStart>0) {
                    message.put(MessageKey.SESSION_START_TIMESTAMP, sessionStart/1000);
                }
            }
            if (MessageType.SESSION_END==messageType) {
                message.put(MessageKey.SESSION_LENGTH, (time-sessionStart)/1000);
            }
            if (null != name) {
                message.put(MessageKey.NAME, name);
            }
            if (null != attributes) {
                message.put(MessageKey.ATTRIBUTES, attributes);
            }
            if (includeLocation && null!=sLocation) {
                message.put(MessageKey.DATA_CONNECTION, sLocation.getProvider());
                message.put(MessageKey.LATITUDE, sLocation.getLatitude());
                message.put(MessageKey.LONGITUDE, sLocation.getLongitude());
            }
            return message;
    }

    public void startSession(String sessionId, long time) {
        try {
            JSONObject message = createMessage(MessageType.SESSION_START, sessionId, time, time, null, null, true);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, UploadStatus.PENDING, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start session message");
        }
    }
    public void stopSession(String sessionId, long sessionStartTime, long time) {
        try {
            JSONObject sessionTiming=new JSONObject();
            sessionTiming.put(MessageKey.SESSION_ID, sessionId);
            sessionTiming.put(MessageKey.TIMESTAMP, time);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, sessionTiming));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to send update session end message");
        }
    }
    public void endSession(String sessionId, long sessionStartTime, long time) {
        stopSession(sessionId, sessionStartTime, time);
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
    }
    public void logCustomEvent(String sessionId, long sessionStartTime, long time, String eventName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.CUSTOM_EVENT, sessionId, sessionStartTime, time, eventName, attributes, true);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, UploadStatus.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start event message");
        }
    }
    public void logScreenView(String sessionId, long sessionStartTime, long time, String screenName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.SCREEN_VIEW, sessionId, sessionStartTime, time, screenName, attributes, true);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, UploadStatus.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle screen view message");
        }
    }

    public void doUpload() {
        mUploadHandler.sendEmptyMessage(UploadHandler.PREPARE_BATCHES);
    }

    public static void setLocation(Location location) {
        sLocation=location;
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
                        dbInsertSession(db, message);
                    }
                    dbInsertMessage(db, message, messageStatus);

                    if (MessageType.SESSION_START!=messageType) {
                        dbUpdateSessionEndTime(db, getMessageSessionId(message), message.getLong(MessageKey.TIMESTAMP));
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
                try {
                    JSONObject sessionTiming = (JSONObject) msg.obj;
                    String sessionId = sessionTiming.getString(MessageKey.SESSION_ID);
                    long time = sessionTiming.getLong(MessageKey.TIMESTAMP);
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    dbUpdateSessionEndTime(db, sessionId, time);
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error updating session end time in mParticle DB", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error with JSON object", e);
                } finally {
                    mDB.close();
                }
                break;
            case CREATE_SESSION_END_MESSAGE:
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
                    // TODO: not yet using session attributes
                    // String sessionAttributes = selectCursor.getString(2);

                    // create a session-end message
                    JSONObject endMessage = MessageManager.createMessage(MessageType.SESSION_END, sessionId, start, end, null, null, true);

                    // insert the record into messages with duration
                    dbInsertMessage(db, endMessage, UploadStatus.READY);

                    // update session status
                    ContentValues sessionValues = new ContentValues();
                    sessionValues.put(SessionTable.UPLOAD_STATUS, UploadStatus.ENDED);
                    db.update("sessions", sessionValues, SessionTable.SESSION_ID + "=?", new String[]{sessionId});

                } catch (SQLiteException e) {
                    Log.e(TAG, "Error creating session end message in mParticle DB", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error with JSON object", e);
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
                    // NOTE: there should be at most one orphan - but process any that are found
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

        private void dbInsertSession(SQLiteDatabase db, JSONObject message) throws JSONException {
            ContentValues values = new ContentValues();
            values.put(SessionTable.SESSION_ID,  message.getString(MessageKey.ID));
            long sessionStartTime =  message.getLong(MessageKey.TIMESTAMP);
            values.put(SessionTable.START_TIME, sessionStartTime);
            values.put(SessionTable.END_TIME, sessionStartTime);
            values.put(SessionTable.UPLOAD_STATUS, UploadStatus.PENDING);
            db.insert("sessions", null, values);
        }

        private void dbInsertMessage(SQLiteDatabase db, JSONObject message, int status) throws JSONException {
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

        private void dbUpdateSessionEndTime(SQLiteDatabase db, String sessionId, long endTime) {
            ContentValues sessionValues = new ContentValues();
            sessionValues.put(SessionTable.END_TIME, endTime);
            String[] whereArgs = {sessionId, Long.toString(endTime) };
            db.update("sessions", sessionValues, SessionTable.SESSION_ID + "=? and " +
                                                        SessionTable.END_TIME+"<?", whereArgs);

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
