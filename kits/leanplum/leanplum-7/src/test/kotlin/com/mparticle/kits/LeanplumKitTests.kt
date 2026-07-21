package com.mparticle.kits

import com.leanplum.Leanplum
import com.leanplum.LeanplumDeviceIdMode
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.MockMParticle
import com.mparticle.identity.MParticleUser
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class LeanplumKitTests {
    private val kit = LeanplumKit()
    private val user: MParticleUser = Mockito.mock(MParticleUser::class.java)
    private val settings = HashMap<String, String>()
    private val userIdentities = HashMap<IdentityType, String>()
    private val mockConfiguration = Mockito.mock(KitConfiguration::class.java)

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name.isNotEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val options = Mockito.mock(MParticleOptions::class.java)
        val factory = KitIntegrationFactory(options)
        val integrations = factory.supportedKits.values
        val className = kit.javaClass.name
        for (integration in integrations) {
            if (integration.name == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateMpidUserId() {
        settings[LeanplumKit.USER_ID_FIELD_KEY] = LeanplumKit.USER_ID_MPID_VALUE
        userIdentities[IdentityType.Email] = "foo email"
        userIdentities[IdentityType.CustomerId] = "foo customer id"

        Mockito.`when`(user.id).thenReturn(5L)

        kit.configuration = mockConfiguration
        var id = kit.generateLeanplumUserId(user, settings, userIdentities)
        Assert.assertEquals("5", id)
        id = kit.generateLeanplumUserId(null, settings, userIdentities)
        Assert.assertNull(id)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateEmailUserId() {
        settings[LeanplumKit.USER_ID_FIELD_KEY] = LeanplumKit.USER_ID_EMAIL_VALUE
        userIdentities[IdentityType.Email] = "foo email"
        userIdentities[IdentityType.CustomerId] = "foo customer id"

        Mockito.`when`(user.id).thenReturn(5L)

        Mockito
            .`when`(
                mockConfiguration.shouldSetIdentity(
                    ArgumentMatchers.any(
                        IdentityType::class.java,
                    ),
                ),
            ).thenReturn(true)
        kit.configuration = mockConfiguration
        var id = kit.generateLeanplumUserId(user, settings, userIdentities)
        Assert.assertEquals("foo email", id)
        userIdentities.remove(IdentityType.Email)
        id = kit.generateLeanplumUserId(user, settings, userIdentities)
        Assert.assertNull(id)
    }

    @Test
    @Throws(Exception::class)
    fun testGenerateCustomerIdlUserId() {
        settings[LeanplumKit.USER_ID_FIELD_KEY] = LeanplumKit.USER_ID_CUSTOMER_ID_VALUE
        userIdentities[IdentityType.Email] = "foo email"
        userIdentities[IdentityType.CustomerId] = "foo customer id"

        Mockito.`when`(user.id).thenReturn(5L)

        Mockito
            .`when`(
                mockConfiguration.shouldSetIdentity(
                    ArgumentMatchers.any(
                        IdentityType::class.java,
                    ),
                ),
            ).thenReturn(true)
        kit.configuration = mockConfiguration
        var id = kit.generateLeanplumUserId(user, settings, userIdentities)
        Assert.assertEquals("foo customer id", id)
        userIdentities.remove(IdentityType.CustomerId)
        id = kit.generateLeanplumUserId(user, settings, userIdentities)
        Assert.assertNull(id)
    }

    @Test
    fun testDeviceIdType() {
        val mparticle = MockMParticle()
        MParticle.setInstance(mparticle)
        val mockDas = "mockDasValue"
        Mockito
            .`when`(MParticle.getInstance()?.Identity()?.deviceApplicationStamp)
            .thenReturn(mockDas)
        mparticle.setAndroidIdDisabled(false)

        kit.setDeviceIdType(LeanplumKit.DEVICE_ID_TYPE_ANDROID_ID)
        Assert.assertEquals(LeanplumDeviceIdMode.ANDROID_ID, Leanplum.mMode)
        Assert.assertNull(Leanplum.deviceId)
        Leanplum.clear()

        kit.setDeviceIdType(LeanplumKit.DEVICE_ID_TYPE_GOOGLE_AD_ID)
        Assert.assertEquals(LeanplumDeviceIdMode.ADVERTISING_ID, Leanplum.mMode)
        Assert.assertNull(Leanplum.deviceId)
        Leanplum.clear()

        kit.setDeviceIdType(LeanplumKit.DEVICE_ID_TYPE_DAS)
        Assert.assertEquals(mockDas, Leanplum.deviceId)
        Assert.assertNull(Leanplum.mMode)
        Leanplum.clear()

        kit.setDeviceIdType("adrbsdtb")
        Assert.assertNull(Leanplum.deviceId)
        Assert.assertNull(Leanplum.mMode)
        Leanplum.clear()

        kit.setDeviceIdType(null)
        Assert.assertNull(Leanplum.deviceId)
        Assert.assertNull(Leanplum.mMode)
        Leanplum.clear()

        mparticle.setAndroidIdDisabled(true)
        kit.setDeviceIdType(LeanplumKit.DEVICE_ID_TYPE_ANDROID_ID)
        Assert.assertNull(Leanplum.mMode)
        Assert.assertNull(Leanplum.deviceId)
    }
}
