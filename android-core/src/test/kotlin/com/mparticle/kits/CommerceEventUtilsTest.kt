package com.mparticle.kits

import org.junit.Assert
import org.junit.Test

class CommerceEventUtilsTest {
    @Test
    @Throws(Exception::class)
    fun testNullProductExpansion() {
        Assert.assertNotNull(CommerceEventUtils.expand(null))
        Assert.assertEquals(0, CommerceEventUtils.expand(null).size.toLong())
    }
}
