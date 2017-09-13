package com.mparticle.identity;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.mparticle.BaseCleanStartedEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
import com.mparticle.utils.MParticleUtils;
import com.mparticle.utils.RandomUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class MParticleUserDelegateTest extends BaseCleanStartedEachTest {
    Context mContext;
    MParticleUserDelegate mUserDelegate;
    RandomUtils mRandom;

    @Override
    protected void beforeClass() throws Exception {
    }

    @Override
    protected void before() throws Exception {
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
                MParticle.IdentityType identityType = MParticle.IdentityType.parseInt(mRandom.randomInt(0, 9));
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
}
