package com.mparticle.kits

import com.mparticle.MParticle.EventType
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
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
