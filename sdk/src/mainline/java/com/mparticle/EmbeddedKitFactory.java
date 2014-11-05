package com.mparticle;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by sdozor on 8/18/14.
 */
class EmbeddedKitFactory extends EmbeddedKitManager.BaseEmbeddedKitFactory {
    protected EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
        return super.createInstance(id, context);
    }
    public static ArrayList<Integer> getSupportedKits(){
        ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();
        supportedKitIds.add(MAT);
        supportedKitIds.add(KOCHAVA);
        supportedKitIds.add(COMSCORE);
        return supportedKitIds;
    }
}
