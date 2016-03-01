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
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;

public class KitManager {
    private Context mContext;
    private AppStateManager mAppStateManager;
    private ConfigManager mConfigManager;
    private MParticle mParticle;
    private ReportingManager mReportingManager;

    public final KitManager setMpInstance(MParticle mParticle) {
        this.mParticle = mParticle;
        return this;
    }

    public MParticle getMpInstance() {
        return this.mParticle;
    }

    public void logEvent(MPEvent event) {
    }

    public void logCommerceEvent(CommerceEvent event) {
    }

    public void logScreen(String eventName, Map<String, String> info) {
    }

    public void leaveBreadcrumb(String breadcrumb) {
    }

    public void logError(String message, Map<String, String> eventData) {
    }

    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
    }

    public void checkForDeepLink() {
    }

    public void logException(Exception exception, Map<String, String> eventData, String message) {
    }

    public void setLocation(Location location) {
    }

    public void logout() {
    }

    public void setUserAttribute(String key, String value) {
    }

    public void removeUserAttribute(String key) {
    }

    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
    }

    public void removeUserIdentity(MParticle.IdentityType id) {
    }

    public void setOptOut(boolean optOutStatus) {

    }

    public Uri getSurveyUrl(int serviceProviderId, Map<String, String> mUserAttributes) {
        return null;
    }

    public boolean onMessageReceived(Context context, Intent intent) {
        return false;
    }

    public boolean onPushRegistration(String token, String senderId) {
        return false;
    }

    public boolean isKitActive(int kitId) {
        return false;
    }

    public Object getKitInstance(int kitId) {
        return null;
    }

    public Set<Integer> getSupportedKits() {
        return null;
    }

    public void updateKits(JSONArray jsonArray) {
    }

    public String getActiveModuleIds() {
        return null;
    }

    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    public void onActivityStarted(Activity activity) {
    }

    public void onActivityResumed(Activity activity) {
    }

    public void onActivityPaused(Activity activity) {
    }

    public void onActivityStopped(Activity activity) {
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    public void onActivityDestroyed(Activity activity) {
    }

    public void setInstallReferrer(Context context, Intent intent) {

    }

    public final void setReportingManager(ReportingManager reportingManager) {
        mReportingManager = reportingManager;
    }

    public final ReportingManager getReportingManager() {
        return mReportingManager;
    }

    public final void setAppStateManager(AppStateManager appStateManager) {
        mAppStateManager = appStateManager;
    }

    public final void setConfigurationManager(ConfigManager configManager) {
        mConfigManager = configManager;
    }

    public final Context getContext() {
        return mContext;
    }

    public final KitManager setContext(Context context) {
        mContext = context;
        return this;
    }

    public boolean isBackgrounded() {
        return false;
    }

    public int getUserBucket() {
        return getConfigurationManager().getUserBucket();
    }

    public ConfigManager getConfigurationManager() {
        return mConfigManager;
    }

    public boolean isOptedOut() {
        return !mConfigManager.isEnabled();
    }

    public WeakReference<Activity> getCurrentActivity() {
        return mAppStateManager.getCurrentActivity();
    }
}
