package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.Session
import io.branch.referral.util.BranchEvent
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class BranchKitTests {
    private val kit: KitIntegration
        get() = BranchMetricsKit()

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

    @After
    fun tearDown() {
        MParticle.setInstance(null)
    }

    private fun mockCurrentSession(sessionId: Long) {
        val session = Mockito.mock(Session::class.java)
        Mockito.`when`(session.sessionID).thenReturn(sessionId)
        val mParticle = Mockito.mock(MParticle::class.java)
        Mockito.`when`(mParticle.currentSession).thenReturn(session)
        MParticle.setInstance(mParticle)
    }

    /**
     * Verifies the current session ID is added to the Branch event's custom data.
     */
    @Test
    fun testAddSessionIdAddsCurrentSession() {
        mockCurrentSession(-7046029254386353131L)
        val branchEvent = Mockito.mock(BranchEvent::class.java)

        BranchMetricsKit().addSessionId(branchEvent, mapOf("color" to "blue"))

        Mockito
            .verify(branchEvent)
            .addCustomDataProperty(BranchMetricsKit.SESSION_ID_KEY, "-7046029254386353131")
    }

    /**
     * Verifies a customer attribute with the session ID key is not overwritten.
     */
    @Test
    fun testAddSessionIdKeepsCustomerValue() {
        mockCurrentSession(-7046029254386353131L)
        val branchEvent = Mockito.mock(BranchEvent::class.java)

        BranchMetricsKit().addSessionId(
            branchEvent,
            mapOf(BranchMetricsKit.SESSION_ID_KEY to "customer-value"),
        )

        Mockito.verifyZeroInteractions(branchEvent)
    }

    /**
     * Verifies nothing is added when there is no current session.
     */
    @Test
    fun testAddSessionIdSkipsWithoutSession() {
        val mParticle = Mockito.mock(MParticle::class.java)
        Mockito.`when`(mParticle.currentSession).thenReturn(null)
        MParticle.setInstance(mParticle)
        val branchEvent = Mockito.mock(BranchEvent::class.java)

        BranchMetricsKit().addSessionId(branchEvent, null)

        Mockito.verifyZeroInteractions(branchEvent)
    }

    /**
     * Verifies nothing is added when the session has no UUID, which the SDK reports as an ID of 0.
     */
    @Test
    fun testAddSessionIdSkipsZeroId() {
        mockCurrentSession(0L)
        val branchEvent = Mockito.mock(BranchEvent::class.java)

        BranchMetricsKit().addSessionId(branchEvent, null)

        Mockito.verifyZeroInteractions(branchEvent)
    }
}
