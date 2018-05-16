package com.mparticle.identity;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.testutils.MParticleUtils;
import com.mparticle.testutils.RandomUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class MParticleUserDelegateTest extends BaseCleanStartedEachTest {
    Context mContext;
    MParticleUserDelegate mUserDelegate;
    RandomUtils mRandom;

    @Before
    public void before() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        mUserDelegate = MParticle.getInstance().Identity().mUserDelegate;
        mRandom = RandomUtils.getInstance();
    }

    @Test
    public void testSetGetUserIdentities() throws Exception {
        Map<Long, Map<MParticle.IdentityType, String>> attributes = new HashMap<Long, Map<MParticle.IdentityType, String>>();
        for (int i = 0; i < 10; i++) {
            Long mpid = new Random().nextLong();
            Map<MParticle.IdentityType, String> pairs = new HashMap<MParticle.IdentityType, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < mRandom.randomInt(1, 10); j++) {
                MParticle.IdentityType identityType = MParticle.IdentityType.parseInt(mRandom.randomInt(0, MParticle.IdentityType.values().length));
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255));
                assertTrue(mUserDelegate.setUserIdentity(value, identityType, mpid));
                pairs.put(identityType, value);
            }
        }

        MParticleUtils.awaitStoreMessage();

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
        for (int i = 0; i < mRandom.randomInt(1, 10); i++) {
            Long mpid = new Random().nextLong();
            Map<String, String> pairs = new HashMap<String, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < mRandom.randomInt(1, 20); j++) {
                String key = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255)).toUpperCase();
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255));
                assertTrue(mUserDelegate.setUserAttribute(key, value, mpid, false));
                pairs.put(key, value);
            }
        }

        MParticleUtils.awaitSetUserAttribute();

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

        MParticleUtils.awaitRemoveUserAttribute();
        
        for (Map.Entry<Long, Map<String, String>> userAttributes : attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(userAttributes.getKey());
            for (Map.Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                assertNull(storedUserAttributes.get(attribute.getKey()));
            }
        }
    }

    @Test
    public void testSetConsentState() throws Exception {
        Long mpid = new Random().nextLong();
        Long mpid2 = new Random().nextLong();
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
        Long mpid = new Random().nextLong();
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
