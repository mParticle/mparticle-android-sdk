package com.mparticle.internal.embedded;

import android.content.Context;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Created by sdozor on 3/13/15.
 */
public class BaseEmbeddedKitFactory {
    protected EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
        return null;
    }

    public static ArrayList<Integer> getSupportedKits() {
        return new ArrayList<Integer>();
    }
}
