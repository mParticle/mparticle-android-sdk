package com.mparticle.identity;

import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.internal.AccessUtils;
import com.mparticle.testutils.BaseCleanStartedEachTest;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class MParticleUserDelegateTest extends BaseCleanStartedEachTest {
    MParticleUserDelegate mUserDelegate;

    @Before
    public void before() throws Exception {
        mUserDelegate = MParticle.getInstance().Identity().mUserDelegate;
    }

    @Test
    public void testSetGetUserIdentities() throws Exception {
        Map<Long, Map<MParticle.IdentityType, String>> attributes = new HashMap<Long, Map<MParticle.IdentityType, String>>();
        for (int i = 0; i < 5; i++) {
            Long mpid = ran.nextLong();
            Map<MParticle.IdentityType, String> pairs = new HashMap<MParticle.IdentityType, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < 3; j++) {
                MParticle.IdentityType identityType = MParticle.IdentityType.parseInt(mRandomUtils.randomInt(0, MParticle.IdentityType.values().length));
                String value = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 25));
                assertTrue(mUserDelegate.setUserIdentity(value, identityType, mpid));
                pairs.put(identityType, value);
            }
        }

        com.mparticle.internal.AccessUtils.awaitMessageHandler();

        Map<Long, Map<MParticle.IdentityType, String>> storedUsersTemp = new HashMap<Long, Map<MParticle.IdentityType, String>>();

        for (Map.Entry<Long, Map<MParticle.IdentityType, String>> user : attributes.entrySet()) {
            Map<MParticle.IdentityType, String> storedUserAttributes = mUserDelegate.getUserIdentities(user.getKey());
            storedUsersTemp.put(user.getKey(), storedUserAttributes);
            for (Map.Entry<MParticle.IdentityType, String> pairs : user.getValue().entrySet()) {
                Object currentAttribute = storedUserAttributes.get(pairs.getKey());
                if (currentAttribute == null) {
                    Log.e("Stuff", "more stuff");
                }
                assertEquals(storedUserAttributes.get(pairs.getKey()), pairs.getValue());
            }
        }
    }

    @Test
    public void testInsertRetrieveDeleteUserAttributes() throws Exception {
        // create and store
        Map<Long, Map<String, String>> attributes = new HashMap<Long, Map<String, String>>();
        for (int i = 0; i < 5; i++) {
            Long mpid = ran.nextLong();
            Map<String, String> pairs = new HashMap<String, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < 3; j++) {
                String key = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55)).toUpperCase();
                String value = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55));
                assertTrue(mUserDelegate.setUserAttribute(key, value, mpid, false));
                pairs.put(key, value);
            }
        }

        AccessUtils.awaitMessageHandler();

        // retrieve and compare
        for (Map.Entry<Long, Map<String, String>> user : attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(user.getKey());
            for (Map.Entry<String, String> pairs : user.getValue().entrySet()) {
                assertEquals(storedUserAttributes.get(pairs.getKey()).toString(), pairs.getValue());
            }
        }

        // delete
        for (Map.Entry<Long, Map<String, String>> userAttributes : attributes.entrySet()) {
            for (Map.Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                assertTrue(mUserDelegate.removeUserAttribute(attribute.getKey(), userAttributes.getKey()));
            }
        }

        AccessUtils.awaitMessageHandler();

        for (Map.Entry<Long, Map<String, String>> userAttributes : attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(userAttributes.getKey());
            for (Map.Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                assertNull(storedUserAttributes.get(attribute.getKey()));
            }
        }
    }

    @Test
    public void testSetConsentState() throws Exception {
        Long mpid = ran.nextLong();
        Long mpid2 = ran.nextLong();
        ConsentState state = mUserDelegate.getConsentState(mpid);
        assertNotNull(state);
        assertNotNull(state.getGDPRConsentState());
        assertEquals(0, state.getGDPRConsentState().size());

        ConsentState.Builder builder = ConsentState.builder();
        builder.addGDPRConsentState("foo", GDPRConsent.builder(true).build());
        mUserDelegate.setConsentState(builder.build(), mpid);
        builder.addGDPRConsentState("foo2", GDPRConsent.builder(true).build());
        mUserDelegate.setConsentState(builder.build(), mpid2);

        assertEquals(1, mUserDelegate.getConsentState(mpid).getGDPRConsentState().size());
        assertTrue(mUserDelegate.getConsentState(mpid).getGDPRConsentState().containsKey("foo"));

        assertEquals(2, mUserDelegate.getConsentState(mpid2).getGDPRConsentState().size());
        assertTrue(mUserDelegate.getConsentState(mpid2).getGDPRConsentState().containsKey("foo"));
        assertTrue(mUserDelegate.getConsentState(mpid2).getGDPRConsentState().containsKey("foo2"));
    }

    @Test
    public void testRemoveConsentState() throws Exception {
        Long mpid = ran.nextLong();
        ConsentState state = mUserDelegate.getConsentState(mpid);
        assertNotNull(state);
        assertNotNull(state.getGDPRConsentState());
        assertEquals(0, state.getGDPRConsentState().size());

        ConsentState.Builder builder = ConsentState.builder();
        builder.addGDPRConsentState("foo", GDPRConsent.builder(true).build());
        mUserDelegate.setConsentState(builder.build(), mpid);

        assertEquals(1, mUserDelegate.getConsentState(mpid).getGDPRConsentState().size());
        assertTrue(mUserDelegate.getConsentState(mpid).getGDPRConsentState().containsKey("foo"));
        mUserDelegate.setConsentState(null, mpid);
        assertEquals(0, mUserDelegate.getConsentState(mpid).getGDPRConsentState().size());

    }
}
