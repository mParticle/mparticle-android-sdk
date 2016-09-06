package com.mparticle.internal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.TelephonyManager;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.messaging.CloudAction;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.ProviderCloudMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class is primarily responsible for generating MPMessage objects, and then adding them to a
 * queue which is then processed in a background thread for further processing and database storage.
 *
 */
public class MessageManager implements MessageManagerCallbacks, ReportingManager {

    private static Context mContext = null;
    private static SharedPreferences mPreferences = null;
    private AppStateManager mAppStateManager;
    private ConfigManager mConfigManager = null;


    /**
     * These two threads are used to do the heavy lifting.
     * The Message Handler primarly stores messages in the database.
     */
    private static final HandlerThread sMessageHandlerThread = new HandlerThread("mParticleMessageHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    /**
     * The upload handler thread primarily queries the database for messages to upload, and then handles network communication.
     */
    private static final HandlerThread sUploadHandlerThread = new HandlerThread("mParticleUploadHandler",
            Process.THREAD_PRIORITY_BACKGROUND);
    /**
     * These are the handlers which manage the queues and threads mentioned above.
     */
    private MessageHandler mMessageHandler;
    public UploadHandler mUploadHandler;
    /**
     * Ideally these threads would not be started in a static initializer
     * block. but this is cleaner than checking if they have been started in
     * the constructor.
     */
    static {
        sMessageHandlerThread.start();
        sUploadHandlerThread.start();
    }
    /**
     * Used to communicate the current location at the time of message generation. Can be set
     * manually by the customer, or automatically via our our location listener, if enabled.
     */
    private Location mLocation;
    /**
     * This broadcast receiver is used to determine the current state of connectivity and battery level.
     * We could query these things every time a message is generated, but this should be more performant.
     */
    private static BroadcastReceiver sStatusBroadcastReceiver;
    /**
     * Used to communicate the current network state at the time of message generation.
     * Also, if we don't have a network communicate, the SDK will not even try to query and upload a batch message
     * to try and save on battery life and memory.
     */
    private static String sActiveNetworkName = "offline";
    /**
     * Keep a reference to the current battery life as populated by the BroadcastReceiver described above.
     */
    private static double sBatteryLevel;
    /**
     * The app-info dictionary in each batch need to know the runtime of the SDK/app itself.
     */
    private static long sStartTime = MPUtility.millitime();
    /**
     * Every state-transition message needs to know if this was an upgrade or an install.
     */
    MParticle.InstallType mInstallType = MParticle.InstallType.AutoDetect;
    /**
     * Batches/messages need to communicate the current telephony status when available.
     */
    private static TelephonyManager sTelephonyManager;
    private boolean mFirstRun = true;

    /**
     * Used solely for unit testing
     */
    public MessageManager() {
        super();
    }

    /**
     * Used solely for unit testing
     */
    public MessageManager(Context appContext, ConfigManager configManager, MParticle.InstallType installType, AppStateManager appStateManager, MessageHandler messageHandler, UploadHandler uploadHandler) {
        mContext = appContext.getApplicationContext();
        mConfigManager = configManager;
        mAppStateManager = appStateManager;
        mMessageHandler = messageHandler;
        mUploadHandler = uploadHandler;
        mPreferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mInstallType = installType;
    }

    public MessageManager(Context appContext, ConfigManager configManager, MParticle.InstallType installType, AppStateManager appStateManager) {
        mContext = appContext.getApplicationContext();
        mConfigManager = configManager;
        mAppStateManager = appStateManager;
        mAppStateManager.setMessageManager(this);
        MParticleDatabase database = new MParticleDatabase(appContext);
        mMessageHandler = new MessageHandler(sMessageHandlerThread.getLooper(), this, database);
        mUploadHandler = new UploadHandler(appContext, sUploadHandlerThread.getLooper(), configManager, database, appStateManager);
        mPreferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mInstallType = installType;

    }

    private static TelephonyManager getTelephonyManager() {
        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return sTelephonyManager;
    }

    public static JSONObject getStateInfo() throws JSONException {
        JSONObject infoJson = new JSONObject();
        if (!MParticle.isDevicePerformanceMetricsDisabled()) {
            infoJson.put(MessageKey.STATE_INFO_CPU, MPUtility.getCpuUsage());
            infoJson.put(MessageKey.STATE_INFO_AVAILABLE_DISK, MPUtility.getAvailableInternalDisk());
            infoJson.put(MessageKey.STATE_INFO_AVAILABLE_EXT_DISK, MPUtility.getAvailableExternalDisk());
            final Runtime rt = Runtime.getRuntime();
            infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_USAGE, rt.totalMemory());
            infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_AVAIL, rt.freeMemory());
            infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_MAX, rt.maxMemory());
        }
        infoJson.put(MessageKey.STATE_INFO_AVAILABLE_MEMORY, MPUtility.getAvailableMemory(mContext));
        infoJson.put(MessageKey.STATE_INFO_TOTAL_MEMORY, getTotalMemory());
        infoJson.put(MessageKey.STATE_INFO_BATTERY_LVL, sBatteryLevel);
        infoJson.put(MessageKey.STATE_INFO_TIME_SINCE_START, MPUtility.millitime() - sStartTime);

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
        infoJson.put(MessageKey.STATE_INFO_NETWORK_TYPE, getTelephonyManager().getNetworkType());
        return infoJson;
    }

    public static long getTotalMemory() {
        long total = mPreferences.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1);
        if (total < 0) {
            total = MPUtility.getTotalMemory(mContext);
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.TOTAL_MEMORY, total);
            edit.apply();
        }
        return total;
    }

    public static long getSystemMemoryThreshold() {
        long threshold = mPreferences.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1);
        if (threshold < 0) {
            threshold = MPUtility.getSystemMemoryThreshold(mContext);
            SharedPreferences.Editor edit = mPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, threshold);
            edit.apply();
        }
        return threshold;
    }

    public MPMessage createFirstRunMessage() throws JSONException {
        return new MPMessage.Builder(MessageType.FIRST_RUN, mAppStateManager.getSession(), mLocation)
                .timestamp(mAppStateManager.getSession().mSessionStartTime)
                .dataConnection(sActiveNetworkName)
                .build();
    }

    public MPMessage startSession() {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.SESSION_START, mAppStateManager.getSession(), mLocation)
                    .timestamp(mAppStateManager.getSession().mSessionStartTime)
                    .build();

            SharedPreferences.Editor editor = mPreferences.edit();
            long timeInFg = mPreferences.getLong(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND, 0);
            if (timeInFg > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_LENGTH, timeInFg / 1000);
                editor.remove(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND);
            }
            String prevSessionId = mPreferences.getString(Constants.PrefKeys.PREVIOUS_SESSION_ID, "");
            editor.putString(Constants.PrefKeys.PREVIOUS_SESSION_ID, mAppStateManager.getSession().mSessionID);
            if (!MPUtility.isEmpty(prevSessionId)) {
                message.put(MessageKey.PREVIOUS_SESSION_ID, prevSessionId);
            }

            long prevSessionStart = mPreferences.getLong(Constants.PrefKeys.PREVIOUS_SESSION_START, -1);
            editor.putLong(Constants.PrefKeys.PREVIOUS_SESSION_START, mAppStateManager.getSession().mSessionStartTime);

            if (prevSessionStart > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_START, prevSessionStart);
            }

            editor.apply();

            mFirstRun = !mPreferences.contains(Constants.PrefKeys.FIRSTRUN + mConfigManager.getApiKey());
            if (mFirstRun) {
                mPreferences.edit().putBoolean(Constants.PrefKeys.FIRSTRUN + mConfigManager.getApiKey(), false).apply();
                try {
                    JSONObject firstRunMessage = createFirstRunMessage();
                    mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, firstRunMessage));
                } catch (JSONException e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create First Run Message");
                }
            }else{
                mMessageHandler.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);
            }


            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

            incrementSessionCounter();
            return message;
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle start session message");
            return null;
        }
    }

    void incrementSessionCounter() {
        int nextCount = getCurrentSessionCounter() + 1;
        if (nextCount >= (Integer.MAX_VALUE / 100)){
            nextCount = 0;
        }
        mPreferences.edit().putInt(Constants.PrefKeys.SESSION_COUNTER, nextCount).apply();
    }

    int getCurrentSessionCounter(){
        return mPreferences.getInt(Constants.PrefKeys.SESSION_COUNTER, 0);
    }

    public void updateSessionEnd(Session session) {
        try {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putLong(Constants.PrefKeys.PREVIOUS_SESSION_FOREGROUND, session.getForegroundTime());
            editor.apply();

            mMessageHandler
                    .sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, session));
        } catch (Exception e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to send update session end message");
        }
    }

    public void endSession(Session session) {
        updateSessionEnd(session);
        mMessageHandler
                .sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, 1, 1, session.mSessionID));
    }

    public MPMessage logEvent(MPEvent event, String currentActivity) {
        if (event != null) {
            try {

                MPMessage message = new MPMessage.Builder(MessageType.EVENT, mAppStateManager.getSession(), mLocation)
                        .name(event.getEventName())
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .length(event.getLength())
                        .flags(event.getCustomFlags())
                        .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                        .build();
                message.put(MessageKey.EVENT_TYPE, event.getEventType());
                message.put(MessageKey.EVENT_START_TIME, message.getTimestamp());


                if (currentActivity != null) {
                    message.put(MessageKey.CURRENT_ACTIVITY, currentActivity);
                }

                int count = mPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
                message.put(MessageKey.EVENT_COUNTER, count);
                mPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, ++count).apply();

                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
            }
        }
        return null;
    }

    public MPMessage logEvent(CommerceEvent event) {
        if (event != null) {
            try {
                MPMessage message = new MPMessage.Builder(event, mAppStateManager.getSession(), mLocation)
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .build();
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
            }
        }
        return null;
    }

    static void resetEventCounter(){
        mPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, 0).apply();
    }

    public MPMessage logScreen(MPEvent event, boolean started) {
        if (event != null && event.getEventName() != null) {
            try {
                MPMessage message = new MPMessage.Builder(MessageType.SCREEN_VIEW, mAppStateManager.getSession(), mLocation)
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .name(event.getEventName())
                        .flags(event.getCustomFlags())
                        .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                        .build();

                message.put(MessageKey.EVENT_START_TIME, mAppStateManager.getSession().mLastEventTime);
                message.put(MessageKey.EVENT_DURATION, 0);
                message.put(MessageKey.SCREEN_STARTED, started ? "activity_started" : "activity_stopped");
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
            }
        }
        return null;
    }

    public MPMessage logBreadcrumb(String breadcrumb) {
        if (breadcrumb != null) {
            try {
                MPMessage message = new MPMessage.Builder(MessageType.BREADCRUMB, mAppStateManager.getSession(), mLocation)
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .build();

                message.put(MessageKey.EVENT_START_TIME, mAppStateManager.getSession().mLastEventTime);
                message.put(MessageKey.BREADCRUMB_SESSION_COUNTER, getCurrentSessionCounter());
                message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb);
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_BREADCRUMB, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle breadcrumb message");

            }
        }
        return null;
    }

    public MPMessage optOut(long time, boolean optOutStatus) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.OPT_OUT, mAppStateManager.getSession(), mLocation)
                    .timestamp(time)
                    .build();
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            return message;
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle opt out message");
            return null;
        }
    }

    public MPMessage logErrorEvent(String errorMessage, Throwable t, JSONObject attributes) {
        return logErrorEvent(errorMessage, t, attributes, true);
    }

    public MPMessage logErrorEvent(String errorMessage, Throwable t, JSONObject attributes, boolean caught) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.ERROR, mAppStateManager.getSession(), mLocation)
                    .timestamp(mAppStateManager.getSession().mLastEventTime)
                    .attributes(attributes)
                    .build();
            if (t != null) {
                message.put(MessageKey.ERROR_MESSAGE, t.getMessage());
                message.put(MessageKey.ERROR_SEVERITY, caught ? "error" : "fatal");
                message.put(MessageKey.ERROR_CLASS, t.getClass().getCanonicalName());
                StringWriter stringWriter = new StringWriter();
                t.printStackTrace(new PrintWriter(stringWriter));
                message.put(MessageKey.ERROR_STACK_TRACE, stringWriter.toString());
                message.put(MessageKey.ERROR_UNCAUGHT, String.valueOf(caught));
                message.put(MessageKey.ERROR_SESSION_COUNT, getCurrentSessionCounter());
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            } else if (errorMessage != null) {
                message.put(MessageKey.ERROR_SEVERITY, "error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            }
            return message;
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle error message");
        }
        return null;
    }

    public MPMessage logNetworkPerformanceEvent(long time, String method, String url, long length, long bytesSent, long bytesReceived, String requestString) {
        if (!MPUtility.isEmpty(url) && !MPUtility.isEmpty(method)) {
            try {
                MPMessage message = new MPMessage.Builder(MessageType.NETWORK_PERFORMNACE, mAppStateManager.getSession(), mLocation)
                        .timestamp(time)
                        .build();
                message.put(MessageKey.NPE_METHOD, method);
                message.put(MessageKey.NPE_URL, url);
                message.put(MessageKey.NPE_LENGTH, length);
                message.put(MessageKey.NPE_SENT, bytesSent);
                message.put(MessageKey.NPE_REC, bytesReceived);
                if (requestString != null) {
                    message.put(MessageKey.NPE_POST_DATA, requestString);
                }
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle network performance message");
            }
        }
        return null;
    }


    public MPMessage setPushRegistrationId(String token, boolean registeringFlag) {
        if (!MPUtility.isEmpty(token)) {
            try {
                MPMessage message = new MPMessage.Builder(MessageType.PUSH_REGISTRATION, mAppStateManager.getSession(), mLocation)
                        .timestamp(System.currentTimeMillis())
                        .build();
                message.put(MessageKey.PUSH_TOKEN, token);
                message.put(MessageKey.PUSH_TOKEN_TYPE, "google");
                message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag);

                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle push registration message");
            }
        }
        return null;
    }

    public void setSessionAttributes() {
        Session session = mAppStateManager.getSession();
        if (session.mSessionAttributes != null) {
            try {
                JSONObject sessionAttributes = new JSONObject();
                sessionAttributes.put(MessageKey.SESSION_ID, mAppStateManager.getSession().mSessionID);
                sessionAttributes.put(MessageKey.ATTRIBUTES, session.mSessionAttributes);
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_ATTRIBUTES,
                        sessionAttributes));
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to send update session attributes message");
            }
        }
    }

    /**
     * We will attempt to upload by default every 10 minutes until the session times out.
     */
    public void startUploadLoop() {
        mUploadHandler.removeMessages(UploadHandler.UPLOAD_MESSAGES);
        mUploadHandler.sendEmptyMessageDelayed(UploadHandler.UPLOAD_MESSAGES, Constants.INITIAL_UPLOAD_DELAY);
    }

    public void doUpload() {
        mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, 1, 0));
    }

    public void setLocation(Location location) {
        mLocation = location;
        ConfigManager.log(MParticle.LogLevel.DEBUG, "Received location update: " + location);
    }

    public Location getLocation() {
        return mLocation;
    }

    public MPMessage logStateTransition(String stateTransInit, String currentActivity,
                                   String launchUri, String launchExtras, String launchSourcePackage, long previousForegroundTime, long suspendedTime, int interruptions) {
        if (!MPUtility.isEmpty(stateTransInit)) {
            try {
                MPMessage message = new MPMessage.Builder(MessageType.APP_STATE_TRANSITION, mAppStateManager.getSession(), mLocation)
                        .timestamp(System.currentTimeMillis())
                        .build();

                message.put(MessageKey.STATE_TRANSITION_TYPE, stateTransInit);
                if (currentActivity != null) {
                    message.put(MessageKey.CURRENT_ACTIVITY, currentActivity);
                }

                boolean crashedInForeground = mPreferences.getBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false);

                if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_INIT) ||
                        stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_FORE)) {
                    mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, true).apply();
                    message.put(MessageKey.ST_LAUNCH_REFERRER, launchUri);
                    message.put(MessageKey.ST_LAUNCH_PARAMS, launchExtras);
                    message.put(MessageKey.ST_LAUNCH_SOURCE_PACKAGE, launchSourcePackage);
                    if (previousForegroundTime > 0) {
                        message.put(MessageKey.ST_LAUNCH_PRV_FORE_TIME, previousForegroundTime);
                    }
                    if (suspendedTime > 0) {
                        message.put(MessageKey.ST_LAUNCH_TIME_SUSPENDED, suspendedTime);
                    }
                    if (interruptions >= 0) {
                        message.put(MessageKey.ST_INTERRUPTIONS, interruptions);
                    }
                    InfluenceOpenMessage influenceOpenMessage = new InfluenceOpenMessage(message.getTimestamp(), mConfigManager.getInfluenceOpenTimeoutMillis());
                    mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.MARK_INFLUENCE_OPEN_GCM, influenceOpenMessage));
                }

                if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_INIT)) {
                    SharedPreferences.Editor editor = mPreferences.edit();

                    if (!mFirstRun) {
                        message.put(MessageKey.APP_INIT_CRASHED, crashedInForeground);
                    }


                    int versionCode = 0;
                    try {
                        PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                        versionCode = pInfo.versionCode;
                    } catch (PackageManager.NameNotFoundException nnfe) {

                    }
                    //if we've seen this device before, and the versionCode is different, then we know it's an upgrade
                    boolean upgrade = (versionCode != mPreferences.getInt(Constants.PrefKeys.INITUPGRADE, versionCode));
                    editor.putInt(Constants.PrefKeys.INITUPGRADE, versionCode).apply();

                    if (!upgrade) {
                        if (mInstallType == MParticle.InstallType.KnownUpgrade) {
                            upgrade = true;
                        } else if (mInstallType == MParticle.InstallType.KnownInstall) {
                            upgrade = false;
                        }
                    }

                    message.put(MessageKey.APP_INIT_FIRST_RUN, mFirstRun);
                    message.put(MessageKey.APP_INIT_UPGRADE, upgrade);
                }

                if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_BG)) {
                    mPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false).apply();
                    mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.CLEAR_PROVIDER_GCM, message.getTimestamp()));
                }

                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle state transition message");
            }
        }
        return null;
    }

    public void logNotification(ProviderCloudMessage cloudMessage, String appState) {
        try{
            MPMessage message = new MPMessage.Builder(MessageType.PUSH_RECEIVED, mAppStateManager.getSession(), mLocation)
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build();

            message.put(MessageKey.PAYLOAD, cloudMessage.getRedactedJsonPayload().toString());
            message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_RECEIVED);

            PushRegistrationHelper.PushRegistration registration = PushRegistrationHelper.getLatestPushRegistration(mContext);
            if ((registration != null) && (registration.instanceId != null) && (registration.instanceId.length() > 0)) {
                message.put(MessageKey.PUSH_TOKEN, registration.instanceId);
            }
            message.put(MessageKey.APP_STATE, appState);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

        }catch (JSONException e) {

        }
    }

    @Override
    public void logNotification(int contentId, String payload, CloudAction action, String appState, int newBehavior) {
        try{
            MPMessage message = new MPMessage.Builder(MessageType.PUSH_RECEIVED, mAppStateManager.getSession(), mLocation)
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build();

            message.put(MessageKey.PAYLOAD, payload);
            message.put(MessageKey.PUSH_BEHAVIOR, newBehavior);
            message.put(MParticleDatabase.GcmMessageTable.CONTENT_ID, contentId);

            if (action == null || action.getActionIdInt() == contentId){
                message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_RECEIVED);
            }else{
                message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_ACTION);
                message.put(MessageKey.PUSH_ACTION_TAKEN, action.getActionIdentifier());
                String title = action.getTitle();
                if (MPUtility.isEmpty(title)){
                    title = action.getActionIdentifier();
                }
                message.put(MessageKey.PUSH_ACTION_NAME, title);
            }

            PushRegistrationHelper.PushRegistration registration = PushRegistrationHelper.getLatestPushRegistration(mContext);
            if ((registration != null) && (registration.instanceId != null) && (registration.instanceId.length() > 0)) {
                message.put(MessageKey.PUSH_TOKEN, registration.instanceId);
            }
            message.put(MessageKey.APP_STATE, appState);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

        }catch (JSONException e) {

        }

    }

    public void logProfileAction(String action) {
        try {

            MPMessage message = new MPMessage.Builder(MessageType.PROFILE, mAppStateManager.getSession(), mLocation)
                    .timestamp(System.currentTimeMillis())
                    .build();

            message.put(Constants.ProfileActions.KEY, action);

            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle log event message");
        }
    }

    @Override
    public MPMessage createMessageSessionEnd(String sessionId, long start, long end, long foregroundLength, JSONObject sessionAttributes) throws JSONException{
        int eventCounter = mPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
        resetEventCounter();
        Session session = new Session();
        session.mSessionID = sessionId;
        session.mSessionStartTime = start;
        MPMessage message = new MPMessage.Builder(MessageType.SESSION_END, session, mLocation)
                .timestamp(end)
                .attributes(sessionAttributes)
                .build();
        message.put(MessageKey.EVENT_COUNTER, eventCounter);
        message.put(MessageKey.SESSION_LENGTH, foregroundLength);
        message.put(MessageKey.SESSION_LENGTH_TOTAL, (end - start));
        message.put(MessageKey.STATE_INFO_KEY, MessageManager.getStateInfo());
        return message;
    }
    
    public MPMessage logUserAttributeChangeMessage(String userAttributeKey, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time) {
        try {
            MPMessage message = new MPMessage.Builder(MessageType.USER_ATTRIBUTE_CHANGE, mAppStateManager.getSession(), mLocation)
                    .timestamp(time)
                    .build();
            message.put(MessageKey.NAME, userAttributeKey);

            if (newValue == null || deleted) {
                newValue = JSONObject.NULL;
            } else if (newValue instanceof List) {
                JSONArray newValueArray = new JSONArray();
                for (int i = 0; i < ((List) newValue).size(); i++) {
                    String value = (String)((List) newValue).get(i);
                    newValueArray.put(value);
                }
                newValue = newValueArray;
            }
            message.put(MessageKey.NEW_ATTRIBUTE_VALUE, newValue);

            if (oldValue == null) {
                oldValue = JSONObject.NULL;
            } else if (oldValue instanceof List) {
                JSONArray oldValueArray = new JSONArray();
                for (int i = 0; i < ((List) oldValue).size(); i++) {
                    String value = (String)((List) oldValue).get(i);
                    oldValueArray.put(value);
                }
                oldValue = oldValueArray;
            }
            message.put(MessageKey.OLD_ATTRIBUTE_VALUE, oldValue);

            message.put(MessageKey.ATTRIBUTE_DELETED, deleted);
            message.put(MessageKey.IS_NEW_ATTRIBUTE, isNewAttribute);
            mMessageHandler.handleMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            return message;
        } catch (JSONException e) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to create mParticle user-attribute-change message");
        }
        return null;
    }

    @Override
    public String getApiKey() {
        return mConfigManager.getApiKey();
    }

    @Override
    public void delayedStart() {
        try {
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
                    ConnectivityManager connectivyManager = (ConnectivityManager) mContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = connectivyManager.getActiveNetworkInfo();
                    setDataConnection(activeNetwork);
                    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                }
                mContext.registerReceiver(sStatusBroadcastReceiver, filter);
            }
        }catch (Exception e){
            //this can sometimes fail due to wonky-device reasons.
        }
    }

    @Override
    public void endUploadLoop() {
        mUploadHandler.removeMessages(UploadHandler.UPLOAD_MESSAGES);
        MParticle.getInstance().upload();
    }

    @Override
    public void checkForTrigger(MPMessage message) {
        if (mConfigManager.shouldTrigger(message)){
            mUploadHandler.removeMessages(UploadHandler.UPLOAD_TRIGGER_MESSAGES);
            mUploadHandler.sendMessageDelayed(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_TRIGGER_MESSAGES, 1, 0), Constants.TRIGGER_MESSAGE_DELAY);
        }
    }

    public void refreshConfiguration() {
        mUploadHandler.sendEmptyMessage(UploadHandler.UPDATE_CONFIG);
    }

    public void initConfigDelayed() {
        mUploadHandler.sendEmptyMessageDelayed(UploadHandler.INIT_CONFIG, 20 * 1000);
    }

    public void saveGcmMessage(MPCloudNotificationMessage cloudMessage, String appState) {
        Message message = mMessageHandler.obtainMessage(MessageHandler.STORE_GCM_MESSAGE, cloudMessage);
        Bundle data = new Bundle();
        data.putString(MParticleDatabase.GcmMessageTable.APPSTATE, appState);
        message.setData(data);
        mMessageHandler.sendMessage(message);
    }

    public void saveGcmMessage(ProviderCloudMessage cloudMessage, String appState) {
        Message message = mMessageHandler.obtainMessage(MessageHandler.STORE_GCM_MESSAGE, cloudMessage);
        Bundle data = new Bundle();
        data.putString(MParticleDatabase.GcmMessageTable.APPSTATE, appState);
        message.setData(data);
        mMessageHandler.sendMessage(message);
    }

    @Override
    public void log(JsonReportingMessage reportingMessage) {
        if (reportingMessage != null) {
            List<JsonReportingMessage> reportingMessageList = new ArrayList<JsonReportingMessage>(1);
            reportingMessageList.add(reportingMessage);
            logAll(reportingMessageList);
        }
    }

    @Override
    public void logAll(List<? extends JsonReportingMessage> messageList) {
        if (messageList != null && messageList.size() > 0) {
            boolean development = ConfigManager.getEnvironment().equals(MParticle.Environment.Development);
            for (int i = 0; i < messageList.size(); i++) {
                messageList.get(i).setDevMode(development);
            }
            Message message = mMessageHandler.obtainMessage(MessageHandler.STORE_REPORTING_MESSAGE_LIST, messageList);
            mMessageHandler.sendMessage(message);
        }
    }

    public Map<String, Object> getAllUserAttributes(final UserAttributeListener listener) {
        Map<String, Object> allUserAttributes = new HashMap<String, Object>();
        if (listener == null || Looper.getMainLooper() != Looper.myLooper()) {
            Map<String, String> userAttributes = mMessageHandler.getUserAttributeSingles();
            Map<String, List<String>> userAttributeLists = mMessageHandler.getUserAttributeLists();
            if (listener != null) {
                listener.onUserAttributesReceived(userAttributes, userAttributeLists);
            }
            if (userAttributes != null) {
                allUserAttributes.putAll(userAttributes);
            }
            if (userAttributeLists != null) {
                allUserAttributes.putAll(userAttributeLists);
            }
            return allUserAttributes;
        }else {
            new AsyncTask<Void, Void, UserAttributeResponse>() {
                @Override
                protected UserAttributeResponse doInBackground(Void... params) {
                    return getUserAttributes();
                }

                @Override
                protected void onPostExecute(UserAttributeResponse attributes) {
                    if (listener != null) {
                        listener.onUserAttributesReceived(attributes.attributeSingles, attributes.attributeLists);
                    }
                }
            }.execute();
            return null;
        }
    }

    public Map<String, String> getUserAttributes(final UserAttributeListener listener) {
        if (listener == null || Looper.getMainLooper() != Looper.myLooper()) {
            Map<String, String> userAttributes = mMessageHandler.getUserAttributeSingles();

            if (listener != null) {
                Map<String, List<String>> userAttributeLists = mMessageHandler.getUserAttributeLists();
                listener.onUserAttributesReceived(userAttributes, userAttributeLists);
            }
            return userAttributes;
        }else {
            new AsyncTask<Void, Void, UserAttributeResponse>() {
                @Override
                protected UserAttributeResponse doInBackground(Void... params) {
                    return getUserAttributes();
                }

                @Override
                protected void onPostExecute(UserAttributeResponse attributes) {
                    if (listener != null) {
                        listener.onUserAttributesReceived(attributes.attributeSingles, attributes.attributeLists);
                    }
                }
            }.execute();
            return null;
        }
    }

    public Map<String, List<String>> getUserAttributeLists() {
        return mMessageHandler.getUserAttributeLists();
    }

    public void removeUserAttribute(String key) {
        UserAttributeRemoval container = new UserAttributeRemoval();
        container.key = key;
        container.time = System.currentTimeMillis();
        Message message = mMessageHandler.obtainMessage(MessageHandler.REMOVE_USER_ATTRIBUTE, container);
        mMessageHandler.sendMessage(message);
    }

    @Override
    public void attributeRemoved(String key) {
        String serializedJsonArray = mPreferences.getString(Constants.PrefKeys.DELETED_USER_ATTRS + mConfigManager.getApiKey(), null);
        JSONArray deletedAtributes;
        try {
            deletedAtributes = new JSONArray(serializedJsonArray);
        } catch (Exception jse) {
            deletedAtributes = new JSONArray();
        }
        deletedAtributes.put(key);

        mPreferences.edit().putString(Constants.PrefKeys.DELETED_USER_ATTRS + mConfigManager.getApiKey(), deletedAtributes.toString()).apply();
    }

    public void setUserAttribute(String key, Object value) {
        UserAttributeResponse container = new UserAttributeResponse();
        container.time = System.currentTimeMillis();
        if (value instanceof List) {
            container.attributeLists = new HashMap<String, List<String>>();
            container.attributeLists.put(key, (List<String>) value);
        }else {
            container.attributeSingles = new HashMap<String, String>();
            container.attributeSingles.put(key, (String) value);
        }
        Message message = mMessageHandler.obtainMessage(MessageHandler.SET_USER_ATTRIBUTE, container);

        mMessageHandler.sendMessage(message);
    }

    public void incrementUserAttribute(String key, int value) {
        Message message = mMessageHandler.obtainMessage(MessageHandler.INCREMENT_USER_ATTRIBUTE, key);
        message.arg1 = value;
        mMessageHandler.sendMessage(message);
    }

    static class UserAttributeResponse {
        Map<String, String> attributeSingles;
        Map<String, List<String>> attributeLists;
        long time;
    }

    static class UserAttributeRemoval {
        String key;
        long time;
    }

    private UserAttributeResponse getUserAttributes() {
        UserAttributeResponse response = new UserAttributeResponse();
        response.attributeSingles = mMessageHandler.getUserAttributeSingles();
        response.attributeLists = mMessageHandler.getUserAttributeLists();
        return response;
    }

    private class StatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context appContext, Intent intent) {
            try {
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
            }catch (Exception e){
                //sometimes we're given a null intent,
                //or even if we have permissions to ACCESS_NETWORK_STATE, the call may fail.
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
            mUploadHandler.setConnected(activeNetwork.isConnectedOrConnecting());
        } else {
            sActiveNetworkName = "offline";
            mUploadHandler.setConnected(false);
        }


    }

    public class InfluenceOpenMessage {
        public final long mTimeStamp;
        public final long mTimeout;

        public InfluenceOpenMessage(long timestamp, long influenceOpenTimeoutMillis) {
            mTimeStamp = timestamp;
            mTimeout = influenceOpenTimeoutMillis;
        }
    }
}
