package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.AttributionError;
import com.mparticle.AttributionListener;
import com.mparticle.AttributionResult;
import com.mparticle.BaseEvent;
import com.mparticle.Configuration;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.KitsLoadedCallback;
import com.mparticle.internal.CoreCallbacks;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.ReportingManager;
import com.mparticle.kits.mappings.CustomMapping;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class KitManagerImpl implements KitManager, AttributionListener, UserAttributeListener, IdentityStateListener {

    private static HandlerThread kitHandlerThread;
    static {
        kitHandlerThread = new HandlerThread("mParticle_kit_thread");
        kitHandlerThread.start();
    }

    private final ReportingManager mReportingManager;
    protected final CoreCallbacks mCoreCallbacks;
    private Handler mKitHandler;
    KitIntegrationFactory mKitIntegrationFactory;
    private DataplanFilter mDataplanFilter = DataplanFilterImpl.EMPTY;
    private KitOptions mKitOptions;
    private volatile List<KitConfiguration> kitConfigurations = new ArrayList<>();

    private static final String RESERVED_KEY_LTV = "$Amount";
    private static final String METHOD_NAME = "$MethodName";
    private static final String LOG_LTV = "LogLTVIncrease";

    private Map<Integer, AttributionResult> mAttributionResultsMap = new TreeMap<>();
    private ArrayList<KitsLoadedListener> kitsLoadedListeners = new ArrayList<>();


    ConcurrentHashMap<Integer, KitIntegration> providers = new ConcurrentHashMap<Integer, KitIntegration>();
    private final Context mContext;

    public KitManagerImpl(Context context, ReportingManager reportingManager, CoreCallbacks coreCallbacks, MParticleOptions options) {
        mContext = context;
        mReportingManager = reportingManager;
        mCoreCallbacks = coreCallbacks;
        mKitIntegrationFactory = new KitIntegrationFactory();
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            instance.Identity().addIdentityStateListener(this);
        }
        if (options != null) {
            for (Configuration configuration : options.getConfigurationsForTarget(this.getClass())) {
                configuration.apply(this);
            }
        }
        initializeKitIntegrationFactory();
    }

    public void setKitOptions(KitOptions kitOptions) {
        mKitOptions = kitOptions;
    }

    /**
     * Need this method so that we can override it during unit tests.
     */
    protected KitConfiguration createKitConfiguration(JSONObject configuration) throws JSONException {
        return KitConfiguration.createKitConfiguration(configuration);
    }

    public void setKitFactory(KitIntegrationFactory kitIntegrationFactory) {
        mKitIntegrationFactory = kitIntegrationFactory;
    }

    private ReportingManager getReportingManager() {
        return mReportingManager;
    }

    public boolean isBackgrounded() {
        return mCoreCallbacks.isBackgrounded();
    }

    public int getUserBucket() {
        return mCoreCallbacks.getUserBucket();
    }

    public boolean isOptedOut() {
        return !mCoreCallbacks.isEnabled();
    }

    public Uri getLaunchUri() {
        return mCoreCallbacks.getLaunchUri();
    }

    public String getLaunchAction() {
        return mCoreCallbacks.getLaunchAction();
    }

    void setIntegrationAttributes(KitIntegration kitIntegration, Map<String, String> integrationAttributes) {
        mCoreCallbacks.setIntegrationAttributes(kitIntegration.getConfiguration().getKitId(), integrationAttributes);
    }

    Map<String, String> getIntegrationAttributes(KitIntegration kitIntegration) {
        return mCoreCallbacks.getIntegrationAttributes(kitIntegration.getConfiguration().getKitId());
    }

    void clearIntegrationAttributes(KitIntegration kitIntegration) {
        setIntegrationAttributes(kitIntegration, null);
    }

    @Override
    public KitsLoadedCallback updateKits(final JSONArray kitConfigs) {
        KitsLoadedCallback callback = new KitsLoadedCallback();
        runOnKitThread(() -> {
                    kitConfigurations = parseKitConfigurations(kitConfigs);
                    runOnMainThread(() -> {
                        configureKits(kitConfigurations);
                        callback.setKitsLoaded();
                    });
                }
        );
        return callback;
    }

    @MainThread
    public void reloadKits() {
        configureKits(kitConfigurations);
    }

    @Override
    public void updateDataplan(@Nullable MParticleOptions.DataplanOptions dataplanOptions) {
        if (dataplanOptions != null) {
            try {
                Logger.info("Updating Data Plan");
                Logger.debug(dataplanOptions.toString());
                mDataplanFilter = new DataplanFilterImpl(dataplanOptions);
            } catch (Exception ex) {
                Logger.warning(ex, "Failed to parse DataplanOptions, Dataplan filtering for Kits will not be applied");
                mDataplanFilter = DataplanFilterImpl.EMPTY;
            }
        } else {
            mDataplanFilter = DataplanFilterImpl.EMPTY;
            Logger.info("Clearing Data Plan");
        }
    }

    /**
     * Update the current list of active kits based on server (or cached) configuration.
     * <p>
     * Note: This method is meant to always be run on the main thread.
     */
    protected synchronized void configureKits(@NonNull List<KitConfiguration> kitConfigurations) {
        if (kitConfigurations == null) {
            kitConfigurations = new ArrayList<>();
        }
        MParticle instance = MParticle.getInstance();
        if (instance == null) {
            //if MParticle has been dereferenced, abandon ship. This will run again when it is restarted
            return;
        }
        MParticleUser user = instance.Identity().getCurrentUser();
        HashSet<Integer> activeIds = new HashSet<Integer>();
        HashMap<Integer, KitIntegration> previousKits = new HashMap<>(providers);

        if (kitConfigurations != null) {
            for (KitConfiguration configuration: kitConfigurations) {
                try {
                    int currentModuleID = configuration.getKitId();
                    if (configuration.shouldExcludeUser(user)) {
                        mCoreCallbacks.getKitListener().kitExcluded(currentModuleID, "User was required to be known, but was not.");
                        continue;
                    }
                    if (!mKitIntegrationFactory.isSupported(configuration.getKitId())) {
                        Logger.debug("Kit id configured but is not bundled: " + currentModuleID);
                        continue;
                    }
                    KitIntegration activeKit = providers.get(currentModuleID);
                    if (activeKit == null) {
                        activeKit = mKitIntegrationFactory.createInstance(KitManagerImpl.this, configuration);
                        if (activeKit.isDisabled() ||
                                !configuration.shouldIncludeFromConsentRules(user)) {
                            Logger.debug("Kit id configured but is filtered or disabled: " + currentModuleID);
                            continue;
                        }
                        activeIds.add(currentModuleID);
                        initializeKit(activeKit);
                        providers.put(currentModuleID, activeKit);
                        mCoreCallbacks.getKitListener().kitStarted(currentModuleID);
                    } else {
                        activeKit.setConfiguration(configuration);
                        if (activeKit.isDisabled() ||
                                !configuration.shouldIncludeFromConsentRules(user)) {
                            continue;
                        }
                        activeIds.add(currentModuleID);
                        activeKit.onSettingsUpdated(configuration.getSettings());
                    }
                } catch (Exception e) {
                    mCoreCallbacks.getKitListener().kitExcluded(configuration.getKitId(), "exception while starting. Exception: " + e.getMessage());
                    Logger.error("Exception while starting kit " + configuration.getKitId() + ": " + e.getMessage());
                }
            }
        }

        Iterator<Integer> ids = providers.keySet().iterator();
        while (ids.hasNext()) {
            Integer id = ids.next();
            if (!activeIds.contains(id)) {
                KitIntegration integration = providers.get(id);
                if (integration != null) {
                    Logger.debug("De-initializing kit: " + integration.getName());
                    clearIntegrationAttributes(integration);
                    integration.onKitDestroy();
                    integration.onKitCleanup();
                }
                ids.remove();
                Intent intent = new Intent(MParticle.ServiceProviders.BROADCAST_DISABLED + id);
                getContext().sendBroadcast(intent);
            }
        }
        onKitsLoaded(new HashMap<>(providers), previousKits, new ArrayList<>(kitConfigurations));
    }

    private void initializeKit(KitIntegration activeKit) {
        Logger.debug("Initializing kit: " + activeKit.getName());
        activeKit.onKitCreate(activeKit.getConfiguration().getSettings(), getContext());
        if (activeKit instanceof KitIntegration.ActivityListener) {
            WeakReference<Activity> activityWeakReference = getCurrentActivity();
            if (activityWeakReference != null) {
                Activity activity = activityWeakReference.get();
                if (activity != null) {
                    KitIntegration.ActivityListener listener = (KitIntegration.ActivityListener) activeKit;
                    getReportingManager().logAll(
                            listener.onActivityCreated(activity, null)
                    );
                    getReportingManager().logAll(
                            listener.onActivityStarted(activity)
                    );
                    getReportingManager().logAll(
                            listener.onActivityResumed(activity)
                    );
                }
            }
        }

        if (activeKit instanceof KitIntegration.AttributeListener) {
            syncUserIdentities((KitIntegration.AttributeListener) activeKit, activeKit.getConfiguration());
        }

        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            Intent mockInstallReferrer = getMockInstallReferrerIntent(instance.getInstallReferrer());
            if (mockInstallReferrer != null) {
                activeKit.setInstallReferrer(mockInstallReferrer);
            }
        }

        if (activeKit instanceof KitIntegration.PushListener) {
            String senderId = mCoreCallbacks.getPushSenderId();
            String instanceId = mCoreCallbacks.getPushInstanceId();
            if (!MPUtility.isEmpty(instanceId)) {
                if (((KitIntegration.PushListener) activeKit).onPushRegistration(instanceId, senderId)) {
                    ReportingMessage message = ReportingMessage.fromPushRegistrationMessage(activeKit);
                    getReportingManager().log(message);
                }
            }
        }

        Intent intent = new Intent(MParticle.ServiceProviders.BROADCAST_ACTIVE + activeKit.getConfiguration().getKitId());
        getContext().sendBroadcast(intent);
    }

    @Override
    public Map<Integer, KitStatus> getKitStatus() {
        Map<Integer, KitStatus> kitStatusMap = new HashMap<>();
        for(Integer kitId: mKitIntegrationFactory.getSupportedKits()) {
            kitStatusMap.put(kitId, KitStatus.NOT_CONFIGURED);
        }
        for(KitConfiguration kitConfiguration: kitConfigurations) {
            kitStatusMap.put(kitConfiguration.getKitId(), KitStatus.STOPPED);
        }
        for(Map.Entry<Integer, KitIntegration> activeKit: providers.entrySet()) {
            if (!activeKit.getValue().isDisabled()) {
                kitStatusMap.put(activeKit.getKey(), KitStatus.ACTIVE);
            }
        }
        return kitStatusMap;
    }

    @Override
    public boolean isKitActive(int serviceProviderId) {
        KitIntegration provider = providers.get(serviceProviderId);
        return provider != null && !provider.isDisabled();
    }

    @Override
    public Object getKitInstance(int kitId) {
        KitIntegration kit = providers.get(kitId);
        return kit == null ? null : kit.getInstance();
    }

    //================================================================================
    // General KitIntegration forwarding
    //================================================================================

    @Override
    public void setLocation(Location location) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (!provider.isDisabled()) {
                    provider.setLocation(location);
                    mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), true, location);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call setLocation for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logNetworkPerformance(String url, long startTime, String method, long length, long bytesSent, long bytesReceived, String requestString, int responseCode) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (!provider.isDisabled()) {
                    List<ReportingMessage> report = provider.logNetworkPerformance(url, startTime, method, length, bytesSent, bytesReceived, requestString, responseCode);
                    getReportingManager().logAll(report);
                    mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), url, startTime, method, length, bytesSent, bytesReceived, requestString, responseCode);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logNetworkPerformance for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public Uri getSurveyUrl(int serviceId, Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists) {
        userAttributes = mDataplanFilter.transformUserAttributes(userAttributes);
        userAttributeLists = mDataplanFilter.transformUserAttributes(userAttributeLists);
        KitIntegration provider = providers.get(serviceId);
        if (provider != null) {
            return provider.getSurveyUrl((Map<String, String>) provider.getConfiguration().filterAttributes(provider.getConfiguration().getUserAttributeFilters(), userAttributes),
                    (Map<String, List<String>>) provider.getConfiguration().filterAttributes(provider.getConfiguration().getUserAttributeFilters(), userAttributeLists));
        } else {
            return null;
        }
    }

    @Override
    public void setOptOut(boolean optOutStatus) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (!provider.isDisabled(true)) {
                    List<ReportingMessage> messages = provider.setOptOut(optOutStatus);
                    getReportingManager().logAll(messages);
                    mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(messages), optOutStatus);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call setOptOut for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
        reloadKits();
    }

    @Override
    public Set<Integer> getSupportedKits() {
        return mKitIntegrationFactory.getSupportedKits();
    }

    @Override
    public void logEvent(BaseEvent event) {
        if (mDataplanFilter != null) {
            event = mDataplanFilter.transformEventForEvent(event);
            if (event == null) {
                return;
            }
        }
        for (KitIntegration provider: providers.values()) {
            try {
                List<ReportingMessage> messages = provider.logBaseEvent(event);
                mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(messages), event);
                mReportingManager.logAll(messages);
            } catch (Exception e) {
                Logger.warning("Failed to call logMPEvent for kit: " + provider.getName() + ": " + e.getMessage());
                mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), false, event, e);
            }
        }
        if (event instanceof MPEvent) {
            logMPEvent((MPEvent) event);
        } else if (event instanceof CommerceEvent) {
            logCommerceEvent((CommerceEvent) event);
        }
    }

    //================================================================================
    // KitIntegration.CommerceListener forwarding
    //================================================================================

    protected void logCommerceEvent(CommerceEvent event) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (!provider.isDisabled()) {
                    CommerceEvent filteredEvent = provider.getConfiguration().filterCommerceEvent(event);
                    if (filteredEvent != null) {
                        if (provider instanceof KitIntegration.CommerceListener) {
                            List<CustomMapping.ProjectionResult> projectedEvents = CustomMapping.projectEvents(
                                    filteredEvent,
                                    provider.getConfiguration().getCustomMappingList(),
                                    provider.getConfiguration().getDefaultCommerceCustomMapping()
                            );
                            if (projectedEvents != null && projectedEvents.size() > 0) {
                                ReportingMessage masterMessage = ReportingMessage.fromEvent(provider, filteredEvent);
                                boolean forwarded = false;
                                for (int i = 0; i < projectedEvents.size(); i++) {
                                    CustomMapping.ProjectionResult result = projectedEvents.get(i);
                                    List<ReportingMessage> report = null;
                                    String messageType = null;
                                    if (result.getMPEvent() != null) {
                                        MPEvent projectedEvent = projectedEvents.get(i).getMPEvent();
                                        report = ((KitIntegration.EventListener) provider).logEvent(projectedEvent);
                                        mCoreCallbacks.getKitListener().onKitApiCalled("logMPEvent()", provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), projectedEvent);
                                        messageType = ReportingMessage.MessageType.EVENT;
                                    } else {
                                        CommerceEvent projectedEvent = projectedEvents.get(i).getCommerceEvent();
                                        report = ((KitIntegration.CommerceListener) provider).logEvent(projectedEvent);
                                        mCoreCallbacks.getKitListener().onKitApiCalled("logMPEvent()", provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), projectedEvent);
                                        messageType = ReportingMessage.MessageType.COMMERCE_EVENT;
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
                                    getReportingManager().log(masterMessage);
                                }
                            } else {
                                List<ReportingMessage> reporting = ((KitIntegration.CommerceListener) provider).logEvent(filteredEvent);
                                mCoreCallbacks.getKitListener().onKitApiCalled("logMPEvent()", provider.getConfiguration().getKitId(), !MPUtility.isEmpty(reporting), filteredEvent);
                                if (reporting != null && reporting.size() > 0) {
                                    getReportingManager().log(
                                            ReportingMessage.fromEvent(provider, filteredEvent)
                                    );
                                }
                            }
                        } else if (provider instanceof KitIntegration.EventListener) {
                            List<MPEvent> events = CommerceEventUtils.expand(filteredEvent);
                            boolean forwarded = false;
                            if (events != null) {
                                for (MPEvent expandedEvent: events) {
                                    List<ReportingMessage> reporting = ((KitIntegration.EventListener) provider).logEvent(expandedEvent);
                                    mCoreCallbacks.getKitListener().onKitApiCalled("logMPEvent()", provider.getConfiguration().getKitId(), !MPUtility.isEmpty(reporting), expandedEvent);
                                    forwarded = forwarded || (reporting != null && reporting.size() > 0);
                                }
                            }
                            if (forwarded) {
                                getReportingManager().log(
                                        ReportingMessage.fromEvent(provider, filteredEvent)
                                );
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logCommerceEvent for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    //================================================================================
    // KitIntegration.PushListener forwarding
    //================================================================================

    @Override
    public boolean onMessageReceived(Context context, Intent intent) {
        for (KitIntegration provider : providers.values()) {
            if (provider instanceof KitIntegration.PushListener) {
                try {
                    if (!provider.isDisabled()) {
                        boolean willHandlePush = ((KitIntegration.PushListener) provider).willHandlePushMessage(intent);
                        mCoreCallbacks.getKitListener().onKitApiCalled("willHandlePushMessage()", provider.getConfiguration().getKitId(), willHandlePush, intent);
                        if (willHandlePush) {
                            ((KitIntegration.PushListener) provider).onPushMessageReceived(context, intent);
                            mCoreCallbacks.getKitListener().onKitApiCalled("onPushMessageReceived()", provider.getConfiguration().getKitId(), null, context, intent);
                            ReportingMessage message = ReportingMessage.fromPushMessage(provider, intent);
                            getReportingManager().log(message);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    Logger.warning("Failed to call onPushMessageReceived for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    @Override
    public boolean onPushRegistration(String token, String senderId) {
        for (KitIntegration provider : providers.values()) {
            if (provider instanceof KitIntegration.PushListener) {
                try {
                    if (!provider.isDisabled()) {
                        boolean onPushRegistration = ((KitIntegration.PushListener) provider).onPushRegistration(token, senderId);
                        mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(),onPushRegistration, token, senderId);
                        if (onPushRegistration) {
                            ReportingMessage message = ReportingMessage.fromPushRegistrationMessage(provider);
                            getReportingManager().log(message);
                        }
                        return true;
                    }
                } catch (Exception e) {
                    Logger.warning("Failed to call onPushRegistration for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
        return false;
    }

    //================================================================================
    // KitIntegration.AttributeListener forwarding
    //================================================================================
    @Override
    public void onUserAttributesReceived(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, Long mpid) {
        userAttributes = mDataplanFilter.transformUserAttributes(userAttributes);
        userAttributeLists = mDataplanFilter.transformUserAttributes(userAttributeLists);
        for (KitIntegration provider : providers.values()) {
            try {
                if ((provider instanceof KitIntegration.AttributeListener || provider instanceof KitIntegration.UserAttributeListener)
                        && !provider.isDisabled()) {
                    Map<String, String> filteredAttributeSingles = (Map<String, String>) KitConfiguration.filterAttributes(provider.getConfiguration().getUserAttributeFilters(),
                            userAttributes);
                    Map<String, List<String>> filteredAttributeLists = (Map<String, List<String>>) KitConfiguration.filterAttributes(provider.getConfiguration().getUserAttributeFilters(),
                            userAttributeLists);
                    if (provider instanceof KitIntegration.AttributeListener) {
                        if (((KitIntegration.AttributeListener) provider).supportsAttributeLists()) {
                            ((KitIntegration.AttributeListener) provider).setAllUserAttributes(filteredAttributeSingles, filteredAttributeLists);
                        } else {
                            Map<String, String> singlesCopy = new HashMap<>(filteredAttributeSingles);
                            for (Map.Entry<String, List<String>> entry : filteredAttributeLists.entrySet()) {
                                singlesCopy.put(entry.getKey(), KitUtils.join(entry.getValue()));
                            }
                            ((KitIntegration.AttributeListener) provider).setAllUserAttributes(singlesCopy, new HashMap<String, List<String>>());
                        }
                    }
                    if (provider instanceof KitIntegration.UserAttributeListener) {
                        if (((KitIntegration.UserAttributeListener) provider).supportsAttributeLists()) {
                            ((KitIntegration.UserAttributeListener) provider).onSetAllUserAttributes(filteredAttributeSingles, filteredAttributeLists, FilteredMParticleUser.getInstance(mpid, provider));
                        } else {
                            Map<String, String> singlesCopy = new HashMap<>(filteredAttributeSingles);
                            for (Map.Entry<String, List<String>> entry : filteredAttributeLists.entrySet()) {
                                singlesCopy.put(entry.getKey(), KitUtils.join(entry.getValue()));
                            }
                            ((KitIntegration.UserAttributeListener) provider).onSetAllUserAttributes(singlesCopy, new HashMap<String, List<String>>(), FilteredMParticleUser.getInstance(mpid, provider));
                        }
                    }
                }
            } catch (Exception e) {
                Logger.warning("Failed to call setUserAttributes for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    private void syncUserIdentities(KitIntegration.AttributeListener attributeListener, KitConfiguration configuration) {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            MParticleUser user = instance.Identity().getCurrentUser();
            if (user != null) {
                Map<MParticle.IdentityType, String> identities = user.getUserIdentities();
                if (identities != null) {
                    for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
                        if (configuration.shouldSetIdentity(entry.getKey())) {
                            attributeListener.setUserIdentity(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setUserAttribute(String attributeKey, String attributeValue, long mpid) {
        if (mDataplanFilter.isUserAttributeBlocked(attributeKey)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                setUserAttribute(provider, attributeKey, attributeValue, mpid);
            } catch (Exception e) {
                Logger.warning("Failed to call setUserAttributes/onSetUserAttribute for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void setUserAttributeList(String attributeKey, List<String> valuesList, long mpid) {
        if (mDataplanFilter.isUserAttributeBlocked(attributeKey)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                setUserAttribute(provider, attributeKey, valuesList, mpid);
            } catch (Exception e) {
                Logger.warning("Failed to call setUserAttributes/onSetUserAttribute for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    private void setUserAttribute(KitIntegration provider, String attributeKey, List<String> valueList, long mpid) {
        if ((provider instanceof KitIntegration.AttributeListener || provider instanceof KitIntegration.UserAttributeListener)
                && !provider.isDisabled()
                && KitConfiguration.shouldForwardAttribute(provider.getConfiguration().getUserAttributeFilters(), attributeKey)) {
            if (provider instanceof KitIntegration.AttributeListener) {
                if (((KitIntegration.AttributeListener) provider).supportsAttributeLists()) {
                    ((KitIntegration.AttributeListener) provider).setUserAttributeList(attributeKey, valueList);
                } else {
                    ((KitIntegration.AttributeListener) provider).setUserAttribute(attributeKey, KitUtils.join(valueList));
                }
            }
            if (provider instanceof KitIntegration.UserAttributeListener) {
                if (((KitIntegration.UserAttributeListener) provider).supportsAttributeLists()) {
                    ((KitIntegration.UserAttributeListener) provider).onSetUserAttributeList(attributeKey, valueList, FilteredMParticleUser.getInstance(mpid, provider));
                } else {
                    ((KitIntegration.UserAttributeListener) provider).onSetUserAttribute(attributeKey, KitUtils.join(valueList), FilteredMParticleUser.getInstance(mpid, provider));
                }
            }
        }
    }

    private void setUserAttribute(KitIntegration provider, String attributeKey, String attributeValue, long mpid) {
        if ((provider instanceof KitIntegration.AttributeListener || provider instanceof KitIntegration.UserAttributeListener)
                && !provider.isDisabled()
                && KitConfiguration.shouldForwardAttribute(provider.getConfiguration().getUserAttributeFilters(),
                attributeKey)) {
            if (provider instanceof KitIntegration.AttributeListener) {
                ((KitIntegration.AttributeListener) provider).setUserAttribute(attributeKey, attributeValue);
            }
            if (provider instanceof KitIntegration.UserAttributeListener) {
                ((KitIntegration.UserAttributeListener) provider).onSetUserAttribute(attributeKey, attributeValue, FilteredMParticleUser.getInstance(mpid, provider));
            }
        }
    }

    @Override
    public void removeUserAttribute(String key, long mpid) {
        if (mDataplanFilter.isUserAttributeBlocked(key)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if ((provider instanceof KitIntegration.AttributeListener || provider instanceof KitIntegration.UserAttributeListener)
                        && !provider.isDisabled()
                        && KitConfiguration.shouldForwardAttribute(provider.getConfiguration().getUserAttributeFilters(), key)) {
                    if (provider instanceof KitIntegration.AttributeListener) {
                        ((KitIntegration.AttributeListener) provider).removeUserAttribute(key);
                    }
                    if (provider instanceof KitIntegration.UserAttributeListener) {
                        ((KitIntegration.UserAttributeListener) provider).onRemoveUserAttribute(key, FilteredMParticleUser.getInstance(mpid, provider));
                    }
                }
            } catch (Exception e) {
                Logger.warning("Failed to call removeUserAttribute/onRemoveUserAttribute for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void incrementUserAttribute(String key, int incrementedBy, String newValue, long mpid) {
        if (mDataplanFilter.isUserAttributeBlocked(key)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if (!provider.isDisabled() && KitConfiguration.shouldForwardAttribute(provider.getConfiguration().getUserAttributeFilters(), key))
                if (provider instanceof KitIntegration.UserAttributeListener) {
                    ((KitIntegration.UserAttributeListener) provider).onIncrementUserAttribute(key, incrementedBy, newValue, FilteredMParticleUser.getInstance(mpid, provider));
                }
                if (provider instanceof KitIntegration.AttributeListener) {
                    ((KitIntegration.AttributeListener) provider).setUserAttribute(key, newValue);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onIncrementUserAttribute for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void setUserTag(String tag, long mpid) {
        if (mDataplanFilter.isUserAttributeBlocked(tag)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.UserAttributeListener && !provider.isDisabled()
                        && KitConfiguration.shouldForwardAttribute(provider.getConfiguration().getUserAttributeFilters(), tag)) {
                    ((KitIntegration.UserAttributeListener) provider).onSetUserTag(tag, FilteredMParticleUser.getInstance(mpid, provider));
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onSetUserTag for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if (mDataplanFilter.isUserIdentityBlocked(identityType)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.AttributeListener && !provider.isDisabled() && provider.getConfiguration().shouldSetIdentity(identityType)) {
                    ((KitIntegration.AttributeListener) provider).setUserIdentity(identityType, id);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call setUserIdentity for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (mDataplanFilter.isUserIdentityBlocked(identityType)) {
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.AttributeListener && !provider.isDisabled()) {
                    ((KitIntegration.AttributeListener) provider).removeUserIdentity(identityType);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call removeUserIdentity for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logout() {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.AttributeListener && !provider.isDisabled()) {
                    List<ReportingMessage> report = ((KitIntegration.AttributeListener) provider).logout();
                    getReportingManager().logAll(report);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logout for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    //================================================================================
    // KitIntegration.EventListener forwarding
    //================================================================================

    public Context getContext() {
        return this.mContext;
    }

    @Override
    public WeakReference<Activity> getCurrentActivity() {
        return mCoreCallbacks.getCurrentActivity();
    }


    protected void logMPEvent(MPEvent event) {
        if (event.isScreenEvent()) {
            logScreen(event);
            return;
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.EventListener && !provider.isDisabled() && provider.getConfiguration().shouldLogEvent(event)) {
                    MPEvent eventCopy = new MPEvent(event);
                    eventCopy.setCustomAttributes(
                            provider.getConfiguration().filterEventAttributes(eventCopy)
                    );
                    List<CustomMapping.ProjectionResult> projectedEvents = CustomMapping.projectEvents(
                            eventCopy,
                            provider.getConfiguration().getCustomMappingList(),
                            provider.getConfiguration().getDefaultEventProjection()
                    );
                    List<ReportingMessage> reportingMessages = new LinkedList<ReportingMessage>();
                    if (projectedEvents == null) {
                        List<ReportingMessage> messages = null;
                        if (eventCopy.getCustomAttributeStrings() != null
                                && eventCopy.getCustomAttributeStrings().containsKey(METHOD_NAME)
                                && eventCopy.getCustomAttributeStrings().get(METHOD_NAME).equals(LOG_LTV)) {
                            messages = ((KitIntegration.CommerceListener) provider).logLtvIncrease(
                                    new BigDecimal(eventCopy.getCustomAttributeStrings().get(RESERVED_KEY_LTV)),
                                    new BigDecimal(eventCopy.getCustomAttributeStrings().get(RESERVED_KEY_LTV)),
                                    eventCopy.getEventName(),
                                    eventCopy.getCustomAttributeStrings());
                        } else {
                            messages = ((KitIntegration.EventListener) provider).logEvent(eventCopy);
                            mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(messages), eventCopy);
                        }
                        if (messages != null && messages.size() > 0) {
                            reportingMessages.addAll(messages);
                        }
                    } else {
                        ReportingMessage masterMessage = ReportingMessage.fromEvent(provider, eventCopy);
                        boolean forwarded = false;
                        for (int i = 0; i < projectedEvents.size(); i++) {
                            MPEvent projectedEvent = projectedEvents.get(i).getMPEvent();
                            List<ReportingMessage> messages = ((KitIntegration.EventListener) provider).logEvent(projectedEvent);
                            mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(messages), projectedEvent);

                            if (messages != null && messages.size() > 0) {
                                forwarded = true;
                                for (ReportingMessage message : messages) {
                                    ReportingMessage.ProjectionReport report = new ReportingMessage.ProjectionReport(
                                            projectedEvents.get(i).getProjectionId(),
                                            ReportingMessage.MessageType.EVENT,
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
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logMPEvent for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logBatch(String batch) {
        for (KitIntegration provider: providers.values()) {
            try {
                if (provider instanceof KitIntegration.BatchListener) {
                    JSONObject jsonObject = new JSONObject(batch);
                    List<ReportingMessage> reportingMessages = ((KitIntegration.BatchListener)provider).logBatch(jsonObject);
                    getReportingManager().logAll(reportingMessages);
                }
            }
            catch (JSONException jse) {
                Logger.error(jse, "Failed to call logBatch (unable to deserialize Batch) for kit" + provider.getName() + ": " + jse.getMessage());
            }
            catch (Exception e) {
                Logger.warning("Failed to call logBatch for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void leaveBreadcrumb(String breadcrumb) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.EventListener && !provider.isDisabled()) {
                    List<ReportingMessage> report = ((KitIntegration.EventListener) provider).leaveBreadcrumb(breadcrumb);
                    getReportingManager().logAll(report);
                    mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), breadcrumb);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call leaveBreadcrumb for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logError(String message, Map<String, String> eventData) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.EventListener && !provider.isDisabled()) {
                    List<ReportingMessage> report = ((KitIntegration.EventListener) provider).logError(message, eventData);
                    getReportingManager().logAll(report);
                    mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), message, eventData);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logError for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logException(Exception exception, Map<String, String> eventData, String message) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.EventListener && !provider.isDisabled()) {
                    List<ReportingMessage> report = ((KitIntegration.EventListener) provider).logException(exception, eventData, message);
                    getReportingManager().logAll(report);
                    mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), exception, message, eventData);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logException for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void logScreen(MPEvent screenEvent) {
        if (mDataplanFilter != null) {
            screenEvent = mDataplanFilter.transformEventForEvent(screenEvent);
            if (screenEvent == null) {
                return;
            }
        }
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.EventListener && !provider.isDisabled() && provider.getConfiguration().shouldLogScreen(screenEvent.getEventName())) {
                    MPEvent filteredEvent = new MPEvent.Builder(screenEvent)
                            .customAttributes(provider.getConfiguration().filterScreenAttributes(null, screenEvent.getEventName(), screenEvent.getCustomAttributeStrings()))
                            .build();

                    List<CustomMapping.ProjectionResult> projectedEvents = CustomMapping.projectEvents(
                            filteredEvent,
                            true,
                            provider.getConfiguration().getCustomMappingList(),
                            provider.getConfiguration().getDefaultEventProjection(),
                            provider.getConfiguration().getDefaultScreenCustomMapping());
                    if (projectedEvents == null) {
                        String eventName = filteredEvent.getEventName();
                        Map<String, String> eventInfo = filteredEvent.getCustomAttributeStrings();
                        List<ReportingMessage> report = ((KitIntegration.EventListener) provider).logScreen(eventName, eventInfo);
                        mCoreCallbacks.getKitListener().onKitApiCalled(provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), eventName, eventInfo);
                        if (report != null && report.size() > 0) {
                            for (ReportingMessage message : report) {
                                message.setMessageType(ReportingMessage.MessageType.SCREEN_VIEW);
                                message.setScreenName(filteredEvent.getEventName());
                            }
                        }
                        getReportingManager().logAll(report);
                    } else {
                        ReportingMessage masterMessage = new ReportingMessage(provider,
                                ReportingMessage.MessageType.SCREEN_VIEW,
                                System.currentTimeMillis(),
                                filteredEvent.getCustomAttributeStrings());
                        boolean forwarded = false;
                        for (CustomMapping.ProjectionResult projectedEvent: projectedEvents) {
                            List<ReportingMessage> report = ((KitIntegration.EventListener) provider).logEvent(projectedEvent.getMPEvent());
                            mCoreCallbacks.getKitListener().onKitApiCalled("logMPEvent()", provider.getConfiguration().getKitId(), !MPUtility.isEmpty(report), projectedEvent);
                            if (report != null && report.size() > 0) {
                                forwarded = true;
                                for (ReportingMessage message : report) {
                                    ReportingMessage.ProjectionReport projectionReport = new ReportingMessage.ProjectionReport(
                                            projectedEvent.getProjectionId(),
                                            ReportingMessage.MessageType.EVENT,
                                            message.getEventName(),
                                            message.getEventTypeString()
                                    );
                                    masterMessage.setMessageType(ReportingMessage.MessageType.SCREEN_VIEW);
                                    masterMessage.setScreenName(message.getEventName());
                                    masterMessage.addProjectionReport(projectionReport);
                                }
                            }
                        }
                        if (forwarded) {
                            getReportingManager().log(masterMessage);
                        }
                    }
                }
            } catch (Exception e) {
                Logger.warning("Failed to call logScreen for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    //================================================================================
    // KitIntegration.ActivityListener forwarding
    //================================================================================

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivityCreated(activity, savedInstanceState);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onActivityCreated for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivityStarted(activity);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onActivityStarted for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivityResumed(activity);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onActivityResumed for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivityPaused(activity);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onResume for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivityStopped(activity);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onActivityStopped for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivitySaveInstanceState(activity, outState);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onActivitySaveInstanceState for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ActivityListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.ActivityListener) provider).onActivityDestroyed(activity);
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onActivityDestroyed for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onSessionEnd() {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.SessionListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.SessionListener) provider).onSessionEnd();
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onSessionEnd for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onSessionStart() {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.SessionListener && !provider.isDisabled()) {
                    List<ReportingMessage> reportingMessages = ((KitIntegration.SessionListener) provider).onSessionStart();
                    getReportingManager().logAll(reportingMessages);
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onSessionStart for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onApplicationForeground() {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ApplicationStateListener) {
                    ((KitIntegration.ApplicationStateListener) provider).onApplicationForeground();
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onApplicationForeground for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onApplicationBackground() {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.ApplicationStateListener) {
                    ((KitIntegration.ApplicationStateListener) provider).onApplicationBackground();
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onApplicationBackground for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public Map<Integer, AttributionResult> getAttributionResults() {
        return mAttributionResultsMap;
    }

    //================================================================================
    // AttributionListener forwarding
    //================================================================================
    @Override
    public void onResult(AttributionResult result) {
        mAttributionResultsMap.put(result.getServiceProviderId(), result);
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            AttributionListener listener = instance.getAttributionListener();
            if (listener != null && result != null) {
                Logger.debug("Attribution result returned: \n" + result.toString());
                listener.onResult(result);
            }
        }
    }

    @Override
    public void onError(AttributionError error) {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            AttributionListener listener = instance.getAttributionListener();
            if (listener != null && error != null) {
                Logger.debug("Attribution error returned: \n" + error.toString());
                listener.onError(error);
            }
        }
    }

    @Override
    public void installReferrerUpdated() {
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            Intent mockIntent = getMockInstallReferrerIntent(instance.getInstallReferrer());
            for (KitIntegration provider : providers.values()) {
                try {
                    if (!provider.isDisabled()) {
                        provider.setInstallReferrer(mockIntent);
                    }
                } catch (Exception e) {
                    Logger.warning("Failed to update Install Referrer for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    //================================================================================
    // IdentityListener forwarding
    //================================================================================
    @Override
    public void onUserIdentified(MParticleUser mParticleUser, MParticleUser previousUser) {
        //due to consent forwarding rules we need to re-verify kits whenever the user changes
        reloadKits();
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.IdentityListener && !provider.isDisabled()) {
                    ((KitIntegration.IdentityListener) provider).onUserIdentified(FilteredMParticleUser.getInstance(mParticleUser, provider));
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onUserIdentified for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }

        mParticleUser.getUserAttributes(this);
    }

    @Override
    public void onIdentifyCompleted(MParticleUser mParticleUser, IdentityApiRequest identityApiRequest) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.IdentityListener && !provider.isDisabled()) {
                    ((KitIntegration.IdentityListener) provider).onIdentifyCompleted(FilteredMParticleUser.getInstance(mParticleUser, provider), new FilteredIdentityApiRequest(identityApiRequest, provider));
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onIdentifyCompleted for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onLoginCompleted(MParticleUser mParticleUser, IdentityApiRequest identityApiRequest) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.IdentityListener && !provider.isDisabled()) {
                    ((KitIntegration.IdentityListener) provider).onLoginCompleted(FilteredMParticleUser.getInstance(mParticleUser, provider), new FilteredIdentityApiRequest(identityApiRequest, provider));
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onLoginCompleted for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onLogoutCompleted(MParticleUser mParticleUser, IdentityApiRequest identityApiRequest) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.IdentityListener && !provider.isDisabled()) {
                    ((KitIntegration.IdentityListener) provider).onLogoutCompleted(FilteredMParticleUser.getInstance(mParticleUser, provider), new FilteredIdentityApiRequest(identityApiRequest, provider));
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onLogoutCompleted for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onModifyCompleted(MParticleUser mParticleUser, IdentityApiRequest identityApiRequest) {
        for (KitIntegration provider : providers.values()) {
            try {
                if (provider instanceof KitIntegration.IdentityListener && !provider.isDisabled()) {
                    ((KitIntegration.IdentityListener) provider).onModifyCompleted(FilteredMParticleUser.getInstance(mParticleUser, provider), new FilteredIdentityApiRequest(identityApiRequest, provider));
                }
            } catch (Exception e) {
                Logger.warning("Failed to call onModifyCompleted for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onConsentStateUpdated(final ConsentState oldState, final ConsentState newState, final long mpid) {
        //Due to consent forwarding rules we need to re-initialize kits whenever the user changes.
        reloadKits();
        for (KitIntegration provider : providers.values()) {
            if (provider instanceof KitIntegration.UserAttributeListener && !provider.isDisabled()) {
                try {
                    ((KitIntegration.UserAttributeListener) provider).onConsentStateUpdated(oldState, newState, FilteredMParticleUser.getInstance(mpid, provider));
                } catch (Exception e) {
                    Logger.warning("Failed to call onConsentStateUpdated for kit: " + provider.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void reset() {
        for (KitIntegration provider : providers.values()) {
            try {
                provider.reset();
            } catch (Exception e) {
                Logger.warning("Failed to call reset for kit: " + provider.getName() + ": " + e.getMessage());
            }
        }
    }

    public void runOnKitThread(Runnable runnable) {
        if (mKitHandler == null) {
            mKitHandler = new Handler(kitHandlerThread.getLooper());
        }
        mKitHandler.post(runnable);
    }

    public void runOnMainThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    public boolean isPushEnabled() {
        return mCoreCallbacks.isPushEnabled();
    }

    public String getPushSenderId() {
        return mCoreCallbacks.getPushSenderId();
    }

    public String getPushInstanceId() {
        return mCoreCallbacks.getPushInstanceId();
    }

    @Nullable
    private static Intent getMockInstallReferrerIntent(@NonNull String referrer) {
        if (!MPUtility.isEmpty(referrer)) {
            Intent fakeReferralIntent = new Intent("com.android.vending.INSTALL_REFERRER");
            fakeReferralIntent.putExtra(com.mparticle.internal.Constants.REFERRER, referrer);
            return fakeReferralIntent;
        } else {
            return null;
        }
    }

    @NonNull
    DataplanFilter getDataplanFilter() {
        if (mDataplanFilter == null) {
            Logger.warning("DataplanFilter could not be found");
            return DataplanFilterImpl.EMPTY;
        } else {
            return mDataplanFilter;
        }
    }

    void setDataplanFilter(@Nullable DataplanFilter dataplanFilter) {
        if (dataplanFilter != null) {
            mDataplanFilter = dataplanFilter;
        } else {
            mDataplanFilter = DataplanFilterImpl.EMPTY;
        }
    }

    private void initializeKitIntegrationFactory() {
        if (mKitIntegrationFactory != null) {
            if (mKitOptions != null) {
                for (Map.Entry<Integer, Class<? extends KitIntegration>> kitEntry : mKitOptions.getKits().entrySet()) {
                    Logger.info("Kit registered: " + kitEntry.getValue().getSimpleName() + "(" + kitEntry.getKey() + ")");
                    mKitIntegrationFactory.addSupportedKit(kitEntry.getKey(), kitEntry.getValue());
                }
            }
            if (mKitIntegrationFactory.supportedKits != null) {
                for (Integer kitId : mKitIntegrationFactory.supportedKits.keySet()) {
                    mCoreCallbacks.getKitListener().kitFound(kitId);
                }
            }
        }
    }

    private List<KitConfiguration> parseKitConfigurations(JSONArray kitConfigs) {
        List<KitConfiguration> configurations = new ArrayList<>();
        if (kitConfigs == null) {
            kitConfigs = new JSONArray();
        }
        for (int i = 0; i < kitConfigs.length(); i++) {
            JSONObject kitConfig = null;
            try {
                kitConfig = kitConfigs.getJSONObject(i);
            } catch (JSONException e) {
                Logger.error(e, "Malformed Kit configuration");
            }
            if (kitConfig != null) {
                try {
                    configurations.add(createKitConfiguration(kitConfig));
                } catch (JSONException e) {
                    int kitId = kitConfig.optInt("id", -1);
                    mCoreCallbacks.getKitListener().kitExcluded(kitId, "exception while starting. Exception: " + e.getMessage());
                    Logger.error("Exception while starting kit: " + kitId + ": " + e.getMessage());
                }
            }
        }
        return configurations;
    }

    public void addKitsLoadedListener(KitsLoadedListener kitsLoadedListener) {
        kitsLoadedListeners.add((KitsLoadedListener) kitsLoadedListener);
    }

    private void onKitsLoaded(Map<Integer, KitIntegration> kits, Map<Integer, KitIntegration> previousKits, List<KitConfiguration> kitConfigs) {
        for(KitsLoadedListener listener: kitsLoadedListeners) {
            listener.onKitsLoaded(kits, previousKits, kitConfigs);
        }
    }

    interface KitsLoadedListener {
        void onKitsLoaded(Map<Integer, KitIntegration> kits, Map<Integer, KitIntegration> previousKits, List<KitConfiguration> kitConfigs);
    }
}
