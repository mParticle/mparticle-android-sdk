package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Process;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.MessageType;
import com.mparticle.MParticle.EventType;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.MPCloudNotificationMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/* package-private */class MessageManager implements MessageManagerCallbacks {

    private static final HandlerThread sMessageHandlerThread = new HandlerThread("mParticleMessageHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    private static final HandlerThread sUploadHandlerThread = new HandlerThread("mParticleUploadHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    static final Runtime rt = Runtime.getRuntime();
    private static String sActiveNetworkName = "offline";
    private Location mLocation;

    private static boolean sFirstRun;
    private static BroadcastReceiver sStatusBroadcastReceiver;
    private static double sBatteryLevel;

    private final MessageHandler mMessageHandler;
    final UploadHandler mUploadHandler;

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
    private MParticle.InstallType mInstallType;

    public MessageManager(Context appContext, ConfigManager configManager) {
        mConfigManager = configManager;
        SQLiteDatabase database = new MParticleDatabase(appContext).getWritableDatabase();
        mMessageHandler = new MessageHandler(sMessageHandlerThread.getLooper(), database, this);
        mUploadHandler = new UploadHandler(appContext, sUploadHandlerThread.getLooper(), configManager, database);
        mPreferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    }

    public void start(Context appContext, Boolean firstRun, MParticle.InstallType installType) {
        mContext = appContext.getApplicationContext();
        if (sStatusBroadcastReceiver == null) {

            //get the previous Intent otherwise the first few messages will have 0 for battery level
            Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            sBatteryLevel = level / (double) scale;

            sStatusBroadcastReceiver = new StatusBroadcastReceiver();
            // NOTE: if permissions are not correct all messages will be tagged as 'offline'
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            if (MPUtility.checkPermission(mContext, android.Manifest.permission.ACCESS_NETWORK_STATE)) {
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
        mInstallType = installType;

        if (!sFirstRun) {
            mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
           // mUploadHandler.sendEmptyMessageDelayed(UploadHandler.CLEANUP, Constants.INITIAL_UPLOAD_DELAY);
        }else{
            boolean installDetected = !MParticle.InstallType.KnownUpgrade.equals(mInstallType) &&
                    (MParticle.InstallType.KnownInstall.equals(mInstallType) ||
                            (mInstallType == MParticle.InstallType.AutoDetect && autoDetectInstall()));
            mPreferences.edit().putBoolean(Constants.PrefKeys.FIRST_RUN_INSTALL, installDetected).commit();
        }

        refreshConfiguration();
        mUploadHandler.sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
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
        String gps = MPUtility.getGpsEnabled(mContext);
        if (gps != null){
            infoJson.put(MessageKey.STATE_INFO_GPS,Boolean.parseBoolean(gps));
        }
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

    MPMessage createFirstRunMessage(long time, String sessionId, long startTime) throws JSONException {
        return new MPMessage.Builder(MessageType.FIRST_RUN, sessionId, mLocation)
                .sessionStartTime(startTime)
                .timestamp(time)
                .build();
    }



    public void startSession(String sessionId, long time) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.SESSION_START, sessionId, mLocation)
                    .sessionStartTime(time)
                    .timestamp(System.currentTimeMillis())
                    .build();

            SharedPreferences.Editor editor = mPreferences.edit();
            long timeInFg = mPreferences.getLong(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND, 0);
            if (timeInFg > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_LENGTH, timeInFg / 1000);
                editor.remove(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND);
            }
            String prevSessionId = mPreferences.getString(Constants.PrefKeys.PREVIOUS_SESSION_ID, "");
            editor.putString(Constants.PrefKeys.PREVIOUS_SESSION_ID, sessionId);
            if (prevSessionId != null && prevSessionId.length() > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_ID, prevSessionId);
            }

            long prevSessionStart = mPreferences.getLong(Constants.PrefKeys.PREVIOUS_SESSION_START, -1);
            editor.putLong(Constants.PrefKeys.PREVIOUS_SESSION_START, time);

            if (prevSessionStart > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_START, prevSessionStart);
            }

            editor.commit();

            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

            if (sFirstRun) {
                try {
                    JSONObject firstRunMessage = createFirstRunMessage(time, sessionId, time);
                    mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, firstRunMessage));
                    sFirstRun = false;
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create First Run Message");
                }
            }

            incrementSessionCounter();

        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle start session message");
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
            long timeInBackground = mPreferences.getLong(Constants.PrefKeys.TIME_IN_BG, 0);
            long foregroundLength = sessionLength - timeInBackground;
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putLong(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND, foregroundLength > 0 ? foregroundLength : sessionLength);
            editor.remove(Constants.PrefKeys.TIME_IN_BG);
            editor.commit();

            JSONObject sessionTiming = new JSONObject();
            sessionTiming.put(MessageKey.SESSION_ID, sessionId);
            sessionTiming.put(MessageKey.TIMESTAMP, stopTime);
            sessionTiming.put(MessageKey.SESSION_LENGTH, foregroundLength);

            mMessageHandler
                    .sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, sessionTiming));

        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to send update session end message");
        }
    }

    public void endSession(String sessionId, long stopTime, long sessionLength) {
        stopSession(sessionId, stopTime, sessionLength);
        mMessageHandler
                .sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, sessionId));
    }

    public void logEvent(String sessionId, long sessionStartTime, long time, String eventName, EventType eventType, JSONObject attributes, long eventLength, String currentActivity) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.EVENT, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(time)
                    .attributes(attributes)
                    .build();

            message.put(MessageKey.EVENT_TYPE, eventType);
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, eventLength);
            if (currentActivity != null){
                message.put(MessageKey.CURRENT_ACTIVITY, currentActivity);
            }
            int count = mPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
            message.put(MessageKey.EVENT_COUNTER, count);
            mPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, ++count).commit();

            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
        }
    }

    private static void resetEventCounter(){
        mPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, 0).commit();
    }

    public void logScreen(String sessionId, long sessionStartTime, long time, String screenName, JSONObject attributes, boolean started) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.SCREEN_VIEW, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(time)
                    .name(screenName)
                    .attributes(attributes)
                    .build();
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.EVENT_DURATION, 0);
            message.put(MessageKey.SCREEN_STARTED, started ? "activity_started" : "activity_stopped");
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
        }
    }

    public void logBreadcrumb(String sessionId, long sessionStartTime, long time, String breadcrumb) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.BREADCRUMB, sessionId, mLocation)
                                        .sessionStartTime(sessionStartTime)
                                        .timestamp(time)
                                        .build();
            // NOTE: event timing is not supported (yet) but the server expects this data
            message.put(MessageKey.EVENT_START_TIME, time);
            message.put(MessageKey.BREADCRUMB_SESSION_COUNTER, getCurrentSessionCounter());
            message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_BREADCRUMB, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle breadcrumb message");
        }
    }

    public void optOut(String sessionId, long sessionStartTime, long time, boolean optOutStatus) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.OPT_OUT, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(time)
                    .build();
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle opt out message");
        }
    }

    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t, JSONObject attributes) {
        logErrorEvent(sessionId, sessionStartTime, time, errorMessage, t, attributes, true);
    }

    public void logErrorEvent(String sessionId, long sessionStartTime, long time, String errorMessage, Throwable t, JSONObject attributes, boolean caught) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.ERROR, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(time)
                    .attributes(attributes)
                    .build();
            if (t != null) {
                message.put(MessageKey.ERROR_MESSAGE, t.getMessage());
                message.put(MessageKey.ERROR_SEVERITY, caught ? "error" : "fatal");
                if (t instanceof MPUnityException){
                    message.put(MessageKey.ERROR_CLASS, t.getMessage());
                    message.put(MessageKey.ERROR_STACK_TRACE, ((MPUnityException)t).getManualStackTrace());
                }else {
                    message.put(MessageKey.ERROR_CLASS, t.getClass().getCanonicalName());
                    StringWriter stringWriter = new StringWriter();
                    t.printStackTrace(new PrintWriter(stringWriter));
                    message.put(MessageKey.ERROR_STACK_TRACE, stringWriter.toString());
                }
                message.put(MessageKey.ERROR_UNCAUGHT, String.valueOf(caught));
                message.put(MessageKey.ERROR_SESSION_COUNT, getCurrentSessionCounter());

            } else {
                message.put(MessageKey.ERROR_SEVERITY, "error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle error message");
        }
    }

    public void logNetworkPerformanceEvent(String sessionId, long sessionStartTime, long time, String method, String url, long length, long bytesSent, long bytesReceived, String requestString) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.NETWORK_PERFORMNACE, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(time)
                    .build();
            message.put(MessageKey.NPE_METHOD, method);
            message.put(MessageKey.NPE_URL, url);
            message.put(MessageKey.NPE_LENGTH, length);
            message.put(MessageKey.NPE_SENT, bytesSent);
            message.put(MessageKey.NPE_REC, bytesReceived);
            if (requestString != null){
                message.put(MessageKey.NPE_POST_DATA, requestString);
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle error message");
        }
    }


    public void setPushRegistrationId(String sessionId, long sessionStartTime, long time, String token, boolean registeringFlag) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.PUSH_REGISTRATION, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(time)
                    .build();
            message.put(MessageKey.PUSH_TOKEN, token);
            message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle push registration message");
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
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to send update session attributes message");
        }
    }

    public void doUpload() {
        mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, 1, 0));
    }

    public void setLocation(Location location) {
        mLocation = location;
        ConfigManager.log(MParticle.LogLevel.DEBUG, "Received location update: " + location);
    }

    public void logStateTransition(String stateTransInit, String sessionId, long sessionStartTime, JSONObject lastNotificationJson, String currentActivity,
                                   String launchUri, String launchExtras, String launchSourcePackage, long previousForegroundTime, long suspendedTime, int interruptions) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.APP_STATE_TRANSITION, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(System.currentTimeMillis())
                    .build();

            message.put(MessageKey.STATE_TRANSITION_TYPE, stateTransInit);
            if (currentActivity != null){
                message.put(MessageKey.CURRENT_ACTIVITY, currentActivity);
            }

            if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_INIT)||
                    stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_FORE)){
                message.put(MessageKey.ST_LAUNCH_REFERRER, launchUri);
                message.put(MessageKey.ST_LAUNCH_PARAMS, launchExtras);
                message.put(MessageKey.ST_LAUNCH_SOURCE_PACKAGE, launchSourcePackage);
                if (previousForegroundTime > 0) {
                    message.put(MessageKey.ST_LAUNCH_PRV_FORE_TIME, previousForegroundTime);
                }
                if (suspendedTime > 0) {
                    message.put(MessageKey.ST_LAUNCH_TIME_SUSPENDED, suspendedTime);
                }
                if (interruptions >= 0){
                    message.put(MessageKey.ST_INTERRUPTIONS, interruptions);
                }

            }

            if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_INIT)){
                SharedPreferences.Editor editor = mPreferences.edit();

                if (!sFirstRun) {
                    message.put(MessageKey.APP_INIT_CRASHED, !mPreferences.getBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false));
                }

                int versionCode = 0;
                try {
                    PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                    versionCode = pInfo.versionCode;
                } catch (PackageManager.NameNotFoundException nnfe) {

                }
                boolean upgrade = (versionCode != mPreferences.getInt(Constants.PrefKeys.INITUPGRADE, 0));
                editor.putInt(Constants.PrefKeys.INITUPGRADE, versionCode).commit();

                boolean installDetected = (mInstallType == MParticle.InstallType.AutoDetect && autoDetectInstall());

                boolean globalUpgrade = upgrade ||
                                (mInstallType == MParticle.InstallType.KnownUpgrade ||
                                        !installDetected);

                message.put(MessageKey.APP_INIT_FIRST_RUN, sFirstRun);
                message.put(MessageKey.APP_INIT_UPGRADE, globalUpgrade);
            }
            if (lastNotificationJson != null){
               message.put(MessageKey.PAYLOAD, lastNotificationJson.toString());
            }
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle state transition message");
        }
    }

    private boolean autoDetectInstall() {
        //heuristic 1: look for install referrer
        if (mPreferences.contains(Constants.PrefKeys.INSTALL_REFERRER)){
            return true;
        }
        //heuristic 2: look for other persisted preferences
        File prefsdir = new File(mContext.getApplicationInfo().dataDir, "shared_prefs");
        if(prefsdir.exists() && prefsdir.isDirectory()) {
            String[] list = prefsdir.list();
            for (String item : list) {
                if (item.contains(".xml") && !item.contains(Constants.PREFS_FILE)) {
                    //remove .xml from the file name
                    String preffile = item.substring(0, item.length() - 4);

                    SharedPreferences sp2 = mContext.getSharedPreferences(preffile, Context.MODE_PRIVATE);
                    Map map = sp2.getAll();

                    if (!map.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void logNotification(String sessionId, long sessionStartTime, AbstractCloudMessage cloudMessage, String type, int actionId) {
        try{
            MPMessage message = new MPMessage.Builder(MessageType.PUSH_RECEIVED, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build();

            message.put(MessageKey.PAYLOAD, cloudMessage.getJsonPayload().toString());
            message.put(MessageKey.PUSH_TYPE, type);
            message.put(MessageKey.PUSH_BEHAVIOR, cloudMessage.getBehavior());
            if (type.equals(Constants.Push.MESSAGE_TYPE_ACTION) && actionId > 0) {
                message.put(MessageKey.PUSH_ACTION_TAKEN, actionId);
            }
            String regId = PushRegistrationHelper.getRegistrationId(mContext);
            if ((regId != null) && (regId.length() > 0)) {
                message.put(MessageKey.PUSH_TOKEN, regId);
            }
            message.put(MessageKey.APP_STATE, cloudMessage.getAppState());
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            if (cloudMessage instanceof MPCloudNotificationMessage) {
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_GCM_MESSAGE, cloudMessage));
            }
        }catch (JSONException e) {

        }

    }

    public void logProfileAction(String action, String sessionId, long sessionStartTime) {
        try {

            MPMessage message = new MPMessage.Builder(MessageType.PROFILE, sessionId, mLocation)
                    .sessionStartTime(sessionStartTime)
                    .timestamp(System.currentTimeMillis())
                    .build();

            message.put(Constants.ProfileActions.KEY, action);

            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
        }
    }

    @Override
    public void checkForTrigger(MPMessage message) {
        JSONArray messageMatches = mConfigManager.getTriggerMessageMatches();
        JSONArray triggerHashes = mConfigManager.getTriggerMessageHashes();
        boolean shouldTrigger = false;

        if (messageMatches != null && messageMatches.length() > 0){
            shouldTrigger = true;
            int i = 0;
            while (shouldTrigger && i < messageMatches.length()){
                try {
                    JSONObject messageMatch = messageMatches.getJSONObject(i);
                    Iterator<?> keys = messageMatch.keys();
                    while(shouldTrigger && keys.hasNext() ){
                        String key = (String)keys.next();
                        shouldTrigger = message.has(key);
                        if (shouldTrigger){
                            try {
                                shouldTrigger = messageMatch.getString(key).equalsIgnoreCase(message.getString(key));
                            }catch (JSONException stringex){
                                try {
                                    shouldTrigger = message.getBoolean(key) == messageMatch.getBoolean(key);
                                }catch (JSONException boolex){
                                    try{
                                        shouldTrigger = message.getDouble(key) == messageMatch.getDouble(key);
                                    }catch (JSONException doubleex){
                                        shouldTrigger = false;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                }
                i++;
            }
        }
        if (!shouldTrigger && triggerHashes != null){
            for (int i = 0; i < triggerHashes.length(); i++){
                try {
                    if (triggerHashes.getInt(i) == message.getTypeNameHash()) {
                        shouldTrigger = true;
                        break;
                    }
                }catch (JSONException jse){

                }
            }
        }
        if (shouldTrigger) {
            mUploadHandler.removeMessages(UploadHandler.UPLOAD_TRIGGER_MESSAGES);
            mUploadHandler.sendMessageDelayed(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_TRIGGER_MESSAGES, 1, 0), Constants.TRIGGER_MESSAGE_DELAY);
        }
    }

    @Override
    public MPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes) throws JSONException{
        int eventCounter = mPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
        resetEventCounter();
        MPMessage message = new MPMessage.Builder(MessageType.SESSION_END, sessionId, mLocation)
                .sessionStartTime(start)
                .timestamp(end)
                .attributes(sessionAttributes)
                .build();
        message.put(MessageKey.EVENT_COUNTER, eventCounter);
        message.put(MessageKey.SESSION_LENGTH, foregroundLength);
        message.put(MessageKey.SESSION_LENGTH_TOTAL, (end - start));
        message.put(MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
        return message;
    }

    @Override
    public String getApiKey() {
        return mConfigManager.getApiKey();
    }

    public void refreshConfiguration() {
        mUploadHandler.sendEmptyMessage(UploadHandler.UPDATE_CONFIG);
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
        if (activeNetwork != null) {
            String activeNetworkName = activeNetwork.getTypeName();
            if (0 != activeNetwork.getSubtype()) {
                activeNetworkName += "/" + activeNetwork.getSubtypeName();
            }
            sActiveNetworkName = activeNetworkName.toLowerCase(Locale.US);
        } else {
            sActiveNetworkName = "offline";
        }

        mUploadHandler.setConnected(activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        ConfigManager.log(MParticle.LogLevel.DEBUG, "Active network has changed: " + sActiveNetworkName);
    }
}
