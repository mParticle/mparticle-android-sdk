package com.mparticle.networking

import com.mparticle.MPEvent
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CustomAttributesTests {

    @Test
    fun testListSerialization() {
        val customAttributesList =
            mutableMapOf<String, Any>("list" to listOf("foo", "bar", "this", "that"))
        val serialized = "[foo, bar, this, that]"
        MPEvent.Builder("Test Event")
            .customAttributes(customAttributesList)
            .build()
            .let {
                val stringifiedCustomAttributes = it.customAttributeStrings
                assertEquals(serialized, stringifiedCustomAttributes!!["list"])
            }

        val customAttributesArrayList =
            mutableMapOf<String, Any>("list" to arrayListOf("foo", "bar", "this", "that"))

        MPEvent.Builder("Test Event")
            .customAttributes(customAttributesArrayList)
            .build()
            .let {
                val stringifiedCustomAttributes = it.customAttributeStrings
                assertEquals(serialized, stringifiedCustomAttributes!!["list"])
            }
    }

    @Test
    fun testMapSerialization() {
        fun test(attributes: Map<String, String?>?) {
            assertNotNull(attributes)
            var stringifiedCustomAttributes = attributes["list"]!!
            assertTrue { stringifiedCustomAttributes.contains("foo=bar") }
            assertTrue { stringifiedCustomAttributes.contains("this=that") }
            stringifiedCustomAttributes
                .replace("foo=bar", "")
                .replace("this=that", "")
                .replace(" ", "")
                .let {
                    assertEquals("{,}", it)
                }
        }

        val customAttributesMap =
            mutableMapOf<String, Any>("list" to mapOf("foo" to "bar", "this" to "that"))
        MPEvent.Builder("Test Event")
            .customAttributes(customAttributesMap)
            .build()
            .let {
                test(it.customAttributeStrings)
            }

        val customAttributesHashMap =
            mutableMapOf<String, Any>("list" to hashMapOf("foo" to "bar", "this" to "that"))

        MPEvent.Builder("Test Event")
            .customAttributes(customAttributesHashMap)
            .build()
            .let {
                test(it.customAttributeStrings)
            }
    }

    @Test
    fun testJSONSerialization() {
        val customAttributesMap = mutableMapOf<String, Any>(
            "list" to mapOf(
                "foo" to "bar",
                "this" to "that"
            ).let { JSONObject(it.toMap()) }
        )
        val serialized = "{\"foo\":\"bar\",\"this\":\"that\"}"
        MPEvent.Builder("Test Event")
            .customAttributes(customAttributesMap)
            .build()
            .let {
                val stringifiedCustomAttributes = it.customAttributeStrings
                assertEquals(serialized, stringifiedCustomAttributes!!["list"])
            }
    }
}
