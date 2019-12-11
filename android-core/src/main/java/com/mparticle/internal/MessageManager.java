package com.mparticle.internal;

import android.annotation.SuppressLint;
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
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import com.mparticle.InstallReferrerHelper;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.identity.AliasRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.MessageType;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.messaging.ProviderCloudMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.mparticle.internal.Constants.MessageKey.ALIAS_REQUEST_TYPE;
import static com.mparticle.internal.Constants.MessageKey.API_KEY;
import static com.mparticle.internal.Constants.MessageKey.DATA;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_CONTEXT;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_ID;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_KEY;
import static com.mparticle.internal.Constants.MessageKey.DATA_PLAN_VERSION;
import static com.mparticle.internal.Constants.MessageKey.DEVICE_APPLICATION_STAMP_ALIAS;
import static com.mparticle.internal.Constants.MessageKey.END_TIME;
import static com.mparticle.internal.Constants.MessageKey.ENVIRONMENT_ALIAS;
import static com.mparticle.internal.Constants.MessageKey.SOURCE_MPID;
import static com.mparticle.internal.Constants.MessageKey.REQUEST_ID;
import static com.mparticle.internal.Constants.MessageKey.REQUEST_TYPE;
import static com.mparticle.internal.Constants.MessageKey.START_TIME;
import static com.mparticle.internal.Constants.MessageKey.DESTINATION_MPID;

/**
 * This class is primarily responsible for generating BaseMPMessage objects, and then adding them to a
 * queue which is then processed in a background thread for further processing and database storage.
 *
 */
public class MessageManager implements MessageManagerCallbacks, ReportingManager {
    private static Context sContext = null;
    static SharedPreferences sPreferences = null;
    static volatile boolean devicePerformanceMetricsDisabled;
    private final DeviceAttributes mDeviceAttributes;
    private AppStateManager mAppStateManager;
    private ConfigManager mConfigManager = null;
    private MParticleDBManager mMParticleDBManager;


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
    MessageHandler mMessageHandler;
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
    private @Nullable Location mLocation;
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
     * The app-customAttributes dictionary in each batch need to know the runtime of the SDK/app itself.
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

    private boolean delayedStartOccurred = false;

    /**
     * Used solely for unit testing
     */
    public MessageManager() {
        super();
        mDeviceAttributes = new DeviceAttributes();
    }

    /**
     * Used solely for unit testing
     */
    public MessageManager(Context appContext, ConfigManager configManager, MParticle.InstallType installType, AppStateManager appStateManager, MParticleDBManager dbManager, MessageHandler messageHandler, UploadHandler uploadHandler) {
        mDeviceAttributes = new DeviceAttributes();
        sContext = appContext.getApplicationContext();
        mConfigManager = configManager;
        mAppStateManager = appStateManager;
        mMParticleDBManager = dbManager;
        mMessageHandler = messageHandler;
        mUploadHandler = uploadHandler;
        sPreferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mInstallType = installType;
    }

    public MessageManager(ConfigManager configManager, AppStateManager appStateManager, boolean devicePerformanceMetricsDisabled, MParticleDBManager dbManager, MParticleOptions options) {
        this.devicePerformanceMetricsDisabled = devicePerformanceMetricsDisabled;
        mDeviceAttributes = new DeviceAttributes();
        sContext = options.getContext().getApplicationContext();
        mConfigManager = configManager;
        mAppStateManager = appStateManager;
        mAppStateManager.setMessageManager(this);
        mMParticleDBManager = dbManager;
        mMessageHandler = new MessageHandler(sMessageHandlerThread.getLooper(), this, options.getContext(), dbManager, options.getDataplanId(), options.getDataplanVersion());
        mUploadHandler = new UploadHandler(options.getContext(), sUploadHandlerThread.getLooper(), configManager, appStateManager, this, dbManager);
        sPreferences = options.getContext().getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        mInstallType = options.getInstallType();
    }

    private static TelephonyManager getTelephonyManager() {
        if (sTelephonyManager == null) {
            sTelephonyManager = (TelephonyManager) sContext.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return sTelephonyManager;
    }

    public boolean isDevicePerformanceMetricsDisabled() {
        return devicePerformanceMetricsDisabled;
    }

    public void setDevicePerformanceMetricsDisabled(boolean disabled) {
        devicePerformanceMetricsDisabled = disabled;
    }

    public static JSONObject getStateInfo() throws JSONException {
        JSONObject infoJson = new JSONObject();
        if (!devicePerformanceMetricsDisabled) {
            infoJson.put(MessageKey.STATE_INFO_AVAILABLE_DISK, MPUtility.getAvailableInternalDisk(sContext));
            infoJson.put(MessageKey.STATE_INFO_AVAILABLE_EXT_DISK, MPUtility.getAvailableExternalDisk(sContext));
            final Runtime rt = Runtime.getRuntime();
            infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_USAGE, rt.totalMemory());
            infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_AVAIL, rt.freeMemory());
            infoJson.put(MessageKey.STATE_INFO_APP_MEMORY_MAX, rt.maxMemory());
        }
        infoJson.put(MessageKey.STATE_INFO_AVAILABLE_MEMORY, MPUtility.getAvailableMemory(sContext));
        infoJson.put(MessageKey.STATE_INFO_TOTAL_MEMORY, getTotalMemory());
        infoJson.put(MessageKey.STATE_INFO_BATTERY_LVL, sBatteryLevel);
        infoJson.put(MessageKey.STATE_INFO_TIME_SINCE_START, MPUtility.millitime() - sStartTime);

        String gps = MPUtility.getGpsEnabled(sContext);
        if (gps != null){
            infoJson.put(MessageKey.STATE_INFO_GPS,Boolean.parseBoolean(gps));
        }
        infoJson.put(MessageKey.STATE_INFO_DATA_CONNECTION, sActiveNetworkName);
        int orientation = MPUtility.getOrientation(sContext);
        infoJson.put(MessageKey.STATE_INFO_ORIENTATION, orientation);
        infoJson.put(MessageKey.STATE_INFO_BAR_ORIENTATION, orientation);
        infoJson.put(MessageKey.STATE_INFO_MEMORY_LOW, MPUtility.isSystemMemoryLow(sContext));
        infoJson.put(MessageKey.STATE_INFO_MEMORY_THRESHOLD, getSystemMemoryThreshold());
        infoJson.put(MessageKey.STATE_INFO_NETWORK_TYPE, getTelephonyManager().getNetworkType());
        return infoJson;
    }

    public static long getTotalMemory() {
        long total = sPreferences.getLong(Constants.MiscStorageKeys.TOTAL_MEMORY, -1);
        if (total < 0) {
            total = MPUtility.getTotalMemory(sContext);
            SharedPreferences.Editor edit = sPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.TOTAL_MEMORY, total);
            edit.apply();
        }
        return total;
    }

    public static long getSystemMemoryThreshold() {
        long threshold = sPreferences.getLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, -1);
        if (threshold < 0) {
            threshold = MPUtility.getSystemMemoryThreshold(sContext);
            SharedPreferences.Editor edit = sPreferences.edit();
            edit.putLong(Constants.MiscStorageKeys.MEMORY_THRESHOLD, threshold);
            edit.apply();
        }
        return threshold;
    }

    public BaseMPMessage createFirstRunMessage() throws JSONException {
        return new BaseMPMessage.Builder(MessageType.FIRST_RUN, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                .timestamp(mAppStateManager.getSession().mSessionStartTime)
                .dataConnection(sActiveNetworkName)
                .build();
    }

    public BaseMPMessage startSession(InternalSession session) {
        try {
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.SESSION_START, session, mLocation, mConfigManager.getMpid())
                    .timestamp(mAppStateManager.getSession().mSessionStartTime)
                    .build();

            SharedPreferences.Editor editor = sPreferences.edit();
            long timeInFg = mConfigManager.getUserStorage().getPreviousSessionForegound();
            if (timeInFg > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_LENGTH, timeInFg / 1000);
                mConfigManager.getUserStorage().clearPreviousTimeInForeground();
            }
            String prevSessionId = mConfigManager.getUserStorage().getPreviousSessionId();
            mConfigManager.getUserStorage().setPreviousSessionId(session.mSessionID);
            if (!MPUtility.isEmpty(prevSessionId)) {
                message.put(MessageKey.PREVIOUS_SESSION_ID, prevSessionId);
            }

            long prevSessionStart = mConfigManager.getUserStorage().getPreviousSessionStart(-1);
            mConfigManager.getUserStorage().setPreviousSessionStart(session.mSessionStartTime);

            if (prevSessionStart > 0) {
                message.put(MessageKey.PREVIOUS_SESSION_START, prevSessionStart);
            }

            editor.apply();

            if (isFirstRunForMessage()) {
                setFirstRunForMessage(false);
                try {
                    JSONObject firstRunMessage = createFirstRunMessage();
                    mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, firstRunMessage));
                } catch (JSONException e) {
                    Logger.warning("Failed to create First Run Message.");
                }
            }else{
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.END_ORPHAN_SESSIONS, mConfigManager.getMpid()));
            }


            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

            mConfigManager.getUserStorage().incrementSessionCounter();
            return message;
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle start session message.");
            return null;
        }
    }

    public void updateSessionEnd(InternalSession session) {
        try {
            mConfigManager.getUserStorage().setPreviousSessionForeground(session.getForegroundTime());
            mMessageHandler
                    .sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_END, session));
        } catch (Exception e) {
            Logger.warning("Failed to send update session end message.");
        }
    }

    public void endSession(InternalSession session) {
        updateSessionEnd(session);
        Map.Entry<String, Set<Long>> entry = new HashMap.SimpleEntry<String, Set<Long>>(session.mSessionID, session.getMpids());
        mMessageHandler
                .sendMessage(mMessageHandler.obtainMessage(MessageHandler.CREATE_SESSION_END_MESSAGE, 1, 1, entry));
    }

    // check if a session has been started, if it has, then it means that the InstallReferrer was received
    // after the current session was started, is out of date, and needs to be updated. If the current
    // session has been started, disregard.
    public void installReferrerUpdated() {
        String sessionId = mAppStateManager.getSession().mSessionID;
        if (mAppStateManager.getSession().isActive()) {
            Message message = mMessageHandler.obtainMessage(MessageHandler.INSTALL_REFERRER_UPDATED, sessionId);
            mMessageHandler.sendMessage(message);
        }
    }


    public BaseMPMessage logEvent(MPEvent event, String currentActivity) {
        if (event != null) {
            try {

                BaseMPMessage message = new MPEventMessage.Builder(MessageType.EVENT, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                        .name(event.getEventName())
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .length(event.getLength())
                        .flags(event.getCustomFlags())
                        .attributes(MPUtility.enforceAttributeConstraints(event.getCustomAttributes()))
                        .build();
                message.put(MessageKey.EVENT_TYPE, event.getEventType());
                message.put(MessageKey.EVENT_START_TIME, message.getTimestamp());


                if (currentActivity != null) {
                    message.put(MessageKey.CURRENT_ACTIVITY, currentActivity);
                }

                int count = sPreferences.getInt(Constants.PrefKeys.EVENT_COUNTER, 0);
                message.put(MessageKey.EVENT_COUNTER, count);
                sPreferences.edit().putInt(Constants.PrefKeys.EVENT_COUNTER, ++count).apply();

                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                Logger.warning("Failed to create mParticle log event message.");
            }
        }
        return null;
    }

    public BaseMPMessage logEvent(CommerceEvent event) {
        if (event != null) {
            try {
                MParticleUser user = MParticle.getInstance().Identity().getCurrentUser();
                Cart cart = null;
                if (user != null) {
                    cart = user.getCart();
                }
                BaseMPMessage message = new MPCommerceMessage.Builder(event, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid(), cart)
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .flags(event.getCustomFlags())
                        .build();
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                Logger.warning("Failed to create mParticle log event message.");
            }
        }
        return null;
    }

    public BaseMPMessage logScreen(MPEvent event, boolean started) {
        if (event != null && event.getEventName() != null) {
            try {
                BaseMPMessage message = new BaseMPMessage.Builder(MessageType.SCREEN_VIEW, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .name(event.getEventName())
                        .flags(event.getCustomFlags())
                        .attributes(MPUtility.enforceAttributeConstraints(event.getCustomAttributes()))
                        .build();

                message.put(MessageKey.EVENT_START_TIME, mAppStateManager.getSession().mLastEventTime);
                message.put(MessageKey.EVENT_DURATION, 0);
                message.put(MessageKey.SCREEN_STARTED, started ? "activity_started" : "activity_stopped");
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                Logger.warning("Failed to create mParticle log event message.");
            }
        }
        return null;
    }

    public BaseMPMessage logBreadcrumb(String breadcrumb) {
        if (breadcrumb != null) {
            try {
                BaseMPMessage message = new BaseMPMessage.Builder(MessageType.BREADCRUMB, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                        .timestamp(mAppStateManager.getSession().mLastEventTime)
                        .build();

                message.put(MessageKey.EVENT_START_TIME, mAppStateManager.getSession().mLastEventTime);
                message.put(MessageKey.BREADCRUMB_SESSION_COUNTER, mConfigManager.getUserStorage().getCurrentSessionCounter());
                message.put(MessageKey.BREADCRUMB_LABEL, breadcrumb);
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_BREADCRUMB, message));
                return message;
            } catch (JSONException e) {
                Logger.warning("Failed to create mParticle breadcrumb message.");

            }
        }
        return null;
    }

    public BaseMPMessage optOut(long time, boolean optOutStatus) {
        try {
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.OPT_OUT, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                    .timestamp(time)
                    .build();
            message.put(MessageKey.OPT_OUT_STATUS, optOutStatus);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            return message;
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle opt out message.");
            return null;
        }
    }

    public BaseMPMessage logErrorEvent(String errorMessage, Throwable t, JSONObject attributes) {
        return logErrorEvent(errorMessage, t, attributes, true);
    }

    public BaseMPMessage logErrorEvent(String errorMessage, Throwable t, JSONObject attributes, boolean caught) {
        try {
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.ERROR, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
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
                message.put(MessageKey.ERROR_SESSION_COUNT, mConfigManager.getUserStorage().getCurrentSessionCounter());
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            } else if (errorMessage != null) {
                message.put(MessageKey.ERROR_SEVERITY, "error");
                message.put(MessageKey.ERROR_MESSAGE, errorMessage);
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            }
            return message;
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle error message.");
        }
        return null;
    }

    public BaseMPMessage logNetworkPerformanceEvent(long time, String method, String url, long length, long bytesSent, long bytesReceived, String requestString) {
        if (!MPUtility.isEmpty(url) && !MPUtility.isEmpty(method)) {
            try {
                BaseMPMessage message = new BaseMPMessage.Builder(MessageType.NETWORK_PERFORMNACE, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
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
                Logger.warning("Failed to create mParticle network performance message.");
            }
        }
        return null;
    }


    public BaseMPMessage setPushRegistrationId(String token, boolean registeringFlag) {
        if (!MPUtility.isEmpty(token)) {
            try {
                mConfigManager.setPushInstanceId(token);
                BaseMPMessage message = new BaseMPMessage.Builder(MessageType.PUSH_REGISTRATION, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                        .timestamp(System.currentTimeMillis())
                        .build();
                message.put(MessageKey.PUSH_TOKEN, token);
                message.put(MessageKey.PUSH_TOKEN_TYPE, "google");
                message.put(MessageKey.PUSH_REGISTER_FLAG, registeringFlag);

                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                Logger.warning("Failed to create mParticle push registration message.");
            }
        }
        return null;
    }

    public void setSessionAttributes() {
        InternalSession session = mAppStateManager.getSession();
        if (session.mSessionAttributes != null) {
            try {
                JSONObject sessionAttributes = new JSONObject();
                sessionAttributes.put(MessageKey.SESSION_ID, mAppStateManager.getSession().mSessionID);
                sessionAttributes.put(MessageKey.ATTRIBUTES, session.mSessionAttributes);
                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.UPDATE_SESSION_ATTRIBUTES,
                        sessionAttributes));
            } catch (JSONException e) {
                Logger.warning("Failed to send update session attributes message.");
            }
        }
    }

    /**
     * We will attempt to upload by default every 10 minutes until the session times out.
     */
    public void startUploadLoop() {
        mUploadHandler.removeMessages(UploadHandler.UPLOAD_MESSAGES, mConfigManager.getMpid());
        mUploadHandler.sendMessageDelayed(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, mConfigManager.getMpid()), Constants.INITIAL_UPLOAD_DELAY);
    }

    public void doUpload() {
        mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.CLEAR_MESSAGES_FOR_UPLOAD));
    }

    @Override
    public void messagesClearedForUpload() {
        mUploadHandler.sendMessage(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_MESSAGES, 1, 0, mConfigManager.getMpid()));
    }


    public void setLocation(@Nullable Location location) {
        mLocation = location;
        Logger.debug("Received location update: " + location);
    }

    public Location getLocation() {
        return mLocation;
    }

    public BaseMPMessage logStateTransition(String stateTransInit, String currentActivity,
                                            String launchUri, String launchExtras, String launchSourcePackage, long previousForegroundTime, long suspendedTime, int interruptions) {
        if (!MPUtility.isEmpty(stateTransInit)) {
            try {
                BaseMPMessage message = new BaseMPMessage.Builder(MessageType.APP_STATE_TRANSITION, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                        .timestamp(System.currentTimeMillis())
                        .build();

                message.put(MessageKey.STATE_TRANSITION_TYPE, stateTransInit);
                if (currentActivity != null) {
                    message.put(MessageKey.CURRENT_ACTIVITY, currentActivity);
                }

                boolean crashedInForeground = sPreferences.getBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false);

                if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_INIT) ||
                        stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_FORE)) {
                    sPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, true).apply();
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
                }

                if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_INIT)) {
                    SharedPreferences.Editor editor = sPreferences.edit();

                    if (!isFirstRunForAST()) {
                        message.put(MessageKey.APP_INIT_CRASHED, crashedInForeground);
                    }


                    int versionCode = 0;
                    try {
                        PackageInfo pInfo = sContext.getPackageManager().getPackageInfo(sContext.getPackageName(), 0);
                        versionCode = pInfo.versionCode;
                    } catch (PackageManager.NameNotFoundException nnfe) {

                    }
                    //if we've seen this device before, and the versionCode is different, then we know it's an upgrade
                    boolean upgrade = (versionCode != sPreferences.getInt(Constants.PrefKeys.INITUPGRADE, versionCode));
                    editor.putInt(Constants.PrefKeys.INITUPGRADE, versionCode).apply();

                    if (!upgrade) {
                        if (mInstallType == MParticle.InstallType.KnownUpgrade) {
                            upgrade = true;
                        } else if (mInstallType == MParticle.InstallType.KnownInstall) {
                            upgrade = false;
                        }
                    }

                    message.put(MessageKey.APP_INIT_FIRST_RUN, isFirstRunForAST());
                    if (isFirstRunForAST()) {
                        setFirstRunForAST(false);
                    }
                    message.put(MessageKey.APP_INIT_UPGRADE, upgrade);
                }

                if (stateTransInit.equals(Constants.StateTransitionType.STATE_TRANS_BG)) {
                    sPreferences.edit().putBoolean(Constants.PrefKeys.CRASHED_IN_FOREGROUND, false).apply();
                }

                mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
                return message;
            } catch (JSONException e) {
                Logger.warning("Failed to create mParticle state transition message.");
            }
        }
        return null;
    }

    public void logNotification(ProviderCloudMessage cloudMessage, String appState) {
        try{
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.PUSH_RECEIVED, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build();

            message.put(MessageKey.PAYLOAD, cloudMessage.getRedactedJsonPayload().toString());
            message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_RECEIVED);

            PushRegistrationHelper.PushRegistration registration = mConfigManager.getPushRegistration();
            if ((registration != null) && (registration.instanceId != null) && (registration.instanceId.length() > 0)) {
                message.put(MessageKey.PUSH_TOKEN, registration.instanceId);
            }
            message.put(MessageKey.APP_STATE, appState);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));

        }catch (JSONException e) {

        }
    }

    @Override
    public void logNotification(int contentId, String payload, String appState, int newBehavior) {
        try{
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.PUSH_RECEIVED, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                    .timestamp(System.currentTimeMillis())
                    .name("gcm")
                    .build();

            message.put(MessageKey.PAYLOAD, payload);
            message.put(MessageKey.PUSH_BEHAVIOR, newBehavior);
            message.put(MessageKey.CONTENT_ID, contentId);
            message.put(MessageKey.PUSH_TYPE, Constants.Push.MESSAGE_TYPE_RECEIVED);

            PushRegistrationHelper.PushRegistration registration = mConfigManager.getPushRegistration();
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

            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.PROFILE, mAppStateManager.getSession(), mLocation, mConfigManager.getMpid())
                    .timestamp(System.currentTimeMillis())
                    .build();

            message.put(Constants.ProfileActions.KEY, action);

            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle log event message.");
        }
    }

    public BaseMPMessage logUserIdentityChangeMessage(JSONObject newIdentity, JSONObject oldIdentity, JSONArray userIdentities, long mpId) {
        try {
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.USER_IDENTITY_CHANGE, mAppStateManager.getSession(), mLocation, mpId)
                    .timestamp(System.currentTimeMillis())
                    .build();
            if (newIdentity != null) {
                message.put(MessageKey.NEW_USER_IDENTITY, newIdentity);
            } else {
                message.put(MessageKey.NEW_USER_IDENTITY, JSONObject.NULL);
            }
            if (oldIdentity != null) {
                message.put(MessageKey.OLD_USER_IDENTITY, oldIdentity);
            } else {
                message.put(MessageKey.OLD_USER_IDENTITY, JSONObject.NULL);
            }
            message.put(MessageKey.USER_IDENTITIES, userIdentities);
            mMessageHandler.sendMessage(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            JSONArray seenIdentities = mConfigManager.markIdentitiesAsSeen(userIdentities, mpId);
            if (seenIdentities != null) {
                mConfigManager.saveUserIdentityJson(seenIdentities, mpId);
            }
            return message;
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle user-identity-change message.");
        } finally {
            mConfigManager.saveUserIdentityJson(userIdentities, mpId);
        }

        return null;
    }

    public BaseMPMessage logUserAttributeChangeMessage(String userAttributeKey, Object newValue, Object oldValue, boolean deleted, boolean isNewAttribute, long time, long mpId) {
        try {
            BaseMPMessage message = new BaseMPMessage.Builder(MessageType.USER_ATTRIBUTE_CHANGE, mAppStateManager.getSession(), mLocation, mpId)
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
            message.put(MessageKey.USER_ATTRIBUTES, mMParticleDBManager.getAllUserAttributesJson(mpId));
            mMessageHandler.handleMessageImpl(mMessageHandler.obtainMessage(MessageHandler.STORE_MESSAGE, message));
            return message;
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle user-attribute-change message.");
        }
        return null;
    }

    @Override
    public String getApiKey() throws MParticleApiClientImpl.MPNoConfigException {
        String apiKey = mConfigManager.getApiKey();
        if (MPUtility.isEmpty(apiKey)) {
            throw new MParticleApiClientImpl.MPNoConfigException();
        }
        return apiKey;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void delayedStart() {
        try {
            if (!delayedStartOccurred) {
                delayedStartOccurred = true;
                //get the previous Intent otherwise the first few messages will have 0 for battery level
                Intent batteryIntent = sContext.getApplicationContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                sBatteryLevel = level / (double) scale;

                sStatusBroadcastReceiver = new StatusBroadcastReceiver();
                // NOTE: if permissions are not correct all messages will be tagged as 'offline'
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                if (MPUtility.checkPermission(sContext, android.Manifest.permission.ACCESS_NETWORK_STATE)) {
                    //same as with battery, get current connection so we don't have to wait for the next change
                    ConnectivityManager connectivityManager = (ConnectivityManager) sContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
                    setDataConnection(activeNetwork);
                    filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                }
                sContext.registerReceiver(sStatusBroadcastReceiver, filter);

                InstallReferrerHelper.fetchInstallReferrer(sContext, new InstallReferrerHelper.InstallReferrerCallback() {
                    @Override
                    public void onReceived(String installReferrer) {
                        if (sContext != null) {
                            InstallReferrerHelper.setInstallReferrer(sContext.getApplicationContext(), installReferrer);
                        }
                    }

                    @Override
                    public void onFailed() {
                        //do nothing, it very may well be the case that the InstallReferrer API is not available
                    }
                });
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
    public void checkForTrigger(BaseMPMessage message) {
        if (mConfigManager.shouldTrigger(message)){
            mUploadHandler.removeMessages(UploadHandler.UPLOAD_TRIGGER_MESSAGES, mConfigManager.getMpid());
            mUploadHandler.sendMessageDelayed(mUploadHandler.obtainMessage(UploadHandler.UPLOAD_TRIGGER_MESSAGES, 1, 0, mConfigManager.getMpid()), Constants.TRIGGER_MESSAGE_DELAY);
        }
    }

    public void refreshConfiguration() {
        mUploadHandler.sendEmptyMessage(UploadHandler.UPDATE_CONFIG);
    }

    public void initConfigDelayed() {
        mUploadHandler.sendEmptyMessageDelayed(UploadHandler.INIT_CONFIG, 20 * 1000);
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
            boolean development = MPUtility.isDevEnv();
            String sessionId = mAppStateManager.getSession().mSessionID;
            for (int i = 0; i < messageList.size(); i++) {
                messageList.get(i).setDevMode(development);
                messageList.get(i).setSessionId(sessionId);
            }
            Message message = mMessageHandler.obtainMessage(MessageHandler.STORE_REPORTING_MESSAGE_LIST, new ReportingMpidMessage(messageList, mConfigManager.getMpid()));
            mMessageHandler.sendMessage(message);
        }
    }

    public Map<String, Object> getUserAttributes(final UserAttributeListener listener, long mpId) {
        return mMParticleDBManager.getUserAttributes(listener, mpId);
    }

    public void removeUserAttribute(String key, long mpId) {
        MParticleDBManager.UserAttributeRemoval container = new MParticleDBManager.UserAttributeRemoval();
        container.key = key;
        container.time = System.currentTimeMillis();
        container.mpId = mpId;
        Message message = mMessageHandler.obtainMessage(MessageHandler.REMOVE_USER_ATTRIBUTE, container);
        mMessageHandler.sendMessage(message);
    }

    @Override
    public void attributeRemoved(String key, long mpId) {
        String serializedJsonArray = mConfigManager.getUserStorage(mpId).getDeletedUserAttributes();
        JSONArray deletedAtributes;
        try {
            deletedAtributes = new JSONArray(serializedJsonArray);
        } catch (Exception jse) {
            deletedAtributes = new JSONArray();
        }
        deletedAtributes.put(key);
        mConfigManager.getUserStorage(mpId).setDeletedUserAttributes(deletedAtributes.toString());
    }

    public void setUserAttribute(String key, Object value, long mpId) {
        setUserAttribute(key, value, mpId, false);
    }

    /**
     * this should almost always be called with "synchronously" == false, do so unless you are
     * really sure you really need to.
     */
    public void setUserAttribute(String key, Object value, long mpId, boolean synchronously) {
        MParticleDBManager.UserAttributeResponse container = new MParticleDBManager.UserAttributeResponse();
        container.time = System.currentTimeMillis();
        container.mpId = mpId;
        if (value instanceof List) {
            container.attributeLists = new HashMap<String, List<String>>();
            container.attributeLists.put(key, (List<String>) value);
        }else {
            container.attributeSingles = new HashMap<String, String>();
            container.attributeSingles.put(key, (String) value);
        }
        if (synchronously) {
            mMessageHandler.setUserAttributes(container);
        } else {
            Message message = mMessageHandler.obtainMessage(MessageHandler.SET_USER_ATTRIBUTE, container);
            mMessageHandler.sendMessage(message);
        }
    }

    public void incrementUserAttribute(String key, int value, long mpId) {
        Map.Entry<String, Long> entry = new HashMap.SimpleEntry<String, Long>(key, mpId);
        Message message = mMessageHandler.obtainMessage(MessageHandler.INCREMENT_USER_ATTRIBUTE, entry);
        message.arg1 = value;
        mMessageHandler.sendMessage(message);
    }

    public synchronized DeviceAttributes getDeviceAttributes() {
        return mDeviceAttributes;
    }

    public Map<MParticle.IdentityType, String> getUserIdentities(long mpId) {
        return mConfigManager.getUserIdentities(mpId);
    }

    public JSONArray getUserIdentityJson(long mpId){
        return mConfigManager.getUserIdentityJson(mpId);
    }

    private MParticleDBManager.UserAttributeResponse getUserAttributes(long mpId) {
        MParticleDBManager.UserAttributeResponse response = new MParticleDBManager.UserAttributeResponse();
        response.attributeSingles = mMParticleDBManager.getUserAttributeSingles(mpId);
        response.attributeLists = mMParticleDBManager.getUserAttributeLists(mpId);
        return response;
    }

    Map<String, List<String>> toStringListMap(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        Map<String, List<String>> listMap = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> entry: map.entrySet()) {
            listMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
        }
        return listMap;
    }

    //this return valuable is significant if it is false. That means that a legacy SDK has on record
    // that this is not the first run for this instance of the application
    boolean isFirstRunForMessageLegacy() {
        return sPreferences.getBoolean(Constants.PrefKeys.FIRSTRUN_OBSELETE + mConfigManager.getApiKey(), true);
    }

    boolean isFirstRunForMessage() {
        return sPreferences.getBoolean(Constants.PrefKeys.FIRSTRUN_MESSAGE + mConfigManager.getApiKey(), true) && isFirstRunForMessageLegacy();
    }

    void setFirstRunForMessage(boolean firstRun) {
        sPreferences.edit()
                .putBoolean(Constants.PrefKeys.FIRSTRUN_MESSAGE + mConfigManager.getApiKey(), firstRun)
                .remove(Constants.PrefKeys.FIRSTRUN_OBSELETE + mConfigManager.getApiKey())
                .apply();
    }

    boolean isFirstRunForAST() {
        return sPreferences.getBoolean(Constants.PrefKeys.FIRSTRUN_AST + mConfigManager.getApiKey(), true) && isFirstRunForMessageLegacy();
    }

    void setFirstRunForAST(boolean firstRun) {
        sPreferences.edit()
                .putBoolean(Constants.PrefKeys.FIRSTRUN_AST + mConfigManager.getApiKey(), firstRun)
                .remove(Constants.PrefKeys.FIRSTRUN_OBSELETE + mConfigManager.getApiKey())
                .apply();
    }

    @SuppressLint("MissingPermission")
    private class StatusBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context appContext, Intent intent) {
            try {
                if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                    ConnectivityManager connectivityManager = (ConnectivityManager) appContext
                            .getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
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

    public BackgroundTaskHandler getTaskHandler() {
        return mUploadHandler;
    }

    public Handler getMessageHandler() {
        return mMessageHandler;
    }

    public MParticleDBManager getMParticleDBManager() {
        return mMParticleDBManager;
    }

    public void disableHandlers(){
        if (mMessageHandler != null) {
            mMessageHandler.disable(true);
        }
        if (mUploadHandler != null) {
            mUploadHandler.disable(true);
        }
    }

    public void logAliasRequest(final AliasRequest aliasRequest) {
        try {
            final MPAliasMessage aliasMessage = new MPAliasMessage(aliasRequest, mConfigManager.getDeviceApplicationStamp(), mConfigManager.getApiKey());
            mMessageHandler.sendMessage(mUploadHandler.obtainMessage(MessageHandler.STORE_ALIAS_MESSAGE, aliasMessage));
        } catch (JSONException e) {
            Logger.warning("Failed to create mParticle opt out message");
        }
    }

    public static class ReportingMpidMessage {
        long mpid;
        List<? extends JsonReportingMessage> reportingMessages;

        public ReportingMpidMessage(List<? extends JsonReportingMessage> reportingMessages, long mpid) {
            this.mpid = mpid;
            this.reportingMessages = reportingMessages;
        }
    }

    boolean hasDelayedStartOccurred() {
        return delayedStartOccurred;
    }

    public static class MPAliasMessage extends JSONObject {

        public MPAliasMessage(String jsonString) throws JSONException {
            super(jsonString);
        }

        protected MPAliasMessage(AliasRequest request, String deviceApplicationStamp, String apiKey) throws JSONException {
            String environment = getStringValue(ConfigManager.getEnvironment());
            String requestId = UUID.randomUUID().toString();

            JSONObject dataJson = new JSONObject()
                    .put(SOURCE_MPID, request.getSourceMpid())
                    .put(DESTINATION_MPID, request.getDestinationMpid())
                    .put(DEVICE_APPLICATION_STAMP_ALIAS, deviceApplicationStamp);

            if (request.getStartTime() != 0) {
                dataJson.put(START_TIME, request.getStartTime());
            }
            if (request.getEndTime() != 0) {
                dataJson.put(END_TIME, request.getEndTime());
            }

            put(DATA, dataJson);
            put(REQUEST_TYPE, ALIAS_REQUEST_TYPE);
            put(REQUEST_ID, requestId);
            put(ENVIRONMENT_ALIAS, environment);
            put(API_KEY, apiKey);
        }

        public AliasRequest getAliasRequest() throws JSONException {
            JSONObject data = getJSONObject(DATA);
            return AliasRequest.builder()
                    .destinationMpid(data.getLong(DESTINATION_MPID))
                    .sourceMpid(data.getLong(SOURCE_MPID))
                    .endTime(data.optLong(END_TIME, 0))
                    .startTime(data.optLong(START_TIME, 0))
                    .build();
        }

        public String getRequestId() throws JSONException {
            return getString(REQUEST_ID);
        }


        protected String getStringValue(MParticle.Environment environment) {
            switch (environment) {
                case Development:
                    return "development";
                case Production:
                    return "production";
                default:
                    return "";
            }
        }
    }

    public static class BaseMPMessage extends JSONObject{
        private long mpId;

        protected BaseMPMessage(){}

        protected BaseMPMessage(BaseMPMessageBuilder builder) throws JSONException{
            mpId = builder.mpid;
            put(MessageKey.TYPE, builder.mMessageType);
            put(MessageKey.TIMESTAMP, builder.mTimestamp);
            if (MessageType.SESSION_START.equals(builder.mMessageType)) {
                put(MessageKey.ID, builder.mSession.mSessionID);
            } else {
                put(MessageKey.SESSION_ID, builder.mSession.mSessionID);

                if (builder.mSession.mSessionStartTime > 0) {
                    put(MessageKey.SESSION_START_TIMESTAMP, builder.mSession.mSessionStartTime);
                }
            }

            if (builder.mName != null) {
                put(MessageKey.NAME, builder.mName);
            }

            if (builder.mCustomFlags != null) {
                JSONObject flagsObject = new JSONObject();
                for (Map.Entry<String, List<String>> entry : builder.mCustomFlags.entrySet()) {
                    List<String> values = entry.getValue();
                    JSONArray valueArray = new JSONArray(values);
                    flagsObject.put(entry.getKey(), valueArray);
                }
                put(MessageKey.EVENT_FLAGS, flagsObject);
            }

            if (builder.mLength != null){
                put(MessageKey.EVENT_DURATION, builder.mLength);
                if (builder.mAttributes == null){
                    builder.mAttributes = new JSONObject();
                }
                if (!builder.mAttributes.has("EventLength")) {
                    //can't be longer than max int milliseconds
                    builder.mAttributes.put("EventLength", Integer.toString(builder.mLength.intValue()));
                }
            }

            if (builder.mAttributes != null) {
                put(MessageKey.ATTRIBUTES, builder.mAttributes);
            }

            if (builder.mDataConnection != null) {
                put(MessageKey.STATE_INFO_DATA_CONNECTION, builder.mDataConnection);
            }

            if (!(MessageType.ERROR.equals(builder.mMessageType) &&
                    !(MessageType.OPT_OUT.equals(builder.mMessageType)))) {
                if (builder.mLocation != null) {
                    JSONObject locJSON = new JSONObject();
                    locJSON.put(MessageKey.LATITUDE, builder.mLocation .getLatitude());
                    locJSON.put(MessageKey.LONGITUDE, builder.mLocation .getLongitude());
                    locJSON.put(MessageKey.ACCURACY, builder.mLocation .getAccuracy());
                    put(MessageKey.LOCATION, locJSON);
                }
            }
        }

        public JSONObject getAttributes(){
            return optJSONObject(MessageKey.ATTRIBUTES);
        }

        public long getTimestamp(){
            try {
                return getLong(MessageKey.TIMESTAMP);
            }catch (JSONException jse){

            }
            return 0;
        }

        public String getSessionId() {
            if (MessageType.SESSION_START.equals(getMessageType())) {
                return optString(MessageKey.ID, Constants.NO_SESSION_ID);
            } else {
                return optString(MessageKey.SESSION_ID, Constants.NO_SESSION_ID);
            }
        }

        public String getMessageType() {
            return optString(MessageKey.TYPE);
        }

        public int getTypeNameHash() {
            return MPUtility.mpHash(getMessageType() + getName());
        }

        public String getName() {
            return optString(MessageKey.NAME);
        }

        public long getMpId() {
            return mpId;
        }

        public static class Builder extends BaseMPMessageBuilder {

            public Builder(String messageType, InternalSession session, @Nullable Location location, long mpId) {
                super(messageType, session, location, mpId);
            }
        }
    }

    public static class BaseMPMessageBuilder {
        final String mMessageType;
        final InternalSession mSession;
        long mTimestamp;
        String mName;
        JSONObject mAttributes;
        Location mLocation;
        String mDataConnection;
        Double mLength = null;
        Map<String, List<String>> mCustomFlags;
        long mpid;

        protected BaseMPMessageBuilder(String messageType, InternalSession session, @Nullable Location location, long mpId) {
            mMessageType = messageType;
            mSession = new InternalSession(session);
            mLocation = location;
            mpid = mpId;
        }

        public BaseMPMessageBuilder timestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        public BaseMPMessageBuilder name(String name) {
            mName = name;
            return this;
        }

        public BaseMPMessageBuilder attributes(JSONObject attributes) {
            mAttributes = attributes;
            return this;
        }

        public BaseMPMessage build() throws JSONException {
            return new BaseMPMessage(this);
        }

        public BaseMPMessageBuilder dataConnection(String dataConnection) {
            mDataConnection = dataConnection;
            return this;
        }

        public BaseMPMessageBuilder length(Double length) {
            mLength = length;
            return this;
        }

        public BaseMPMessageBuilder flags(Map<String, List<String>> customFlags) {
            mCustomFlags = customFlags;
            return this;
        }
    }

    public static class MPCommerceMessage extends BaseMPMessage {

        protected MPCommerceMessage(Builder builder) throws JSONException {
            super(builder);
            addCommerceEventInfo(this, builder.commerceEvent, builder.cart);
        }

        private static void addCommerceEventInfo(BaseMPMessage message, CommerceEvent event, @Nullable Cart cart) {
            try {

                if (event.getScreen() != null) {
                    message.put(Constants.Commerce.SCREEN_NAME, event.getScreen());
                }
                if (event.getNonInteraction() != null) {
                    message.put(Constants.Commerce.NON_INTERACTION, event.getNonInteraction().booleanValue());
                }
                if (event.getCurrency() != null) {
                    message.put(Constants.Commerce.CURRENCY, event.getCurrency());
                }
                if (event.getCustomAttributes() != null) {
                    message.put(Constants.Commerce.ATTRIBUTES, MPUtility.mapToJson(event.getCustomAttributes()));
                }
                if (event.getProductAction() != null) {
                    JSONObject productAction = new JSONObject();
                    message.put(Constants.Commerce.PRODUCT_ACTION_OBJECT, productAction);
                    productAction.put(Constants.Commerce.PRODUCT_ACTION, event.getProductAction());
                    if (event.getCheckoutStep() != null) {
                        productAction.put(Constants.Commerce.CHECKOUT_STEP, event.getCheckoutStep());
                    }
                    if (event.getCheckoutOptions() != null) {
                        productAction.put(Constants.Commerce.CHECKOUT_OPTIONS, event.getCheckoutOptions());
                    }
                    if (event.getProductListName() != null) {
                        productAction.put(Constants.Commerce.PRODUCT_LIST_NAME, event.getProductListName());
                    }
                    if (event.getProductListSource() != null) {
                        productAction.put(Constants.Commerce.PRODUCT_LIST_SOURCE, event.getProductListSource());
                    }
                    if (event.getTransactionAttributes() != null) {
                        TransactionAttributes transactionAttributes = event.getTransactionAttributes();
                        if (transactionAttributes.getId() != null) {
                            productAction.put(Constants.Commerce.TRANSACTION_ID, transactionAttributes.getId());
                        }
                        if (transactionAttributes.getAffiliation() != null) {
                            productAction.put(Constants.Commerce.TRANSACTION_AFFILIATION, transactionAttributes.getAffiliation());
                        }
                        if (transactionAttributes.getRevenue() != null) {
                            productAction.put(Constants.Commerce.TRANSACTION_REVENUE, transactionAttributes.getRevenue());
                        }
                        if (transactionAttributes.getTax() != null) {
                            productAction.put(Constants.Commerce.TRANSACTION_TAX, transactionAttributes.getTax());
                        }
                        if (transactionAttributes.getShipping() != null) {
                            productAction.put(Constants.Commerce.TRANSACTION_SHIPPING, transactionAttributes.getShipping());
                        }
                        if (transactionAttributes.getCouponCode() != null) {
                            productAction.put(Constants.Commerce.TRANSACTION_COUPON_CODE, transactionAttributes.getCouponCode());
                        }
                    }
                    if (event.getProducts() != null && event.getProducts().size() > 0) {
                        JSONArray products = new JSONArray();
                        for (int i = 0; i < event.getProducts().size(); i++) {
                            products.put(new JSONObject(event.getProducts().get(i).toString()));
                        }
                        productAction.put(Constants.Commerce.PRODUCT_LIST, products);
                    }
                    if (event.getProductAction().equals(Product.PURCHASE)) {
                        if (cart != null) {
                            cart.clear();
                        }

                    }
                    if (cart != null) {
                        JSONObject cartJsonObject = new JSONObject(cart.toString());
                        if (cartJsonObject.length() > 0) {
                            message.put("sc", cartJsonObject);
                        }
                    }
                }
                if (event.getPromotionAction() != null) {
                    JSONObject promotionAction = new JSONObject();
                    message.put(Constants.Commerce.PROMOTION_ACTION_OBJECT, promotionAction);
                    promotionAction.put(Constants.Commerce.PROMOTION_ACTION, event.getPromotionAction());
                    if (event.getPromotions() != null && event.getPromotions().size() > 0) {
                        JSONArray promotions = new JSONArray();
                        for (int i = 0; i < event.getPromotions().size(); i++) {
                            promotions.put(getPromotionJson(event.getPromotions().get(i)));
                        }
                        promotionAction.put(Constants.Commerce.PROMOTION_LIST, promotions);
                    }
                }
                if (event.getImpressions() != null && event.getImpressions().size() > 0) {
                    JSONArray impressions = new JSONArray();
                    for (Impression impression : event.getImpressions()) {
                        JSONObject impressionJson = new JSONObject();
                        if (impression.getListName() != null) {
                            impressionJson.put(Constants.Commerce.IMPRESSION_LOCATION, impression.getListName());
                        }
                        if (impression.getProducts() != null && impression.getProducts().size() > 0) {
                            JSONArray productsJson = new JSONArray();
                            impressionJson.put(Constants.Commerce.IMPRESSION_PRODUCT_LIST, productsJson);
                            for (Product product : impression.getProducts()) {
                                productsJson.put(new JSONObject(product.toString()));
                            }
                        }
                        if (impressionJson.length() > 0) {
                            impressions.put(impressionJson);
                        }
                    }
                    if (impressions.length() > 0) {
                        message.put(Constants.Commerce.IMPRESSION_OBJECT, impressions);
                    }
                }


            } catch (Exception jse) {

            }
        }

        private static JSONObject getPromotionJson(Promotion promotion) {
            JSONObject json = new JSONObject();
            try {
                if (!MPUtility.isEmpty(promotion.getId())) {
                    json.put("id", promotion.getId());
                }
                if (!MPUtility.isEmpty(promotion.getName())) {
                    json.put("nm", promotion.getName());
                }
                if (!MPUtility.isEmpty(promotion.getCreative())) {
                    json.put("cr", promotion.getCreative());
                }
                if (!MPUtility.isEmpty(promotion.getPosition())) {
                    json.put("ps", promotion.getPosition());
                }
            } catch (JSONException jse) {

            }
            return json;
        }

        public static class Builder extends BaseMPMessageBuilder {
            private CommerceEvent commerceEvent;
            private Cart cart;

            public Builder(CommerceEvent commerceEvent, InternalSession session, Location location, long mpId, Cart cart) {
                super(MessageType.COMMERCE_EVENT, session, location, mpId);
                this.commerceEvent = commerceEvent;
                this.cart = cart;
            }

            @Override
            public BaseMPMessage build() throws JSONException {
                return new MPCommerceMessage(this);
            }
        }
    }

    public static class MPEventMessage extends BaseMPMessage {

        protected MPEventMessage(Builder builder) throws JSONException {
            super(builder);
        }

        public static class Builder extends BaseMPMessageBuilder {

            public Builder(String messageType, InternalSession session, Location location, long mpId) {
                super(messageType, session, location, mpId);
            }

            @Override
            public BaseMPMessage build() throws JSONException {
                return new MPEventMessage(this);
            }
        }
    }
}
