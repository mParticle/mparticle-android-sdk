package com.mparticle.internal.embedded;


import android.content.Context;

import com.mparticle.MParticle;

import org.json.JSONException;

import java.util.ArrayList;

public class EmbeddedKitFactory {
    private final static int KOCHAVA = 37;
    private final static int COMSCORE = 39;
    private final static int KAHUNA = 56;
    private final static int FORESEE = MParticle.ServiceProviders.FORESEE_ID;
    private final static int ADJUST = 68;

    final EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
        switch (id){
            case KOCHAVA:
                return new EmbeddedKochava(context);
            case COMSCORE:
                return new EmbeddedComscore(context);
            case KAHUNA:
                return new EmbeddedKahuna(context);
            case FORESEE:
                return new EmbeddedForesee(context);
            case ADJUST:
                return new EmbeddedAdjust(context);
            default:
                return null;
        }
    }

    public static ArrayList<Integer> getSupportedKits() {
        ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();
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
                return true;
        }
        return false;
    }
}
