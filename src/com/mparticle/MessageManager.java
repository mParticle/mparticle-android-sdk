package com.mparticle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import com.mparticle.Constants.ConfigKeys;
import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.Constants.Status;

/* package-private */ class MessageManager {

    private static final String TAG = Constants.LOG_TAG;

    private static final HandlerThread sMessageHandlerThread = new HandlerThread("mParticleMessageHandler", Process.THREAD_PRIORITY_BACKGROUND);
    private static final HandlerThread sUploadHandlerThread = new HandlerThread("mParticleUploadHandler", Process.THREAD_PRIORITY_BACKGROUND);

    private static String sActiveNetworkName = "offline";
    private static Location sLocation;
    private static boolean sDebugMode;

    private final MessageHandler mMessageHandler;
    private final UploadHandler mUploadHandler;

    // This constructor is needed to enable mocking with Mockito and Dexmaker and should never be called
    /* package-private */ MessageManager() { throw new UnsupportedOperationException(); }

    // Package-private constructor for unit tests. Use getInstance to return the MessageManager
    /* package-private */ MessageManager(MessageHandler messageHandler, UploadHandler uploadHandler) {
        mMessageHandler = messageHandler;
        mUploadHandler = uploadHandler;
    }

    public static MessageManager getInstance(Context appContext, String apiKey, String secret, Properties config) {
        if (!sMessageHandlerThread.isAlive()) {
            sMessageHandlerThread.start();
            sUploadHandlerThread.start();

            // note: if permissions are not correct all messages will be tagged as 'offline'
            if (PackageManager.PERMISSION_GRANTED ==
                    appContext.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                BroadcastReceiver receiver = new NetworkStatusBroadcastReceiver();
                appContext.registerReceiver(receiver, filter);
            }
        }

        MessageHandler messageHandler = new MessageHandler(appContext, sMessageHandlerThread.getLooper(), apiKey);
        UploadHandler uploadHandler = new UploadHandler(appContext, sUploadHandlerThread.getLooper(), apiKey, secret);

        MessageManager messageManager = new MessageManager(messageHandler, uploadHandler);

        if (config.containsKey(ConfigKeys.UPLOAD_INTERVAL)) {
            try {
                messageManager.setUploadInterval(1000 * Integer.parseInt(config.getProperty(ConfigKeys.UPLOAD_INTERVAL)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.UPLOAD_INTERVAL + "' setting");
            }
        }
        if (config.containsKey(ConfigKeys.ENABLE_SSL)) {
            try {
                messageManager.setSecureTransport(Boolean.parseBoolean(config.getProperty(ConfigKeys.ENABLE_SSL)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.ENABLE_SSL + "' setting");
            }
        }
        if (config.containsKey(ConfigKeys.PROXY_HOST) && config.containsKey(ConfigKeys.PROXY_PORT) ) {
            try {
                messageManager.setConnectionProxy(config.getProperty(ConfigKeys.PROXY_HOST),
                        Integer.parseInt(config.getProperty(ConfigKeys.PROXY_PORT)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.PROXY_HOST +
                        "' and '" + ConfigKeys.PROXY_PORT + "' settings");
            }
        }
        if (config.containsKey(ConfigKeys.ENABLE_COMPRESSION)) {
            try {
                uploadHandler.setCompressionEnabled(Boolean.parseBoolean(config.getProperty(ConfigKeys.ENABLE_COMPRESSION)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.ENABLE_COMPRESSION + "' setting");
            }
        }

        messageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
        uploadHandler.sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
        uploadHandler.sendEmptyMessageDelayed(UploadHandler.CLEANUP, Constants.INITIAL_UPLOAD_DELAY);

        return messageManager;
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
        JSONObject message = createMessage(MessageType.SESSION_END, sessionId, start, end, null, attributes, true);
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
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, 0);
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

    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t) {
        try {
            JSONObject message = createMessage(MessageType.ERROR, sessionId, sessionStartTime, time, null, null, false);
            if (null!=t) {
                message.put(MessageKey.ERROR_SEVERITY, "fatal");
                message.put(MessageKey.ERROR_CLASS, t.getClass().getCanonicalName());
                message.put(MessageKey.ERROR_MESSAGE, t.getMessage());
                StringWriter stringWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(stringWriter));
                message.put(MessageKey.ERROR_STACK_TRACE, stringWriter.toString());
            } else {
                message.put(MessageKey.ERROR_SEVERITY, "error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, Status.READY, 0, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle error message");
        }
    }

    public void setPushRegistrationId(String sessionId, long sessionStartTime, long time, String token, boolean registeringFlag) {
        try {
            JSONObject message = createMessage(MessageType.PUSH_REGISTRATION, sessionId, sessionStartTime, time, null, null, false);
            message.put(MessageKey.PUSH_TOKEN, token);
            message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag);
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
        mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, 1, 0));
    }

    public static void setLocation(Location location) {
        sLocation=location;
        if (sDebugMode) {
            Log.d(TAG, "Received location update: " + location);
        }
    }

    public void setConnectionProxy(String host, int port) {
        mUploadHandler.setConnectionProxy(host, port);
    }

    public void setSecureTransport(boolean sslEnabled) {
        mUploadHandler.setConnectionScheme( sslEnabled ? "https" : "http" );
    }

    public void setUploadInterval(int uploadInterval) {
        mUploadHandler.setUploadInterval(uploadInterval);
    }

    private static class NetworkStatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context appContext, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivyManager = (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                MessageManager.setDataConnection(activeNetwork);
            }
        }
    }

    public static void setDataConnection(NetworkInfo activeNetwork) {
        if (null!=activeNetwork) {
            String activeNetworkName = activeNetwork.getTypeName();
            if (0!=activeNetwork.getSubtype()) {
                activeNetworkName += "/" + activeNetwork.getSubtypeName();
            }
            sActiveNetworkName = activeNetworkName.toLowerCase(Locale.US);
        } else {
            sActiveNetworkName = "offline";
        }
        if (sDebugMode) {
            Log.d(TAG, "Active network has changed: " + sActiveNetworkName);
        }
    }

    public void setDebugMode(boolean debugMode) {
        sDebugMode = debugMode;
        mUploadHandler.setDebugMode(debugMode);
    }

}
