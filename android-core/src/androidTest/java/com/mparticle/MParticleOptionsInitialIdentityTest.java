package com.mparticle;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.mparticle.identity.AccessUtils;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.utils.RandomUtils;
import com.mparticle.utils.TestingUtils;

import org.junit.Test;

import java.util.Map;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class MParticleOptionsInitialIdentityTest extends BaseCleanStartedEachTest {
    RandomUtils mRandomUtils = RandomUtils.getInstance();
    Context mContext;

    @Override
    protected void beforeClass() throws Exception {
    }

    @Override
    protected void before() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        MParticle.setInstance(null);
    }

    @Test
    public void testInitialIdentitiesPresent() throws Exception {
        for (int i = 0; i < 20; i++) {
            final boolean[] checked = new boolean[1];
            final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

            IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                    .userIdentities(identities)
                    .build();
            MParticleOptions options = MParticleOptions.builder(mContext)
                    .credentials("key", "value")
                    .identify(request)
                    .build();

            AccessUtils.setIdentityApiClient(new AccessUtils.IdentityApiClient() {
                @Override
                public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                    if (request.getUserIdentities().size() == 0 && identities.size() != 0) {
                        return null;
                    }
                    assertEquals(request.getUserIdentities().size(), identities.size());
                    for (Map.Entry<MParticle.IdentityType, String> entry : request.getUserIdentities().entrySet()) {
                        assertEquals(identities.get(entry.getKey()), entry.getValue());
                    }
                    checked[0] = true;
                    return null;
                }
            }, true);

            MParticle.start(options);

            TestingUtils.checkAllBool(checked, 1, 10);
            MParticle.setInstance(null);
        }
    }


    @Test
    public void testNoInitialIdentityNoStoredIdentity() throws Exception {
        final boolean[] checked = new boolean[1];

        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "value")
                .build();
        AccessUtils.setIdentityApiClient(new AccessUtils.IdentityApiClient() {
            @Override
            public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                assertTrue(request.getUserIdentities().size() == 0);
                checked[0] = true;
                return null;
            }
        }, true);
        MParticle.start(options);

        TestingUtils.checkAllBool(checked, 1, 10);
    }

    @Test
    public void testNoInitialIdentity() throws Exception {
        for (int i = 0; i < 10; i++) {
            final boolean[] checked = new boolean[1];

            final Long currentMpid = new Random().nextLong();
            final Map<MParticle.IdentityType, String> identities = mRandomUtils.getRandomUserIdentities();

            MParticleOptions options = MParticleOptions.builder(mContext)
                    .credentials("key", "value")
                    .build();
            MParticle.start(options);

            MParticle.getInstance().getConfigManager().setMpid(currentMpid);


            for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
                AccessUtils.setUserIdentity(entry.getValue(), entry.getKey(), currentMpid);
            }

            MParticle.setInstance(null);

            options = MParticleOptions.builder(mContext)
                    .credentials("key", "value")
                    .build();
            AccessUtils.setIdentityApiClient(new AccessUtils.IdentityApiClient() {
                @Override
                public IdentityHttpResponse identify(IdentityApiRequest request) throws Exception {
                    assertTrue(MParticle.getInstance().getConfigManager().getMpid() == currentMpid);
                    if (request.getUserIdentities().size() == identities.size()) {
                        for (Map.Entry<MParticle.IdentityType, String> entry : request.getUserIdentities().entrySet()) {
                            assertEquals(identities.get(entry.getKey()), entry.getValue());
                        }
                        checked[0] = true;
                    }
                    return null;
                }
            }, true);

            MParticle.start(options);

            TestingUtils.checkAllBool(checked, 1, 10);
        }



    }
}
