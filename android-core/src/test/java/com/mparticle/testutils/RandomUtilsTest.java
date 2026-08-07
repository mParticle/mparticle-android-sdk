package com.mparticle.testutils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mparticle.MParticle;

import org.junit.Test;

import java.util.Map;

/**
 * Guards the invariants that random identity generation must hold for the identity tests to be
 * deterministic.
 *
 * <p>An empty identity map is not an innocuous edge case: {@code MParticleIdentityClientImpl.modify()}
 * short-circuits and returns 200 without issuing an HTTP request when there are no identity
 * changes, so a test that awaits the modify request hangs and then fails on an unrelated
 * assertion. That was the cause of the intermittent
 * {@code MParticleIdentityClientImplTest.testModifyMessage} failures.
 */
public class RandomUtilsTest {
    private static final int ITERATIONS = 1000;

    private final RandomUtils randomUtils = new RandomUtils();

    @Test
    public void testRandomUserIdentitiesNeverEmpty() {
        for (int i = 0; i < ITERATIONS; i++) {
            assertNonEmptyAndAliasFree(randomUtils.getRandomUserIdentities());
        }
    }

    @Test
    public void testBoundedRandomUserIdentitiesNeverEmpty() {
        int poolSize = MParticle.IdentityType.values().length;
        for (int max = 1; max <= poolSize + 1; max++) {
            // Fewer iterations per bound: the pre-fix failure rate here was 1-in-22, so this is
            // still overwhelmingly likely to catch a regression.
            for (int i = 0; i < ITERATIONS / 10; i++) {
                Map<MParticle.IdentityType, String> identities =
                        randomUtils.getRandomUserIdentities(max);
                assertNonEmptyAndAliasFree(identities);
                assertTrue(
                        "expected at most " + max + " identities but got " + identities.size(),
                        identities.size() <= max);
            }
        }
    }

    @Test
    public void testMockRandomUserIdentitiesNeverEmpty() {
        for (int i = 0; i < ITERATIONS; i++) {
            assertNonEmptyAndAliasFree(
                    com.mparticle.mock.utils.RandomUtils.getInstance().getRandomUserIdentities());
        }
    }

    private void assertNonEmptyAndAliasFree(Map<MParticle.IdentityType, String> identities) {
        assertFalse("random user identities must never be empty", identities.isEmpty());
        assertFalse(
                "Alias is not a settable user identity and must never be generated",
                identities.containsKey(MParticle.IdentityType.Alias));
    }
}
