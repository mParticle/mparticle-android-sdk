package com.mparticle;

import com.mparticle.internal.embedded.EmbeddedKitFactory;

import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.Assert.assertNotNull;

public class EKTests {
    @Test
    public void testSupportKits(){
        ArrayList<Integer> kits = EmbeddedKitFactory.getSupportedKits();
        assertNotNull(kits);

    }

}
