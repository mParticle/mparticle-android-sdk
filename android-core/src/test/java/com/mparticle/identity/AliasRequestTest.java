package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AliasRequestTest {
    Random random = new Random();

    @Test
    public void testAliasRequestBuilder() {
        MParticleUser sourceUser = Mockito.mock(MParticleUser.class);
        MParticleUser destinationUser = Mockito.mock(MParticleUser.class);

        long startTime = System.currentTimeMillis() - 100;
        long endTime = System.currentTimeMillis();
        long sourceMpid = random.nextLong();
        long destinationMpid = random.nextLong();

        Mockito.when(sourceUser.getId()).thenReturn(sourceMpid);
        Mockito.when(sourceUser.getFirstSeenTime()).thenReturn(startTime);
        Mockito.when(sourceUser.getLastSeenTime()).thenReturn(endTime);

        Mockito.when(destinationUser.getId()).thenReturn(destinationMpid);

        AliasRequest request = AliasRequest.builder(sourceUser, destinationUser).build();

        assertEquals(destinationMpid, request.getDestinationMpid());
        assertEquals(sourceMpid, request.getSourceMpid());
        assertEquals(request.getStartTime(), startTime);
        assertEquals(request.getEndTime(), endTime);
    }

    @Test
    public void testBasicBuilder() {
        long startTime = random.nextLong();
        long endTime = random.nextLong();
        long sourceMpid = random.nextLong();
        long destinationMpid = random.nextLong();

        AliasRequest.Builder  builder = AliasRequest.builder()
                .destinationMpid(destinationMpid)
                .sourceMpid(sourceMpid)
                .startTime(startTime)
                .endTime(endTime);

        AliasRequest request = builder.build();

        assertEquals(sourceMpid, request.getSourceMpid());
        assertEquals(destinationMpid, request.getDestinationMpid());
        assertEquals(startTime, request.getStartTime());
        assertEquals(endTime, request.getEndTime());
    }

    @Test
    public void testMaxWindowEnforcement() {
        MParticle.setInstance(new MockMParticle());
        Mockito.when(MParticle.getInstance().Internal().getConfigManager().getAliasMaxWindow()).thenReturn(1);

        MParticleUser sourceUser = Mockito.mock(MParticleUser.class);
        MParticleUser destinationUser = Mockito.mock(MParticleUser.class);

        long earliestLegalStartTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

        long startTime = 0;
        long endTime = System.currentTimeMillis();
        long sourceMpid = random.nextLong();
        long destinationMpid = random.nextLong();

        Mockito.when(sourceUser.getId()).thenReturn(sourceMpid);
        Mockito.when(sourceUser.getFirstSeenTime()).thenReturn(startTime);
        Mockito.when(sourceUser.getLastSeenTime()).thenReturn(endTime);
        Mockito.when(destinationUser.getId()).thenReturn(destinationMpid);

        AliasRequest request = AliasRequest.builder(sourceUser, destinationUser).build();

        assertEquals(sourceMpid, request.getSourceMpid());
        assertEquals(destinationMpid, request.getDestinationMpid());

        //since request.getStartTime generates it's own earliestLegalStartTime based on System.currentTimeMillis(),
        //we need to give some wiggle room for time elapsed during test
        assertTrue(Math.abs(earliestLegalStartTime - request.getStartTime()) < 50);
        assertEquals(endTime, request.getEndTime());

    }
}
