package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPSideloadedKit;

import org.json.JSONException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KitIntegrationFactory {

    final Map<Integer, Class> supportedKits = new HashMap<>();
    final static Map<Integer, MPSideloadedKit> sideloadedKits = new HashMap<>();
    private static int minSideloadedKitId = 10000000;
    private static int sideloadedKitNextId = minSideloadedKitId;

    public KitIntegrationFactory(MParticleOptions options) {
        mergeIntegrations(options);
        loadIntegrations();
    }

    public static int getSideloadedKitId() {
        return sideloadedKitNextId++;
    }

    /**
     * This is the canonical method mapping all known Kit/Module IDs to Kit class names.
     *
     * @return a mapping of module Ids to kit classes
     */
    protected static Map<Integer, String> getKnownIntegrations() {
        Map<Integer, String> kits = new HashMap<Integer, String>();
        kits.put(MParticle.ServiceProviders.ADJUST, "com.mparticle.kits.AdjustKit");
        kits.put(MParticle.ServiceProviders.APPBOY, "com.mparticle.kits.AppboyKit");
        kits.put(MParticle.ServiceProviders.BRANCH_METRICS, "com.mparticle.kits.BranchMetricsKit");
        kits.put(MParticle.ServiceProviders.COMSCORE, "com.mparticle.kits.ComscoreKit");
        kits.put(MParticle.ServiceProviders.KOCHAVA, "com.mparticle.kits.KochavaKit");
        kits.put(MParticle.ServiceProviders.FORESEE_ID, "com.mparticle.kits.ForeseeKit");
        kits.put(MParticle.ServiceProviders.LOCALYTICS, "com.mparticle.kits.LocalyticsKit");
        kits.put(MParticle.ServiceProviders.FLURRY, "com.mparticle.kits.FlurryKit");
        kits.put(MParticle.ServiceProviders.WOOTRIC, "com.mparticle.kits.WootricKit");
        kits.put(MParticle.ServiceProviders.CRITTERCISM, "com.mparticle.kits.CrittercismKit");
        kits.put(MParticle.ServiceProviders.TUNE, "com.mparticle.kits.TuneKit");
        kits.put(MParticle.ServiceProviders.APPSFLYER, "com.mparticle.kits.AppsFlyerKit");
        kits.put(MParticle.ServiceProviders.APPTENTIVE, "com.mparticle.kits.ApptentiveKit");
        kits.put(MParticle.ServiceProviders.BUTTON, "com.mparticle.kits.ButtonKit");
        kits.put(MParticle.ServiceProviders.URBAN_AIRSHIP, "com.mparticle.kits.UrbanAirshipKit");
        kits.put(MParticle.ServiceProviders.LEANPLUM, "com.mparticle.kits.LeanplumKit");
        kits.put(MParticle.ServiceProviders.APPTIMIZE, "com.mparticle.kits.ApptimizeKit");
        kits.put(MParticle.ServiceProviders.REVEAL_MOBILE, "com.mparticle.kits.RevealMobileKit");
        kits.put(MParticle.ServiceProviders.RADAR, "com.mparticle.kits.RadarKit");
        kits.put(MParticle.ServiceProviders.ITERABLE, "com.mparticle.kits.IterableKit");
        kits.put(MParticle.ServiceProviders.SKYHOOK, "com.mparticle.kits.SkyhookKit");
        kits.put(MParticle.ServiceProviders.SINGULAR, "com.mparticle.kits.SingularKit");
        kits.put(MParticle.ServiceProviders.ADOBE, "com.mparticle.kits.AdobeKit");
        kits.put(MParticle.ServiceProviders.TAPLYTICS, "com.mparticle.kits.TaplyticsKit");
        kits.put(MParticle.ServiceProviders.OPTIMIZELY, "com.mparticle.kits.OptimizelyKit");
        kits.put(MParticle.ServiceProviders.RESPONSYS, "com.mparticle.kits.ResponsysKit");
        kits.put(MParticle.ServiceProviders.CLEVERTAP, "com.mparticle.kits.CleverTapKit");
        kits.put(MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE, "com.mparticle.kits.GoogleAnalyticsFirebaseKit");
        kits.put(MParticle.ServiceProviders.GOOGLE_ANALYTICS_FIREBASE_GA4, "com.mparticle.kits.GoogleAnalyticsFirebaseGA4Kit");
        kits.put(MParticle.ServiceProviders.PILGRIM, "com.mparticle.kits.PilgrimKit");
        kits.put(MParticle.ServiceProviders.ONETRUST, "com.mparticle.kits.OneTrustKit");
        kits.put(MParticle.ServiceProviders.SWRVE, "com.mparticle.kits.SwrveKit");
        kits.put(MParticle.ServiceProviders.BLUESHIFT, "com.mparticle.kits.BlueshiftKit");
        kits.put(MParticle.ServiceProviders.NEURA, "com.mparticle.kits.NeuraKit");
        for (Map.Entry<Integer, MPSideloadedKit> entry : sideloadedKits.entrySet()) {
            kits.put(entry.getKey(), ((MPSideloadedKit) entry.getValue()).getKit().getClass().getName());
        }
        return kits;
    }

    public KitIntegration createInstance(KitManagerImpl manager, KitConfiguration configuration) throws JSONException, ClassNotFoundException {
        KitIntegration kit = null;
        if (configuration.getKitId() >= minSideloadedKitId) {
            kit = retrieveSideloadedKit(manager, configuration);
        } else {
            kit = createInstance(manager, configuration.getKitId());
        }
        if (kit != null) {
            kit.setConfiguration(configuration);
        }
        return kit;
    }

    private KitIntegration retrieveSideloadedKit(KitManagerImpl manager, KitConfiguration configuration) {
        try {
            KitIntegration kit = (KitIntegration) sideloadedKits.get(configuration.getKitId()).getKit();
            kit.setKitManager(manager);
            return kit;
        } catch (Exception e) {
            return null;
        }
    }

    public KitIntegration createInstance(KitManagerImpl manager, int moduleId) throws JSONException, ClassNotFoundException {
        if (!supportedKits.isEmpty()) {
            try {
                Constructor<KitIntegration> constructor = supportedKits.get(moduleId).getConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance()
                        .setKitManager(manager);
            } catch (Exception e) {
                Logger.debug(e, "Failed to create Kit with ID: " + moduleId);
            }
        }
        return null;
    }

    private void mergeIntegrations(MParticleOptions options) {
        for (MPSideloadedKit entry : options.getSideloadedKits()) {
            if (entry.getKit() instanceof KitIntegration && !getKnownIntegrations().containsKey(((KitIntegration) entry.getKit()).getConfiguration().getKitId())) {
                sideloadedKits.put(((KitIntegration) entry.getKit()).getConfiguration().getKitId(), entry);
            }
        }
    }


    private void loadIntegrations() {
        Map<Integer, String> knownIntegrations = getKnownIntegrations();
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
        return supportedKits.containsKey(kitModuleId);
    }
}
