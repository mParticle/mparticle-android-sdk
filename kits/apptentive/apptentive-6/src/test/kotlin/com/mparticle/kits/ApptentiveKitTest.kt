package com.mparticle.kits

import android.content.Context
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.util.InternalUseOnly
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class ApptentiveKitTest {
    private val kit: ApptentiveKit
        get() = ApptentiveKit()

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
            (kit as KitIntegration).onKitCreate(settings, Mockito.mock(Context::class.java))
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
    fun testLastNameValueOnSetUserAttribute() {
        mockkStatic(Apptentive::class)
        every { Apptentive.setPersonName(any()) } returns Unit
        val key = MParticle.UserAttributes.LASTNAME
        val value = "Doe"

        // when
        kit.onSetUserAttribute(key, value)

        // then
        verify { Apptentive.setPersonName("Doe") }
    }

    @OptIn(InternalUseOnly::class)
    @Test
    fun testFirstNameValueOnSetUserAttribute() {
        mockkStatic(Apptentive::class)
        every { Apptentive.setPersonName(any()) } returns Unit

        val key = MParticle.UserAttributes.FIRSTNAME
        val value = "John"

        // when
        kit.onSetUserAttribute(key, value)

        // then
        verify { Apptentive.setPersonName("John") }
    }

    @Test
    fun testPersonCustomData() {
        mockkStatic(Apptentive::class)
        every { Apptentive.addCustomPersonData(any<String>(), any<String>()) } returns Unit
        every { Apptentive.addCustomPersonData(any<String>(), 30) } returns Unit
        val key = "age"
        val value = "30"

        // when
        kit.onSetUserAttribute(key, value)

        // then
        // enableTypeDetection is false & 30 will be passed as string
        verify { Apptentive.addCustomPersonData("age", "30") }
    }

    @Test
    fun testOnSetUserAttributeWithNullKey() {
        mockkStatic(Apptentive::class)

        val key = null
        val value = "30"

        // when
        kit.onSetUserAttribute(key, value)

        // then
        verify(exactly = 0) { Apptentive.setPersonName(any()) }
        verify(exactly = 0) { Apptentive.addCustomPersonData(any(), any<String>()) }
        verify(exactly = 0) { Apptentive.addCustomPersonData(any(), any<Number>()) }
        verify(exactly = 0) { Apptentive.addCustomPersonData(any(), any<Boolean>()) }
    }

    // onSetAllUserAttributes test

    @Test
    fun testFullNameInTheUserAttributes() {
        val userAttributes =
            mutableMapOf(
                MParticle.UserAttributes.FIRSTNAME to "John",
                MParticle.UserAttributes.LASTNAME to "Doe",
            )

        mockkStatic(Apptentive::class)
        every { Apptentive.setPersonName(any()) } returns Unit

        // when
        kit.onSetAllUserAttributes(userAttributes, null)

        verify {
            Apptentive.setPersonName("John Doe")
        }
    }

    @Test
    fun testListOfCustomPersonData() {
        val userAttributes =
            mutableMapOf(
                "key1" to "value1",
                "key2" to "20",
            )
        mockkStatic(Apptentive::class)
        every { Apptentive.addCustomPersonData(any<String>(), any<String>()) } returns Unit
        every { Apptentive.addCustomPersonData(any<String>(), 20) } returns Unit
        kit.onSetAllUserAttributes(userAttributes, null)

        verify {
            Apptentive.addCustomPersonData("key1", "value1")
            Apptentive.addCustomPersonData("key2", "20")
        }
    }
}
