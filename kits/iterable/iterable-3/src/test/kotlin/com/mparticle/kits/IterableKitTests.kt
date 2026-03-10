package com.mparticle.kits

import com.mparticle.MParticleOptions
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class IterableKitTests {
    private val kit: KitIntegration
        get() = IterableKit()

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(!name.isNullOrEmpty())
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
}
