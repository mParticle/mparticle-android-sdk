package com.mparticle.internal.embedded;

import android.content.Intent;
import android.location.Location;

import com.mparticle.MParticle;
import com.mparticle.MPProduct;

import org.json.JSONObject;

/**
 * Created by sdozor on 3/14/14.
 */
interface IEmbeddedKit {
    void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) throws Exception;
    void logTransaction(MPProduct transaction) throws Exception;
    void logScreen(String screenName, JSONObject eventAttributes) throws Exception;
    void setLocation(Location location);
    void setUserAttributes(JSONObject mUserAttributes);
    void removeUserAttribute(String key);
    void setUserIdentity(String id, MParticle.IdentityType identityType);
    void logout();
    void removeUserIdentity(String id);
    void handleIntent(Intent intent);
}
