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

import com.mparticle.AttributionResult;
import com.mparticle.BaseEvent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.WrapperSdkVersion;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;
import com.mparticle.rokt.RoktConfig;
import com.mparticle.rokt.RoktEmbeddedView;

import org.json.JSONArray;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface KitManager {

    WeakReference<Activity> getCurrentActivity();

    void logEvent(BaseEvent event);

    void logScreen(MPEvent screenEvent);

    void logBatch(String jsonObject);

    void leaveBreadcrumb(String breadcrumb);

    void logError(String message, Map<String, String> eventData);

    void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode);

    void logException(Exception exception, Map<String, String> eventData, String message);

    void setLocation(Location location);

    void logout();

    void setUserAttribute(String key, String value, long mpid);

    void setUserAttributeList(String key, List<String> value, long mpid);

    void removeUserAttribute(String key, long mpid);

    void setUserTag(String tag, long mpid);

    void incrementUserAttribute(String key, Number incrementValue, String newValue, long mpid);

    void onConsentStateUpdated(ConsentState oldState, ConsentState newState, long mpid);

    void setUserIdentity(String id, MParticle.IdentityType identityType);

    void removeUserIdentity(MParticle.IdentityType id);

    void setOptOut(boolean optOutStatus);

    Uri getSurveyUrl(int serviceProviderId, Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists);

    boolean onMessageReceived(Context context, Intent intent);

    boolean onPushRegistration(String instanceId, String senderId);

    boolean isKitActive(int kitId);

    Object getKitInstance(int kitId);

    Set<Integer> getSupportedKits();

    KitsLoadedCallback updateKits(JSONArray jsonArray);

    void updateDataplan(@NonNull MParticleOptions.DataplanOptions dataplanOptions);

    @NonNull
    Map<Integer, KitStatus> getKitStatus();

    void onActivityCreated(Activity activity, Bundle savedInstanceState);

    void onActivityStarted(Activity activity);

    void onActivityResumed(Activity activity);

    void onActivityPaused(Activity activity);

    void onActivityStopped(Activity activity);

    void onActivitySaveInstanceState(Activity activity, Bundle outState);

    void onActivityDestroyed(Activity activity);

    void onSessionEnd();

    void onSessionStart();

    void installReferrerUpdated();

    void onApplicationForeground();

    void onApplicationBackground();

    Map<Integer, AttributionResult> getAttributionResults();

    void onIdentifyCompleted(MParticleUser user, IdentityApiRequest request);

    void onLoginCompleted(MParticleUser user, IdentityApiRequest request);

    void onLogoutCompleted(MParticleUser user, IdentityApiRequest request);

    void onModifyCompleted(MParticleUser user, IdentityApiRequest request);

    void reset();

    void execute(@NonNull String viewName,
                 @NonNull Map<String, String> attributes,
                 @Nullable MParticle.MpRoktEventCallback mpRoktEventCallback,
                 @Nullable Map<String, WeakReference<RoktEmbeddedView>> placeHolders,
                 @Nullable Map<String, WeakReference<Typeface>> fontTypefaces,
                 @Nullable RoktConfig config);

    void setWrapperSdkVersion(@NonNull WrapperSdkVersion wrapperSdkVersion);

    void purchaseFinalized(@NonNull String placementId, @NonNull String catalogItemId, boolean status);

    enum KitStatus {
        NOT_CONFIGURED,
        STOPPED,
        ACTIVE
    }
}