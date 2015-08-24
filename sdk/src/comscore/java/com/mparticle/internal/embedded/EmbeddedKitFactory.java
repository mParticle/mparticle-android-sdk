package com.mparticle.internal.embedded;


import com.mparticle.MParticle;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class EmbeddedKitFactory {
    final static int FORESEE = MParticle.ServiceProviders.FORESEE_ID;
    final static int COMSCORE = MParticle.ServiceProviders.COMSCORE;

    EmbeddedProvider createInstance(int id, EmbeddedKitManager ekManager) throws JSONException, ClassNotFoundException{
        switch (id){
            case COMSCORE:
                return new EmbeddedComscore(ekManager);
            case FORESEE:
                return new EmbeddedForesee(ekManager);

            default:
                return null;
        }
    }

    public static ArrayList<Integer> getSupportedKits() {
        ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();
        supportedKitIds.add(COMSCORE);
        supportedKitIds.add(FORESEE);
        return supportedKitIds;
    }

    public boolean isSupported(int kitModuleId) {
        switch (kitModuleId){
            case COMSCORE:
            case FORESEE:
                return true;
        }
        return false;
    }
}
