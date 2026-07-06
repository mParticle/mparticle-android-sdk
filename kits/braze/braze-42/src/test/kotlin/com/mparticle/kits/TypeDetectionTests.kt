package com.mparticle.kits

import com.mparticle.kits.AppboyKit.StringTypeParser
import org.junit.Assert
import org.junit.Test

class TypeDetectionTests {
    @Test
    fun testEnableTypeDetection() {
        val parser = SomeParser(true)
        Assert.assertEquals("foo", parser.parseValue("key", "foo"))
        Assert.assertEquals(1, parser.parseValue("key", "1"))
        Assert.assertEquals(-99, parser.parseValue("key", "-99"))
        Assert.assertEquals(Int.MAX_VALUE, parser.parseValue("key", Int.MAX_VALUE.toString()))
        Assert.assertEquals(Int.MIN_VALUE, parser.parseValue("key", Int.MIN_VALUE.toString()))
        Assert.assertEquals(
            Int.MAX_VALUE + 1L,
            parser.parseValue("key", (Int.MAX_VALUE + 1L).toString()),
        )
        Assert.assertEquals(
            Int.MIN_VALUE - 1L,
            parser.parseValue("key", (Int.MIN_VALUE - 1L).toString()),
        )
        Assert.assertEquals(Long.MAX_VALUE, parser.parseValue("key", Long.MAX_VALUE.toString()))
        Assert.assertEquals(Long.MIN_VALUE, parser.parseValue("key", Long.MIN_VALUE.toString()))
        Assert.assertEquals(432.2561, parser.parseValue("key", "432.2561"))
        Assert.assertEquals(-1.0001, parser.parseValue("key", "-1.0001"))
        Assert.assertEquals(false, parser.parseValue("key", "false"))
        Assert.assertEquals(true, parser.parseValue("key", "True"))
        Assert.assertTrue(parser.parseValue("key", "1.0") is Double)
        Assert.assertTrue(parser.parseValue("key", (Int.MAX_VALUE + 1L).toString()) is Long)
        Assert.assertTrue(parser.parseValue("key", Int.MAX_VALUE.toString()) is Int)
        Assert.assertTrue(parser.parseValue("key", "0") is Int)
        Assert.assertTrue(parser.parseValue("key", "true") is Boolean)
        Assert.assertTrue(parser.parseValue("key", "True") is Boolean)
    }

    @Test
    fun testDisableTypeDetection() {
        val parser = SomeParser(false)
        Assert.assertEquals("foo", parser.parseValue("key", "foo"))
        Assert.assertEquals("1", parser.parseValue("key", "1"))
        Assert.assertEquals(
            (Int.MAX_VALUE + 1L).toString(),
            parser.parseValue("key", (Int.MAX_VALUE + 1L).toString()),
        )
        Assert.assertEquals("432.2561", parser.parseValue("key", "432.2561"))
        Assert.assertEquals("true", parser.parseValue("key", "true"))
    }

    private inner class SomeParser internal constructor(
        enableTypeDetection: Boolean?,
    ) : StringTypeParser(
        enableTypeDetection!!,
    ) {
        override fun toString(
            key: String,
            value: String,
        ) {
            // do nothing
        }

        override fun toInt(
            key: String,
            value: Int,
        ) {
            // do nothing
        }

        override fun toLong(
            key: String,
            value: Long,
        ) {
            // do nothing
        }

        override fun toDouble(
            key: String,
            value: Double,
        ) {
            // do nothing
        }

        override fun toBoolean(
            key: String,
            value: Boolean,
        ) {
            // do nothing
        }
    }
}
