package com.mparticle.kits

import com.mparticle.kits.CustomDataParser.parseValue
import org.junit.Assert
import org.junit.Test

class CustomDataParserTest {
    @Test
    fun testParseData() {
        // boolean
        Assert.assertEquals(true, parseValue("true"))
        Assert.assertEquals(true, parseValue("True"))
        Assert.assertEquals(false, parseValue("false"))
        Assert.assertEquals(false, parseValue("False"))

        // integer
        Assert.assertEquals(12345, parseValue("12345"))
        Assert.assertEquals(-12345, parseValue("-12345"))
        Assert.assertEquals(Int.MIN_VALUE, parseValue(Int.MIN_VALUE.toString()))
        Assert.assertEquals(Int.MAX_VALUE, parseValue(Int.MAX_VALUE.toString()))

        // long
        Assert.assertEquals(Long.MIN_VALUE, parseValue(Long.MIN_VALUE.toString()))
        Assert.assertEquals(Long.MAX_VALUE, parseValue(Long.MAX_VALUE.toString()))

        // double
        Assert.assertEquals(3.14, parseValue("3.14"))
        Assert.assertEquals(-3.14, parseValue("-3.14"))

        // string
        Assert.assertEquals("test", parseValue("test"))
    }
}
