package com.mparticle.kits

import android.content.Context
import android.content.SharedPreferences
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.identity.MParticleUser
import org.junit.After
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

    @After
    fun resetPrefersUserId() {
        IterableKit.prefersUserId = false
    }

    private fun kitWith(
        honorOptOut: Boolean,
        session: IterableSession,
        context: Context? = null,
    ): IterableKit {
        val testKit = IterableKit()
        val kitManager = Mockito.mock(KitManagerImpl::class.java)
        Mockito.`when`(kitManager.context).thenReturn(context)
        testKit.kitManager = kitManager
        val configuration = Mockito.mock(KitConfiguration::class.java)
        Mockito.`when`(configuration.shouldHonorOptOut()).thenReturn(honorOptOut)
        testKit.setConfiguration(configuration)
        testKit.iterableSession = session
        return testKit
    }

    @Test
    fun testSetOptOutDisablesPushAndLogsOutOfIterable() {
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(true, session)

        val messages = testKit.setOptOut(true)

        Mockito.verify(session).disablePush()
        Mockito.verify(session).setEmail(null)
        Assert.assertEquals(1, messages.size)
    }

    @Test
    fun testSetOptOutLogsOutByUserIdWhenUserIdIsPreferred() {
        IterableKit.prefersUserId = true
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(true, session)

        testKit.setOptOut(true)

        Mockito.verify(session).disablePush()
        Mockito.verify(session).setUserId(null)
        Mockito.verify(session, Mockito.never()).setEmail(Mockito.anyString())
    }

    @Test
    fun testSetOptOutLeavesIterableAloneWhenKitDoesNotHonorOptOut() {
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(false, session)

        val messages = testKit.setOptOut(true)

        Mockito.verifyZeroInteractions(session)
        Assert.assertTrue(messages.isEmpty())
    }

    @Test
    fun testSetOptOutFalseLeavesIterableAlone() {
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(true, session)

        val messages = testKit.setOptOut(false)

        Mockito.verifyZeroInteractions(session)
        Assert.assertTrue(messages.isEmpty())
    }

    @Test
    fun testIdentityResultAfterOptOutDoesNotIdentifyIterable() {
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(true, session)
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(user.userIdentities).thenReturn(mapOf(IdentityType.Email to "test-user@example.com"))
        testKit.setOptOut(true)

        testKit.handleOnSuccess("test-user-id", user)

        Mockito.verify(session, Mockito.never()).setEmail("test-user@example.com")
        Mockito.verify(session, Mockito.never()).setUserId("test-user-id")
    }

    @Test
    fun testIdentityResultBeforeOptOutIdentifiesIterable() {
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(true, session)
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(user.userIdentities).thenReturn(mapOf(IdentityType.Email to "test-user@example.com"))

        testKit.handleOnSuccess("test-user-id", user)

        Mockito.verify(session).setEmail("test-user@example.com")
    }

    @Test
    fun testOptOutMarkIsWrittenAtOptOutAndTakenOnce() {
        val preferences = Mockito.mock(SharedPreferences::class.java)
        val editor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(preferences.edit()).thenReturn(editor)
        Mockito.`when`(editor.putBoolean(IterableKit.LOGGED_OUT_BY_KIT, true)).thenReturn(editor)
        Mockito.`when`(editor.remove(IterableKit.LOGGED_OUT_BY_KIT)).thenReturn(editor)
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.getSharedPreferences(IterableKit.OPT_OUT_PREFERENCES, Context.MODE_PRIVATE)).thenReturn(preferences)
        val session = Mockito.mock(IterableSession::class.java)
        val testKit = kitWith(true, session, context)

        testKit.setOptOut(true)

        Mockito.verify(editor).putBoolean(IterableKit.LOGGED_OUT_BY_KIT, true)

        Mockito.`when`(preferences.getBoolean(IterableKit.LOGGED_OUT_BY_KIT, false)).thenReturn(true)
        Assert.assertTrue(IterableKit().takeOptOutMark(context))
        Mockito.verify(editor).remove(IterableKit.LOGGED_OUT_BY_KIT)

        Mockito.`when`(preferences.getBoolean(IterableKit.LOGGED_OUT_BY_KIT, false)).thenReturn(false)
        Assert.assertFalse(IterableKit().takeOptOutMark(context))
        Assert.assertFalse(IterableKit().takeOptOutMark(null))
    }
}
