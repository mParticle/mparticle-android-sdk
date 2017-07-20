package com.mparticle.identity;

import com.mparticle.MParticle;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AccessUtils {

    public static void clearIdentityApiClient() {
        MParticle.getInstance().Identity().setApiClient(null, false);
    }

    public static void setIdentityApiClient(MParticleIdentityClient identityClient, boolean overrideNextInstance) {
        IdentityApi.setApiClient(identityClient, overrideNextInstance);
    }

    public static void setUserIdentity(String value, MParticle.IdentityType identityType, long mpid) {
        MParticle.getInstance().Identity().mUserDelegate.setUserIdentity(value, identityType, mpid);
    }

    public static class IdentityApiClient implements MParticleIdentityClient {
        CountDownLatch mLatch;

        public IdentityApiClient() {}

        public IdentityApiClient(CountDownLatch latch) {
            this.mLatch  = latch;
        }

        @Override
        public IdentityHttpResponse login(IdentityApiRequest request) throws Exception {
            tryCountdown();
            return null;
        }

        @Override
        public IdentityHttpResponse logout(IdentityApiRequest request) throws Exception {
            tryCountdown();
            return null;
        }

        @Override
        public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
            tryCountdown();
            return null;
        }

        @Override
        public IdentityHttpResponse modify(IdentityApiRequest request) throws Exception {
            tryCountdown();
            return null;
        }

        private void tryCountdown() {
            if (mLatch != null) {
                mLatch.countDown();
            }
        }
    }

    public static void clearUserIdentities(MParticleUser user) {
        Map<MParticle.IdentityType, String> nullUserIdentities = new HashMap<MParticle.IdentityType, String>();
        for (MParticle.IdentityType identityType : MParticle.IdentityType.values()) {
            nullUserIdentities.put(identityType, null);
        }
        user.setUserIdentities(nullUserIdentities);
    }
}
