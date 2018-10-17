package com.mparticle.kits;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CommerceEventUtilsTest {

    @Test
    public void testNullProductExpansion() throws Exception {
        assertNotNull(CommerceEventUtils.expand(null));
        assertEquals(0, CommerceEventUtils.expand(null).size());
    }
}
