package com.mparticle.identity;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
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

public class MParticleUserDelegateTest {
    Context mContext;
    MParticleUserDelegate mUserDelegate;
    static RandomUtils mRandom;
    private CountDownLatch latch;
    int delaySeconds = 5;

    @BeforeClass
    public static void preConditions() {
        Looper.prepare();
        mRandom = RandomUtils.getInstance();
        InstrumentationRegistry.getTargetContext().deleteDatabase(MParticleDatabaseHelper.DB_NAME);
    }

    @Before
    public void setup() {
        latch = new CountDownLatch(1);
        mContext = InstrumentationRegistry.getContext();
        MParticle.setInstance(null);
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        mUserDelegate = MParticle.getInstance().Identity().mUserDelegate;
    }

    @Test
    public void testSetGetUserIdentities() throws Exception {
        Map<Long, Map<MParticle.IdentityType, String>> attributes = new HashMap<Long, Map<MParticle.IdentityType, String>>();
        for (int i = 0; i < 10; i++) {
            Long mpid = new Random().nextLong();
            Map<MParticle.IdentityType, String> pairs = new HashMap<MParticle.IdentityType, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < mRandom.randomInt(1, 55); j++) {
                MParticle.IdentityType identityType = MParticle.IdentityType.parseInt(mRandom.randomInt(0, 9));
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255));
                assertTrue(mUserDelegate.setUserIdentity(value, identityType, mpid));
                pairs.put(identityType, value);
            }
        }

        latch.await(delaySeconds, TimeUnit.SECONDS);

        Map<Long, Map<MParticle.IdentityType, String>> storedUsersTemp = new HashMap<Long, Map<MParticle.IdentityType, String >>();

        for (Map.Entry<Long, Map<MParticle.IdentityType, String>> user: attributes.entrySet()) {
            Map<MParticle.IdentityType, String> storedUserAttributes = mUserDelegate.getUserIdentities(user.getKey());
            storedUsersTemp.put(user.getKey(), storedUserAttributes);
            for (Map.Entry<MParticle.IdentityType, String> pairs: user.getValue().entrySet()) {
                Object currentAttribute = storedUserAttributes.get(pairs.getKey());
                if (currentAttribute == null) {
                    Log.e("Stuff","more stuff");
                }
                assertEquals(storedUserAttributes.get(pairs.getKey()), pairs.getValue());
            }
        }
    }

    @Test
    public void testInsertRetreiveDeleteUserAttributes() throws Exception {
        Map<Long, Map<String, String>> attributes = new HashMap<Long, Map<String, String>>();
        // create and store
        for (int i = 0; i < 10; i++) {
            Long mpid = new Random().nextLong();
            Map<String, String> pairs = new HashMap<String, String>();
            attributes.put(mpid, pairs);
            for (int j = 0; j < mRandom.randomInt(1, 55); j++) {
                String key = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255)).toUpperCase();
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255));
                assertTrue(mUserDelegate.setUserAttribute(key, value, mpid, true));
                pairs.put(key, value);
            }
        }

        // retrieve and compare
        for (Map.Entry<Long, Map<String, String>> user: attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(user.getKey());
            for (Map.Entry<String, String> pairs: user.getValue().entrySet()) {
                assertEquals(storedUserAttributes.get(pairs.getKey()).toString(), pairs.getValue());
            }
        }

        // delete
        for (Map.Entry<Long, Map<String, String>> userAttributes: attributes.entrySet()) {
            for (Map.Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                mUserDelegate.removeUserAttribute(attribute.getKey(), userAttributes.getKey());
            }
        }

        latch.await(delaySeconds, TimeUnit.SECONDS);

        for (Map.Entry<Long, Map<String, String>> userAttributes: attributes.entrySet()) {
            Map<String, Object> storedUserAttributes = mUserDelegate.getUserAttributes(userAttributes.getKey());
            for (Map.Entry<String, String> attribute : userAttributes.getValue().entrySet()) {
                assertNull(storedUserAttributes.get(attribute.getKey()));
            }
        }
    }
}
