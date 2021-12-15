package com.mparticle;

import static org.junit.Assert.assertTrue;

import com.mparticle.internal.MPUtility;

import org.junit.Test;

public class MPUtilityVariantTest {

    @Test
    public void testFirebasePreset() {
        assertTrue(MPUtility.isFirebaseAvailable());
    }
}
