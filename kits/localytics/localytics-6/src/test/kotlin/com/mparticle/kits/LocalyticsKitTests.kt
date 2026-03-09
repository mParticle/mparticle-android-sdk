package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticleOptions
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class LocalyticsKitTests {
    private val kit: KitIntegration
        get() = LocalyticsKit()

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
    fun testGetCustomDimensionIndex() {
        val kit = kit as LocalyticsKit
        val settings = HashMap<String, String>()
        kit.configuration = Mockito.mock(KitConfiguration::class.java)
        settings[LocalyticsKit.API_KEY] = "test"
        settings[LocalyticsKit.CUSTOM_DIMENSIONS] =
            "[ { \"maptype\":\"UserAttributeClass.Name\", \"value\":\"Dimension 0\", \"map\":\"foo-key-0\" }, { \"maptype\":\"UserAttributeClass.Name\", \"value\":\"Dimension 1\", \"map\":\"foo-key-1\" }, ]"
        Mockito.`when`(kit.settings).thenReturn(settings)
        var index = kit.getDimensionIndexForAttribute(null)
        Assert.assertEquals(-1, index.toLong())
        index = kit.getDimensionIndexForAttribute("foo-key-0")
        Assert.assertEquals(0, index.toLong())
        index = kit.getDimensionIndexForAttribute("fOo-Key-0")
        Assert.assertEquals(0, index.toLong())
        index = kit.getDimensionIndexForAttribute("fOo-Key-1")
        Assert.assertEquals(1, index.toLong())
    }
}
