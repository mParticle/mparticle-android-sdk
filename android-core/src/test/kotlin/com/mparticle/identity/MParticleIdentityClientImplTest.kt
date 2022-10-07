package com.mparticle.identity

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.internal.ConfigManager
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class MParticleIdentityClientImplTest {
    @Test
    @Throws(Exception::class)
    fun testConvertIdentityTypeToString() {
        for (type in IdentityType.values()) {
            Assert.assertEquals(
                type,
                MParticleIdentityClientImpl.getIdentityType(
                    MParticleIdentityClientImpl.getStringValue(type)
                )
            )
        }
    }

    @Test
    fun testOperatingSystemToString() {
        // make sure that all the cases are covered, default is not getting returned
        // if this test fails, it might be because you added a new OperatingSystem enum, but forgot
        // to update this method
        val osStringValues = HashSet<String>()
        for (operatingSystem in MParticle.OperatingSystem.values()) {
            val osString = MParticleIdentityClientImpl(
                Mockito.mock(
                    Context::class.java
                ),
                Mockito.mock(
                    ConfigManager::class.java
                ),
                operatingSystem
            ).operatingSystemString
            Assert.assertFalse(osStringValues.contains(osString))
            osStringValues.add(osString)
        }
    }
}
