package com.mparticle;

import org.json.JSONObject;

/**
 * Created by sdozor on 3/14/14.
 */
public interface IEmbeddedKit {
    void logEvent(MParticle.EventType type, String name, JSONObject eventAttributes) throws Exception;
    void logTransaction(MPTransaction transaction) throws Exception;
    void logScreen(String screenName, JSONObject eventAttributes) throws Exception;
}
