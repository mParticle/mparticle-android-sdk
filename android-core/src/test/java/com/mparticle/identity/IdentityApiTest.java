package com.mparticle.identity;

import com.mparticle.internal.MessageManager;
import com.mparticle.mock.AbstractMParticleUser;
import com.mparticle.testutils.AndroidUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdentityApiTest {
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

    @Test
    public void testAliasUsersValidationRejection() {
        IdentityApi identityApi = new IdentityApi();
        identityApi.mMessageManager = new MessageManager() {
            @Override
            public void logAliasRequest(AliasRequest aliasRequest) {
                fail("should not logged Alias Request:\n" + aliasRequest.toString());
            }
        };

        //missing previousMpid
        AliasRequest request = AliasRequest.builder()
                .destinationMpid(123)
                .startTime(1)
                .endTime(2)
                .build();
        assertFalse(identityApi.aliasUsers(request));

        //missing newMpid
        request = AliasRequest.builder()
                .sourceMpid(123)
                .startTime(1)
                .endTime(2)
                .build();
        assertFalse(identityApi.aliasUsers(request));

        //newMpid and previousMpid are not unique
        request = AliasRequest.builder()
                .destinationMpid(123)
                .sourceMpid(123)
                .startTime(1)
                .endTime(2)
                .build();
        assertFalse(identityApi.aliasUsers(request));

        //endTime before startTime
        request = AliasRequest.builder()
                .destinationMpid(123)
                .sourceMpid(456)
                .startTime(2)
                .endTime(1)
                .build();
        assertFalse(identityApi.aliasUsers(request));

        //endTime and or startTime do not exist
        request = AliasRequest.builder()
                .destinationMpid(1)
                .sourceMpid(2)
                .build();
        assertFalse(identityApi.aliasUsers(request));

        request = AliasRequest.builder()
                .destinationMpid(1)
                .sourceMpid(2)
                .startTime(3)
                .build();
        assertFalse(identityApi.aliasUsers(request));

        request = AliasRequest.builder()
                .destinationMpid(1)
                .sourceMpid(2)
                .endTime(3).build();
        assertFalse(identityApi.aliasUsers(request));
    }

    @Test
    public void testAliasUsersValidationAcceptance() {
        IdentityApi identityApi = new IdentityApi();
        final AndroidUtils.Mutable<Boolean> requestMade = new AndroidUtils.Mutable(false);
        identityApi.mMessageManager = new MessageManager() {
            @Override
            public void logAliasRequest(AliasRequest aliasRequest) {
                requestMade.value = true;
            }
        };
        AliasRequest request = AliasRequest.builder()
                .destinationMpid(1)
                .sourceMpid(2)
                .startTime(3)
                .endTime(4)
                .build();
        assertTrue(identityApi.aliasUsers(request));
        assertTrue(requestMade.value);
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