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
import com.mparticle.MParticleAPI.EventType;

/* package-private */class MessageManager {

    private static final String TAG = Constants.LOG_TAG;

    private static final HandlerThread sMessageHandlerThread = new HandlerThread("mParticleMessageHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    private static final HandlerThread sUploadHandlerThread = new HandlerThread("mParticleUploadHandler",
            Process.THREAD_PRIORITY_BACKGROUND);

    private static String sActiveNetworkName = "offline";
    private static Location sLocation;
    private static boolean sDebugMode;
    private static boolean sFirstRun;
    private static BroadcastReceiver sNetworkStatusBroadcastReceiver;

    private final MessageHandler mMessageHandler;
    private final UploadHandler mUploadHandler;

    static {
        // ideally these threads would not be started in a static initializer
        // block. but this is cleaner than checking if they have been started in
        // the constructor.
        sMessageHandlerThread.start();
        sUploadHandlerThread.start();
    }

    // This constructor is needed to enable mocking with Mockito and Dexmaker only
    /* package-private */MessageManager() {
        throw new UnsupportedOperationException();
    }

    // Package-private constructor for unit tests only
    /* package-private */MessageManager(MessageHandler messageHandler, UploadHandler uploadHandler) {
        mMessageHandler = messageHandler;
        mUploadHandler = uploadHandler;
    }

    public MessageManager(Context appContext, String apiKey, String secret, Properties config) {

        mMessageHandler = new MessageHandler(appContext, sMessageHandlerThread.getLooper(), apiKey);
        mUploadHandler = new UploadHandler(appContext, sUploadHandlerThread.getLooper(), apiKey, secret);

        int uploadInterval = 0;
        
        if(config.containsKey(ConfigKeys.DEBUG_MODE)) {
        	boolean debugMode = Boolean.parseBoolean(config.getProperty(ConfigKeys.DEBUG_MODE));
        	
        	if(debugMode) {
        		if(config.containsKey(ConfigKeys.DEBUG_UPLOAD_INTERVAL)) {
        			uploadInterval = 1000 * Integer.parseInt(config.getProperty(ConfigKeys.DEBUG_UPLOAD_INTERVAL));
        		}
        		else {
        			uploadInterval = (int)Constants.DEFAULT_UPLOAD_INTERVAL;
        		}
        		
        		this.setUploadInterval(uploadInterval);
        	}
        }
        
        if (uploadInterval == 0 && config.containsKey(ConfigKeys.UPLOAD_INTERVAL)) {
            try {
                this.setUploadInterval(1000 * Integer.parseInt(config.getProperty(ConfigKeys.UPLOAD_INTERVAL)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.UPLOAD_INTERVAL + "' setting");
            }
        }
        if (config.containsKey(ConfigKeys.ENABLE_SSL)) {
            try {
                this.setSecureTransport(Boolean.parseBoolean(config.getProperty(ConfigKeys.ENABLE_SSL)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.ENABLE_SSL + "' setting");
            }
        }
        if (config.containsKey(ConfigKeys.PROXY_HOST) && config.containsKey(ConfigKeys.PROXY_PORT)) {
            try {
                this.setConnectionProxy(config.getProperty(ConfigKeys.PROXY_HOST),
                        Integer.parseInt(config.getProperty(ConfigKeys.PROXY_PORT)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.PROXY_HOST +
                        "' and '" + ConfigKeys.PROXY_PORT + "' settings");
            }
        }
        if (config.containsKey(ConfigKeys.ENABLE_COMPRESSION)) {
            try {
                mUploadHandler.setCompressionEnabled(Boolean.parseBoolean(config
                        .getProperty(ConfigKeys.ENABLE_COMPRESSION)));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to configure mParticle with '" + ConfigKeys.ENABLE_COMPRESSION + "' setting");
            }
        }

    }

    public void start(Context appContext, Boolean firstRun) {
        if (null == sNetworkStatusBroadcastReceiver) {
            sNetworkStatusBroadcastReceiver = new NetworkStatusBroadcastReceiver();
            // NOTE: if permissions are not correct all messages will be tagged as 'offline'
            if (PackageManager.PERMISSION_GRANTED == appContext
                    .checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                appContext.registerReceiver(sNetworkStatusBroadcastReceiver, filter);
            }
        }
        
        sFirstRun = firstRun;
       
        if(!sFirstRun) {
        	mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
        	mUploadHandler.sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
        	mUploadHandler.sendEmptyMessageDelayed(UploadHandler.CLEANUP, Constants.INITIAL_UPLOAD_DELAY);
        }
    }

    /* package-private */static JSONObject createMessage(String messageType, String sessionId, long sessionStart,
            long time, String name, JSONObject attributes, boolean includeConnLoc) throws JSONException {
        JSONObject message = new JSONObject();
        message.put(MessageKey.TYPE, messageType);
        message.put(MessageKey.TIMESTAMP, time);
        if (MessageType.SESSION_START == messageType) {
            message.put(MessageKey.ID, sessionId);
        } else {
            if(null != sessionId) {
            	message.put(MessageKey.SESSION_ID, sessionId);	
            }
        	
            message.put(MessageKey.ID, UUID.randomUUID().toString());
            if (sessionStart > 0) {
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
            if (null != sLocation) {
                JSONObject locJSON = new JSONObject();
                locJSON.put(MessageKey.LATITUDE, sLocation.getLatitude());
                locJSON.put(MessageKey.LONGITUDE, sLocation.getLongitude());
                locJSON.put(MessageKey.ACCURACY, sLocation.getAccuracy());
                message.put(MessageKey.LOCATION, locJSON);
            }
        }
        return message;
    }
    
    /* package-private */static JSONObject createFirstRunMessage(long time) throws JSONException {
        JSONObject message = createMessage(MessageType.FIRST_RUN, null, 0, time, null, null, true);
        return message;
    }

    /* package-private */static JSONObject createMessageSessionEnd(String sessionId, long start, long end, long length,
            JSONObject attributes) throws JSONException {
        JSONObject message = createMessage(MessageType.SESSION_END, sessionId, start, end, null, attributes, true);
        message.put(MessageKey.SESSION_LENGTH, length);
        return message;
    }

    public void startSession(String sessionId, long time, String launchUri) {
        try {
        	if(sFirstRun) {
            	try {
    				JSONObject firstRunMessage = createFirstRunMessage(time);
    				mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, firstRunMessage));
    				sFirstRun = false;
    				if (mUploadHandler != null) {
    					mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES));
    				} else {
        				Log.w(TAG, "Failed to send First Run Message, no upload handler");
    				}
    			} catch (JSONException e) { 
    				Log.w(TAG, "Failed to create First Run Message");
    			}
            }
        	
            JSONObject message = createMessage(MessageType.SESSION_START, sessionId, time, time, null, null, true);
            message.put(MessageKey.LAUNCH_REFERRER, launchUri);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start session message");
        }
    }

    public void stopSession(String sessionId, long stopTime, long sessionLength) {
        try {
            JSONObject sessionTiming = new JSONObject();
            sessionTiming.put(MessageKey.SESSION_ID, sessionId);
            sessionTiming.put(MessageKey.TIMESTAMP, stopTime);
            sessionTiming.put(MessageKey.SESSION_LENGTH, sessionLength);
            mMessageHandler
                    .sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, sessionTiming));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to send update session end message");
        }
    }

    public void endSession(String sessionId, long stopTime, long sessionLength) {
        stopSession(sessionId, stopTime, sessionLength);
        mMessageHandler
                .sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
    }

    public void logEvent(String sessionId, long sessionStartTime, long time, String eventName, EventType eventType, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.EVENT, sessionId, sessionStartTime, time, eventName,
                    attributes, true);
            message.put(MessageKey.EVENT_TYPE, eventType);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, 0);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle log event message");
        }
    }

    public void logScreenView(String sessionId, long sessionStartTime, long time, String screenName,
            JSONObject attributes) {
        logEvent(sessionId, sessionStartTime, time, screenName, EventType.Unknown, attributes);
    }

    public void optOut(String sessionId, long sessionStartTime, long time, boolean optOutStatus) {
        try {
            JSONObject message = createMessage(MessageType.OPT_OUT, sessionId, sessionStartTime, time, null, null,
                    false);
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle opt out message");
        }
    }

    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t) {
    	logErrorEvent(sessionId, sessionStartTime, time, errorMessage, t, true);
    }
    
    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t, boolean caught) {
        try {
            JSONObject message = createMessage(MessageType.ERROR, sessionId, sessionStartTime, time, null, null, false);
            if (null != t) {
                message.put(MessageKey.ERROR_SEVERITY, "fatal");
                message.put(MessageKey.ERROR_CLASS, t.getClass().getCanonicalName());
                message.put(MessageKey.ERROR_MESSAGE, t.getMessage());
                StringWriter stringWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(stringWriter));
                message.put(MessageKey.ERROR_STACK_TRACE, stringWriter.toString());
                message.put(MessageKey.ERROR_UNCAUGHT, String.valueOf(caught));
            } else {
                message.put(MessageKey.ERROR_SEVERITY, "error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle error message");
        }
    }

    public void setPushRegistrationId(String token, boolean registeringFlag) {
        try {
            JSONObject message = createMessage(MessageType.PUSH_REGISTRATION, null, 0, System.currentTimeMillis(),
                    null, null, false);
            message.put(MessageKey.PUSH_TOKEN, token);
            message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle error message");
        }
    }

    public void setSessionAttributes(String sessionId, JSONObject mSessionAttributes) {
        try {
            JSONObject sessionAttributes = new JSONObject();
            sessionAttributes.put(MessageKey.SESSION_ID, sessionId);
            sessionAttributes.put(MessageKey.ATTRIBUTES, mSessionAttributes);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_ATTRIBUTES,
                    sessionAttributes));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to send update session attributes message");
        }
    }

    public void doUpload() {
        mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, 1, 0));
    }

    public static void setLocation(Location location) {
        sLocation = location;
        if (sDebugMode) {
            Log.d(TAG, "Received location update: " + location);
        }
    }

    public void setConnectionProxy(String host, int port) {
        mUploadHandler.setConnectionProxy(host, port);
    }
    
    public void setServiceHost(String hostName) {
    	mUploadHandler.setServiceHost(hostName);
    }

    public void setSecureTransport(boolean sslEnabled) {
        mUploadHandler.setConnectionScheme(sslEnabled ? "https" : "http");
    }

    public void setUploadInterval(long uploadInterval) {
        mUploadHandler.setUploadInterval(uploadInterval);
    }

    private static class NetworkStatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context appContext, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivyManager = (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                MessageManager.setDataConnection(activeNetwork);
            }
        }
    }

    public static void setDataConnection(NetworkInfo activeNetwork) {
        if (null != activeNetwork) {
            String activeNetworkName = activeNetwork.getTypeName();
            if (0 != activeNetwork.getSubtype()) {
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

    public void setSandboxMode(boolean sandboxMode) {
        if(sandboxMode) {
        	setUploadInterval(Constants.DEBUG_UPLOAD_INTERVAL);
        }
    	// pass this on
    	mUploadHandler.setSandboxMode(sandboxMode);
    }
}
