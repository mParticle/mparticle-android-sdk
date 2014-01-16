package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import com.mparticle.Constants.ConfigKeys;
import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.MParticle.EventType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/* package-private */class MessageManager {

    private static final String TAG = Constants.LOG_TAG;

    private static final HandlerThread sMessageHandlerThread = new HandlerThread("mParticleMessageHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    private static final HandlerThread sUploadHandlerThread = new HandlerThread("mParticleUploadHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    static final Runtime rt = Runtime.getRuntime();
    private static String sActiveNetworkName = "offline";
    private static Location sLocation;
    private static boolean sDebugMode;
    private static boolean sFirstRun;
    private static BroadcastReceiver sStatusBroadcastReceiver;
    private static double sBatteryLevel;

    private final MessageHandler mMessageHandler;
    private final UploadHandler mUploadHandler;

    static {
        // ideally these threads would not be started in a static initializer
        // block. but this is cleaner than checking if they have been started in
        // the constructor.
        sMessageHandlerThread.start();
        sUploadHandlerThread.start();
    }

    private static Context mContext = null;
    private static long sStartTime = System.currentTimeMillis();
    private static SharedPreferences mPreferences = null;

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
        mPreferences = appContext.getSharedPreferences(Constants.MISC_FILE, Context.MODE_PRIVATE);
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
        mContext = appContext.getApplicationContext();
        if (null == sStatusBroadcastReceiver) {

            //get the previous Intent otherwise the first few messages will have 0 for battery level
            Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            sBatteryLevel = level / (double)scale;

            sStatusBroadcastReceiver = new StatusBroadcastReceiver();
            // NOTE: if permissions are not correct all messages will be tagged as 'offline'
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            if (PackageManager.PERMISSION_GRANTED == mContext
                    .checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {

                //same as with battery, get current connection so we don't have to wait for the next change
                ConnectivityManager connectivyManager = (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                MessageManager.setDataConnection(activeNetwork);
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            }
            mContext.registerReceiver(sStatusBroadcastReceiver, filter);
        }

        sFirstRun = firstRun;
       
        if(!sFirstRun) {
        	mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
        	mUploadHandler.sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
        	mUploadHandler.sendEmptyMessageDelayed(UploadHandler.CLEANUP, Constants.INITIAL_UPLOAD_DELAY);
        }
    }

    /* package-private */static JSONObject createMessage(String messageType, String sessionId, long sessionStart,
            long time, String name, JSONObject attributes) throws JSONException {
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
        if (!(MessageType.ERROR.equals(messageType) && !(MessageType.OPT_OUT.equals(messageType)))) {
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

    public static JSONObject getStateInfo() throws JSONException {
        JSONObject infoJson = new JSONObject();
        infoJson.put(MessageKey.STATE_INFO_CPU, MPUtility.getCpuUsage());
        infoJson.put(MessageKey.STATE_INFO_AVAILABLE_MEMORY, MPUtility.getAvailableMemory(mContext));
        infoJson.put(MessageKey.STATE_INFO_TOTAL_MEMORY, getTotalMemory());
        infoJson.put(MessageKey.STATE_INFO_BATTERY_LVL, sBatteryLevel);
        infoJson.put(MessageKey.STATE_INFO_TIME_SINCE_START, System.currentTimeMillis() - sStartTime);
        infoJson.put(MessageKey.STATE_INFO_AVAILABLE_DISK, MPUtility.getAvailableInternalDisk());
        infoJson.put(MessageKey.STATE_INFO_AVAILABLE_EXT_DISK, MPUtility.getAvailableExternalDisk());
        infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_USAGE, rt.totalMemory());
        infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_AVAIL, rt.freeMemory());
        infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_MAX, rt.maxMemory());
        infoJson.put(MessageKey.STATE_INFO_GPS, MPUtility.getGpsEnabled(mContext));
        infoJson.put(MessageKey.STATE_INFO_DATA_CONNECTION, sActiveNetworkName);
        int orientation = MPUtility.getOrientation(mContext);
        infoJson.put(MessageKey.STATE_INFO_ORIENTATION, orientation);
        infoJson.put(MessageKey.STATE_INFO_BAR_ORIENTATION, orientation);
        infoJson.put(MessageKey.STATE_INFO_MEMORY_LOW, MPUtility.isSystemMemoryLow(mContext));
        infoJson.put(MessageKey.STATE_INFO_MEMORY_THRESHOLD, getSystemMemoryThreshold());
        return infoJson;
    }

    public static long getTotalMemory() {
        long total = mPreferences.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1);
        if (total < 0){
            total = MPUtility.getTotalMemory(mContext);
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.TOTAL_MEMORY, total);
            edit.commit();
        }
        return total;
    }

    public static long getSystemMemoryThreshold() {
        long threshold = mPreferences.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1);
        if (threshold < 0){
            threshold = MPUtility.getSystemMemoryThreshold(mContext);
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, threshold);
            edit.commit();
        }
        return threshold;
    }

    /* package-private */static JSONObject createFirstRunMessage(long time) throws JSONException {
        JSONObject message = createMessage(MessageType.FIRST_RUN, null, 0, time, null, null);
        return message;
    }

    /* package-private */static JSONObject createMessageSessionEnd(String sessionId, long start, long end, long length,
            JSONObject attributes) throws JSONException {
        JSONObject message = createMessage(MessageType.SESSION_END, sessionId, start, end, null, attributes);
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
        	
            JSONObject message = createMessage(MessageType.SESSION_START, sessionId, time, time, null, null);
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
                    attributes);
            message.put(MessageKey.EVENT_TYPE, eventType);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, 0);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle log event message");
        }
    }

    public void logScreen(String sessionId, long sessionStartTime, long time, String screenName, JSONObject attributes) {
        try {
            JSONObject message = createMessage(MessageType.SCREEN_VIEW, sessionId, sessionStartTime, time, screenName,
                    attributes);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, 0);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle log event message");
        }
    }

    public void optOut(String sessionId, long sessionStartTime, long time, boolean optOutStatus) {
        try {
            JSONObject message = createMessage(MessageType.OPT_OUT, sessionId, sessionStartTime, time, null, null);
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle opt out message");
        }
    }

    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t, JSONObject attributes) {
    	logErrorEvent(sessionId, sessionStartTime, time, errorMessage, t, attributes, true);
    }
    
    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t, JSONObject attributes, boolean caught) {
        try {
            JSONObject message = createMessage(MessageType.ERROR, sessionId, sessionStartTime, time, null, attributes);
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
                    null, null);
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

    public void logStateTransition(String stateTransInit, String sessionId, long sessionStartTime) {
        try {
            JSONObject message = createMessage(MessageType.APP_STATE_TRANSITION, sessionId, sessionStartTime, System.currentTimeMillis(),
                    null, null);
            message.put(MessageKey.STATE_TRANSITION_TYPE, stateTransInit);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle state transition message");
        }
    }

    private static class StatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context appContext, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivyManager = (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                MessageManager.setDataConnection(activeNetwork);
            }else if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())){
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                sBatteryLevel = level / (double)scale;

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
