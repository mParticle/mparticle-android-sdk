package com.mparticle.identity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;
import com.mparticle.consent.ConsentState;
import com.mparticle.mock.AbstractMParticleUser;
import com.mparticle.testutils.RandomUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static junit.framework.TestCase.assertTrue;

public class IdentityApiTest {
    RandomUtils randomUtils = new RandomUtils();
    Random ran = new Random();

    //IdentityApi should order their users from greatest lastSeenTime to least
    @Test
    public void testUserOrder() {
        IdentityApi identityApi = new IdentityApi();

        List<MParticleUser> users = new ArrayList<MParticleUser>();
        for (int i = 0; i < 10; i++) {
            users.add(new MockUser(ran.nextLong(), ran.nextLong()));
        }

        identityApi.sortUsers(users);

        long lastTime = Long.MAX_VALUE;
        for (MParticleUser user: users) {
            assertTrue(user.getLastSeenTime() <= lastTime);
            lastTime = user.getLastSeenTime();
        }

    }

    class MockUser extends AbstractMParticleUser {
        long mpid, lastSeenTime;

        MockUser(long mpid, long lastSeenTime) {
            this.mpid = mpid;
            this.lastSeenTime = lastSeenTime;
        }

        @Override
        public long getLastSeenTime() {
            return lastSeenTime;
        }

        @Override
        public long getId() {
            return mpid;
        }
    }
}
