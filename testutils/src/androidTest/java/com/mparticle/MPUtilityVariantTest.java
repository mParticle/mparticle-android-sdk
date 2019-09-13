package com.mparticle;

import com.mparticle.internal.MPUtility;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class MPUtilityVariantTest {

    @Test
    public void testFirebasePreset() {
        assertTrue(MPUtility.isFirebaseAvailable());
    }
}
