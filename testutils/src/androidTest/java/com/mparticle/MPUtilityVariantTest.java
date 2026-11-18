package com.mparticle;

import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.mparticle.internal.MPUtility;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MPUtilityVariantTest {

    @Test
    public void testFirebasePreset() {
        assertTrue(MPUtility.isFirebaseAvailable());
    }
}
