package com.mparticle.kits;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPActivityCallbacks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class KitManager implements MPActivityCallbacks, DeepLinkListener {
    KitFactory ekFactory;
    private ConfigManager mConfigManager;
    private AppStateManager mAppStateManager;
    private ReportingManager mReportingManager;
    ConcurrentHashMap<Integer, AbstractKit> providers = new ConcurrentHashMap<Integer, AbstractKit>(0);

    Context context;

    public KitManager(Context context) {
        this.context = context;
        ekFactory = new KitFactory(this);
    }

    //called from a background thread by the ConfigManager when we get new configuration
    public void updateKits(JSONArray kitConfigs) {
        if (kitConfigs == null) {
            providers.clear();
        } else {
            HashSet<Integer> activeIds = new HashSet<Integer>();

            for (int i = 0; i < kitConfigs.length(); i++) {
                try {
                    JSONObject current = kitConfigs.getJSONObject(i);
                    int currentId = current.getInt(AbstractKit.KEY_ID);
                    activeIds.add(currentId);
                    if (ekFactory.isSupported(currentId)) {

                        if (!providers.containsKey(currentId)) {
                            AbstractKit provider = ekFactory.createInstance(currentId).parseConfig(current);
                            if (provider.disabled()) {
                                continue;
                            }
                            provider.update();
                            providers.put(currentId, provider);
                            if (providers.get(currentId).isRunning()) {
                                Intent intent = new Intent(MParticle.ServiceProviders.BROADCAST_ACTIVE + currentId);
                                context.sendBroadcast(intent);
                            }
                        }else {
                            providers.get(currentId).parseConfig(current).update();
                        }

                        providers.get(currentId).setUserAttributes(MParticle.getInstance().getUserAttributes());
                        syncUserIdentities(providers.get(currentId));
                    }
                } catch (JSONException jse) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Exception while parsing kit configuration: " + jse.getMessage());
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.ERROR, "Exception while starting kit: " + e.getMessage());
                }
            }

            Iterator<Integer> ids = providers.keySet().iterator();
            while (ids.hasNext()) {
                Integer id = ids.next();
                if (!activeIds.contains(id)) {
                    ids.remove();
                    Intent intent = new Intent(MParticle.ServiceProviders.BROADCAST_DISABLED + id);
                    context.sendBroadcast(intent);
                }
            }
        }
    }

    private void syncUserIdentities(AbstractKit provider) {
        JSONArray identities = MParticle.getInstance().getUserIdentities();
        if (identities != null) {
            for (int i = 0; i < identities.length(); i++) {
                try {
                    JSONObject identity = identities.getJSONObject(i);
                    MParticle.IdentityType type = MParticle.IdentityType.parseInt(identity.getInt(Constants.MessageKey.IDENTITY_NAME));
                    String id = identity.getString(Constants.MessageKey.IDENTITY_VALUE);
                    provider.setUserIdentity(id, type);
                } catch (JSONException jse) {
                    //swallow
                }
            }
        }
    }

    public void logEvent(MPEvent event) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (provider instanceof ClientSideForwarder && !provider.disabled() && provider.shouldLogEvent(event)) {
                    MPEvent eventCopy = new MPEvent(event);
                    eventCopy.setInfo(
                            provider.filterEventAttributes(eventCopy, provider.mAttributeFilters)
                    );
                    List<Projection.ProjectionResult> projectedEvents = provider.projectEvents(eventCopy);
                    List<ReportingMessage> reportingMessages = new LinkedList<ReportingMessage>();
                    if (projectedEvents == null) {
                        List<ReportingMessage> messages = null;
                        if (eventCopy.getInfo() != null
                                && eventCopy.getInfo().containsKey(Constants.MethodName.METHOD_NAME)
                                && eventCopy.getInfo().get(Constants.MethodName.METHOD_NAME).equals(Constants.MethodName.LOG_LTV)){
                            messages = ((ClientSideForwarder) provider).logLtvIncrease(
                                            new BigDecimal(eventCopy.getInfo().get(Constants.MessageKey.RESERVED_KEY_LTV)),
                                            eventCopy.getEventName(),
                                            eventCopy.getInfo());
                        }else {
                            messages = ((ClientSideForwarder) provider).logEvent(eventCopy);
                        }
                        if (messages != null && messages.size() > 0) {
                            reportingMessages.addAll(messages);
                        }
                    } else {
                        ReportingMessage masterMessage = ReportingMessage.fromEvent(provider, eventCopy);
                        boolean forwarded = false;
                        for (int i = 0; i < projectedEvents.size(); i++) {
                            List<ReportingMessage> messages = ((ClientSideForwarder) provider).logEvent(projectedEvents.get(i).getMPEvent());
                            if (messages != null && messages.size() > 0) {
                                forwarded = true;
                                for (ReportingMessage message : messages) {
                                    ReportingMessage.ProjectionReport report = new ReportingMessage.ProjectionReport(
                                            projectedEvents.get(i).getProjectionId(),
                                            Constants.MessageType.EVENT,
                                            message.getEventName(),
                                            message.getEventTypeString()
                                    );
                                    masterMessage.addProjectionReport(report);
                                }

                            }
                        }
                        if (forwarded) {
                            reportingMessages.add(masterMessage);
                        }
                    }
                    mReportingManager.logAll(reportingMessages);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logEvent for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void logCommerceEvent(CommerceEvent event) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (provider instanceof ClientSideForwarder && !provider.disabled()) {
                    CommerceEvent filteredEvent = provider.filterCommerceEvent(event);
                    if (filteredEvent != null) {
                        if (provider instanceof ECommerceForwarder) {
                            List<Projection.ProjectionResult> projectedEvents = provider.projectEvents(filteredEvent);
                            if (projectedEvents != null && projectedEvents.size() > 0) {
                                ReportingMessage masterMessage = ReportingMessage.fromEvent(provider, filteredEvent);
                                boolean forwarded = false;
                                for (int i = 0; i < projectedEvents.size(); i++) {
                                    Projection.ProjectionResult result = projectedEvents.get(i);
                                    List<ReportingMessage> report = null;
                                    String messageType = null;
                                    if (result.getMPEvent() != null) {
                                        report = ((ECommerceForwarder) provider).logEvent(projectedEvents.get(i).getMPEvent());
                                        messageType = Constants.MessageType.EVENT;
                                    } else {
                                        report =((ECommerceForwarder) provider).logEvent(projectedEvents.get(i).getCommerceEvent());
                                        messageType = Constants.MessageType.COMMERCE_EVENT;
                                    }
                                    if (report != null && report.size() > 0) {
                                        forwarded = true;
                                        for (ReportingMessage message : report) {
                                            masterMessage.addProjectionReport(
                                                    new ReportingMessage.ProjectionReport(projectedEvents.get(i).getProjectionId(),
                                                            messageType,
                                                            message.getEventName(),
                                                            message.getEventTypeString())
                                            );
                                        }
                                    }
                                }
                                if (forwarded) {
                                    mReportingManager.log(masterMessage);
                                }
                            } else {
                                List<ReportingMessage> reporting = ((ECommerceForwarder) provider).logEvent(filteredEvent);
                                if (reporting != null && reporting.size() > 0) {
                                    mReportingManager.log(
                                            ReportingMessage.fromEvent(provider, filteredEvent)
                                    );
                                }
                            }
                        } else {
                            List<MPEvent> events = CommerceEventUtil.expand(filteredEvent);
                            boolean forwarded = false;
                            if (events != null) {
                                for (int i = 0; i < events.size(); i++) {
                                    List<ReportingMessage> reporting = ((ClientSideForwarder) provider).logEvent(events.get(i));
                                    forwarded = forwarded || (reporting != null && reporting.size() > 0);
                                }
                            }
                            if (forwarded) {
                                mReportingManager.log(
                                        ReportingMessage.fromEvent(provider, filteredEvent)
                                );
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logCommerceEvent for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void logScreen(String screenName, Map<String, String> eventAttributes) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (provider instanceof ClientSideForwarder && !provider.disabled() && provider.shouldLogScreen(screenName)) {
                    MPEvent syntheticScreenEvent = new MPEvent.Builder(screenName, MParticle.EventType.Navigation).info(eventAttributes).build();
                    syntheticScreenEvent.setInfo(provider.filterEventAttributes(null, screenName, provider.mScreenAttributeFilters, eventAttributes));
                    List<Projection.ProjectionResult> projectedEvents = provider.projectEvents(syntheticScreenEvent, true);
                    if (projectedEvents == null) {
                        List<ReportingMessage> report = ((ClientSideForwarder) provider).logScreen(screenName, syntheticScreenEvent.getInfo());
                        if (report != null && report.size() > 0) {
                            for (ReportingMessage message : report) {
                                message.setMessageType(Constants.MessageType.SCREEN_VIEW);
                                message.setScreenName(screenName);
                            }
                        }
                        mReportingManager.logAll(report);
                    } else {
                        ReportingMessage masterMessage = new ReportingMessage(provider,
                                Constants.MessageType.SCREEN_VIEW,
                                System.currentTimeMillis(),
                                syntheticScreenEvent.getInfo());
                        boolean forwarded = false;
                        for (int i = 0; i < projectedEvents.size(); i++) {
                            List<ReportingMessage> report = ((ClientSideForwarder) provider).logEvent(projectedEvents.get(i).getMPEvent());
                            if (report != null && report.size() > 0) {
                                forwarded = true;
                                for (ReportingMessage message : report) {
                                    ReportingMessage.ProjectionReport projectionReport = new ReportingMessage.ProjectionReport(
                                            projectedEvents.get(i).getProjectionId(),
                                            Constants.MessageType.EVENT,
                                            message.getEventName(),
                                            message.getEventTypeString()
                                    );
                                    masterMessage.addProjectionReport(projectionReport);
                                }
                            }
                        }
                        if (forwarded) {
                            mReportingManager.log(masterMessage);
                        }
                    }
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logScreen for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void setLocation(Location location) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.setLocation(location);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setLocation for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void setUserAttributes(JSONObject userAttributes) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.setUserAttributes(provider.filterAttributes(provider.mUserAttributeFilters, userAttributes));
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setUserAttributes for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void removeUserAttribute(String key) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.removeUserAttribute(key);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call removeUserAttribute for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled() && provider.shouldSetIdentity(identityType)) {
                    provider.setUserIdentity(id, identityType);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setUserIdentity for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void logout() {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> report = provider.logout();
                    mReportingManager.logAll(report);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logout for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void removeUserIdentity(String id) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.removeUserIdentity(id);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call removeUserIdentity for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void handleIntent(Intent intent) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> report = provider.handleIntent(intent);
                    mReportingManager.logAll(report);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call handleIntent for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void startSession() {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.startSession();
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call startSession for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void endSession() {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.endSession();
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call endSession for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, int activityCount) {
        for (AbstractKit provider : providers.values()) {
            if (provider instanceof ActivityLifecycleForwarder) {
                try {
                    if (!provider.disabled()) {
                        List<ReportingMessage> reportingMessages = ((ActivityLifecycleForwarder) provider).onActivityCreated(activity, activityCount);
                        mReportingManager.logAll(reportingMessages);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onCreate for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity, int currentCount) {
        for (AbstractKit provider : providers.values()) {
            if (provider instanceof ActivityLifecycleForwarder) {
                try {
                    if (!provider.disabled()) {
                        List<ReportingMessage> reportingMessages = ((ActivityLifecycleForwarder) provider).onActivityResumed(activity, currentCount);
                        mReportingManager.logAll(reportingMessages);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity, int activityCount) {
        for (AbstractKit provider : providers.values()) {
            if (provider instanceof ActivityLifecycleForwarder) {
                try {
                    if (!provider.disabled()) {
                        List<ReportingMessage> reportingMessages = ((ActivityLifecycleForwarder) provider).onActivityPaused(activity, activityCount);
                        mReportingManager.logAll(reportingMessages);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity, int currentCount) {
        for (AbstractKit provider : providers.values()) {
            if (provider instanceof ActivityLifecycleForwarder) {
                try {
                    if (!provider.disabled()) {
                        List<ReportingMessage> reportingMessages = ((ActivityLifecycleForwarder) provider).onActivityStopped(activity, currentCount);
                        mReportingManager.logAll(reportingMessages);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity, int currentCount) {
        for (AbstractKit provider : providers.values()) {
            if (provider instanceof ActivityLifecycleForwarder) {
                try {
                    if (!provider.disabled()) {
                        List<ReportingMessage> reportingMessages = ((ActivityLifecycleForwarder) provider).onActivityStarted(activity, currentCount);
                        mReportingManager.logAll(reportingMessages);
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call onResume for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public boolean isKitUrl(String url) {
        for (AbstractKit provider : providers.values()) {
            if (provider.isOriginator(url)) {
                return true;
            }
        }
        return false;
    }

    public String getActiveModuleIds() {
        if (providers.isEmpty()) {
            return "";
        } else {
            Set keys = providers.keySet();
            StringBuilder buffer = new StringBuilder(keys.size() * 3);

            Iterator<Integer> it = keys.iterator();
            while (it.hasNext()) {
                Integer next = it.next();
                if (providers.get(next) != null && providers.get(next).isRunning() && !providers.get(next).disabled()) {
                    buffer.append(next);
                    if (it.hasNext()) {
                        buffer.append(",");
                    }
                }
            }
            return buffer.toString();
        }
    }

    public Uri getSurveyUrl(int serviceId, JSONObject userAttributes) {
        AbstractKit provider = providers.get(serviceId);
        if (provider instanceof ISurveyProvider) {
            return ((ISurveyProvider) provider).getSurveyUrl(provider.filterAttributes(provider.mUserAttributeFilters, userAttributes));
        } else {
            return null;
        }
    }

    public Context getContext() {
        return context;
    }

    public ConfigManager getConfigurationManager() {
        return mConfigManager;
    }

    public AppStateManager getAppStateManager() {
        return mAppStateManager;
    }

    public void setConfigManager(ConfigManager configManager) {
        mConfigManager = configManager;
    }

    public void setAppStateManager(AppStateManager appStateManager) {
        this.mAppStateManager = appStateManager;
    }

    public void setReportingManager(ReportingManager reportingManager) {
        mReportingManager = reportingManager;
    }

    public boolean handleGcmMessage(Intent intent) {
        for (AbstractKit provider : providers.values()) {
            if (provider instanceof PushProvider) {
                try {
                    if (!provider.disabled()) {
                        List<ReportingMessage> messages = ((PushProvider) provider).handleGcmMessage(intent);
                        mReportingManager.logAll(messages);
                        if (messages != null){
                            return true;
                        }
                    }
                } catch (Exception e) {
                    ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call handleGcmMessage for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    public boolean isProviderActive(int serviceProviderId) {
        AbstractKit provider = providers.get(serviceProviderId);
        return provider != null && !provider.disabled() && provider.isRunning();
    }

    public Object getKitInstance(int kitId, Activity activity) {
        AbstractKit kit = providers.get(kitId);
        return kit == null ? null : kit.getInstance(activity);
    }

    public void setOptOut(boolean optOutStatus) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> messages = provider.setOptOut(optOutStatus);
                    mReportingManager.logAll(messages);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call setOptOut for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    private static boolean setInstallReferrer(Context context, Intent intent, String className) {
        try {
            Class clazz = Class.forName(className);
            Constructor<BroadcastReceiver> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            BroadcastReceiver receiver = constructor.newInstance();
            receiver.onReceive(context, intent);
            return true;
        }catch (Exception e) {
        }
        return false;
    }

    public static void setInstallReferrer(Context context, Intent intent) {
        if (setInstallReferrer(context, intent, "com.adjust.sdk.AdjustReferrerReceiver")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Sent referral info to Adjust SDK");
        }
        if (setInstallReferrer(context, intent, "com.kochava.android.tracker.ReferralCapture")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Sent referral info to Kochava SDK");
        }
        if (setInstallReferrer(context, intent, "io.branch.referral.InstallListener")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Sent referral info to Branch SDK");
        }
        if (setInstallReferrer(context, intent, "com.localytics.android.ReferralReceiver")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Sent referral info to Localytics SDK");
        }
        if (setInstallReferrer(context, intent, "com.flurry.android.InstallReceiver")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Sent referral info to Flurry SDK");
        }
    }

    public List<Integer> getSupportedKits() {
        return ekFactory.getSupportedKits();
    }

    public void leaveBreadcrumb(String breadcrumb) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> report = provider.leaveBreadcrumb(breadcrumb);
                    mReportingManager.logAll(report);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call leaveBreadcrumb for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void logError(String message, Map<String, String> eventData) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> report = provider.logError(message, eventData);
                    mReportingManager.logAll(report);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logError for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void logException(Exception exception, Map<String, String> eventData, String message) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> report = provider.logException(exception, eventData, message);
                    mReportingManager.logAll(report);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logException for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    List<ReportingMessage> report = provider.logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, requestString, responseCode);
                    mReportingManager.logAll(report);
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call logNetworkPerformance for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void checkForDeepLink() {
        for (AbstractKit provider : providers.values()) {
            try {
                if (!provider.disabled()) {
                    provider.checkForDeepLink();
                }
            } catch (Exception e) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Failed to call checkForDeeplink for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onResult(DeepLinkResult result) {
        DeepLinkListener listener = MParticle.getInstance().getDeepLinkListener();
        if (listener != null && result != null) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Deep link result returned: \n" + result.toString());
            listener.onResult(result);
        }
    }

    @Override
    public void onError(DeepLinkError error) {
        DeepLinkListener listener = MParticle.getInstance().getDeepLinkListener();
        if (listener != null && error != null) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Deep link error returned: \n" + error.toString());
            listener.onError(error);
        }
    }
}
