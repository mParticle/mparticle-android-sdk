package com.mparticle.internal

import android.content.Context
import android.telephony.TelephonyManager
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.junit.Assert
import org.junit.Test
import java.util.Hashtable
import java.util.TreeMap

class MPUtilityTest : BaseCleanInstallEachTest() {
    @Test
    fun testInstantAppDetectionTest() {
        Assert.assertFalse(MPUtility.isInstantApp(mContext))
    }

    @Test
    @Throws(Exception::class)
    fun testNullMapKey() {
        val map = HashMap<Any?, Any?>()
        map["key1"] = "val1"
        map["key2"] = "val2"
        Assert.assertFalse(MPUtility.containsNullKey(map))
        map[null] = "val3"
        Assert.assertTrue(MPUtility.containsNullKey(map))
        val map2 = Hashtable<Any?, Any?>()
        map2["key1"] = "val1"
        map2["key2"] = "val2"
        Assert.assertFalse(MPUtility.containsNullKey(map2))
        val map3 = TreeMap<Any?, Any?>()
        Assert.assertFalse(MPUtility.containsNullKey(map3))
        val map4 = LinkedHashMap<Any?, Any?>()
        Assert.assertFalse(MPUtility.containsNullKey(map4))
    }

    @Test
    @Throws(Exception::class)
    fun testGetInstrumentedNetworkType() {
        val manager = mContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val result = MPUtility.getNetworkType(mContext, manager)
        Assert.assertNull(result)
    }
}
