package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.internal.KitManager;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base Kit implementation - all Kits must subclass this.
 */
public abstract class KitIntegration {

    private KitManager kitManager;
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
        return !getConfiguration().passesBracketing(kitManager.getUserBucket()) ||
                (getConfiguration().shouldHonorOptOut() && !kitManager.isOptedOut());

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
     */
    protected abstract List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context);

    /**
     * This method will be called when an integration has been disabled. Ideally, any unncessesary memory should
     * be cleaned up when this method is called. After this method is called, mParticle will no longer delegate events
     * to the kit, and on subsquent application startups, the Kit will not be initialized at all.
     */
    protected void onKitDestroy() {
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

    Uri getSurveyUrl(Map<String, String> userAttributes) {
        return null;
    }

    /**
     * @param optedOut
     * @return
     */
    public abstract List<ReportingMessage> setOptOut(boolean optedOut);

    protected final KitManager getKitManager() {
        return this.kitManager;
    }

    public final KitIntegration setKitManager(KitManager kitManager) {
        this.kitManager = kitManager;
        return this;
    }

    public void checkForDeepLink() {
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
     * Kits should implement this interface when their underlying service has the notion
     * of a user with attributes.
     */
    public interface AttributeListener {

        void setUserAttribute(String attributeKey, String attributeValue);

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

        /**
         * If your Kit supports the notion of eCommerce events, the mParticle
         * SDK will send the Kit CommerceEvents via the {@link #logEvent(CommerceEvent)} method
         * in this interface.
         * <p/>
         * If your Kit does not support eCommerce, the mParticle SDK will transform CommerceEvents
         * into one or more MPEvents and pass the result into {@link EventListener#logEvent(MPEvent)}
         *
         * @return true if eCommerce/revenue events are supported.
         */
        boolean isCommerceSupported();
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
         * @param keyset the keyset of the Bundle that was received within a push-received Intent
         * @return true if this push should be handled by the given Kit
         */
        boolean willHandleMessage(Set<String> keyset);

        /**
         * If a Kit returns true from {@link #willHandleMessage(Set)}, this method will be called immediately after.
         * Kits should implement this method to show or otherwise react to a received push message.
         *
         * @param context
         * @param pushIntent
         */
        void onMessageReceived(Context context, Intent pushIntent);

        /**
         * This method will be called when the mParticle SDK successfully registers to receive
         * push messages. This method will be call on app startup and repetitively to aggressively
         * sync tokens as they're updated.
         *
         * @param instanceId the device instance ID registered with the GCM scope
         * @param senderId the senderid with permissions for this instanceId
         * @return true if the push registration was processed.
         */
        boolean onPushRegistration(String instanceId, String senderId);
    }
}
