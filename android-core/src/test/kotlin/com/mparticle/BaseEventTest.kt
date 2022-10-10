package com.mparticle

import org.junit.Assert
import org.junit.Test

class BaseEventTest {
    @Test
    fun testEventType() {
        val baseEvent = BaseEvent(BaseEvent.Type.COMMERCE_EVENT)
        Assert.assertEquals(BaseEvent.Type.COMMERCE_EVENT, baseEvent.type)
    }

    @Test
    fun testCustomFlags() {
        val baseEvent = BaseEvent(BaseEvent.Type.BREADCRUMB)
        Assert.assertNull(baseEvent.customFlags)
        val values1: MutableList<String> = ArrayList()
        values1.add("val1")
        values1.add("val2")
        values1.add("val3")
        val values2: MutableList<String> = ArrayList()
        values2.add("val2")
        val customFlags = HashMap<String?, List<String>>()
        customFlags["key1"] = values1
        customFlags["key2"] = values2
        customFlags["key3"] = ArrayList()
        baseEvent.customFlags = customFlags

        // should not be able to add null key
        customFlags[null] = ArrayList()
        baseEvent.customFlags = customFlags
        Assert.assertEquals(3, baseEvent.customFlags?.size)
        baseEvent.customFlags = null
        Assert.assertNull(baseEvent.customFlags)
    }
}
