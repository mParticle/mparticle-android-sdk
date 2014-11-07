package com.mparticle.internal.embedded;

import android.content.Context;


import org.json.JSONException;

/**
 * Created by sdozor on 8/18/14.
 */
class EmbeddedKitFactory extends EmbeddedKitManager.BaseEmbeddedKitFactory {
    protected EmbeddedProvider createInstance(int id, Context context) throws JSONException, ClassNotFoundException{
        return super.createInstance(id, context);
    }
}
