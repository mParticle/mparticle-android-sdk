package com.mparticle.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import com.mparticle.mock.MockContext
import com.mparticle.mock.utils.RandomUtils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.Collections
import java.util.UUID

class MPUtilityTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var instance: MPUtility

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        instance = MPUtility()
    }

    @Test
    @Throws(Exception::class)
    fun testSetCheckedAttribute() {
        val attributes = JSONObject()
        MPUtility.setCheckedAttribute(attributes, "some key", "some value", false, true)
        Assert.assertEquals("some value", attributes.getString("some key"))
        MPUtility.setCheckedAttribute(attributes, "some key 2", "some value 2", false, false)
        Assert.assertEquals("some value 2", attributes.getString("some key 2"))
    }

    @Test
    @Throws(Exception::class)
    fun testSetKeyThatsTooLong() {
        val attributes = JSONObject()
        val builder = StringBuilder()
        for (i in 0..256) {
            builder.append("a")
        }
        val keyThatsTooLong = builder.toString()
        MPUtility.setCheckedAttribute(attributes, keyThatsTooLong, "some value 2", false, true)
        Assert.assertFalse(attributes.has(keyThatsTooLong))
    }

    @Test
    fun testGetBuildUUID() {
        UUID.fromString(MPUtility.getBuildUUID(null))
        Assert.assertTrue(
            "UUIDs should have been the same",
            MPUtility.getBuildUUID("12345678") == MPUtility.getBuildUUID("12345678")
        )
        Assert.assertFalse(
            "UUIDs should have been different",
            MPUtility.getBuildUUID("1234567") == MPUtility.getBuildUUID("12345678")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSetValueThatsTooLong() {
        val attributes = JSONObject()
        val builder = StringBuilder()
        for (i in 0 until Constants.LIMIT_ATTR_VALUE + 1) {
            builder.append("a")
        }
        val valueThatsTooLong = builder.toString()
        MPUtility.setCheckedAttribute(attributes, "mykey", valueThatsTooLong, false, false)
        Assert.assertFalse(attributes.has("mykey"))
    }

    @Test
    @Throws(Exception::class)
    fun testSetUserValueThatsTooLong() {
        val attributes = JSONObject()
        val builder = StringBuilder()
        for (i in 0 until Constants.LIMIT_ATTR_VALUE + 1) {
            builder.append("a")
        }
        val valueThatsTooLong = builder.toString()
        MPUtility.setCheckedAttribute(attributes, "mykey", valueThatsTooLong, false, true)
        Assert.assertFalse(attributes.has("mykey"))
    }

    @Test
    @Throws(Exception::class)
    fun googleAdIdInfoWithoutPlayServicesAvailable() {
        Assert.assertNull(MPUtility.getAdIdInfo(MockContext()))
    }

    @Test
    @Throws(Exception::class)
    fun testIsAppDebuggableTrue() {
        val context = Mockito.mock(
            Context::class.java
        )
        val applicationInfo = Mockito.mock(
            ApplicationInfo::class.java
        )
        applicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE
        Mockito.`when`(context.applicationInfo).thenReturn(applicationInfo)
        Assert.assertTrue(MPUtility.isAppDebuggable(context))
    }

    @Test
    @Throws(Exception::class)
    fun testIsAppDebuggableFalse() {
        val context = Mockito.mock(
            Context::class.java
        )
        val applicationInfo = Mockito.mock(
            ApplicationInfo::class.java
        )
        applicationInfo.flags = 0
        Mockito.`when`(context.applicationInfo).thenReturn(applicationInfo)
        Assert.assertFalse(MPUtility.isAppDebuggable(context))
    }

    @Test
    @Throws(Exception::class)
    fun testIsAppDebuggableDoesNotModify() {
        val context = Mockito.mock(
            Context::class.java
        )
        val applicationInfo = ApplicationInfo()
        applicationInfo.flags = 5
        Mockito.`when`(context.applicationInfo).thenReturn(applicationInfo)
        MPUtility.isAppDebuggable(context)
        Assert.assertEquals(5, applicationInfo.flags.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testGetNetworkType() {
        val context = Mockito.mock(
            Context::class.java
        )
        val telephonyManager = Mockito.mock(
            TelephonyManager::class.java
        )
        var result = MPUtility.getNetworkType(context, telephonyManager)
        Assert.assertEquals(0, result)
        result = MPUtility.getNetworkType(context, null)
        Assert.assertNull(result)
    }

    @Test
    @Throws(Exception::class)
    fun testMapToJson() {
        Assert.assertNull(MPUtility.mapToJson(null))
        for (i in 0..9) {
            val testMap = HashMap<String, String?>()
            val testJson = JSONObject()
            for (j in 0..9) {
                val key = RandomUtils.getInstance().getAlphaNumericString(10)
                val value = RandomUtils.getInstance().getAlphaNumericString(18)
                testMap[key] = value
                testJson.put(key, value)
            }
            assertUnorderedJsonEqual(testJson, MPUtility.mapToJson(testMap))
        }
    }

    @Test
    @Throws(Exception::class)
    fun testMapToJsonLists() {
        Assert.assertNull(MPUtility.mapToJson(null))
        for (i in 0..9) {
            val testMap = HashMap<String, Any?>()
            val testJson = JSONObject()
            for (j in 0..9) {
                val list: MutableList<String> = ArrayList()
                val jsonArray = JSONArray()
                for (k in 0..2) {
                    val value = RandomUtils.getInstance().getAlphaNumericString(18)
                    list.add(value)
                    jsonArray.put(value)
                }
                val key = RandomUtils.getInstance().getAlphaNumericString(10)
                testMap[key] = list
                testJson.put(key, jsonArray)
            }
            testMap["bar"] = "foobar"
            testJson.put("bar", "foobar")
            assertUnorderedJsonEqual(testJson, MPUtility.mapToJson(testMap))
        }
    }

    private fun assertUnorderedJsonEqual(object1: JSONObject, object2: JSONObject) {
        if (object1 === object2) {
            return
        }
        Assert.assertEquals(object1.length().toLong(), object2.length().toLong())
        val keys: MutableIterator<Any?> = object1.keys()
        while (keys.hasNext()) {
            val key: Any? = keys.next()
            try {
                val obj1Val = object1[key.toString()]
                val obj2Val = object2[key.toString()]
                // Dealing with nested JSONObjects, not going to deal with nested JSONArray's.
                if (obj1Val is JSONObject && obj2Val is JSONObject) {
                    assertUnorderedJsonEqual(obj1Val, obj2Val)
                } else if (obj1Val is JSONArray && obj1Val is JSONArray) {
                    assertUnorderedJsonEqual(obj1Val, obj2Val as JSONArray)
                } else {
                    Assert.assertEquals(obj1Val, obj2Val)
                }
            } catch (jse: JSONException) {
                Assert.fail(jse.message)
            }
        }
    }

    private fun assertUnorderedJsonEqual(object1: JSONArray, object2: JSONArray) {
        if (object1 === object2) {
            return
        }
        val list1 = toList(object1)
        val list2 = toList(object2)
        Assert.assertEquals(list1.size.toLong(), list2.size.toLong())
        val comparator =
            Comparator<Any> { o1, o2 -> (o1 as Comparable<Any>).compareTo(o2) }
        Collections.sort(list1, comparator)
        Collections.sort(list2, comparator)
        for (i in list1.indices) {
            Assert.assertEquals(list1[i], list2[i])
        }
    }

    private fun toList(jsonArray: JSONArray): List<Any> {
        val list: MutableList<Any> = ArrayList()
        for (i in 0 until jsonArray.length()) {
            try {
                list.add(jsonArray[i])
            } catch (e: JSONException) {
                Assert.fail(e.message)
            }
        }
        return list
    }

    @Test
    fun testNumberDetection() {
        Assert.assertEquals(12, MPUtility.toNumberOrString("12"))
        Assert.assertEquals(1.5, MPUtility.toNumberOrString("1.5"))
        Assert.assertEquals(-1.5, MPUtility.toNumberOrString("-1.5"))
        Assert.assertEquals(0, MPUtility.toNumberOrString("0"))
        // too big for a Long, should return a String
        Assert.assertEquals(
            3.245987293478593E47,
            MPUtility.toNumberOrString("324598729347859283749857293487598237459872398475")
        )
        Assert.assertEquals(
            3.245987293478593E46,
            MPUtility.toNumberOrString("32459872934785928374985729348759823745987239847.5")
        )
        Assert.assertEquals("asdvasd", MPUtility.toNumberOrString("asdvasd"))
        Assert.assertEquals("234sdvsda", MPUtility.toNumberOrString("234sdvsda"))
        Assert.assertNull(MPUtility.toNumberOrString(null))
    }

    @Test
    fun testGetOrientation() {
        val mockResources = Mockito.mock(
            Resources::class.java
        )
        val context = Mockito.mock(
            Context::class.java
        )
        val displayMetrics = Mockito.mock(
            DisplayMetrics::class.java
        )
        `when`(context.getResources()).thenReturn(mockResources)
        `when`(mockResources.getDisplayMetrics()).thenReturn(displayMetrics)
        displayMetrics.widthPixels = 1080
        displayMetrics.heightPixels = 1920
        val orientation: Int = MPUtility.getOrientation(context)
        Assert.assertEquals(Configuration.ORIENTATION_PORTRAIT, orientation)
    }

    @Test
    fun testGetOrientation_When_ORIENTATION_LANDSCAPE() {
        val mockResources = Mockito.mock(
            Resources::class.java
        )
        val context = Mockito.mock(
            Context::class.java
        )
        val displayMetrics = Mockito.mock(
            DisplayMetrics::class.java
        )
        `when`(context.getResources()).thenReturn(mockResources)
        `when`(mockResources.getDisplayMetrics()).thenReturn(displayMetrics)
        displayMetrics.widthPixels = 1953
        displayMetrics.heightPixels = 1920
        val orientation: Int = MPUtility.getOrientation(context)
        Assert.assertEquals(Configuration.ORIENTATION_LANDSCAPE, orientation)
    }

    @Test
    fun testGetOrientation_When_ORIENTATION_SQUARE() {
        val mockResources = Mockito.mock(
            Resources::class.java
        )
        val context = Mockito.mock(
            Context::class.java
        )
        val displayMetrics = Mockito.mock(
            DisplayMetrics::class.java
        )
        `when`(context.getResources()).thenReturn(mockResources)
        `when`(mockResources.getDisplayMetrics()).thenReturn(displayMetrics)
        displayMetrics.widthPixels = 850
        displayMetrics.heightPixels = 850
        val orientation: Int = MPUtility.getOrientation(context)
        Assert.assertEquals(Configuration.ORIENTATION_SQUARE, orientation)
    }

    @Test
    fun testGetOrientation_When_Context_IS_NULL() {
        val context: Context? = null
        val orientation: Int = MPUtility.getOrientation(context)
        Assert.assertEquals(Configuration.ORIENTATION_UNDEFINED, orientation)
    }
}
