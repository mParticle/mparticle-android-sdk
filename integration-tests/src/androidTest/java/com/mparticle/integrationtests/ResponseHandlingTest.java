package com.mparticle.integrationtests;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/** How the SDK handles server responses that differ from the happy path. */
@RunWith(AndroidJUnit4.class)
public class ResponseHandlingTest extends BaselineTest {

    /** An accepted batch must be sent exactly once, even when the 2xx has no JSON body. */
    @Test
    public void eventsAcceptedWithEmptyBody() throws Exception {
        WireMockAdmin.stub(new JSONObject()
                .put("priority", 1)
                .put("request", new JSONObject()
                        .put("method", "POST")
                        .put("urlPathPattern", "/v2/[^/]+/events"))
                .put("response", new JSONObject().put("status", 202)));
        MParticle mParticle = start();
        mParticle.logEvent(new MPEvent.Builder("Empty Response Event").build());
        uploadAndVerify();
    }
}
