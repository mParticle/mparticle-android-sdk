package com.mparticle.kits;

import android.app.Activity;

import com.mparticle.MParticle;
import com.mparticle.kits.AbstractKit;
import com.mparticle.kits.ActivityLifecycleForwarder;
import com.mparticle.kits.ReportingMessage;
import com.wootric.androidsdk.Wootric;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WootricKit extends AbstractKit implements ActivityLifecycleForwarder {
    private static final String WOOTRIC_HOST = "wootric.com";

    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ACCOUNT_TOKEN = "accountToken";

    String endUserEmail;
    HashMap<String, String> endUserProperties;

    Wootric wootric;

    @Override
    public String getName() {
        return "Wootric";
    }

    @Override
    public boolean isOriginator(String uri) {
        return uri != null && uri.toLowerCase().contains(WOOTRIC_HOST);
    }

    @Override
    protected AbstractKit update() {
        return this;
    }

    @Override
    public void setUserIdentity(String id, MParticle.IdentityType identityType) {
        if(MParticle.IdentityType.Email.equals(identityType)) {
            endUserEmail = id;
        }
    }

    @Override
    void setUserAttributes(JSONObject mUserAttributes) {
        prepareEndUserProperties(mUserAttributes);
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, int i) {
        wootric = Wootric.init(
                activity,
                properties.get(CLIENT_ID),
                properties.get(CLIENT_SECRET),
                properties.get(ACCOUNT_TOKEN));

        wootric.setProperties(endUserProperties);
        wootric.setEndUserEmail(endUserEmail);

        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity, int i) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity, int i) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity, int i) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity, int i) {
        return null;
    }

    private void prepareEndUserProperties(JSONObject propertiesJson) {
        if(endUserProperties == null) {
            endUserProperties = new HashMap<>();
        } else {
            endUserProperties.clear();
        }

        Iterator<String> iterator = propertiesJson.keys();
        while(iterator.hasNext()) {
            String key = iterator.next();

            try {
                String value = propertiesJson.getString(key);
                endUserProperties.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
