package com.mparticle.internal.embedded;


import com.mparticle.MParticle;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class EmbeddedKitFactory {
    final static int APPBOY = MParticle.ServiceProviders.APPBOY;
    final static int FORESEE = MParticle.ServiceProviders.FORESEE_ID;
    final static int ADJUST = MParticle.ServiceProviders.ADJUST;
    final static int KOCHAVA = MParticle.ServiceProviders.KOCHAVA;
    final static int COMSCORE = MParticle.ServiceProviders.COMSCORE;
    final static int KAHUNA = MParticle.ServiceProviders.KAHUNA;
    final static int BRANCH_METRICS = MParticle.ServiceProviders.BRANCH_METRICS;

    EmbeddedProvider createInstance(int id, EmbeddedKitManager ekManager) throws JSONException, ClassNotFoundException{
        switch (id){
            case KOCHAVA:
                return new EmbeddedKochava(id, ekManager);
            case COMSCORE:
                return new EmbeddedComscore(id, ekManager);
            case KAHUNA:
                return new EmbeddedKahuna(id, ekManager);
            case FORESEE:
                return new EmbeddedForesee(id, ekManager);
            case ADJUST:
                return new EmbeddedAdjust(id, ekManager);
            case APPBOY:
                return new EmbeddedAppboy(id, ekManager);
            case BRANCH_METRICS:
                return new EmbeddedBranchMetrics(id, ekManager);
            default:
                return null;
        }
    }

    public static ArrayList<Integer> getSupportedKits() {
        ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();
        supportedKitIds.add(APPBOY);
        supportedKitIds.add(KOCHAVA);
        supportedKitIds.add(COMSCORE);
        supportedKitIds.add(KAHUNA);
        supportedKitIds.add(FORESEE);
        supportedKitIds.add(ADJUST);
        supportedKitIds.add(BRANCH_METRICS);
        return supportedKitIds;
    }

    public boolean isSupported(int kitModuleId) {
        switch (kitModuleId){
            case BRANCH_METRICS:
            case KOCHAVA:
            case COMSCORE:
            case KAHUNA:
            case FORESEE:
            case ADJUST:
            case APPBOY:
                return true;
        }
        return false;
    }
}
