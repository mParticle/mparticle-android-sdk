package com.mparticle.internal.embedded;

import android.content.Intent;
import android.location.Location;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.MPProduct;

import org.json.JSONObject;

import java.util.Map;

interface IEmbeddedKit {
    void logEvent(MPEvent event, Map<String, String> attributes) throws Exception;
    void logTransaction(MPProduct transaction) throws Exception;
    void logScreen(String screenName, Map<String, String> eventAttributes) throws Exception;
    void setLocation(Location location);
    void setUserAttributes(JSONObject mUserAttributes);
    void removeUserAttribute(String key);
    void setUserIdentity(String id, MParticle.IdentityType identityType);
    void logout();
    void removeUserIdentity(String id);
    void handleIntent(Intent intent);
    void startSession();
    void endSession();
}
