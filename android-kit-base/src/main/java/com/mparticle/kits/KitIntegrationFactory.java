package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.Logger;
import com.mparticle.internal.SideloadedKit;

import org.json.JSONException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KitIntegrationFactory {

    final Map<Integer, Class> supportedKits = new HashMap<>();
    public static int minSideloadedKitId = 1000000;
    private static int sideloadedKitNextId = minSideloadedKitId;
    private Map<Integer, String> knownIntegrations = new HashMap<Integer, String>();

    public KitIntegrationFactory(MParticleOptions options) {
        supportedKits.clear();
        knownIntegrations.clear();
        setupKnownIntegrations();
        loadIntegrations(options);
    }

    public static int getSideloadedKitId() {
        return sideloadedKitNextId++;
    }

    /**
     * This is the canonical method mapping all known Kit/Module IDs to Kit class names.
     * Mapping of module Ids to kit classes
     */
    private void setupKnownIntegrations() {
        knownIntegrations.put(MParticle.ServiceProviders.ADJUST, "com.mparticle.kits.AdjustKit");
        knownIntegrations.put(MParticle.ServiceProviders.APPBOY, "com.mparticle.kits.AppboyKit");
        knownIntegrations.put(MParticle.ServiceProviders.BRANCH_METRICS, "com.mparticle.kits.BranchMetricsKit");
        knownIntegrations.put(MParticle.ServiceProviders.COMSCORE, "com.mparticle.kits.ComscoreKit");
        knownIntegrations.put(MParticle.ServiceProviders.KOCHAVA, "com.mparticle.kits.KochavaKit");
        knownIntegrations.put(MParticle.ServiceProviders.FORESEE_ID, "com.mparticle.kits.ForeseeKit");
        knownIntegrations.put(MParticle.ServiceProviders.LOCALYTICS, "com.mparticle.kits.LocalyticsKit");
        knownIntegrations.put(MParticle.ServiceProviders.FLURRY, "com.mparticle.kits.FlurryKit");
        knownIntegrations.put(MParticle.ServiceProviders.WOOTRIC, "com.mparticle.kits.WootricKit");
        knownIntegrations.put(MParticle.ServiceProviders.CRITTERCISM, "com.mparticle.kits.CrittercismKit");
        knownIntegrations.put(MParticle.ServiceProviders.TUNE, "com.mparticle.kits.TuneKit");
        knownIntegrations.put(MParticle.ServiceProviders.APPSFLYER, "com.mparticle.kits.AppsFlyerKit");
        knownIntegrations.put(MParticle.ServiceProviders.APPTENTIVE, "com.mparticle.kits.ApptentiveKit");
        knownIntegrations.put(MParticle.ServiceProviders.BUTTON, "com.mparticle.kits.ButtonKit");
        knownIntegrations.put(MParticle.ServiceProviders.URBAN_AIRSHIP, "com.mparticle.kits.UrbanAirshipKit");
        knownIntegrations.put(MParticle.ServiceProviders.LEANPLUM, "com.mparticle.kits.LeanplumKit");
        knownIntegrations.put(MParticle.ServiceProviders.APPTIMIZE, "com.mparticle.kits.ApptimizeKit");
        knownIntegrations.put(MParticle.ServiceProviders.REVEAL_MOBILE, "com.mparticle.kits.RevealMobileKit");
        knownIntegrations.put(MParticle.ServiceProviders.RADAR, "com.mparticle.kits.RadarKit");
        knownIntegrations.put(MParticle.ServiceProviders.ITERABLE, "com.mparticle.kits.IterableKit");
        knownIntegrations.put(MParticle.ServiceProviders.SKYHOOK, "com.mparticle.kits.SkyhookKit");
        knownIntegrations.put(MParticle.ServiceProviders.SINGULAR, "com.mparticle.kits.SingularKit");
        knownIntegrations.put(MParticle.ServiceProviders.ADOBE, "com.mparticle.kits.AdobeKit");
        knownIntegrations.put(MParticle.ServiceProviders.TAPLYTICS, "com.mparticle.kits.TaplyticsKit");
        knownIntegrations.put(MParticle.ServiceProviders.OPTIMIZELY, "com.mparticle.kits.OptimizelyKit");
        knownIntegrations.put(MParticle.ServiceProviders.RESPONSYS, "com.mparticle.kits.ResponsysKit");
        knownIntegrations.put(MParticle.ServiceProviders.CLEVERTAP, "com.mparticle.kits.CleverTapKit");
        knownIntegrations.put(MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE, "com.mparticle.kits.GoogleAnalyticsFirebaseKit");
        knownIntegrations.put(MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE_GA4, "com.mparticle.kits.GoogleAnalyticsFirebaseGA4Kit");
        knownIntegrations.put(MParticle.ServiceProviders.PILGRIM, "com.mparticle.kits.PilgrimKit");
        knownIntegrations.put(MParticle.ServiceProviders.ONETRUST, "com.mparticle.kits.OneTrustKit");
        knownIntegrations.put(MParticle.ServiceProviders.SWRVE, "com.mparticle.kits.SwrveKit");
        knownIntegrations.put(MParticle.ServiceProviders.BLUESHIFT, "com.mparticle.kits.BlueshiftKit");
        knownIntegrations.put(MParticle.ServiceProviders.NEURA, "com.mparticle.kits.NeuraKit");
    }

    public KitIntegration createInstance(KitManagerImpl manager, KitConfiguration configuration) throws JSONException, ClassNotFoundException {
        KitIntegration kit = createInstance(manager, configuration.getKitId());
        if (kit != null) {
            kit.setConfiguration(configuration);
        }
        return kit;
    }

    public KitIntegration createInstance(KitManagerImpl manager, int moduleId) throws JSONException, ClassNotFoundException {
        if (!supportedKits.isEmpty()) {
            try {
                Constructor<KitIntegration> constructor = supportedKits.get(moduleId).getConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance().setKitManager(manager);
            } catch (Exception e) {
                Logger.debug(e, "Failed to create Kit with ID: " + moduleId);
            }
        }
        return null;
    }

    private void loadSideloadedIntegrations(MParticleOptions options) {
        for (SideloadedKit entry : options.getSideloadedKits()) {
            if (entry instanceof MPSideloadedKit && !knownIntegrations.containsKey(((MPSideloadedKit) entry).getConfiguration().getKitId())) {
                int kitId = ((MPSideloadedKit) entry).getConfiguration().getKitId();
                Class kitClazz = entry.getClass();
                knownIntegrations.put(kitId, kitClazz.getName());
            }
        }
    }

    private void loadIntegrations(MParticleOptions options) {
        loadSideloadedIntegrations(options);
        for (Map.Entry<Integer, String> entry : knownIntegrations.entrySet()) {
            Class kitClass = loadKit(entry.getValue());
            if (kitClass != null) {
                supportedKits.put(entry.getKey(), kitClass);
                Logger.debug(entry.getValue().substring(entry.getValue().lastIndexOf(".") + 1) + " detected.");
            }
        }
    }

    private Class loadKit(String className) {
        try {
            return Class.forName(className);  // nosemgrep
        } catch (Exception e) {
            Logger.verbose("Kit not bundled: ", className);
        }
        return null;
    }

    public void addSupportedKit(int kitId, Class<? extends KitIntegration> kitIntegration) {
        Class previousKit = supportedKits.get(kitId);
        if (previousKit != null) {
            Logger.warning("Overriding kitId " + kitId + ". KitIntegration: " + kitIntegration.getName() + " will replace existing KitIntegration: " + previousKit.getName());
        }
        supportedKits.put(kitId, kitIntegration);
    }

    /**
     * Get the module IDs of the Kits that have been detected.
     *
     * @return
     */
    public Set<Integer> getSupportedKits() {
        return supportedKits.keySet();
    }

    public boolean isSupported(int kitModuleId) {
        return supportedKits.containsKey(kitModuleId) || kitModuleId >= minSideloadedKitId;
    }
}
