package com.mparticle.kits;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class FilteredIdentityRequestTest extends BaseKitManagerStarted {

    private TestKit kitInstance;

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
    public void testFilteredIdentityRequest() throws Exception {
        MParticleUser currentUser = MParticle.getInstance().Identity().getCurrentUser();
        Long mpid = currentUser.getId();

        AccessUtils.setUserIdentity("identity1", MParticle.IdentityType.Other, mpid);
        AccessUtils.setUserIdentity("identity2", MParticle.IdentityType.Microsoft, mpid);
        AccessUtils.setUserIdentity("identity3", MParticle.IdentityType.Other3, mpid);
        AccessUtils.setUserIdentity("identity4", MParticle.IdentityType.Facebook, mpid);
        AccessUtils.setUserIdentity("identity5", MParticle.IdentityType.CustomerId, mpid);
        AccessUtils.setUserIdentity("identity6", MParticle.IdentityType.Alias, mpid);

        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .userIdentity(MParticle.IdentityType.Other, "identity1")
                .userIdentity(MParticle.IdentityType.Microsoft, "identity2")
                .userIdentity(MParticle.IdentityType.Other3, "identity3")
                .userIdentity(MParticle.IdentityType.Facebook, "identity4")
                .userIdentity(MParticle.IdentityType.CustomerId, "identity5")
                .userIdentity(MParticle.IdentityType.Alias, "identity6")
                .build();
        assertEquals(request.getOldIdentities().size(), 6);
        assertEquals(request.getUserIdentities().size(), 6);
        FilteredIdentityApiRequest filteredIdentityApiRequest = new FilteredIdentityApiRequest(request, kitInstance);
        assertEquals(filteredIdentityApiRequest.getOldIdentities().size(), 4);
        assertEquals(filteredIdentityApiRequest.getNewIdentities().size(), 4);
    }





    public static class TestKit extends BaseTestKit  {

        public TestKit() {
            super();
        }
    }
}
