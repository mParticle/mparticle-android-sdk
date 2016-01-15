package com.mparticle.kits;

import android.app.Activity;

import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.wootric.androidsdk.Wootric;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class WootricKit extends AbstractKit {
    private static final String WOOTRIC_HOST = "wootric.com";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ACCOUNT_TOKEN = "accountToken";

    String endUserEmail;
    String endUserCustomerId;
    HashMap<String, String> endUserProperties;

    Wootric wootric;

    @Override
    public Wootric getInstance(Activity activity) {
        wootric = Wootric.init(
                activity,
                properties.get(CLIENT_ID),
                properties.get(CLIENT_SECRET),
                properties.get(ACCOUNT_TOKEN));

        wootric.setProperties(endUserProperties);
        setWootricIdentity(wootric);

        return wootric;
    }

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
        }else if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            endUserCustomerId = id;
        }
        if (wootric != null) {
            setWootricIdentity(wootric);
        }
    }

    private void setWootricIdentity(Wootric wootric) {
        boolean endUserEmailSet = (endUserEmail != null && !endUserEmail.isEmpty());
        String endUserIdentifier = endUserEmailSet ? endUserEmail : endUserCustomerId;
        wootric.setEndUserEmail(endUserIdentifier);
    }

    @Override
    void setUserAttributes(JSONObject mUserAttributes) {
        prepareEndUserProperties(mUserAttributes);
        if (wootric != null) {
            wootric.setProperties(endUserProperties);
        }
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
                ConfigManager.log(MParticle.LogLevel.DEBUG, e.toString());
            }
        }
    }
}
