package com.mparticle;


import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.webkit.WebView;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.messaging.CloudAction;
import com.mparticle.messaging.MPCloudNotificationMessage;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.MessagingConfigCallbacks;
import com.mparticle.messaging.ProviderCloudMessage;
import com.mparticle.segmentation.SegmentListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Map;

public class MockMParticle extends MParticle {

    @Override
    Boolean checkSessionTimeout() {
        return super.checkSessionTimeout();
    }

    @Override
    public void activityStarted(Activity activity) {
        super.activityStarted(activity);
    }

    @Override
    public void activityStopped(Activity activity) {
        super.activityStopped(activity);
    }

    @Override
    public void beginSession() {
        super.beginSession();
    }

    @Override
    public void endSession() {
        super.endSession();
    }

    @Override
    boolean isSessionActive() {
        return super.isSessionActive();
    }

    @Override
    public void upload() {
        super.upload();
    }

    @Override
    public void setInstallReferrer(String referrer) {
        super.setInstallReferrer(referrer);
    }

    @Override
    public String getInstallReferrer() {
        return super.getInstallReferrer();
    }

    @Override
    public void logEvent(String eventName, EventType eventType) {
        super.logEvent(eventName, eventType);
    }

    @Override
    public void logEvent(String eventName, EventType eventType, String category) {
        super.logEvent(eventName, eventType, category);
    }

    @Override
    public void logEvent(String eventName, EventType eventType, long eventLength) {
        super.logEvent(eventName, eventType, eventLength);
    }

    @Override
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo) {
        super.logEvent(eventName, eventType, eventInfo);
    }

    @Override
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo, String category) {
        super.logEvent(eventName, eventType, eventInfo, category);
    }

    @Override
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo, long eventLength) {
        super.logEvent(eventName, eventType, eventInfo, eventLength);
    }

    @Override
    public void logEvent(String eventName, EventType eventType, Map<String, String> eventInfo, long eventLength, String category) {
        super.logEvent(eventName, eventType, eventInfo, eventLength, category);
    }

    @Override
    public void logEvent(MPEvent event) {
        super.logEvent(event);
    }

    @Override
    public void logLtvIncrease(BigDecimal valueIncreased, String eventName, Map<String, String> contextInfo) {
        super.logLtvIncrease(valueIncreased, eventName, contextInfo);
    }

    @Override
    public void logProductEvent(MPProduct.Event event, MPProduct product) {
        super.logProductEvent(event, product);
    }

    @Override
    public void logTransaction(MPProduct product) {
        super.logTransaction(product);
    }

    @Override
    public void logScreen(String screenName) {
        super.logScreen(screenName);
    }

    @Override
    public void logScreen(String screenName, Map<String, String> eventData) {
        super.logScreen(screenName, eventData);
    }

    @Override
    public void leaveBreadcrumb(String breadcrumb) {
        super.leaveBreadcrumb(breadcrumb);
    }

    @Override
    public void logError(String message) {
        super.logError(message);
    }

    @Override
    public void logError(String message, Map<String, String> eventData) {
        super.logError(message, eventData);
    }

    @Override
    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString) {
        super.logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, requestString);
    }

    @Override
    public void setNetworkTrackingEnabled(boolean enabled) {
        super.setNetworkTrackingEnabled(enabled);
    }

    @Override
    public void excludeUrlFromNetworkPerformanceMeasurement(String url) {
        super.excludeUrlFromNetworkPerformanceMeasurement(url);
    }

    @Override
    public void addNetworkPerformanceQueryOnlyFilter(String filter) {
        super.addNetworkPerformanceQueryOnlyFilter(filter);
    }

    @Override
    public void resetNetworkPerformanceExclusionsAndFilters() {
        super.resetNetworkPerformanceExclusionsAndFilters();
    }

    @Override
    public void logException(Exception exception) {
        super.logException(exception);
    }

    @Override
    public void logException(Exception exception, Map<String, String> eventData) {
        super.logException(exception, eventData);
    }

    @Override
    public void logException(Exception exception, Map<String, String> eventData, String message) {
        super.logException(exception, eventData, message);
    }

    @Override
    public void enableLocationTracking(String provider, long minTime, long minDistance) {

    }

    @Override
    public void disableLocationTracking() {

    }

    @Override
    public void setLocation(Location location) {

    }

    @Override
    public void setSessionAttribute(String key, Object value) {

    }

    @Override
    public void incrementSessionAttribute(String key, int value) {

    }

    @Override
    public void logout() {
        super.logout();
    }

    @Override
    public void setUserAttribute(String key, Object value) {

    }

    @Override
    public void incrementUserAttribute(String key, int value) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void setUserTag(String tag) {

    }

    @Override
    public void removeUserTag(String tag) {

    }

    @Override
    public void setUserIdentity(String id, IdentityType identityType) {

    }

    @Override
    public JSONArray getUserIdentities() {
        return new JSONArray();
    }

    @Override
    public void removeUserIdentity(String id) {

    }

    @Override
    public Boolean getOptOut() {
        return false;
    }

    @Override
    public void setOptOut(Boolean optOutStatus) {

    }

    @Override
    public Uri getSurveyUrl(int serviceProviderId) {
        return null;
    }

    @Override
    public void setEnvironment(Environment environment) {

    }

    @Override
    public Environment getEnvironment() {
        return Environment.Production;
    }

    @Override
    public void setUploadInterval(int uploadInterval) {

    }

    @Override
    public void enableUncaughtExceptionLogging() {

    }

    @Override
    public void disableUncaughtExceptionLogging() {

    }


    @Override
    public Boolean isAutoTrackingEnabled() {
        return false;
    }

    @Override
    public int getSessionTimeout() {
        return 60;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {

    }

    @Override
    public void getUserSegments(long timeout, String endpointId, SegmentListener listener) {

    }

    @Override
    public void registerWebView(WebView webView) {

    }

    @Override
    public void setLogLevel(LogLevel level) {

    }

    @Override
    public MPMessagingAPI Messaging() {
        return new MockMessagingAPI();
    }

    @Override
    public MPMediaAPI Media() {
        return super.Media();
    }

    @Override
    void saveGcmMessage(MPCloudNotificationMessage cloudMessage, String appState) {

    }

    @Override
    void saveGcmMessage(ProviderCloudMessage cloudMessage, String appState) {

    }

    @Override
    public void logPushRegistration(String registrationId) {

    }

    @Override
    void logNotification(MPCloudNotificationMessage cloudMessage, CloudAction action, boolean startSession, String appState, int behavior) {

    }

    @Override
    void logNotification(ProviderCloudMessage cloudMessage, boolean startSession, String appState) {

    }

    @Override
    void refreshConfiguration() {

    }

    @Override
    public JSONObject getUserAttributes() {
        return new JSONObject();
    }

    @Override
    void logUnhandledError(Throwable t) {

    }

    private class MockMessagingAPI extends MPMessagingAPI {

        public MockMessagingAPI() {
            super(null, null);
        }


        @Override
        public void setPushNotificationIcon(int resId) {

        }

        @Override
        public void setPushNotificationTitle(int resId) {

        }

        @Override
        public void enablePushNotifications(String senderId) {

        }

        @Override
        public void disablePushNotifications() {

        }

        @Override
        public void setNotificationSoundEnabled(Boolean enabled) {

        }

        @Override
        public void setNotificationVibrationEnabled(Boolean enabled) {

        }
    }
}
