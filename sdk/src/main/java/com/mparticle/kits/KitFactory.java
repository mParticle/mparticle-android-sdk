package com.mparticle.kits;


import com.mparticle.BuildConfig;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;

import org.json.JSONException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class KitFactory {
    final static int APPBOY = MParticle.ServiceProviders.APPBOY;
    final static int FORESEE = MParticle.ServiceProviders.FORESEE_ID;
    final static int ADJUST = MParticle.ServiceProviders.ADJUST;
    final static int KOCHAVA = MParticle.ServiceProviders.KOCHAVA;
    final static int COMSCORE = MParticle.ServiceProviders.COMSCORE;
    final static int KAHUNA = MParticle.ServiceProviders.KAHUNA;
    final static int BRANCH_METRICS = MParticle.ServiceProviders.BRANCH_METRICS;
    final static int LOCALYTICS = MParticle.ServiceProviders.LOCALYTICS;
    final static int FLURRY = MParticle.ServiceProviders.FLURRY;
    final static int WOOTRIC = MParticle.ServiceProviders.WOOTRIC;
    final static int CRITTERCISM = MParticle.ServiceProviders.CRITTERCISM;
    final static int TUNE = MParticle.ServiceProviders.TUNE;
    final static int APPSFLYER = MParticle.ServiceProviders.APPSFLYER;
    final static int APPTENTIVE = MParticle.ServiceProviders.APPTENTIVE;
    private Map<Integer, AbstractKit> supportedKits = new HashMap<Integer, AbstractKit>();
    private ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();

    public KitFactory(KitManager ekManager) {
        loadIntegrations(ekManager);
    }

    AbstractKit createInstance(int id) throws JSONException, ClassNotFoundException{
        return supportedKits.get(id);
    }
    private void loadIntegrations(KitManager ekManager) {
        if (loadKit(ekManager, ADJUST, "com.mparticle.kits.AdjustKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Adjust Kit detected.");
        }
        if (loadKit(ekManager, APPBOY, "com.mparticle.kits.AppboyKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Appboy Kit detected.");
        }
        if (loadKit(ekManager, BRANCH_METRICS, "com.mparticle.kits.BranchMetricsKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Branch Metrics Kit detected.");
        }
        if (loadKit(ekManager, COMSCORE, "com.mparticle.kits.ComscoreKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Comscore Kit detected.");
        }
        if (loadKit(ekManager, KAHUNA, "com.mparticle.kits.KahunaKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Kahuna Kit detected.");
        }
        if (loadKit(ekManager, KOCHAVA, "com.mparticle.kits.KochavaKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Kochava Kit detected.");
        }
        if (loadKit(ekManager, FORESEE, "com.mparticle.kits.ForeseeKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Foresee Kit detected.");
        }
        if (loadKit(ekManager, LOCALYTICS, "com.mparticle.kits.LocalyticsKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Localytics Kit detected.");
        }
        if (loadKit(ekManager, FLURRY, "com.mparticle.kits.FlurryKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Flurry Kit detected.");
        }
        if (loadKit(ekManager, WOOTRIC, "com.mparticle.kits.WootricKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Wootric Kit detected.");
        }
        if (loadKit(ekManager, CRITTERCISM, "com.mparticle.kits.CrittercismKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Crittercism Kit detected.");
        }
        if (loadKit(ekManager, TUNE, "com.mparticle.kits.TuneKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Tune Kit detected.");
        }
        if (loadKit(ekManager, APPSFLYER, "com.mparticle.kits.AppsFlyerKit")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "AppsFlyer Kit detected.");
        }
        if (loadKit(ekManager, APPTENTIVE, "com.mparticle.kits.Apptentive")) {
            ConfigManager.log(MParticle.LogLevel.DEBUG, "Apptentive Kit detected.");
        }
    }

    private boolean loadKit(KitManager ekManager, int kitId, String className) {
        try {
            Class clazz = Class.forName(className);
            try {
                Constructor<AbstractKit> constructor = clazz.getConstructor();
                AbstractKit kit = constructor.newInstance()
                        .setKitManager(ekManager)
                        .setId(kitId);
                supportedKits.put(kitId, kit);
                return supportedKitIds.add(kitId);
            } catch (Exception e) {
                if (BuildConfig.MP_DEBUG) {
                    ConfigManager.log(MParticle.LogLevel.DEBUG, "Could not create " + clazz.getCanonicalName() + ".\n" + e);
                }
            }
        } catch (ClassNotFoundException e) {
            if (BuildConfig.MP_DEBUG) {
                ConfigManager.log(MParticle.LogLevel.DEBUG, "Kit not bundled: ", className);
            }
        }
        return false;
    }

    public ArrayList<Integer> getSupportedKits() {
        return supportedKitIds;
    }

    public boolean isSupported(int kitModuleId) {
        return supportedKits.containsKey(kitModuleId);
    }
}
