package com.mparticle.internal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.mparticle.AttributionResult;
import com.mparticle.BaseEvent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.WrapperSdkVersion;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.rokt.RoktConfig;
import com.mparticle.rokt.RoktEmbeddedView;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KitFrameworkWrapper implements KitManager {
    private final Context mContext;
    final CoreCallbacks mCoreCallbacks;
    private final ReportingManager mReportingManager;
    KitManager mKitManager;
    private final MParticleOptions mOptions;
    private volatile boolean frameworkLoadAttempted = false;
    private static volatile boolean kitsLoaded = false;

    private Queue eventQueue;
    private Queue<AttributeChange> attributeQueue;
    private volatile boolean registerForPush = false;
    private static final List<KitsLoadedListener> kitsLoadedListeners = new ArrayList<>();

    public KitFrameworkWrapper(Context context, ReportingManager reportingManager, ConfigManager configManager, AppStateManager appStateManager, MParticleOptions options) {
        this(context, reportingManager, configManager, appStateManager, false, options);
    }

    public KitFrameworkWrapper(Context context, ReportingManager reportingManager, ConfigManager configManager, AppStateManager appStateManager, boolean testing, MParticleOptions options) {
        this.mOptions = options;
        this.mContext = testing ? context : new KitContext(context);
        this.mReportingManager = reportingManager;
        this.mCoreCallbacks = new CoreCallbacksImpl(this, configManager, appStateManager);
        kitsLoaded = false;
    }

    @WorkerThread
    public void loadKitLibrary() {
        if (!frameworkLoadAttempted) {
            Logger.debug("Loading Kit Framework.");
            frameworkLoadAttempted = true;
            try {
                Class clazz = Class.forName("com.mparticle.kits.KitManagerImpl");
                Constructor<KitFrameworkWrapper> constructor = clazz.getConstructor(Context.class, ReportingManager.class, CoreCallbacks.class, MParticleOptions.class);
                KitManager kitManager = constructor.newInstance(mContext, mReportingManager, mCoreCallbacks, mOptions);
                JSONArray configuration = mCoreCallbacks.getLatestKitConfiguration();
                Logger.debug("Kit Framework loaded.");
                this.mKitManager = kitManager;
                if (!MPUtility.isEmpty(configuration)) {
                    Logger.debug("Restoring previous Kit configuration.");
                    kitManager
                            .updateKits(configuration)
                            .onKitsLoaded(() -> {
                                        mKitManager = kitManager;
                                        setKitsLoaded(true);
                                        updateDataplan(mCoreCallbacks.getDataplanOptions());
                                    }
                            );
                } else {
                    updateDataplan(mCoreCallbacks.getDataplanOptions());
                }
            } catch (Exception e) {
                Logger.debug("No Kit Framework detected.");
                setKitsLoaded(true);
            }
        }
    }

    boolean getFrameworkLoadAttempted() {
        return frameworkLoadAttempted;
    }

    Queue getEventQueue() {
        return eventQueue;
    }

    Queue<AttributeChange> getAttributeQueue() {
        return attributeQueue;
    }

    void setKitManager(KitManager manager) {
        mKitManager = manager;
    }

    public boolean getKitsLoaded() {
        return kitsLoaded;
    }

    public void addKitsLoadedListener(KitsLoadedListener listener) {
        if (listener != null) {
            if (kitsLoaded) {
                listener.onKitsLoaded();
            } else {
                kitsLoadedListeners.add(listener);
            }
        }
    }

    void setKitsLoaded(boolean kitsLoaded) {
        this.kitsLoaded = kitsLoaded;
        if (kitsLoaded) {
            replayAndDisableQueue();
        } else {
            disableQueuing();
        }
        List<KitsLoadedListener> kitsLoadedListenersCopy = new ArrayList<>(kitsLoadedListeners);
        for (KitsLoadedListener kitsLoadedListener : kitsLoadedListenersCopy) {
            if (kitsLoadedListener != null) {
                kitsLoadedListener.onKitsLoaded();
            }
        }
        kitsLoadedListeners.clear();
    }

    synchronized void disableQueuing() {
        if (eventQueue != null) {
            eventQueue.clear();
            eventQueue = null;
            Logger.debug("Kit initialization complete. Disabling event queueing.");
        }

        if (attributeQueue != null) {
            attributeQueue.clear();
            attributeQueue = null;
        }
    }

    void replayEvents() {
        if (mKitManager == null) {
            return;
        }

        mKitManager.onSessionStart();

        if (registerForPush) {
            String instanceId = mCoreCallbacks.getPushInstanceId();
            String senderId = mCoreCallbacks.getPushSenderId();
            if (!MPUtility.isEmpty(instanceId)) {
                mKitManager.onPushRegistration(instanceId, senderId);
            }
        }

        if (eventQueue != null && eventQueue.size() > 0) {
            Logger.debug("Replaying events after receiving first kit configuration.");
            for (Object event : eventQueue) {
                if (event instanceof MPEvent) {
                    MPEvent mpEvent = (MPEvent) event;
                    if (mpEvent.isScreenEvent()) {
                        mKitManager.logScreen(mpEvent);
                    } else {
                        mKitManager.logEvent(mpEvent);
                    }
                } else if (event instanceof BaseEvent) {
                    mKitManager.logEvent((BaseEvent) event);
                }
            }
        }

        if (attributeQueue != null && attributeQueue.size() > 0) {
            Logger.debug("Replaying user attributes after receiving first kit configuration.");
            for (AttributeChange attributeChange : attributeQueue) {
                switch (attributeChange.type) {
                    case AttributeChange.SET_ATTRIBUTE:
                        if (attributeChange.value == null) {
                            mKitManager.setUserAttribute(attributeChange.key, null, attributeChange.mpid);
                        } else if (attributeChange.value instanceof String) {
                            mKitManager.setUserAttribute(attributeChange.key, (String) attributeChange.value, attributeChange.mpid);
                        } else if (attributeChange.value instanceof List) {
                            mKitManager.setUserAttributeList(attributeChange.key, (List<String>) attributeChange.value, attributeChange.mpid);
                        }
                        break;
                    case AttributeChange.REMOVE_ATTRIBUTE:
                        mKitManager.removeUserAttribute(attributeChange.key, attributeChange.mpid);
                        break;
                    case AttributeChange.INCREMENT_ATTRIBUTE:
                        if (attributeChange.value instanceof String) {
                            mKitManager.incrementUserAttribute(attributeChange.key, attributeChange.incrementedBy, (String) attributeChange.value, attributeChange.mpid);
                        }
                        break;
                    case AttributeChange.TAG:
                        mKitManager.setUserTag(attributeChange.key, attributeChange.mpid);
                        break;
                }
            }
        }
    }

    synchronized public void replayAndDisableQueue() {
        replayEvents();
        disableQueuing();
    }

    synchronized boolean queueEvent(Object event) {
        if (getKitsLoaded()) {
            return false;
        }

        if (eventQueue == null) {
            eventQueue = new ConcurrentLinkedQueue<Object>();
        }
        //It's an edge case to even need this, so 10
        //should be enough.
        if (eventQueue.size() < 10) {
            Logger.debug("Queuing Kit event while waiting for initial configuration.");
            eventQueue.add(event);
        }
        return true;
    }

    boolean queueAttributeRemove(String key, long mpid) {
        return queueAttribute(new AttributeChange(key, mpid));
    }

    boolean queueAttributeSet(String key, Object value, long mpid) {
        return queueAttribute(new AttributeChange(key, value, mpid, AttributeChange.SET_ATTRIBUTE));
    }

    boolean queueAttributeTag(String key, long mpid) {
        return queueAttribute(new AttributeChange(key, mpid, AttributeChange.TAG));
    }

    boolean queueAttributeIncrement(String key, Number incrementedBy, String newValue, long mpid) {
        return queueAttribute(new AttributeChange(key, incrementedBy, newValue, mpid));
    }

    synchronized boolean queueAttribute(AttributeChange change) {
        if (getKitsLoaded()) {
            return false;
        }

        if (attributeQueue == null) {
            attributeQueue = new ConcurrentLinkedQueue<AttributeChange>();
        }
        attributeQueue.add(change);
        return true;
    }

    static class AttributeChange {
        final String key;
        final Object value;
        final long mpid;
        final int type;
        Number incrementedBy;

        static final int REMOVE_ATTRIBUTE = 1;
        static final int SET_ATTRIBUTE = 2;
        static final int INCREMENT_ATTRIBUTE = 3;
        static final int TAG = 4;

        AttributeChange(String key, long mpid) {
            this.key = key;
            this.value = null;
            this.mpid = mpid;
            this.type = REMOVE_ATTRIBUTE;
        }

        AttributeChange(String key, Object value, long mpid, int type) {
            this.key = key;
            this.value = value;
            this.mpid = mpid;
            this.type = type;
        }

        AttributeChange(String key, long mpid, int type) {
            this.key = key;
            this.value = null;
            this.mpid = mpid;
            this.type = type;
        }

        AttributeChange(String key, Number incrementedBy, String newValue, long mpid) {
            this.key = key;
            this.value = newValue;
            this.incrementedBy = incrementedBy;
            this.mpid = mpid;
            this.type = INCREMENT_ATTRIBUTE;
        }
    }

    public WeakReference<Activity> getCurrentActivity() {
        return mCoreCallbacks.getCurrentActivity();
    }

    @Override
    public void logEvent(BaseEvent event) {
        if (!queueEvent(event) && mKitManager != null) {
            mKitManager.logEvent(event);
        }
    }

    @Override
    public void logScreen(MPEvent screenEvent) {
        if (!queueEvent(screenEvent) && mKitManager != null) {
            mKitManager.logScreen(screenEvent);
        }
    }

    @Override
    public void logBatch(String jsonObject) {
        if (mKitManager != null) {
            mKitManager.logBatch(jsonObject);
        }
    }

    @Override
    public void leaveBreadcrumb(String breadcrumb) {
        if (mKitManager != null) {
            mKitManager.leaveBreadcrumb(breadcrumb);
        }
    }

    @Override
    public void logError(String message, Map<String, String> eventData) {
        if (mKitManager != null) {
            mKitManager.logError(message, eventData);
        }
    }

    @Override
    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        if (mKitManager != null) {
            mKitManager.logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, requestString, responseCode);
        }
    }

    @Override
    public void logException(Exception exception, Map<String, String> eventData, String message) {
        if (mKitManager != null) {
            mKitManager.logException(exception, eventData, message);
        }
    }

    @Override
    public void setLocation(Location location) {
        if (mKitManager != null) {
            mKitManager.setLocation(location);
        }
    }

    @Override
    public void logout() {
        if (mKitManager != null) {
            mKitManager.logout();
        }
    }

    @Override
    public void setUserAttribute(String key, String value, long mpid) {
        if (!queueAttributeSet(key, value, mpid) && mKitManager != null) {
            mKitManager.setUserAttribute(key, value, mpid);
        }
    }

    @Override
    public void setUserAttributeList(String key, List<String> value, long mpid) {
        if (!queueAttributeSet(key, value, mpid) && mKitManager != null) {
            mKitManager.setUserAttributeList(key, value, mpid);
        }
    }

    @Override
    public void removeUserAttribute(String key, long mpid) {
        if (!queueAttributeRemove(key, mpid) && mKitManager != null) {
            mKitManager.removeUserAttribute(key, mpid);
        }
    }

    @Override
    public void setUserTag(String tag, long mpid) {
        if (!queueAttributeTag(tag, mpid) && mKitManager != null) {
            mKitManager.setUserTag(tag, mpid);
        }
    }

    @Override
    public void incrementUserAttribute(String key, Number incrementValue, String newValue, long mpid) {
        if (!queueAttributeIncrement(key, incrementValue, newValue, mpid) && mKitManager != null) {
            mKitManager.incrementUserAttribute(key, incrementValue, newValue, mpid);
        }
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (mKitManager != null) {
            mKitManager.setUserIdentity(id, identityType);
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType id) {
        if (mKitManager != null) {
            mKitManager.removeUserIdentity(id);
        }
    }

    @Override
    public void setOptOut(boolean optOutStatus) {
        if (mKitManager != null) {
            mKitManager.setOptOut(optOutStatus);
        }
    }

    @Override
    public Uri getSurveyUrl(int kitId, Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists) {
        if (mKitManager != null) {
            return mKitManager.getSurveyUrl(kitId, userAttributes, userAttributeLists);
        }
        return null;
    }

    @Override
    public boolean onMessageReceived(Context context, Intent intent) {
        if (mKitManager != null) {
            return mKitManager.onMessageReceived(context, intent);
        }
        return false;
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        if (getKitsLoaded() && mKitManager != null) {
            mKitManager.onPushRegistration(instanceId, senderId);
        } else {
            registerForPush = true;
        }
        return false;
    }

    @Override
    public boolean isKitActive(int kitId) {
        if (mKitManager != null) {
            return mKitManager.isKitActive(kitId);
        }
        return false;
    }

    @Override
    public Object getKitInstance(int kitId) {
        if (mKitManager != null) {
            return mKitManager.getKitInstance(kitId);
        }
        return null;
    }

    @Override
    public Set<Integer> getSupportedKits() {
        if (mKitManager != null) {
            return mKitManager.getSupportedKits();
        }
        return null;
    }

    @Override
    public KitsLoadedCallback updateKits(JSONArray kitConfiguration) {
        KitsLoadedCallback kitsLoadedCallback = new KitsLoadedCallback();
        if (mKitManager != null) {
            // we may have initialized the KitManagerImpl but didn't have a cached config to initialize
            // any kits with. In this case, we will wait until this next config update to replay + disable queueing
            if (!kitsLoaded) {
                mKitManager
                        .updateKits(kitConfiguration)
                        .onKitsLoaded(() -> {
                                    setKitsLoaded(true);
                                    kitsLoadedCallback.setKitsLoaded();
                                }
                        );
            } else {
                return mKitManager.updateKits(kitConfiguration);
            }
        }
        return kitsLoadedCallback;
    }

    @Override
    public void updateDataplan(@Nullable MParticleOptions.DataplanOptions dataplanOptions) {
        if (mKitManager != null) {
            mKitManager.updateDataplan(dataplanOptions);
        }
    }

    @Override
    public Map<Integer, KitStatus> getKitStatus() {
        if (mKitManager != null) {
            return mKitManager.getKitStatus();
        } else {
            return new HashMap<>();
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (mKitManager != null) {
            mKitManager.onActivityCreated(activity, savedInstanceState);
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mKitManager != null) {
            mKitManager.onActivityStarted(activity);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (mKitManager != null) {
            mKitManager.onActivityResumed(activity);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (mKitManager != null) {
            mKitManager.onActivityPaused(activity);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (mKitManager != null) {
            mKitManager.onActivityStopped(activity);
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (mKitManager != null) {
            mKitManager.onActivitySaveInstanceState(activity, outState);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mKitManager != null) {
            mKitManager.onActivityDestroyed(activity);
        }
    }

    @Override
    public void onSessionEnd() {
        if (mKitManager != null) {
            mKitManager.onSessionEnd();
        }
    }

    @Override
    public void onSessionStart() {
        if (mKitManager != null) {
            mKitManager.onSessionStart();
        }
    }

    @Override
    public void installReferrerUpdated() {
        if (mKitManager != null) {
            mKitManager.installReferrerUpdated();
        }
    }

    @Override
    public void onApplicationForeground() {
        if (mKitManager != null) {
            mKitManager.onApplicationForeground();
        }
    }

    @Override
    public void onApplicationBackground() {
        if (mKitManager != null) {
            mKitManager.onApplicationBackground();
        }
    }

    @Override
    public Map<Integer, AttributionResult> getAttributionResults() {
        if (mKitManager != null) {
            return mKitManager.getAttributionResults();
        }
        return new TreeMap<Integer, AttributionResult>();
    }

    @Override
    public void onIdentifyCompleted(MParticleUser user, IdentityApiRequest request) {
        if (mKitManager != null) {
            mKitManager.onIdentifyCompleted(user, request);
        }
    }

    @Override
    public void onLoginCompleted(MParticleUser user, IdentityApiRequest request) {
        if (mKitManager != null) {
            mKitManager.onLoginCompleted(user, request);
        }
    }

    @Override
    public void onLogoutCompleted(MParticleUser user, IdentityApiRequest request) {
        if (mKitManager != null) {
            mKitManager.onLogoutCompleted(user, request);
        }
    }

    @Override
    public void onModifyCompleted(MParticleUser user, IdentityApiRequest request) {
        if (mKitManager != null) {
            mKitManager.onModifyCompleted(user, request);
        }
    }

    @Override
    public void onConsentStateUpdated(ConsentState oldState, ConsentState newState, long mpid) {
        if (mKitManager != null) {
            mKitManager.onConsentStateUpdated(oldState, newState, mpid);
        }
    }

    @Override
    public void reset() {
        if (mKitManager != null) {
            mKitManager.reset();
        }
    }

    @Override
    public void execute(@NonNull String viewName,
                        @NonNull Map<String, String> attributes,
                        @Nullable MParticle.MpRoktEventCallback mpRoktEventCallback,
                        @Nullable Map<String, WeakReference<RoktEmbeddedView>> placeHolders,
                        @Nullable Map<String, WeakReference<Typeface>> fontTypefaces,
                        @Nullable RoktConfig config) {
        if (mKitManager != null) {
            mKitManager.execute(viewName,
                    attributes,
                    mpRoktEventCallback,
                    placeHolders,
                    fontTypefaces,
                    config);
        }
    }

    @Override
    public void setWrapperSdkVersion(@NonNull WrapperSdkVersion wrapperSdkVersion) {
        if (mKitManager != null) {
            mKitManager.setWrapperSdkVersion(wrapperSdkVersion);
        }
    }

    @Override
    public void purchaseFinalized(@NonNull String placementId, @NonNull String catalogItemId, boolean status) {
        if (mKitManager != null) {
            mKitManager.purchaseFinalized(placementId, catalogItemId, status);
        }
    }

    static class CoreCallbacksImpl implements CoreCallbacks {
        KitFrameworkWrapper mKitFrameworkWrapper;
        ConfigManager mConfigManager;
        AppStateManager mAppStateManager;

        public CoreCallbacksImpl(KitFrameworkWrapper kitFrameworkWrapper, ConfigManager configManager, AppStateManager appStateManager) {
            mKitFrameworkWrapper = kitFrameworkWrapper;
            mConfigManager = configManager;
            mAppStateManager = appStateManager;
        }


        @Override
        public boolean isBackgrounded() {
            return mAppStateManager.isBackgrounded();
        }

        @Override
        public int getUserBucket() {
            return mConfigManager.getUserBucket();
        }

        @Override
        public boolean isEnabled() {
            return mConfigManager.isEnabled();
        }

        @Override
        public void setIntegrationAttributes(int kitId, Map<String, String> integrationAttributes) {
            mConfigManager.setIntegrationAttributes(kitId, integrationAttributes);
        }

        @Override
        public Map<String, String> getIntegrationAttributes(int kitId) {
            return mConfigManager.getIntegrationAttributes(kitId);
        }

        @Override
        public WeakReference<Activity> getCurrentActivity() {
            return mAppStateManager.getCurrentActivity();
        }

        @Override
        public JSONArray getLatestKitConfiguration() {
            return mConfigManager.getLatestKitConfiguration();
        }

        @Override
        public MParticleOptions.DataplanOptions getDataplanOptions() {
            return mConfigManager.getDataplanOptions();
        }

        @Override
        public boolean isPushEnabled() {
            return mConfigManager.isPushEnabled();
        }

        @Override
        public String getPushSenderId() {
            return mConfigManager.getPushSenderId();
        }

        @Override
        public String getPushInstanceId() {
            return mConfigManager.getPushInstanceId();
        }

        @Override
        public Uri getLaunchUri() {
            return mAppStateManager.getLaunchUri();
        }

        @Override
        public String getLaunchAction() {
            return mAppStateManager.getLaunchAction();
        }

        @Override
        public KitListener getKitListener() {
            return kitListener;
        }

        private KitListener kitListener = new KitListener() {
            @Override
            public void kitFound(int kitId) {
                InternalListenerManager.getListener().onKitDetected(kitId);
            }

            @Override
            public void kitConfigReceived(int kitId, String configuration) {
                InternalListenerManager.getListener().onKitConfigReceived(kitId, configuration);
            }

            @Override
            public void kitExcluded(int kitId, String reason) {
                InternalListenerManager.getListener().onKitExcluded(kitId, reason);
            }

            @Override
            public void kitStarted(int kitId) {
                InternalListenerManager.getListener().onKitStarted(kitId);
            }

            @Override
            public void onKitApiCalled(int kitId, Boolean used, Object... objects) {
                InternalListenerManager.getListener().onKitApiCalled(kitId, used, objects);
            }

            @Override
            public void onKitApiCalled(String methodName, int kitId, Boolean used, Object... objects) {
                InternalListenerManager.getListener().onKitApiCalled(methodName, kitId, used, objects);
            }
        };
    }
}