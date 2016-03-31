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
import java.util.Map;
import java.util.Set;

public class MockKitManager implements KitManager {
    public MockKitManager(Context context) {
        super();
    }


    @Override
    public WeakReference<Activity> getCurrentActivity() {
        return null;
    }

    @Override
    public void logEvent(MPEvent event) {

    }

    @Override
    public void logCommerceEvent(CommerceEvent event) {

    }

    @Override
    public void logScreen(MPEvent screenEvent) {

    }

    @Override
    public void leaveBreadcrumb(String breadcrumb) {

    }

    @Override
    public void logError(String message, Map<String, String> eventData) {

    }

    @Override
    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {

    }

    @Override
    public void checkForDeepLink() {

    }

    @Override
    public void logException(Exception exception, Map<String, String> eventData, String message) {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void logout() {

    }

    @Override
    public void setUserAttribute(String key, String value) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {

    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType id) {

    }

    @Override
    public void setOptOut(boolean optOutStatus) {

    }

    @Override
    public Uri getSurveyUrl(int serviceProviderId, Map<String, String> mUserAttributes) {
        return null;
    }

    @Override
    public boolean onMessageReceived(Context context, Intent intent) {
        return false;
    }

    @Override
    public boolean onPushRegistration(String instanceId, String senderId) {
        return false;
    }

    @Override
    public boolean isKitActive(int kitId) {
        return false;
    }

    @Override
    public Object getKitInstance(int kitId) {
        return null;
    }

    @Override
    public Set<Integer> getSupportedKits() {
        return null;
    }

    @Override
    public void updateKits(JSONArray jsonArray) {

    }

    @Override
    public String getActiveModuleIds() {
        return "this is a fake module id string";
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

}
