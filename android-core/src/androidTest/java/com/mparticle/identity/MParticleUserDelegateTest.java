package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.MParticleTest;
import com.mparticle.commerce.Product;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.tables.mp.BaseTableTest;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
import com.mparticle.internal.dto.MParticleUserDTO;
import com.mparticle.internal.dto.UserAttributeRemoval;
import com.mparticle.utils.RandomUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public void testSetUser() throws Exception {
        List<MParticleUserDTO> users = new ArrayList<MParticleUserDTO>();
        //create and store UserAttributes;
        for (int i = 0; i < 10; i++) {
            Map<MParticle.IdentityType, String> identities = new HashMap<MParticle.IdentityType, String>();
            Long mpid = new Random().nextLong();
            for (int j = 0; j < mRandom.randomInt(1, 55); j++) {
                MParticle.IdentityType identityType = MParticle.IdentityType.parseInt(mRandom.randomInt(0, 9));
                String value = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255));
                identities.put(identityType, value);
            }
            Map<String, Object> attributes = new HashMap<String, Object>();
            if (new Random().nextBoolean()) {
                for (int j = 0; j < mRandom.randomInt(1, 55); j++) {
                    String key = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255)).toUpperCase();
                    String value = mRandom.getAlphaNumericString(mRandom.randomInt(1, 255));
                    attributes.put(key, value);
                }
            }
            MParticleUserDTO userDto;
            if (attributes != null) {
                userDto = new MParticleUserDTO(mpid, identities, attributes);
            } else {
                userDto = new MParticleUserDTO(mpid, identities);
            }
            users.add(userDto);
            assertTrue(mUserDelegate.setUser(userDto));
        }

        latch.await(delaySeconds, TimeUnit.SECONDS);

        //check for stored user attribuites
        for (MParticleUserDTO userDTO: users) {
            Map<MParticle.IdentityType, String> storedUserIdentitites = mUserDelegate.getUserIdentities(userDTO.getMpId());
            for (Map.Entry<MParticle.IdentityType, String> identity: userDTO.getIdentities().entrySet()) {
                assertEquals(storedUserIdentitites.get(identity.getKey()), identity.getValue());

            }
            if (!MPUtility.isEmpty(userDTO.getUserAttributes())) {
                Map<String, Object> storedUserAttribtues = mUserDelegate.getUserAttributes(userDTO.getMpId());
                for (Map.Entry<String, Object> attribute : userDTO.getUserAttributes().entrySet()) {
                    assertEquals(storedUserAttribtues.get(attribute.getKey()), attribute.getValue());
                }
            }
        }
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
