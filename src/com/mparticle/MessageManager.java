package com.mparticle;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.Status;

//TODO: this should be package-private but is accessed from the tests
@SuppressWarnings("javadoc")
public class MessageManager {

    private static final String TAG = Constants.LOG_TAG;

    private static MessageManager sMessageManager;
    private static HandlerThread sMessageHandlerThread;
    private MessageHandler mMessageHandler;
    private static HandlerThread sUploadHandlerThread;
    private UploadHandler mUploadHandler;

    private static String sActiveNetworkName = "offline";
    private static Location sLocation;

    private MessageManager(Context appContext, String apiKey, String secret, long uploadInterval) {
        mMessageHandler = new MessageHandler(appContext, sMessageHandlerThread.getLooper());
        mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
        mUploadHandler = new UploadHandler(appContext, sUploadHandlerThread.getLooper(), apiKey, secret, uploadInterval);
        mUploadHandler.sendEmptyMessage(UploadHandler.FETCH_CONFIG);
        mUploadHandler.sendEmptyMessageDelayed(UploadHandler.PERIODIC_UPLOAD, Constants.INITIAL_UPLOAD_DELAY);
    }

    public static MessageManager getInstance(Context appContext, String apiKey, String secret, long uploadInterval) {
        if (null == MessageManager.sMessageManager) {
            sMessageHandlerThread = new HandlerThread("mParticleMessageHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sMessageHandlerThread.start();
            sUploadHandlerThread = new HandlerThread("mParticleUploadHandlerThread",
                    Process.THREAD_PRIORITY_BACKGROUND);
            sUploadHandlerThread.start();
            MessageManager.sMessageManager = new MessageManager(appContext, apiKey, secret, uploadInterval);

            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            BroadcastReceiver receiver = new NetworkStatusBroadcastReceiver((MessageManager)sMessageManager);
            appContext.registerReceiver(receiver, filter);

            ExceptionHandler exHandler = new ExceptionHandler(MessageManager.sMessageManager, Thread.getDefaultUncaughtExceptionHandler());
            Thread.currentThread().setUncaughtExceptionHandler(exHandler);
        }
        return MessageManager.sMessageManager;
    }

    /* package-private */ static JSONObject createMessage(String messageType, String sessionId, long sessionStart, long time, String name, JSONObject attributes, boolean includeConnLoc) throws JSONException {
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
            if (includeConnLoc) {
                message.put(MessageKey.DATA_CONNECTION, sActiveNetworkName);
                if (null!=sLocation) {
                    JSONObject locJSON = new JSONObject();
                    locJSON.put(MessageKey.LATITUDE, sLocation.getLatitude());
                    locJSON.put(MessageKey.LONGITUDE, sLocation.getLongitude());
                    locJSON.put(MessageKey.ACCURACY, sLocation.getAccuracy());
                    message.put(MessageKey.LOCATION, locJSON);
                }
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
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.READY, 0, message));
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
    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, JSONObject attributes, Throwable t) {
        try {
            JSONObject message = createMessage(MessageType.ERROR, sessionId, sessionStartTime, time, null, attributes, false);
            if (null!=t) {
                message.put(MessageKey.ERROR_SEVERITY, "Exception");
                message.put(MessageKey.ERROR_CLASS, t.getClass().getCanonicalName());
                message.put(MessageKey.ERROR_MESSAGE, t.getMessage());
                message.put(MessageKey.ERROR_STACK_TRACE, "TBD");
                // TODO: stack trace
            } else {
                message.put(MessageKey.ERROR_SEVERITY, "Error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle error message");
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
        public void onReceive(Context appContext, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivyManager = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
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
            String activeNetworkName = activeNetwork.getTypeName();
            if (0!=activeNetwork.getSubtype()) {
                activeNetworkName += "/" + activeNetwork.getSubtypeName();
            }
            sActiveNetworkName = activeNetworkName.toLowerCase();
        } else {
            sActiveNetworkName = "offline";
        }
    }

}
