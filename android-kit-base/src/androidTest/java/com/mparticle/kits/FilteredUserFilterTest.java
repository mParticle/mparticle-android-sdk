package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.MParticleUser;
import com.mparticle.utils.MParticleUtils;
import com.mparticle.utils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

public class FilteredUserFilterTest extends BaseKitManagerStarted {


    @Override
    protected Map<String, JSONObject> registerCustomKits() {
        Map<String, JSONObject> customKits = new HashMap<>();
        JSONObject userAttributesFilters = new JSONObject();
        try {
            userAttributesFilters = new JSONObject()
                    .put("hs", new JSONObject()
                                    .put("ua", new JSONObject()
                                            .put(String.valueOf(KitUtils.hashForFiltering("FilterThis")), 0)
                                            .put(String.valueOf(KitUtils.hashForFiltering("AlsoFilterThis")), 0)
                                            .put(String.valueOf(KitUtils.hashForFiltering("Don't filter this")), 1))
                                    .put("uid", new JSONObject()
                                            .put(String.valueOf(MParticle.IdentityType.Microsoft.getValue()), "0")
                                            .put(String.valueOf(MParticle.IdentityType.Other3.getValue()), "0")
                                            .put(String.valueOf(MParticle.IdentityType.CustomerId.getValue()), "1")
                                            .put(String.valueOf(MParticle.IdentityType.Email.getValue()), "1")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        customKits.put(TestKit.class.getName(), userAttributesFilters);
        return customKits;
    }

    TestKit kitInstance;

    @Override
    protected void before() throws Exception {
        super.before();
        TestKit.setOnKitCreate(new BaseTestKit.OnKitCreateListener() {
            @Override
            public void onKitCreate(BaseTestKit kitIntegration) {
                if (kitIntegration instanceof TestKit) {
                    kitInstance = (TestKit)kitIntegration;
                }
            }
        });
        long endTime = System.currentTimeMillis() + (10 * 1000);
        while (kitInstance == null && System.currentTimeMillis() < endTime) {}
        if (kitInstance == null) {
            fail("Kit not started");
        }
    }

    @Test
    public void testFilterUserAttributes() throws InterruptedException {
        MParticleUser currentUser = MParticle.getInstance().Identity().getCurrentUser();

        for (String attributeKey: currentUser.getUserAttributes().keySet()) {
            currentUser.setUserAttribute(attributeKey, null);
        }
        MParticleUtils.awaitSetUserAttribute();

        currentUser.setUserAttribute("FilterThis", "attribute");
        currentUser.setUserAttribute("AlsoFilterThis", "attribute1");
        currentUser.setUserAttribute("Don't filter this", "attribute2");
        currentUser.setUserAttribute("randomkey", "attribute3");
        currentUser.setUserAttribute("randomkey1", "attribute4");

        MParticleUtils.awaitSetUserAttribute();
        assertEquals(currentUser.getUserAttributes().size(), 5);
        assertEquals(kitInstance.getCurrentUser().getUserAttributes().size(), 3);
    }

    @Test
    public void testFilterUserIdentities() throws Exception {
        MParticleUser currentUser = MParticle.getInstance().Identity().getCurrentUser();
        Long mpid = currentUser.getId();

        AccessUtils.setUserIdentity("identity1", MParticle.IdentityType.Other, mpid);
        AccessUtils.setUserIdentity("identity2", MParticle.IdentityType.Microsoft, mpid);
        AccessUtils.setUserIdentity("identity3", MParticle.IdentityType.Other3, mpid);
        AccessUtils.setUserIdentity("identity4", MParticle.IdentityType.Facebook, mpid);
        AccessUtils.setUserIdentity("identity5", MParticle.IdentityType.CustomerId, mpid);
        AccessUtils.setUserIdentity("identity6", MParticle.IdentityType.Alias, mpid);

        assertEquals(currentUser.getUserIdentities().size(), 6);
        assertEquals(kitInstance.getCurrentUser().getUserIdentities().size(), 4);
        assertNull(kitInstance.getCurrentUser().getUserIdentities().get(MParticle.IdentityType.Microsoft));
        assertNull(kitInstance.getCurrentUser().getUserIdentities().get(MParticle.IdentityType.Other3));
    }

    public static class TestKit extends BaseTestKit {

        public TestKit() {super();}

        @Override
        public String getName() {
            return "testName";
        }

        @Override
        public List<ReportingMessage> setOptOut(boolean optedOut) {
            return null;
        }
    }
}
