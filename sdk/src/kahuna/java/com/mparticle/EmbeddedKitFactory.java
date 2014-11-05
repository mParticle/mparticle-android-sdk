package com.mparticle;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by sdozor on 8/18/14.
 */
class EmbeddedKitFactory extends EmbeddedKitManager.BaseEmbeddedKitFactory {
    private final static int KAHUNA = 56;

    protected EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
        switch (id){
            case KAHUNA:
                return new EmbeddedKahuna(context);
        }
        return super.createInstance(id, context);
    }

    public static ArrayList<Integer> getSupportedKits(){
        ArrayList<Integer> supportedKitIds = new ArrayList<Integer>();
        supportedKitIds.add(MAT);
        supportedKitIds.add(KOCHAVA);
        supportedKitIds.add(COMSCORE);
        supportedKitIds.add(KAHUNA);
        return supportedKitIds;
    }
}