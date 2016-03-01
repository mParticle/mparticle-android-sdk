package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;

import com.mparticle.MParticle;
import com.wootric.androidsdk.Wootric;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WootricKit extends KitIntegration implements KitIntegration.AttributeListener {
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String ACCOUNT_TOKEN = "accountToken";

    String endUserEmail;
    String endUserCustomerId;
    HashMap<String, String> endUserProperties;

    Wootric wootric;

    @Override
    public Wootric getInstance() {
        WeakReference<Activity> activityWeakReference = getCurrentActivity();
        if (activityWeakReference != null) {
            Activity activity = activityWeakReference.get();
            if (activity != null) {
                wootric = Wootric.init(
                        activity,
                        getSettings().get(CLIENT_ID),
                        getSettings().get(CLIENT_SECRET),
                        getSettings().get(ACCOUNT_TOKEN));

                wootric.setProperties(endUserProperties);
                setWootricIdentity(wootric);
            }
        }

        return wootric;
    }

    @Override
    public String getName() {
        return "Wootric";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        getInstance();
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        return null;
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String id) {
        if(MParticle.IdentityType.Email.equals(identityType)) {
            endUserEmail = id;
        }else if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            endUserCustomerId = id;
        }
        if (wootric != null) {
            setWootricIdentity(wootric);
        }
    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if(MParticle.IdentityType.Email.equals(identityType)) {
            endUserEmail = null;
        }else if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            endUserCustomerId = null;
        }
        if (wootric != null) {
            setWootricIdentity(wootric);
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    private void setWootricIdentity(Wootric wootric) {
        boolean endUserEmailSet = (endUserEmail != null && !endUserEmail.isEmpty());
        String endUserIdentifier = endUserEmailSet ? endUserEmail : endUserCustomerId;
        wootric.setEndUserEmail(endUserIdentifier);
    }

    @Override
    public void setUserAttribute(String key, String value) {
        prepareEndUserProperties(key, value);
        if (wootric != null) {
            wootric.setProperties(endUserProperties);
        }
    }

    @Override
    public void removeUserAttribute(String key) {
        if (wootric != null && endUserProperties != null && endUserProperties.remove(KitUtils.sanitizeAttributeKey(key)) != null) {
            wootric.setProperties(endUserProperties);
        }
    }

    private void prepareEndUserProperties(String key, String value) {
        if(endUserProperties == null) {
            endUserProperties = new HashMap<>();
        }
        endUserProperties.put(KitUtils.sanitizeAttributeKey(key), value);
    }
}
