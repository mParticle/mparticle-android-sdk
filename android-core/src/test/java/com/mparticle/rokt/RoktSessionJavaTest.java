package com.mparticle.rokt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import org.junit.Test;

public class RoktSessionJavaTest {
    @Test
    public void supportsRequiredTokenAndOptionalExpiryConstructors() {
        RoktSession sessionWithoutExpiry = new RoktSession("session-id", "session-token");
        RoktSession sessionWithExpiry = new RoktSession("session-id", "session-token", 123L);

        assertEquals("session-token", sessionWithoutExpiry.getSessionToken());
        assertEquals(Long.valueOf(123L), sessionWithExpiry.getExpiresAt());
        assertFalse(
                Arrays.stream(RoktSession.class.getConstructors())
                        .anyMatch(constructor -> constructor.getParameterCount() == 1));
    }
}
