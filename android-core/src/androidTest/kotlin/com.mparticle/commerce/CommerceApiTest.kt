package com.mparticle.commerce;

import com.mparticle.MParticle;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class CommerceApiTest extends BaseCleanStartedEachTest {


    //just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    public void testCommerceProductEvent() throws InterruptedException {
        Product product = new Product.Builder("name", "sku", 10.00)
                .build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.DETAIL, product)
                .build();
        MParticle.getInstance().logEvent(commerceEvent);
        MParticle.getInstance().upload();
        verifyEventSent();
    }

    //just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    public void testCommercePromotionEvent() throws InterruptedException {
        Promotion promotion = new Promotion()
                .setName("name")
                .setId("123");
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Promotion.CLICK, promotion)
                .build();
        MParticle.getInstance().logEvent(commerceEvent);
        MParticle.getInstance().upload();
        verifyEventSent();
    }

    //just verify that we can log an event and it will get sent to the server. Not testing the event message
    @Test
    public void testCommerceImpressionEvent() throws InterruptedException {
        Product product = new Product.Builder("name", "sku", 10.00)
                .build();
        Impression impression = new Impression("my impression", product);
        CommerceEvent commerceEvent = new CommerceEvent.Builder(impression)
                .build();
        MParticle.getInstance().logEvent(commerceEvent);
        MParticle.getInstance().upload();
        verifyEventSent();
    }

    private void verifyEventSent() throws InterruptedException {
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl())
                .bodyMatch(new MockServer.JSONMatch() {
                    @Override
                    public boolean isMatch(JSONObject jsonObject) {
                        boolean found = false;
                        JSONArray messagesJSON = jsonObject.optJSONArray("msgs");
                        if (messagesJSON != null) {
                            for (int i = 0; i < messagesJSON.length(); i++) {
                                JSONObject messageJSON = messagesJSON.optJSONObject(i);
                                if (messageJSON != null) {
                                    String type = messageJSON.optString("dt");
                                    if ("cm".equals(type)) {
                                        found = true;
                                    }
                                }
                            }
                        }
                        return found;
                    }
                }));
    }

}
