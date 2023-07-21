package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.BaseEvent;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base Kit implementation - all Kits must subclass this.
 */
public abstract class KitIntegration {

    private static final String KIT_PREFERENCES_FILE = "mp::kit::";
    private KitManagerImpl kitManager;
    private KitConfiguration mKitConfiguration;

    /**
     * Determine if the application is currently in the background. This is derived
     * by the mParticle SDK based on whether there is currently an Activity visible.
     *
     * @return true if the application is background
     */
    protected final boolean isBackgrounded() {
        return kitManager.isBackgrounded();
    }

    /**
     * Get an application Context object.
     *
     * @return an application Context, this will never be null.
     */
    public final Context getContext() {
        return kitManager.getContext();
    }

    /**
     * Get a WeakReference to an Activity. The mParticle SDK maintains a WeakReference
     * to the currently visible Activity. This will sometimes return null (if the app is in the background), and will
     * sometimes return a WeakReference with no attached Activity - so be sure to code safely.
     *
     * @return a WeakReference to an Activity, or null.
     */
    public final WeakReference<Activity> getCurrentActivity() {
        return kitManager.getCurrentActivity();
    }

    /**
     * Get the configuration of the Kit. The KitConfiguration object contains
     * all of the settings and filters of a Kit. Generally there's no need to use
     * this API, and you can instead just call {@link #getSettings()}.
     *
     * @return
     */
    public KitConfiguration getConfiguration() {
        return mKitConfiguration;
    }

    public final KitIntegration setConfiguration(KitConfiguration configuration) {
        mKitConfiguration = configuration;
        return this;
    }

    /**
     * Kits should override this method in order to provide implementing developers
     * direct access to their APIs. Though generally we don't want developers to need direct
     * access - it can be unavoidable if there are APIs in the underlying Kit SDK for which
     * the mParticle SDK has no analogue or abstraction.
     *
     * @return
     */
    public Object getInstance() {
        return null;
    }

    public boolean isDisabled() {
        return isDisabled(false);
    }

    public boolean isDisabled(boolean isOptOutEvent) {
        return !getConfiguration().passesBracketing(kitManager.getUserBucket()) ||
                (getConfiguration().shouldHonorOptOut() && kitManager.isOptedOut() && !isOptOutEvent);
    }

    @Deprecated
    public final Map<MParticle.IdentityType, String> getUserIdentities() {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            MParticleUser user = instance.Identity().getCurrentUser();
            if (user != null) {
                Map<MParticle.IdentityType, String> identities = user.getUserIdentities();
                identities = getKitManager().getDataplanFilter().transformIdentities(identities);
                Map<MParticle.IdentityType, String> filteredIdentities = new HashMap<MParticle.IdentityType, String>(identities.size());
                for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
                    if (getConfiguration().shouldSetIdentity(entry.getKey())) {
                        filteredIdentities.put(entry.getKey(), entry.getValue());
                    }
                }
                return identities;
            }
        }
        return new HashMap<MParticle.IdentityType, String>();
    }

    /**
     * Retrieve filtered user attributes. Use this method to retrieve user attributes at any time.
     * To ensure that filtering is respected, kits must use this method rather than the public API.
     *
     * If the KitIntegration implements the {@link AttributeListener} interface and returns true
     * for {@link AttributeListener#supportsAttributeLists()}, this method will pass back all attributes
     * as they are (as String values or as List&lt;String&gt; values). Otherwise, this method will comma-separate
     * the List values and return back all String values.
     *
     * @return a Map of attributes according to the logic above.
     */
    @Deprecated
    public final Map<String, Object> getAllUserAttributes() {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            MParticleUser user = instance.Identity().getCurrentUser();
            if (user != null) {
                Map<String, Object> userAttributes = user.getUserAttributes();
                if (kitManager != null) {
                    userAttributes = kitManager.getDataplanFilter().transformUserAttributes(userAttributes);
                }
                Map<String, Object> attributes = (Map<String, Object>) KitConfiguration.filterAttributes(
                        getConfiguration().getUserAttributeFilters(),
                        userAttributes
                );
                if ((this instanceof AttributeListener) && ((AttributeListener) this).supportsAttributeLists()) {
                    return attributes;
                } else {
                    for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                        if (entry.getValue() instanceof List) {
                            attributes.put(entry.getKey(), KitUtils.join((List) entry.getValue()));
                        }
                    }
                    return attributes;
                }
            }
        }
        return new HashMap<String, Object>();
    }

    public final MParticleUser getCurrentUser() {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            MParticleUser user = instance.Identity().getCurrentUser();
            return FilteredMParticleUser.getInstance(user, this);
        }
        return FilteredMParticleUser.getInstance(null, this);
    }

    public final MParticleUser getUser(Long mpid) {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            MParticleUser user =instance.Identity().getUser(mpid);
            return FilteredMParticleUser.getInstance(user, this);
        }
        return FilteredMParticleUser.getInstance(null, this);
    }

    /**
     * Kits must override this an provide a human-readable name of their service. This name will
     * show up in ADB logs.
     *
     * @return the name of your company/sdk
     */
    public abstract String getName();

    /**
     * Kits must override this method and should use it for initialization. This method will only be called
     * once. The first time that a given user installs and opens an app, this may occur anywhere from milliseconds
     * after launch to minutes, depending on the user's data connection.
     *
     * @param settings the settings that have been configured in mParticle UI. Use this to extract your API key, etc
     * @param context  an Application Context object
     *
     * @throws IllegalArgumentException if the kit is unable to start based on the provided settings.
     */
    protected abstract List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) throws IllegalArgumentException;

    /**
     * This method will be called when an integration has been disabled. Ideally, any unnecessary memory should
     * be cleaned up when this method is called. After this method is called, mParticle will no longer delegate events
     * to the kit, and on subsequent application startups, the Kit will not be initialized at all.
     */
    protected void onKitDestroy() {
    }

    /**
     * Called by the KitManager when this kit is to be removed.
     */
    final void onKitCleanup() {
        try {
            Map allValues = getKitPreferences().getAll();
            if (allValues != null && allValues.size() > 0) {
                getKitPreferences().edit().clear().apply();
            }
        }catch (NullPointerException npe) {

        }
    }

    /**
     * Get the SharedPreferences file for this Kit. Kits should use this rather than their own file, as the mParticle SDK
     * will automatically remove this file if the kit is removed from the app.
     *
     * @return
     */
    protected final SharedPreferences getKitPreferences() {
        return this.getContext().getSharedPreferences(KIT_PREFERENCES_FILE + getConfiguration().getKitId(), Context.MODE_PRIVATE);
    }

    /**
     * The mParticle SDK is able to track a user's location based on provider and accuracy settings, and additionally allows
     * developers to manually set a location. In either case, this method will be called on a Kit whenever a new location object
     * is received.
     *
     * @param location
     */
    public void setLocation(Location location) {
    }

    /**
     * Retrieve the settings that are configured for this Kit, such as API key, etc
     *
     * @return a Map of settings
     */
    public final Map<String, String> getSettings() {
        return getConfiguration().getSettings();
    }

    List<ReportingMessage> logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        return null;
    }

    public Uri getSurveyUrl(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists) {
        return null;
    }

    /**
     * @param optedOut
     * @return
     */
    public abstract List<ReportingMessage> setOptOut(boolean optedOut);

    protected final KitManagerImpl getKitManager() {
        return this.kitManager;
    }

    public final KitIntegration setKitManager(KitManagerImpl kitManager) {
        this.kitManager = kitManager;
        return this;
    }

    @Nullable
    public List<ReportingMessage> logBaseEvent(@NonNull BaseEvent baseEvent) {
        return Collections.emptyList();
    }

    @Nullable
    public List<ReportingMessage> logEvent(@NonNull MPEvent baseEvent) {
        return Collections.emptyList();
    }

    /**
     * Set integration attributes. Integration attributes are designed to facilitate communication between a kit
     * and its respective server integration. They will be persisted across application exits and launches.
     *
     * @param integrationAttributes
     */
    protected final void setIntegrationAttributes(Map<String, String> integrationAttributes) {
        getKitManager().setIntegrationAttributes(this, integrationAttributes);
    }

    /**
     * Get integration attributes. Integration attributes are designed to facilitate communication between a kit
     * and its respective server integration. They will be persisted across application exits and launches.
     *
     * @return
     */
    protected final Map<String, String> getIntegrationAttributes() {
        return getKitManager().getIntegrationAttributes(this);
    }

    /**
     * Remove all integration attributes set for this integration.
     */
    protected final void clearIntegrationAttributes() {
        getKitManager().clearIntegrationAttributes(this);
    }

    /**
     * Implement this method to receive com.android.vending.INSTALL_REFERRER Intents that
     * have been captured by the mParticle SDK. Developers/users of the SDK may also call setInstallReferrer
     * at any time after an install has occurred.
     *
     * @param intent an intent with the INSTALL_REFERRER action
     */
    public void setInstallReferrer(Intent intent) {

    }

    /**
     * Implement this method to listen for when settings are updated while the kit is already active.
     *
     * @param settings
     */
    public void onSettingsUpdated(Map<String, String> settings) {

    }

    /**
     * Queues and groups network requests on the MParticle Core network handler
     */
    public void executeNetworkRequest(Runnable runnable) {
        getKitManager().runOnKitThread(runnable);
    }

    /**
     * Indicates that the user wishes to remove personal data and shutdown a Kit and/or underlying 3rd
     * party SDK
     */
    protected void reset() {

    }

    /**
     * Kits should implement this interface when they require Activity callbacks for any reason.
     * <p/>
     * The mParticle SDK will automatically call every method of this interface in API > 14 devices. Otherwise only
     * {@link #onActivityStarted(Activity)} and {@link #onActivityStopped(Activity)} will be called.
     */
    public interface ActivityListener {

        List<ReportingMessage> onActivityCreated(Activity activity, Bundle savedInstanceState);

        List<ReportingMessage> onActivityStarted(Activity activity);

        List<ReportingMessage> onActivityResumed(Activity activity);

        List<ReportingMessage> onActivityPaused(Activity activity);

        List<ReportingMessage> onActivityStopped(Activity activity);

        List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle outState);

        List<ReportingMessage> onActivityDestroyed(Activity activity);
    }

    /**
     * Kit should implement this interface to listen for mParticle session-start and session-end messages.
     *
     */
    public interface SessionListener {
        /**
         * mParticle will start a new session when:
         *  1. The app is brought to the foreground, and there isn't already an active session.
         *  2. Any event (ie custom events, screen events, user attributes, etc) is logged by the hosting app
         *
         * @return
         */
        List<ReportingMessage> onSessionStart();

        /**
         * mParticle will end a session when the app is sent to the background, after the session timeout (defaulted to 60s)
         *
         * @return
         */
        List<ReportingMessage> onSessionEnd();
    }

    /**
     * Kits should implement this interface when their underlying service has the notion
     * of a user with attributes.
     */
    @Deprecated
    public interface AttributeListener {

        void setUserAttribute(String attributeKey, String attributeValue);

        void setUserAttributeList(String attributeKey, List<String> attributeValueList);

        /**
         * Indicate to the mParticle Kit framework if this AttributeListener supports attribute-values as lists.
         *
         * If an AttributeListener returns false, the setUserAttributeList method will never be called. Instead, setUserAttribute
         * will be called with the attribute-value lists combined as a csv.
         *
         * @return true if this AttributeListener supports attribute values as lists.
         */
        boolean supportsAttributeLists();

        void setAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists);

        void removeUserAttribute(String key);

        void setUserIdentity(MParticle.IdentityType identityType, String identity);

        void removeUserIdentity(MParticle.IdentityType identityType);

        /**
         * The mParticle SDK exposes a logout API, allowing developers to track an event
         * when a user logs out of their app/platform. Use this opportunity to perform the appropriate logic
         * as per your platforms logout paradigm, such as clearing user attributes.
         *
         * @return Kits should return a List of ReportingMessages indicating that the logout was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logout();

    }

    /**
     * Kits should implement this interface in order to listen for eCommerce events
     */
    public interface CommerceListener {

        /**
         * The mParticle SDK exposes a basic lifetime-value-increase API. This allows developers to increase the value associated with a user
         * in the case where no particular event is appropriate.
         *
         * @param valueIncreased the amount that the LTV should be increased by.
         * @param valueTotal     the total value that mParticle now associates with the user after this increase
         * @param eventName      an optional name to associate with the LTV increase
         * @param contextInfo    an optional Map of attributes to associate with the LTV increase
         * @return Kits should return a List of ReportingMessages indicating that the LTV increase was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo);

        /**
         * The mParticle SDK exposes a rich eCommerce API, allowing developers to keep track of purchases and many other
         * product-related interactions.
         * <p/>
         * CommerceEvents may contain several Products, Impressions, and/or Promotions. Depending on the nature of your SDK's API, you should iterate over
         * the items within a CommerceEvent and forward several discrete events, returning a ReportingMessage for each API call.
         *
         * @param event the CommerceEvent that was logged
         * @return Kits should return a List of ReportingMessages indicating that the CommerceEvent was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logEvent(CommerceEvent event);
    }

    /**
     * Kits should implement this listener to ensure they receive events as they are
     * sent into the mParticle SDK. The methods in this listener generally have a 1-1 mapping
     * with the mParticle SDKs public API - it is the reasonability of Kit-writers to faithfully
     * map the appropriate mParticle APIs onto the proper APIs in their service.
     */
    public interface EventListener {

        /**
         * The mParticle SDK exposes a breadcrumb API, allowing developers to track transactions as they occur.
         * Implementing Kits may optionally forward breadcrumbs to their SDK as regular events if there is no direct API analogue.
         *
         * @param breadcrumb a human-readable, typically short label of a step in a transaction, funnel, etc
         * @return Kits should return a List of ReportingMessages indicating that the breadcrumb was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> leaveBreadcrumb(String breadcrumb);

        /**
         * The mParticle SDK exposes an error API, allowing developers to keep track of handled errors.
         *
         * @param message         a description of the error
         * @param errorAttributes optional attributes to associate with the error
         * @return Kits should return a List of ReportingMessages indicating that the error was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logError(String message, Map<String, String> errorAttributes);

        /**
         * The mParticle SDK exposes an exception API, allowing developers to keep track of handled exceptions.
         *
         * @param exception           the exception that was caught
         * @param message             a description of the error
         * @param exceptionAttributes optional attributes to associate with the error
         * @return Kits should return a List of ReportingMessages indicating that the exception was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logException(Exception exception, Map<String, String> exceptionAttributes, String message);

        /**
         * The mParticle SDK exposes a general event API, allowing developers to keep track of any activity with their app.
         *
         * @param event the MPEvent that was logged.
         * @return Kits should return a List of ReportingMessages indicating that the MPEvent was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logEvent(MPEvent event);

        /**
         * The mParticle SDK exposes a screen-view API, allowing developers to keep track of screens that are viewed by the user. Some mParticle integrations
         * use screen views for funnel analysis. Implementing Kits may optionally forward screen views to their SDK as regular events if there is no direct API analogue
         *
         * @param screenName       the name of the screen
         * @param screenAttributes optional Map of attributes associated with this event
         * @return Kits should return a List of ReportingMessages indicating that the screen-view was processed one or more times, or null if it was not processed
         */
        List<ReportingMessage> logScreen(String screenName, Map<String, String> screenAttributes);
    }

    /**
     * Kits should implement this interface when they have Google Cloud Messaging/push features.
     */
    public interface PushListener {

        /**
         * Kits must implement this method to inspect a push message to determine if it's intended for their
         * SDK. Typically, a given push service will include a predefined key within the payload of a
         * push message that uniquely identifies the push by company.
         *
         * @param intent the Intent object from the push-received broadcast
         * @return true if this push should be handled by the given Kit
         */
        boolean willHandlePushMessage(Intent intent);

        /**
         * If a Kit returns true from {@link #willHandlePushMessage(Intent)}, this method will be called immediately after.
         * Kits should implement this method to show or otherwise react to a received push message.
         *
         * @param context
         * @param pushIntent
         */
        void onPushMessageReceived(Context context, Intent pushIntent);

        /**
         * This method will be called when the mParticle SDK successfully registers to receive
         * push messages. This method will be call on app startup and repetitively to aggressively
         * sync tokens as they're updated.
         *
         * @param instanceId the device instance ID registered with the FCM scope
         * @param senderId the senderid with permissions for this instanceId
         * @return true if the push registration was processed.
         */
        boolean onPushRegistration(String instanceId, String senderId);
    }

    public interface ApplicationStateListener {

        /**
         * Application has entered the foreground
         */
        void onApplicationForeground();

        /**
         * Application has been sent to the background or terminated
         */
        void onApplicationBackground();

    }

    public interface IdentityListener {

        void onIdentifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest);

        void onLoginCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest);

        void onLogoutCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest);

        void onModifyCompleted(MParticleUser mParticleUser, FilteredIdentityApiRequest identityApiRequest);

        void onUserIdentified(MParticleUser mParticleUser);

    }

    public interface UserAttributeListener {

        void onIncrementUserAttribute (String key, Number incrementedBy, String value, FilteredMParticleUser user);

        void onRemoveUserAttribute(String key, FilteredMParticleUser user);

        void onSetUserAttribute(String key, Object value, FilteredMParticleUser user);

        void onSetUserTag(String key, FilteredMParticleUser user);

        void onSetUserAttributeList(String attributeKey, List<String> attributeValueList, FilteredMParticleUser user);

        void onSetAllUserAttributes(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, FilteredMParticleUser user);

        boolean supportsAttributeLists();

        void onConsentStateUpdated(ConsentState oldState, ConsentState newState, FilteredMParticleUser user);
    }

    public interface BatchListener {
        List<ReportingMessage> logBatch(JSONObject jsonObject);
    }
}
