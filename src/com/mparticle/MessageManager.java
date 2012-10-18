package com.mparticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.mparticle.MessageDatabase.MessageTable;

public class MessageManager {

    private final static String TAG = "MParticle";

    private static MessageManager sMessageManager;
    private static HandlerThread sMessageHandlerThread;

    private Context mContext;
    // temporary list for development/testing
    public List<JSONObject> messages = new ArrayList<JSONObject>();

    private MessageHandler mMessageHandler;

    public interface MessageType {
        public static final String SESSION_START = "ss";
        public static final String SESSION_END = "se";
        public static final String CUSTOM_EVENT = "e";
        public static final String SCREEN_VIEW = "v";
        public static final String OPT_OUT = "o";
    }

    public interface MessageKey {
        public static final String TYPE = "dt";
        public static final String ID = "id";
        public static final String TIMESTAMP = "ct";
        public static final String APPLICATION_KEY = "a";
        public static final String APPLICATION_VERSION = "av";
        public static final String MPARTICLE_VERSION = "sdk";
        public static final String DATA_CONNECTION = "dct";
        public static final String LATITUDE = "lat";
        public static final String LONGITUDE = "lng";
        public static final String SESSION_ID = "sid";
        public static final String SESSION_LENGTH = "sls";
        public static final String ATTRIBUTES = "attrs";
        public static final String NAME = "n";
        // device keys
        public static final String DEVICE_ID = "duid";
        public static final String MANUFACTURER = "dma";
        public static final String PLATFORM = "dp";
        public static final String OS_VERSION = "dosv";
        public static final String MODEL = "dmdl";
        public static final String SCREEN_HEIGHT = "dsh";
        public static final String SCREEN_WIDTH = "dsw";
        public static final String DEVICE_COUNTRY = "dc";
        public static final String DEVICE_LOCALE_COUNTRY = "dlc";
        public static final String DEVICE_LOCALE_LANGUAGE = "dll";
        // network keys
        public static final String NETWORK_COUNTRY = "nc";
        public static final String NETWORK_CARRIER = "nca";
        public static final String MOBILE_NETWORK_CODE = "mnc";
        public static final String MOBILE_COUNTRY_CODE = "mcc";
    }

    private MessageManager(Context context) {
        mContext = context.getApplicationContext();
        mMessageHandler = new MessageHandler(mContext, sMessageHandlerThread.getLooper());
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

    public void storeMessage(String messageType, Map<String, String> eventData) {
        try {
            JSONObject eventObject = new JSONObject();
            eventObject.put(MessageKey.TYPE, messageType);
            eventObject.put(MessageKey.ID, UUID.randomUUID().toString());
            eventObject.put(MessageKey.TIMESTAMP, System.currentTimeMillis());
            if (null != eventData) {
                eventObject.put(MessageKey.ATTRIBUTES, eventData);
            }
            messages.add(eventObject);

            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, eventObject));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static final class MessageHandler extends Handler {
        private MessageDatabase mDB;
        private Context mContext;

        public static final int STORE_MESSAGE = 0;

        public MessageHandler(Context context, Looper looper) {
            super(looper);
            mContext = context;
            mDB = new MessageDatabase(mContext);

            Log.i(TAG, "Instatiated a message handler");
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
            case STORE_MESSAGE:
                JSONObject eventObject = (JSONObject) msg.obj;

                try {
                    SQLiteDatabase db = mDB.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put(MessageTable.MESSAGE_TYPE, eventObject.getString(MessageKey.TYPE));
                    values.put(MessageTable.MESSAGE_TIME, eventObject.getLong(MessageKey.TIMESTAMP));
                    values.put(MessageTable.UUID, eventObject.getString(MessageKey.ID));
                    values.put(MessageTable.MESSAGE, eventObject.toString());
                    db.insert("messages", null, values);
                } catch (SQLiteException e) {
                    Log.e(TAG, "Error saving event to mParticle DB", e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error with JSON object", e);
                } finally {
                    mDB.close();
                }
                break;

            default:
                break;
            }

        }

    }

}
