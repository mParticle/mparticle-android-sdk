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

    EmbeddedProvider createInstance(int id, EmbeddedKitManager ekManager) throws JSONException, ClassNotFoundException{
        switch (id){
            case KOCHAVA:
                return new EmbeddedKochava(ekManager);
            case COMSCORE:
                return new EmbeddedComscore(ekManager);
            case KAHUNA:
                return new EmbeddedKahuna(ekManager);
            case FORESEE:
                return new EmbeddedForesee(ekManager);
            case ADJUST:
                return new EmbeddedAdjust(ekManager);
            case APPBOY:
                return new EmbeddedAppboy(ekManager);
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
        return supportedKitIds;
    }

    public boolean isSupported(int kitModuleId) {
        switch (kitModuleId){
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
