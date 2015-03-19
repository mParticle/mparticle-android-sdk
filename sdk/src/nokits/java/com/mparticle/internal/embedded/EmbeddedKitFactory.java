package com.mparticle.internal.embedded;


import android.content.Context;

import com.mparticle.MParticle;

import org.json.JSONException;

import java.util.ArrayList;

public class EmbeddedKitFactory {

    protected EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
        return null;

    }

    public static ArrayList<Integer> getSupportedKits() {
        return new ArrayList<Integer>();
    }

    public boolean isSupported(int kitModuleId) {
        return false;
    }
}
