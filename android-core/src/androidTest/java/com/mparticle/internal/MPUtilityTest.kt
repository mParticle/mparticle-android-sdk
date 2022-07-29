package com.mparticle.internal

import android.telephony.TelephonyManager
import com.mparticle.testing.BaseTest
import com.mparticle.testing.context
class MPUtilityTest : BaseTest() {
    @org.junit.Test
    fun testInstantAppDetectionTest() {
        org.junit.Assert.assertFalse(MPUtility.isInstantApp(context))
    }

    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testNullMapKey() {
        var map = mutableMapOf<String?, String?>(
            "key1" to "val1",
            "key2" to "val2"
        )
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
        map[null] = "val3"
        org.junit.Assert.assertTrue(MPUtility.containsNullKey(map))
        map = mutableMapOf()
        map["key1"] = "val1"
        map["key2"] = "val2"
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
        map = java.util.TreeMap(map)
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
        map = java.util.LinkedHashMap(map)
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
    }

    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testGetInstrumentedNetworkType() {
        val manager: TelephonyManager =
            context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
        val result = MPUtility.getNetworkType(context, manager)
        org.junit.Assert.assertNull(result)
    }
}
