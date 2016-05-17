package com.mparticle.internal;

import android.app.Activity;
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
    private volatile boolean loadAttempted;

    private boolean shouldCheckForDeepLink = false;
    private Queue queuedEvents;
    private boolean queueEvents = true;
    private boolean registerForPush = false;

    public KitFrameworkWrapper(Context context, ReportingManager reportingManager, ConfigManager configManager, AppStateManager appStateManager) {
        this.mContext = context;
        this.mReportingManager = reportingManager;
        this.mConfigManager = configManager;
        this.mAppStateManager = appStateManager;
    }

    public void loadKitLibrary() {
        if (!loadAttempted) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Loading Kit Framework.");
            loadAttempted = true;
            try {
                Class clazz = Class.forName("com.mparticle.kits.KitManagerImpl");
                Constructor<KitFrameworkWrapper> constructor = clazz.getConstructor(Context.class, ReportingManager.class, ConfigManager.class, AppStateManager.class);
                constructor.setAccessible(true);
                mKitManager = constructor.newInstance(mContext, mReportingManager, mConfigManager, mAppStateManager);
                JSONArray configuration = mConfigManager.getLatestKitConfiguration();
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Kit Framework loaded.");
                if (configuration != null) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Restoring previous Kit configuration.");
                    updateKits(configuration);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "No Kit Framework detected.");
                disableQueuing();
            }
        }
    }

    private void disableQueuing() {
        queueEvents = false;
        if (queuedEvents != null) {
            queuedEvents.clear();
            queuedEvents = null;
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Kit initialization complete. Disabling event queueing.");
        }
    }

    private void replayEvents() {
        if (mKitManager == null) {
            return;
        }

        WeakReference<Activity> activityWeakReference = getCurrentActivity();
        if (activityWeakReference != null) {
            Activity activity = activityWeakReference.get();
            if (activity != null) {
                mKitManager.onActivityCreated(activity, null);
                mKitManager.onActivityStarted(activity);
                mKitManager.onActivityResumed(activity);
            }
        }

        if (registerForPush) {
            PushRegistrationHelper.PushRegistration registration = PushRegistrationHelper.getLatestPushRegistration(mContext);
            if (registration != null) {
                mKitManager.onPushRegistration(registration.instanceId, registration.senderId);
            }
        }

        if (shouldCheckForDeepLink) {
            mKitManager.checkForDeepLink();
        }

        if (queuedEvents == null || queuedEvents.size() == 0) {
            return;
        }

        ConfigManager.log(MParticle.LogLevel.DEBUG, "Replaying events after receiving first kit configuration.");
        for (Object event : queuedEvents) {
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

    public void replayAndDisableQueue() {
        replayEvents();
        disableQueuing();
    }

    private boolean queueEvent(Object event) {
        if (!queueEvents) {
            return false;
        }

        if (queuedEvents == null) {
            queuedEvents = new ConcurrentLinkedQueue<Object>();
        }
        //it's an edge case to even need this, so 10
        //should be enough.
        if (queuedEvents.size() <= 10) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Queuing Kit event while waiting for initial configuration.");
            queuedEvents.add(event);
        }
        return true;
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
            mKitManager.logEvent(screenEvent);
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
        if (mKitManager != null) {
            mKitManager.checkForDeepLink();
        } else {
            shouldCheckForDeepLink = true;
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
    public void setUserAttribute(String key, String value) {
        if (mKitManager != null) {
            mKitManager.setUserAttribute(key, value);
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        if (mKitManager != null) {
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
    public Uri getSurveyUrl(int kitId, Map<String, String> userAttributes) {
        if (mKitManager != null) {
            return mKitManager.getSurveyUrl(kitId, userAttributes);
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
        if (!queueEvents && mKitManager != null) {
            mKitManager.onPushRegistration(instanceId, senderId);
        }else {
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
}
