package com.mparticle

import com.mparticle.internal.Constants
import org.junit.Assert
import org.junit.Test

class MPEventTests {
    @Test
    fun testBasicBuilder() {
        val event =
            MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category")
                .duration(1234.0).build()
        Assert.assertTrue(event.eventName == "test name")
        Assert.assertTrue(event.eventType == MParticle.EventType.Navigation)
        Assert.assertTrue(event.category == "test category")
        Assert.assertTrue(event.length == 1234.0)
    }

    @Test
    fun testScreenBuilder() {
        val event = MPEvent.Builder("test name").category("test category").duration(1234.0).build()
        Assert.assertTrue(event.eventName == "test name")
        Assert.assertTrue(event.eventType == MParticle.EventType.Other)
        Assert.assertTrue(event.category == "test category")
        Assert.assertTrue(event.length == 1234.0)
    }

    @Test
    fun testSerialization() {
        val eventString =
            MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category")
                .duration(1234.0).toString()
        val event = MPEvent.Builder.parseString(eventString)?.build()
        Assert.assertTrue(event?.eventName == "test name")
        Assert.assertTrue(event?.eventType == MParticle.EventType.Navigation)
        Assert.assertTrue(event?.category == "test category")
        Assert.assertTrue(event?.length == 1234.0)
    }

    @Test
    fun testEventLength() {
        val event =
            MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category")
                .build()
        Assert.assertNull(event.length)
    }

    @Test
    fun testTimer() {
        val eventBuilder = MPEvent.Builder("test name", MParticle.EventType.Navigation)
        eventBuilder.startTime()
        val startTime = System.currentTimeMillis()
        try {
            Thread.sleep(20)
        } catch (e: InterruptedException) {
            Assert.fail(e.toString())
        }
        eventBuilder.endTime()
        val duration = System.currentTimeMillis() - startTime
        eventBuilder.build().length?.let { Assert.assertEquals(duration.toDouble(), it, 5.0) }
    }

    @Test
    fun testEventNameLength() {
        val nameBuilder = StringBuilder(Constants.LIMIT_ATTR_KEY + 10)
        for (i in 0 until Constants.LIMIT_ATTR_KEY + 10) {
            nameBuilder.append("0")
        }
        val eventBuilder =
            MPEvent.Builder(nameBuilder.toString(), MParticle.EventType.Navigation).build()
        Assert.assertEquals(
            Constants.LIMIT_ATTR_KEY.toLong(),
            eventBuilder.eventName.length.toLong()
        )
    }

    @Test
    fun testCopyConstructor() {
        // Test the most basic event - there was a bug when there were no attributes.
        var event = MPEvent.Builder("test name", MParticle.EventType.Other).build()
        var copiedEvent = MPEvent(event)
        Assert.assertEquals(event.eventName, copiedEvent.eventName)
        Assert.assertEquals(event.eventType, copiedEvent.eventType)
        val attributes = HashMap<String, String?>()
        attributes["key 1"] = "value 1"
        attributes["key 2"] = "value 2"
        event = MPEvent.Builder("another name", MParticle.EventType.Social)
            .category("category")
            .duration(12345.0)
            .customAttributes(attributes)
            .addCustomFlag("cool flag key", "flag 1 value 1")
            .addCustomFlag("cool flag key", "flag 1 value 2")
            .addCustomFlag("cool flag key 2", "flag 2 value 1")
            .build()
        copiedEvent = MPEvent(event)
        Assert.assertEquals("another name", copiedEvent.eventName)
        Assert.assertEquals(MParticle.EventType.Social, copiedEvent.eventType)
        Assert.assertEquals("category", copiedEvent.category)
        Assert.assertEquals("value 1", copiedEvent.customAttributeStrings?.get("key 1"))
        Assert.assertEquals("value 2", copiedEvent.customAttributeStrings?.get("key 2"))
        val flags = copiedEvent.customFlags
        flags?.let {
            Assert.assertEquals(flags["cool flag key"]?.size, 2)
            Assert.assertEquals(flags["cool flag key 2"]?.size, 1)
            Assert.assertEquals(flags["cool flag key"]?.get(0), "flag 1 value 1")
            Assert.assertEquals(flags["cool flag key"]?.get(1), "flag 1 value 2")
            Assert.assertEquals(flags["cool flag key 2"]?.get(0), "flag 2 value 1")
        }
    }
}
