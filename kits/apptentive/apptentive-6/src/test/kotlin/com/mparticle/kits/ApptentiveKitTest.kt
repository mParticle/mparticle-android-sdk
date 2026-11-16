package com.mparticle.kits

import android.content.Context
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.util.InternalUseOnly
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.identity.MParticleUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class ApptentiveKitTest {
    private val kit: ApptentiveKit
        get() = ApptentiveKit()

    @After
    fun tearDown() {
        unmockkAll()
    }

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

    // user change tests

    private fun fakeUser(mpid: Long): MParticleUser {
        val user = mockk<MParticleUser>()
        every { user.id } returns mpid
        every { user.userIdentities } returns emptyMap()
        return user
    }

    private fun stubApptentivePersonCalls() {
        mockkStatic(Apptentive::class)
        every { Apptentive.setMParticleId(any()) } returns Unit
        every { Apptentive.removeCustomPersonData(any()) } returns Unit
        every { Apptentive.addCustomPersonData(any<String>(), any<String>()) } returns Unit
        every { Apptentive.addCustomPersonData(any<String>(), any<Number>()) } returns Unit
        every { Apptentive.addCustomPersonData(any<String>(), any<Boolean>()) } returns Unit
        every { Apptentive.setPersonName(any()) } returns Unit
    }

    @Test
    fun testLogoutSetsTheNewUsersMParticleId() {
        stubApptentivePersonCalls()
        val apptentiveKit = ApptentiveKit()

        apptentiveKit.onLoginCompleted(fakeUser(1001L), null)
        apptentiveKit.onLogoutCompleted(fakeUser(2002L), null)

        verify(exactly = 1) { Apptentive.setMParticleId("1001") }
        verify(exactly = 1) { Apptentive.setMParticleId("2002") }
    }

    @Test
    fun testUserChangeRemovesThePreviousUsersCustomPersonData() {
        stubApptentivePersonCalls()
        val apptentiveKit = ApptentiveKit()

        apptentiveKit.onLoginCompleted(fakeUser(1001L), null)
        apptentiveKit.onSetUserAttribute("plan", "basic")
        apptentiveKit.onSetUserAttribute("visits", "30")
        apptentiveKit.onLogoutCompleted(fakeUser(2002L), null)

        verify(exactly = 1) { Apptentive.removeCustomPersonData("plan") }
        verify(exactly = 1) { Apptentive.removeCustomPersonData("visits") }
        verify(exactly = 1) { Apptentive.removeCustomPersonData("visits_number") }
    }

    @Test
    fun testSameUserKeepsCustomPersonData() {
        stubApptentivePersonCalls()
        val apptentiveKit = ApptentiveKit()

        apptentiveKit.onLoginCompleted(fakeUser(1001L), null)
        apptentiveKit.onSetUserAttribute("plan", "basic")
        apptentiveKit.onIdentifyCompleted(fakeUser(1001L), null)
        apptentiveKit.onModifyCompleted(fakeUser(1001L), null)
        apptentiveKit.onUserIdentified(fakeUser(1001L))

        verify(exactly = 0) { Apptentive.removeCustomPersonData(any()) }
        verify(exactly = 1) { Apptentive.setMParticleId("1001") }
    }

    @Test
    fun testRemovedCustomPersonDataIsNotRemovedAgainOnUserChange() {
        stubApptentivePersonCalls()
        val apptentiveKit = ApptentiveKit()

        apptentiveKit.onLoginCompleted(fakeUser(1001L), null)
        apptentiveKit.onSetUserAttribute("plan", "basic")
        apptentiveKit.onRemoveUserAttribute("plan")
        apptentiveKit.onUserIdentified(fakeUser(2002L))

        verify(exactly = 1) { Apptentive.removeCustomPersonData("plan") }
    }

    @Test
    fun testUserChangeDoesNotCombineNameParts() {
        stubApptentivePersonCalls()
        val apptentiveKit = ApptentiveKit()

        apptentiveKit.onLoginCompleted(fakeUser(1001L), null)
        apptentiveKit.onSetUserAttribute(MParticle.UserAttributes.FIRSTNAME, "Firstone")
        apptentiveKit.onUserIdentified(fakeUser(2002L))
        apptentiveKit.onSetUserAttribute(MParticle.UserAttributes.LASTNAME, "Lasttwo")

        verify(exactly = 1) { Apptentive.setPersonName("Lasttwo") }
        verify(exactly = 0) { Apptentive.setPersonName("Firstone Lasttwo") }
    }

    @Test
    fun testUnknownUserDoesNotChangeTheBinding() {
        stubApptentivePersonCalls()
        val apptentiveKit = ApptentiveKit()

        apptentiveKit.onLoginCompleted(fakeUser(1001L), null)
        apptentiveKit.onSetUserAttribute("plan", "basic")
        apptentiveKit.onUserIdentified(null)
        apptentiveKit.onUserIdentified(fakeUser(0L))

        verify(exactly = 0) { Apptentive.removeCustomPersonData(any()) }
        verify(exactly = 0) { Apptentive.setMParticleId("0") }
    }
}
