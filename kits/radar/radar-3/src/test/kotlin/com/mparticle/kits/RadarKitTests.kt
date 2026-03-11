package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.KitIntegrationFactory
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.HashMap

class RadarKitTests {
    private val kit: KitIntegration
        get() = RadarKit()

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(!name.isNullOrEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings, Mockito.mock(Context::class.java))
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNotNull(e)
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
    fun testSetUser() {
        val kit = RadarKit()
        kit.mRunAutomatically = false
        val o = JSONObject()
        o.put("mParticleId", "5")
        Assert.assertFalse(kit.setUserAndTrack(null, "foo", o))
        val user = Mockito.mock(MParticleUser::class.java)
        val identities = HashMap<IdentityType, String>()
        identities[IdentityType.CustomerId] = "foo"
        Mockito.`when`(user.userIdentities).thenReturn(identities)
        Mockito.`when`(user.id).thenReturn(5L)
        Assert.assertFalse(kit.setUserAndTrack(user, "foo", o, true))
        Assert.assertTrue(kit.setUserAndTrack(user, "bar", o, true))
        Assert.assertTrue(kit.setUserAndTrack(user, null, o, true))
        identities.clear()
        Assert.assertTrue(kit.setUserAndTrack(user, "foo", o, true))
    }
}
