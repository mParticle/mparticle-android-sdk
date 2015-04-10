package com.mparticle;

import com.mparticle.MParticle;
import com.mparticle.internal.embedded.EmbeddedKitFactory;

import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class EKTests {
    @Test
    public void testSupportKits(){
        ArrayList<Integer> kits = EmbeddedKitFactory.getSupportedKits();
        assertNotNull(kits);
        assertTrue(kits.contains(MParticle.ServiceProviders.FORESEE_ID));
        assertTrue(kits.contains(37));
        assertTrue(kits.contains(39));
        assertTrue(kits.contains(56));
        assertTrue(kits.contains(68));
    }

}
