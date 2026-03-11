package com.mparticle.kits

import com.mparticle.MParticleOptions
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.KitIntegrationFactory
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class KitTests {
    private val kit: KitIntegration
        get() = OneTrustKit()

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name.isNotEmpty())
    }

//    /**
//     * Kit *should* throw an exception when they're initialized with the wrong settings.
//     *
//     */
//    @Test
//    @Throws(Exception::class)
//    fun testOnKitCreate() {
//        var e: Exception? = null
//        try {
//            val kit = kit
//            val settings: MutableMap<String, String> = mutableMapOf()
//            settings["fake setting"] = "fake"
//            kit.onKitCreate(settings, Mockito.mock(Context::class.java))
//        } catch (ex: Exception) {
//            e = ex
//        }
//        Assert.assertNotNull(e)
//    }
//
//    /**
//     * This test should ensure that whatever the consent state is, if a new GDPR consent is created,
//     * it should be added to the consent state  GDPR map
//     */
//    @Test
//    fun testCreateConsentEventGDPR() {
//        val timestamp = System.currentTimeMillis()
//        val user = Mockito.mock(MParticleUser::class.java)
//        val gdprConsent = GDPRConsent.builder(false).build()
//        var currentConsentState =
//            ConsentState.builder().addGDPRConsentState("purpose1", gdprConsent).build()
//        `when`(user.consentState).thenReturn(currentConsentState)
//        assertEquals(1, currentConsentState.gdprConsentState.size)
//        assertEquals(gdprConsent, currentConsentState.gdprConsentState.get("purpose1"))
//        assertNull(currentConsentState.ccpaConsentState)
//        currentConsentState =
//            kit.createOrUpdateConsent(
//                user,
//                "purpose2",
//                1,
//                OneTrustKit.ConsentRegulation.GDPR,
//                timestamp
//            )!!
//        assertEquals(2, currentConsentState.gdprConsentState.size)
//        assertEquals(gdprConsent, currentConsentState.gdprConsentState.get("purpose1"))
//        assertEquals(currentConsentState.gdprConsentState.get("purpose2")?.timestamp, timestamp)
//        assertTrue(currentConsentState.gdprConsentState.containsKey("purpose2"))
//        assertEquals(true, currentConsentState.gdprConsentState.get("purpose2")!!.isConsented)
//        assertNull(currentConsentState.ccpaConsentState)
//    }
//
//    /**
//     * This test must ensure that any CCPA consent creates is added to the constent state.
//     * By design a new CCPA consent overrides the previous one.
//     */
//    @Test
//    fun testCreateConsentEventCCPA() {
//        val timestamp = System.currentTimeMillis()
//        val user = Mockito.mock(MParticleUser::class.java)
//        val ccpaConsent = CCPAConsent.builder(false).location("loc1").build()
//        var currentConsentState = ConsentState.builder().setCCPAConsentState(ccpaConsent).build()
//        `when`(user.consentState).thenReturn(currentConsentState)
//        assertEquals(0, currentConsentState.gdprConsentState.size)
//        assertEquals(ccpaConsent, currentConsentState.ccpaConsentState)
//        assertEquals("loc1", currentConsentState.ccpaConsentState?.location)
//        assertEquals(false, currentConsentState.ccpaConsentState?.isConsented)
//
//        currentConsentState =
//            kit.createOrUpdateConsent(
//                user,
//                "ccpa",
//                1,
//                OneTrustKit.ConsentRegulation.CCPA,
//                timestamp
//            )!!
//        assertEquals(0, currentConsentState.gdprConsentState.size)
//        assertEquals(currentConsentState.ccpaConsentState?.timestamp, timestamp)
//        assertEquals(true, currentConsentState.ccpaConsentState?.isConsented)
//        assertNull(currentConsentState.ccpaConsentState?.location)
//    }
//
//    @Test
//    fun testCreateUpdate() {
//        val timestamp = System.currentTimeMillis()
//        val user = Mockito.mock(MParticleUser::class.java)
//        val ccpaConsent = CCPAConsent.builder(false).timestamp(timestamp).build()
//        var currentConsentStateBuilder = ConsentState.builder().setCCPAConsentState(ccpaConsent)
//
//        val gdprConsent = GDPRConsent.builder(false).timestamp(timestamp).build()
//        var currentConsentState = currentConsentStateBuilder.addGDPRConsentState("purpose1", gdprConsent).build()
//        `when`(user.consentState).thenReturn(currentConsentState)
//        assertEquals(1, currentConsentState.gdprConsentState.size)
//        assertEquals(gdprConsent, currentConsentState.gdprConsentState.get("purpose1"))
//        currentConsentState =
//            kit.createOrUpdateConsent(
//                user,
//                "purpose2",
//                1,
//                OneTrustKit.ConsentRegulation.GDPR,
//                timestamp
//            )!!
//        assertEquals(2, currentConsentState.gdprConsentState.size)
//        assertEquals(gdprConsent, currentConsentState.gdprConsentState.get("purpose1"))
//        assertEquals(gdprConsent.timestamp,  currentConsentState.gdprConsentState.get("purpose1")?.timestamp)
//        assertEquals(currentConsentState.gdprConsentState.get("purpose2")?.timestamp, timestamp)
//        assertTrue(currentConsentState.gdprConsentState.containsKey("purpose2"))
//        assertEquals(true, currentConsentState.gdprConsentState.get("purpose2")!!.isConsented)
//        assertEquals(currentConsentState.ccpaConsentState?.timestamp, timestamp)
//        assertEquals(false, currentConsentState.ccpaConsentState?.isConsented)
//    }

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
