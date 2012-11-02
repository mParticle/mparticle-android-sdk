package com.mparticle;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.Status;
import com.mparticle.MessageDatabase.MessageTable;
import com.mparticle.MessageDatabase.SessionTable;

//TODO: this should be package-private but is accessed from the demo
@SuppressWarnings("javadoc")
public class MessageManager {

    private static final String TAG = "mParticleAPI";

    private static MessageManager sMessageManager;
    private static HandlerThread sMessageHandlerThread;
    private MessageHandler mMessageHandler;
    private static HandlerThread sUploadHandlerThread;
    private UploadHandler mUploadHandler;

    private Context mContext;

    private static String sActiveNetworkName;
    private static Location sLocation;

    private MessageManager(Context context, String apiKey, String secret) {
        mContext = context;
        mMessageHandler = new MessageHandler(mContext, sMessageHandlerThread.getLooper());
        mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
        mUploadHandler = new UploadHandler(mContext, sUploadHandlerThread.getLooper(), apiKey, secret);
        mUploadHandler.sendEmptyMessage(UploadHandler.FETCH_CONFIG);
    }

    public static MessageManager getInstance(Context context, String apiKey, String secret) {
        if (null == MessageManager.sMessageManager) {
            sMessageHandlerThread = new HandlerThread("mParticleMessageHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sMessageHandlerThread.start();
            sUploadHandlerThread = new HandlerThread("mParticleUploadHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sUploadHandlerThread.start();
            MessageManager.sMessageManager = new MessageManager(context, apiKey, secret);

            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            BroadcastReceiver receiver = new NetworkStatusBroadcastReceiver((MessageManager)sMessageManager);
            context.registerReceiver(receiver, filter);

        }
        return MessageManager.sMessageManager;
    }

    /* package-private */ static JSONObject createMessage(String messageType, String sessionId, long sessionStart, long time, String name, JSONObject attributes, boolean includeLocation) throws JSONException {
            JSONObject message = new JSONObject();
            message.put(MessageKey.TYPE, messageType);
            message.put(MessageKey.TIMESTAMP, time);
            if (MessageType.SESSION_START==messageType) {
                message.put(MessageKey.ID, sessionId);
            } else {
                message.put(MessageKey.SESSION_ID, sessionId);
                message.put(MessageKey.ID, UUID.randomUUID().toString());
                if (sessionStart>0) {
                    message.put(MessageKey.SESSION_START_TIMESTAMP, sessionStart);
                }
            }
            if (null != name) {
                message.put(MessageKey.NAME, name);
            }
            if (null != attributes) {
                message.put(MessageKey.ATTRIBUTES, attributes);
            }
            if (null != sActiveNetworkName) {
                message.put(MessageKey.DATA_CONNECTION, sActiveNetworkName);
            }
            if (includeLocation && null!=sLocation) {
                JSONObject locJSON = new JSONObject();
                locJSON.put(MessageKey.LATITUDE, sLocation.getLatitude());
                locJSON.put(MessageKey.LONGITUDE, sLocation.getLongitude());
                locJSON.put(MessageKey.ACCURACY, sLocation.getAccuracy());
                message.put(MessageKey.LOCATION, locJSON);
            }
            return message;
    }

    /* package-private */ static JSONObject createMessageSessionEnd(String sessionId, long start, long end, long length, JSONObject attributes) throws JSONException {
        JSONObject message = MessageManager.createMessage(MessageType.SESSION_END, sessionId, start, end, null, attributes, true);
        message.put(MessageKey.SESSION_LENGTH, length);
        return message;
    }

    public void startSession(String sessionId, long time) {
        try {
            JSONObject message = createMessage(MessageType.SESSION_START, sessionId, time, time, null, null, true);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.PENDING, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start session message");
        }
    }
    public void stopSession(String sessionId, long stopTime, long sessionLength) {
        try {
            JSONObject sessionTiming=new JSONObject();
            sessionTiming.put(MessageKey.SESSION_ID, sessionId);
            sessionTiming.put(MessageKey.TIMESTAMP, stopTime);
            sessionTiming.put(MessageKey.SESSION_LENGTH, sessionLength);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, sessionTiming));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to send update session end message");
        }
    }
    public void endSession(String sessionId, long stopTime, long sessionLength) {
        stopSession(sessionId, stopTime, sessionLength);
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
    }
    public void logCustomEvent(String sessionId, long sessionStartTime, long time, String eventName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.CUSTOM_EVENT, sessionId, sessionStartTime, time, eventName, attributes, true);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start event message");
        }
    }
    public void logScreenView(String sessionId, long sessionStartTime, long time, String screenName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.SCREEN_VIEW, sessionId, sessionStartTime, time, screenName, attributes, true);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle screen view message");
        }
    }
    public void optOut(String sessionId, long sessionStartTime, long time, boolean optOutStatus) {
        try {
            JSONObject message = createMessage(MessageType.OPT_OUT, sessionId, sessionStartTime, time, null, null, false);
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle screen view message");
        }
    }
    public void setSessionAttributes(String sessionId, JSONObject mSessionAttributes) {
        try {
            JSONObject sessionAttributes=new JSONObject();
            sessionAttributes.put(MessageKey.SESSION_ID, sessionId);
            sessionAttributes.put(MessageKey.ATTRIBUTES, mSessionAttributes);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_ATTRIBUTES, sessionAttributes));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to send update session attributes message");
        }
    }

    public void doUpload() {
        mUploadHandler.sendEmptyMessage(UploadHandler.PREPARE_UPLOADS);
    }

    public static void setLocation(Location location) {
        sLocation=location;
    }

    public static final class MessageHandler extends Handler {
        private MessageDatabase mDB;
        private Context mContext;

        public static final int STORE_MESSAGE = 0;
        public static final int UPDATE_SESSION_ATTRIBUTES = 2;
        public static final int UPDATE_SESSION_END = 3;
        public static final int CREATE_SESSION_END_MESSAGE = 4;
        public static final int END_ORPHAN_SESSIONS = 5;

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
                        dbUpdateSessionEndTime(db, getMessageSessionId(message), message.getLong(MessageKey.TIMESTAMP), 0);
                    }
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error saving event to mParticle DB", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error with JSON object", e);
                } finally {
                    mDB.close();
                }
                break;
            case UPDATE_SESSION_ATTRIBUTES:
                try {
                    JSONObject sessionAttributes = (JSONObject) msg.obj;
                    String sessionId = sessionAttributes.getString(MessageKey.SESSION_ID);
                    String attributes = sessionAttributes.getString(MessageKey.ATTRIBUTES);
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    dbUpdateSessionAttributes(db, sessionId, attributes);
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error updating session attributes in mParticle DB", e);
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
                    long sessionLength = sessionTiming.getLong(MessageKey.SESSION_LENGTH);
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    dbUpdateSessionEndTime(db, sessionId, time, sessionLength);
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
                    String[] selectionArgs = new String[]{sessionId};
                    String[] sessionColumns = new String[]{SessionTable.START_TIME, SessionTable.END_TIME, SessionTable.SESSION_LENGTH, SessionTable.ATTRIBUTES};
                    Cursor selectCursor = db.query(SessionTable.TABLE_NAME, sessionColumns, SessionTable.SESSION_ID+"=?", selectionArgs, null, null, null);
                    selectCursor.moveToFirst();
                    long start = selectCursor.getLong(0);
                    long end = selectCursor.getLong(1);
                    long length = selectCursor.getLong(2);
                    String attributes = selectCursor.getString(3);
                    JSONObject sessionAttributes=null;
                    if (null!=attributes) {
                        sessionAttributes= new JSONObject(attributes);
                    }

                    // create a session-end message
                    JSONObject endMessage = MessageManager.createMessageSessionEnd(sessionId, start, end, length, sessionAttributes);

                    // insert the record into messages with duration
                    dbInsertMessage(db, endMessage, Status.READY);

                    // mark session messages ready for BATCH mode upload
                    dbUpdateMessageStatus(db, sessionId, Status.BATCH_READY);

                    // delete the processed session record
                    db.delete(SessionTable.TABLE_NAME, SessionTable.SESSION_ID + "=?", new String[]{sessionId});

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
                    Cursor selectCursor = db.query(SessionTable.TABLE_NAME, sessionColumns,
                            SessionTable.STATUS+"!="+Status.ENDED, null, null, null, null);
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
            values.put(SessionTable.SESSION_LENGTH, 0);
            values.put(SessionTable.STATUS, Status.PENDING);
            db.insert(SessionTable.TABLE_NAME, null, values);
        }

        private void dbInsertMessage(SQLiteDatabase db, JSONObject message, int status) throws JSONException {
            String messageType = message.getString(MessageKey.TYPE);
            ContentValues contentValues = new ContentValues();
            contentValues.put(MessageTable.MESSAGE_TYPE, messageType);
            contentValues.put(MessageTable.MESSAGE_TIME, message.getLong(MessageKey.TIMESTAMP));
            contentValues.put(MessageTable.UUID, message.getString(MessageKey.ID));
            contentValues.put(MessageTable.SESSION_ID, getMessageSessionId(message));
            contentValues.put(MessageTable.MESSAGE, message.toString());
            contentValues.put(MessageTable.STATUS, status);
            db.insert(MessageTable.TABLE_NAME, null, contentValues);
        }

        private void dbUpdateMessageStatus(SQLiteDatabase db, String sessionId, long status) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MessageTable.STATUS, status);
            String[] whereArgs = {sessionId };
            db.update(MessageTable.TABLE_NAME, contentValues, MessageTable.SESSION_ID + "=?", whereArgs);
        }

        private void dbUpdateSessionAttributes(SQLiteDatabase db, String sessionId, String attributes) {
            ContentValues sessionValues = new ContentValues();
            sessionValues.put(SessionTable.ATTRIBUTES, attributes);
            String[] whereArgs = {sessionId };
            db.update(SessionTable.TABLE_NAME, sessionValues, SessionTable.SESSION_ID + "=?", whereArgs);
        }

        private void dbUpdateSessionEndTime(SQLiteDatabase db, String sessionId, long endTime, long sessionLength) {
            ContentValues sessionValues = new ContentValues();
            sessionValues.put(SessionTable.END_TIME, endTime);
            if (sessionLength>0) {
                sessionValues.put(SessionTable.SESSION_LENGTH, sessionLength);
            }
            String[] whereArgs = {sessionId };
            db.update(SessionTable.TABLE_NAME, sessionValues, SessionTable.SESSION_ID + "=?", whereArgs);
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

    /* Possibly for development only */
    public void setConnectionProxy(String host, int port) {
        mUploadHandler.setConnectionProxy(host, port);
    }

    private static class NetworkStatusBroadcastReceiver extends BroadcastReceiver {
        WeakReference<MessageManager> mWeakMessageManager;
        public NetworkStatusBroadcastReceiver(MessageManager messageManager) {
            super();
            mWeakMessageManager = new WeakReference<MessageManager>(messageManager);
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivyManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                MessageManager messageManager = mWeakMessageManager.get();
                if (messageManager!=null) {
                    messageManager.setDataConnection(activeNetwork);
                }
            }
        }
    }

    public void setDataConnection(NetworkInfo activeNetwork) {
        if (null!=activeNetwork) {
            sActiveNetworkName = activeNetwork.getTypeName().toLowerCase();
        } else {
            sActiveNetworkName = null;
        }
    }

}
