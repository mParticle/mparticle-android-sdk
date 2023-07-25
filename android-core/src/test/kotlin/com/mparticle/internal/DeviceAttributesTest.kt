package com.mparticle.internal

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockSharedPreferences
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class DeviceAttributesTest {
    @Test
    @Throws(Exception::class)
    fun testCollectAppInfo() {
        MParticle.setInstance(MockMParticle())
        val context = MockContext()
        context.getSharedPreferences(null, 0).edit()
            .putString(Constants.PrefKeys.INSTALL_REFERRER, "install referrer").apply()
        val appInfo = DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context)
        Assert.assertTrue(appInfo.getString("apn") == "com.mparticle.test")
        Assert.assertTrue(appInfo.getString("abn") == "42")
        Assert.assertTrue(appInfo.getString("ain") == "com.mparticle.test.installer")
        Assert.assertTrue(appInfo.getString("an") == "test label")
        Assert.assertTrue(appInfo.getString("av") == "42")
        Assert.assertTrue(!MPUtility.isEmpty(appInfo.getString("bid")))
        Assert.assertNotNull(appInfo.getBoolean("dbg"))
        Assert.assertNotNull(appInfo.getBoolean("pir"))
        Assert.assertNotNull(appInfo.getLong("ict"))
        Assert.assertNotNull(appInfo.getInt("lc"))
        Assert.assertNotNull(appInfo.getLong("lud"))
        Assert.assertNotNull(appInfo.getInt("lcu"))
        Assert.assertNotNull(appInfo.getLong("ud"))
        Assert.assertNotNull(appInfo.getInt("env"))
        Assert.assertEquals("install referrer", appInfo.getString("ir"))
        Assert.assertEquals(true, appInfo.getBoolean("fi"))
    }

    @Test
    @Throws(Exception::class)
    fun testAppInfoInstallTime() {
        val context = MockContext()
        val prefs = context.getSharedPreferences(null, 0) as MockSharedPreferences
        val now: Long = 10012
        prefs.putLong("mp::ict", now).commit()
        val appInfo = DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context)
        Assert.assertEquals(now, appInfo.getLong("ict"))
        val appInfo2 = DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context)
        Assert.assertEquals(now, appInfo2.getLong("ict"))
    }

    @Test
    @Throws(Exception::class)
    fun testAppInfoLaunchCount() {
        val context: Context = MockContext()
        // Clear out the stored data for the current user, so we don't get any launches from previous tests.
        ConfigManager(
            context,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ).deleteUserStorage(
            context,
            ConfigManager.getMpid(context)
        )
        var appInfo: JSONObject? = null
        val launchCount = 20
        for (i in 0..19) {
            appInfo = DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context)
        }
        Assert.assertEquals(launchCount.toLong(), appInfo?.getInt("lc")?.toLong())
    }

    @Test
    fun testOperatingSystemToString() {
        // make sure that all the cases are covered, default is not getting returned
        // if this test fails, it might be because you added a new OperatingSystem enum, but forgot
        // to update this method
        val osStringValues: MutableSet<String> = HashSet()
        for (operatingSystem in MParticle.OperatingSystem.values()) {
            val osString = DeviceAttributes(operatingSystem).operatingSystemString
            Assert.assertFalse(osStringValues.contains(osString))
            osStringValues.add(osString)
        }
    }
}
