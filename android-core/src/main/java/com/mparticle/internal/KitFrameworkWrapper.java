package com.mparticle.internal;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KitFrameworkWrapper implements KitManager {
    private final Context mContext;
    private final AppStateManager mAppStateManager;
    private final ConfigManager mConfigManager;
    private final ReportingManager mReportingManager;
    private KitManager mKitManager;
    private volatile boolean frameworkLoadAttempted = false;
    private static volatile boolean kitsLoaded = false;

    private Queue eventQueue;
    private Queue<AttributeChange> attributeQueue;
    private volatile boolean registerForPush = false;
    private volatile boolean shouldCheckForDeepLink = false;
    private static KitsLoadedListener kitsLoadedListener;

    public KitFrameworkWrapper(Context context, ReportingManager reportingManager, ConfigManager configManager, AppStateManager appStateManager) {
        this(context, reportingManager, configManager, appStateManager, false);
    }

    public KitFrameworkWrapper(Context context, ReportingManager reportingManager, ConfigManager configManager, AppStateManager appStateManager, boolean testing) {
        this.mContext = testing ? context : new KitContext(context);
        this.mReportingManager = reportingManager;
        this.mConfigManager = configManager;
        this.mAppStateManager = appStateManager;
        kitsLoaded = false;
    }

    public void loadKitLibrary() {
        if (!frameworkLoadAttempted) {
            Logger.debug("Loading Kit Framework.");
            frameworkLoadAttempted = true;
            try {
                Class clazz = Class.forName("com.mparticle.kits.KitManagerImpl");
                Constructor<KitFrameworkWrapper> constructor = clazz.getConstructor(Context.class, ReportingManager.class, ConfigManager.class, AppStateManager.class);
                mKitManager = constructor.newInstance(mContext, mReportingManager, mConfigManager, mAppStateManager);
                JSONArray configuration = mConfigManager.getLatestKitConfiguration();
                Logger.debug("Kit Framework loaded.");
                if (configuration != null) {
                    Logger.debug("Restoring previous Kit configuration.");
                    updateKits(configuration);
                }
            } catch (Exception e) {
                Logger.debug("No Kit Framework detected.");
                disableQueuing();
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

    public static boolean getKitsLoaded() {
        return kitsLoaded;
    }

    public static void setKitsLoadedListener(KitsLoadedListener listener) {
        if (kitsLoaded) {
            listener.onKitsLoaded();
        } else {
            kitsLoadedListener = listener;
        }
    }

    void setKitsLoaded(boolean kitsLoaded) {
        this.kitsLoaded = kitsLoaded;
        if (kitsLoadedListener != null) {
            kitsLoadedListener.onKitsLoaded();
            kitsLoadedListener = null;
        }
    }

    synchronized void disableQueuing() {
        setKitsLoaded(true);
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
            PushRegistrationHelper.PushRegistration registration = PushRegistrationHelper.getLatestPushRegistration(mContext);
            if (registration != null) {
                mKitManager.onPushRegistration(registration.instanceId, registration.senderId);
            }
        }

        if (shouldCheckForDeepLink) {
            mKitManager.checkForDeepLink();
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
                } else if (event instanceof CommerceEvent) {
                    mKitManager.logCommerceEvent((CommerceEvent) event);
                }
            }
        }

        if (attributeQueue != null && attributeQueue.size() > 0) {
            Logger.debug("Replaying user attributes after receiving first kit configuration.");
            for (AttributeChange attributeChange : attributeQueue) {
                if (attributeChange.removal) {
                    mKitManager.removeUserAttribute(attributeChange.key);
                } else if (attributeChange.value == null) {
                    mKitManager.setUserAttribute(attributeChange.key, null);
                } else if (attributeChange.value instanceof String) {
                    mKitManager.setUserAttribute(attributeChange.key, (String) attributeChange.value);
                } else if (attributeChange.value instanceof List) {
                    mKitManager.setUserAttributeList(attributeChange.key, (List<String>) attributeChange.value);
                }
            }
        }
    }

    synchronized public void replayAndDisableQueue() {
        setKitsLoaded(true);
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
        //it's an edge case to even need this, so 10
        //should be enough.
        if (eventQueue.size() < 10) {
            Logger.debug("Queuing Kit event while waiting for initial configuration.");
            eventQueue.add(event);
        }
        return true;
    }

    boolean queueAttribute(String key, Object value) {
        return queueAttribute(new AttributeChange(key, value));
    }

    boolean queueAttribute(String key) {
        return queueAttribute(new AttributeChange(key));
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
        final boolean removal;

        AttributeChange(String key, Object value) {
            this.key = key;
            this.value = value;
            this.removal = false;
        }

        AttributeChange(String key) {
            this.key = key;
            this.value = null;
            this.removal = true;
        }
    }

    public WeakReference<Activity> getCurrentActivity() {
        return mAppStateManager.getCurrentActivity();
    }

    @Override
    public void logEvent(MPEvent event) {
        if (!queueEvent(event) && mKitManager != null) {
            mKitManager.logEvent(event);
        }
    }

    @Override
    public void logCommerceEvent(CommerceEvent event) {
        if (!queueEvent(event) && mKitManager != null) {
            mKitManager.logCommerceEvent(event);
        }
    }

    @Override
    public void logScreen(MPEvent screenEvent) {
        if (!queueEvent(screenEvent) && mKitManager != null) {
            mKitManager.logScreen(screenEvent);
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
    public void checkForDeepLink() {
        if (mKitManager != null && getKitsLoaded()) {
            mKitManager.checkForDeepLink();
        } else {
            shouldCheckForDeepLink = true;
        }
    }

    boolean getShouldCheckForDeepLink() {
        return shouldCheckForDeepLink;
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
    public void setUserAttribute(String key, String value) {
        if (!queueAttribute(key, value) && mKitManager != null) {
            mKitManager.setUserAttribute(key, value);
        }
    }

    @Override
    public void setUserAttributeList(String key, List<String> value) {
        if (!queueAttribute(key, value) && mKitManager != null) {
            mKitManager.setUserAttributeList(key, value);
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        if (!queueAttribute(key) && mKitManager != null) {
            mKitManager.removeUserAttribute(key);
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
    public void updateKits(JSONArray kitConfiguration) {
        if (mKitManager != null) {
            mKitManager.updateKits(kitConfiguration);
        }
    }

    @Override
    public String getActiveModuleIds() {
        if (mKitManager != null) {
            return mKitManager.getActiveModuleIds();
        }
        return null;
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
}