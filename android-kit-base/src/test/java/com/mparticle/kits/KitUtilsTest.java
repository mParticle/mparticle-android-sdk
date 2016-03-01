package com.mparticle.kits;

import org.junit.Test;

import static org.junit.Assert.*;

public class KitUtilsTest {

    @Test
    public void testSanitizeAttributeKey() throws Exception {
        assertNull(KitUtils.sanitizeAttributeKey(null));
        assertEquals("", KitUtils.sanitizeAttributeKey(""));
        assertEquals("TestTest", KitUtils.sanitizeAttributeKey("$TestTest"));
        assertEquals("$", KitUtils.sanitizeAttributeKey("$$"));
        assertEquals("", KitUtils.sanitizeAttributeKey("$"));
    }
}