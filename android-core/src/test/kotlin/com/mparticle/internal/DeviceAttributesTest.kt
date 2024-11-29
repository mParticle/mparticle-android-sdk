package com.mparticle.internal

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import com.mparticle.MParticle
import com.mparticle.MockMParticle
import com.mparticle.internal.PushRegistrationHelper.PushRegistration
import com.mparticle.mock.MockContext
import com.mparticle.mock.MockSharedPreferences
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.lang.reflect.Field

@RunWith(PowerMockRunner::class)
@PrepareForTest(Context::class, Application::class, BluetoothAdapter::class, TelephonyManager::class)
class DeviceAttributesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        PowerMockito.mockStatic(BluetoothAdapter::class.java)
        PowerMockito.mockStatic(TelephonyManager::class.java)
    }

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

    @Test
    fun testDeviceInfo() {
        val application = PowerMockito.mock(Application::class.java)
        val mockAdapter = mock(BluetoothAdapter::class.java)
        val mockTelephonyManager = mock(TelephonyManager::class.java)
        val mockResources = mock(Resources::class.java)
        val mockDisplayMetrics = mock(DisplayMetrics::class.java)
        val mockConfiguration = mock(Configuration::class.java)
        `when`(application.getResources()).thenReturn(mockResources)

        PowerMockito.`when`(BluetoothAdapter.getDefaultAdapter()).thenReturn(mockAdapter)
        `when`(mockResources.displayMetrics).thenReturn(mockDisplayMetrics)
        `when`(mockResources.configuration).thenReturn(mockConfiguration)
        Mockito.`when`(application.getSystemService(Context.TELEPHONY_SERVICE))
            .thenReturn(mockTelephonyManager)
        Mockito.`when`(mockTelephonyManager.phoneType)
            .thenReturn(TelephonyManager.PHONE_TYPE_GSM)
        Mockito.`when`(mockTelephonyManager.networkOperatorName)
            .thenReturn("TestingNetworkOperatorName")
        Mockito.`when`(mockTelephonyManager.networkCountryIso)
            .thenReturn("US")
        Mockito.`when`(mockTelephonyManager.networkOperator)
            .thenReturn("310260")
        val packageManager = Mockito.mock(PackageManager::class.java)
        doReturn(packageManager).`when`(application).packageManager
        doReturn("TestPackage").`when`(application).packageName
        doReturn("TestInstallerPackage").`when`(packageManager).getInstallerPackageName("TestPackage")
        DeviceAttributes.setDeviceImei("123kalksdlkasd")
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(application)
        Assert.assertNotNull(attributes)
    }

    @Test
    fun testGetAppInfo() {
        val mDeviceAttributes = DeviceAttributes(MParticle.OperatingSystem.ANDROID)
        val application = PowerMockito.mock(Application::class.java)
        val mockSharedPreferences = PowerMockito.mock(SharedPreferences::class.java)
        val mockSharedPreferencesEditor = PowerMockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(application.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)).thenReturn(mockSharedPreferences)
        Mockito.`when`(application.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE).edit()).thenReturn(mockSharedPreferencesEditor)
        val field: Field = DeviceAttributes::class.java.getDeclaredField("appInfo")
        field.isAccessible = true
        val mockJson = JSONObject().apply {
            put("key", "value")
        }
        // Set the private field value
        field.set(mDeviceAttributes, mockJson)
        val attributes = mDeviceAttributes.getAppInfo(application, true) as JSONObject
        Assert.assertNotNull(attributes)
    }

    @Test
    fun testWhen_OperatingSystem_Is_NULL() {
        val mDeviceAttributes = DeviceAttributes(null)
        Assert.assertEquals("Android", mDeviceAttributes.operatingSystemString)
    }

    @Test
    @PrepareForTest(MPUtility::class, BluetoothAdapter::class, TelephonyManager::class)
    fun testUpdateDeviceInfo() {
        context = mock(Context::class.java)
        val application = PowerMockito.mock(Application::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        PowerMockito.mockStatic(MPUtility::class.java)
        val mockAdIdInfo = PowerMockito.mock(MPUtility.AdIdInfo::class.java)

        Mockito.`when`(MPUtility.getAdIdInfo(context)).thenReturn(mockAdIdInfo)

        doReturn(packageManager).`when`(application).packageManager
        doReturn("TestPackage").`when`(application).packageName
        doReturn("TestInstallerPackage").`when`(packageManager).getInstallerPackageName("TestPackage")
        DeviceAttributes.setDeviceImei("123kalksdlkasd")
        val mockMp = MockMParticle()
        MParticle.setInstance(mockMp)
        val registration = PushRegistration("instance id", "1234545")
        Mockito.`when`(
            MParticle.getInstance()!!.Internal().configManager.pushRegistration
        ).thenReturn(registration)
        val result = MPUtility.getAdIdInfo(context)
        val attributes =
            DeviceAttributes(MParticle.OperatingSystem.ANDROID).getDeviceInfo(application)
        Assert.assertNotNull(attributes)
    }
}
