package com.mparticle.kits;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalKit {

    private KitIntegration kit;
    private KitConfiguration configuration;

    public LocalKit() {
        try {
            configuration = KitConfiguration.createKitConfiguration(getMinimumObject());
        } catch (Exception e) {}
    }

    private JSONObject getMinimumObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", KitIntegrationFactory.generateRandomKey());
        return obj;
    }

    public KitIntegration getKit() {
        return kit;
    }

    public KitConfiguration getConfiguration() {
        return configuration;
    }
}
