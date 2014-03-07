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
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.MParticle.EventType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
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
    private ConfigManager mConfigManager = null;

    public MessageManager(Context appContext, ConfigManager configManager) {
        mConfigManager = configManager;
        mMessageHandler = new MessageHandler(appContext, sMessageHandlerThread.getLooper(), configManager.getApiKey());
        mUploadHandler = new UploadHandler(appContext, sUploadHandlerThread.getLooper(), configManager);
        mPreferences = appContext.getSharedPreferences(Constants.MISC_FILE, Context.MODE_PRIVATE);
    }

    public void start(Context appContext, Boolean firstRun) {
        mContext = appContext.getApplicationContext();
        if (null == sStatusBroadcastReceiver) {

            //get the previous Intent otherwise the first few messages will have 0 for battery level
            Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            sBatteryLevel = level / (double) scale;

            sStatusBroadcastReceiver = new StatusBroadcastReceiver();
            // NOTE: if permissions are not correct all messages will be tagged as 'offline'
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            if (PackageManager.PERMISSION_GRANTED == mContext
                    .checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)) {

                //same as with battery, get current connection so we don't have to wait for the next change
                ConnectivityManager connectivyManager = (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                setDataConnection(activeNetwork);
                filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            }
            mContext.registerReceiver(sStatusBroadcastReceiver, filter);
        }

        sFirstRun = firstRun;

        if (!sFirstRun) {
            mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
            mUploadHandler.sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
            mUploadHandler.sendEmptyMessageDelayed(UploadHandler.CLEANUP, Constants.INITIAL_UPLOAD_DELAY);
        }
        mUploadHandler.sendEmptyMessage(UploadHandler.UPDATE_CONFIG);
    }

    /* package-private */
    static JSONObject createMessage(String messageType, String sessionId, long sessionStart,
                                    long time, String name, JSONObject attributes) throws JSONException {
        JSONObject message = new JSONObject();
        message.put(MessageKey.TYPE, messageType);
        message.put(MessageKey.TIMESTAMP, time);
        if (MessageType.SESSION_START == messageType) {
            message.put(MessageKey.ID, sessionId);
        } else {
            if (null != sessionId) {
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
        if (total < 0) {
            total = MPUtility.getTotalMemory(mContext);
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.TOTAL_MEMORY, total);
            edit.commit();
        }
        return total;
    }

    public static long getSystemMemoryThreshold() {
        long threshold = mPreferences.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1);
        if (threshold < 0) {
            threshold = MPUtility.getSystemMemoryThreshold(mContext);
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, threshold);
            edit.commit();
        }
        return threshold;
    }

    /* package-private */
    static JSONObject createFirstRunMessage(long time, String sessionId, long startTime) throws JSONException {
        JSONObject message = createMessage(MessageType.FIRST_RUN, sessionId, startTime, time, null, null);
        return message;
    }

    /* package-private */
    static JSONObject createMessageSessionEnd(String sessionId, long start, long end, long length,
                                              JSONObject attributes) throws JSONException {
        JSONObject message = createMessage(MessageType.SESSION_END, sessionId, start, end, null, attributes);
        message.put(MessageKey.SESSION_LENGTH, length);
        return message;
    }

    public void startSession(String sessionId, long time, String launchUri) {
        try {
            JSONObject message = createMessage(MessageType.SESSION_START, sessionId, time, time, null, null);
            message.put(MessageKey.LAUNCH_REFERRER, launchUri);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

            if (sFirstRun) {
                try {
                    JSONObject firstRunMessage = createFirstRunMessage(time, sessionId, time);
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

            incrementSessionCounter();


        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle start session message");
        }
    }

    private void incrementSessionCounter() {
        int nextCount = getCurrentSessionCounter() + 1;
        if (nextCount >= (Integer.MAX_VALUE / 100)){
            nextCount = 0;
        }
        mPreferences.edit().putInt(Constants.PrefKeys.SESSION_COUNTER, nextCount).commit();
    }

    private int getCurrentSessionCounter(){
        return mPreferences.getInt(Constants.PrefKeys.SESSION_COUNTER, 0);
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

    public void logEvent(String sessionId, long sessionStartTime, long time, String eventName, EventType eventType, JSONObject attributes, long eventLength) {
        try {
            JSONObject message = createMessage(MessageType.EVENT, sessionId, sessionStartTime, time, eventName,
                    attributes);
            message.put(MessageKey.EVENT_TYPE, eventType);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, eventLength);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle log event message");
        }
    }

    public void logScreen(String sessionId, long sessionStartTime, long time, String screenName, JSONObject attributes, boolean started) {
        try {
            JSONObject message = createMessage(MessageType.SCREEN_VIEW, sessionId, sessionStartTime, time, screenName,
                    attributes);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, 0);
            message.put(MessageKey.SCREEN_STARTED, started ? "activity_started" : "activity_stopped");
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle log event message");
        }
    }

    public void logBreadcrumb(String sessionId, long sessionStartTime, long time, String breadcrumb) {
        try {
            JSONObject message = createMessage(MessageType.BREADCRUMB, sessionId, sessionStartTime, time, null,
                    null);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.BREADCRUMB_SESSION_COUNTER, getCurrentSessionCounter());
            message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_BREADCRUMB, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle breadcrumb message");
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
                message.put(MessageKey.ERROR_SEVERITY, caught ? "error" : "fatal");
                message.put(MessageKey.ERROR_CLASS, t.getClass().getCanonicalName());
                message.put(MessageKey.ERROR_MESSAGE, t.getMessage());
                StringWriter stringWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(stringWriter));
                message.put(MessageKey.ERROR_STACK_TRACE, stringWriter.toString());
                message.put(MessageKey.ERROR_UNCAUGHT, String.valueOf(caught));

                message.put(MessageKey.ERROR_SESSION_COUNT, getCurrentSessionCounter());

            } else {
                message.put(MessageKey.ERROR_SEVERITY, "error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle error message");
        }
    }

    public void logNetworkPerformanceEvent(String sessionId, long sessionStartTime, long time, String method, String url, long length, long bytesSent, long bytesReceived) {
        try {
            JSONObject message = createMessage(MessageType.NETWORK_PERFORMNACE, sessionId, sessionStartTime, time, null, null);
            message.put(MessageKey.NPE_METHOD, method);
            message.put(MessageKey.NPE_URL, url);
            message.put(MessageKey.NPE_LENGTH, length);
            message.put(MessageKey.NPE_SENT, bytesSent);
            message.put(MessageKey.NPE_REC, bytesReceived);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle error message");
        }
    }


    public void setPushRegistrationId(String sessionId, long sessionStartTime, long time, String token, boolean registeringFlag) {
        try {
            JSONObject message = createMessage(MessageType.PUSH_REGISTRATION, sessionId, sessionStartTime, time,
                    null, null);
            message.put(MessageKey.PUSH_TOKEN, token);
            message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle push registration message");
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

    public void setLocation(Location location) {
        sLocation = location;
        if (mConfigManager.isDebug()) {
            Log.d(TAG, "Received location update: " + location);
        }
    }

    public void logStateTransition(String stateTransInit, String sessionId, long sessionStartTime, Bundle lastNotificationBundle) {
        try {
            JSONObject message = createMessage(MessageType.APP_STATE_TRANSITION, sessionId, sessionStartTime, System.currentTimeMillis(),
                    null, null);
            message.put(MessageKey.STATE_TRANSITION_TYPE, stateTransInit);
            if (lastNotificationBundle != null){
                JSONObject attributes = new JSONObject();
                for (String key : lastNotificationBundle.keySet()){
                    try{
                        attributes.put(key, lastNotificationBundle.get(key));
                    }catch(JSONException ex){

                    }
                }
               message.put(MessageKey.PAYLOAD, attributes.toString());
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to create mParticle state transition message");
        }
    }

    public void logNotification(String sessionId, long sessionStartTime, Bundle bundle, String state) {
        JSONObject attributes = new JSONObject();
        for (String key : bundle.keySet()){
            try{
                attributes.put(key, bundle.get(key));
            }catch(JSONException ex){

            }
        }
        try{
            JSONObject message = createMessage(MessageType.PUSH_RECEIVED, sessionId, sessionStartTime, System.currentTimeMillis(), "gcm", null);
            message.put(MessageKey.PAYLOAD, attributes.toString());
            String regId = PushRegistrationHelper.getRegistrationId(mContext);
            if ((regId != null) && (regId.length() > 0)) {
                message.put(MessageKey.PUSH_TOKEN, regId);
            }
            message.put(MessageKey.APP_STATE, state);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        }catch (JSONException e) {

        }

    }

    private class StatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context appContext, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager connectivyManager = (ConnectivityManager) appContext
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                MessageManager.this.setDataConnection(activeNetwork);
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                sBatteryLevel = level / (double) scale;

            }
        }
    }

    public void setDataConnection(NetworkInfo activeNetwork) {
        if (null != activeNetwork) {
            String activeNetworkName = activeNetwork.getTypeName();
            if (0 != activeNetwork.getSubtype()) {
                activeNetworkName += "/" + activeNetwork.getSubtypeName();
            }
            sActiveNetworkName = activeNetworkName.toLowerCase(Locale.US);
        } else {
            sActiveNetworkName = "offline";
        }
        if (mConfigManager.isDebug()) {
            Log.d(TAG, "Active network has changed: " + sActiveNetworkName);
        }
    }

    public void setConnectionProxy(String host, int port) {
        mUploadHandler.setConnectionProxy(host, port);
    }
}
