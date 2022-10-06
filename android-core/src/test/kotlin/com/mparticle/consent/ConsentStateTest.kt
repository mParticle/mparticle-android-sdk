package com.mparticle.consent

import org.junit.Assert
import org.junit.Test

class ConsentStateTest {
    @Test
    fun removeAllGDPRConsentState() {
        val state = ConsentState.builder()
        Assert.assertEquals(0, state.build().gdprConsentState.size.toLong())
        state.addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
        state.addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(false).build())
        state.setCCPAConsentState(CCPAConsent.builder(false).build())
        Assert.assertEquals(2, state.build().gdprConsentState.size.toLong())
        state.setGDPRConsentState(null)
        state.removeCCPAConsentState()
        Assert.assertEquals(0, state.build().gdprConsentState.size.toLong())
        Assert.assertNull(state.build().ccpaConsentState)
    }

    @Test
    fun addGDPRConsentState() {
        val state = ConsentState.builder()
        Assert.assertEquals(0, state.build().gdprConsentState.size.toLong())
        state.addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
        state.addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(false).build())
        Assert.assertEquals(2, state.build().gdprConsentState.size.toLong())
    }

    @Test
    fun removeGDPRConsentState() {
        val state = ConsentState.builder()
        state.addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
        state.addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(false).build())
        Assert.assertEquals(2, state.build().gdprConsentState.size.toLong())
        state.removeGDPRConsentState("foo-purpose-1")
        Assert.assertEquals(1, state.build().gdprConsentState.size.toLong())
        state.removeGDPRConsentState("")
    }

    @Test
    fun addCCPAConsentState() {
        val state = ConsentState.builder()
        Assert.assertEquals(0, state.build().gdprConsentState.size.toLong())
        state.setCCPAConsentState(CCPAConsent.builder(true).build())
        Assert.assertNotNull(state.build().ccpaConsentState)
    }

    @Test
    fun addCCPAConsentStateMultiple() {
        val state = ConsentState.builder()
        Assert.assertEquals(0, state.build().gdprConsentState.size.toLong())
        state.setCCPAConsentState(CCPAConsent.builder(true).build())
        state.setCCPAConsentState(CCPAConsent.builder(false).build())
        Assert.assertNotNull(state.build().ccpaConsentState)
        Assert.assertFalse(state.build().ccpaConsentState!!.isConsented)
    }

    @Test
    fun removeCCPAConsentState() {
        val state = ConsentState.builder()
        Assert.assertEquals(0, state.build().gdprConsentState.size.toLong())
        state.setCCPAConsentState(CCPAConsent.builder(true).build())
        Assert.assertNotNull(state.build().ccpaConsentState)
        state.removeCCPAConsentState()
        Assert.assertNull(state.build().ccpaConsentState)
    }

    @Test
    @Throws(Exception::class)
    fun testSerializeNullConsent() {
        val state = ConsentState.withConsentState(ConsentState.builder().build().toString()).build()
        Assert.assertNotNull(state)
        Assert.assertEquals(0, state.gdprConsentState.size.toLong())
        Assert.assertNull(state.ccpaConsentState)
    }

    @Test
    @Throws(Exception::class)
    fun testSerializeMultipleConsent() {
        val state = ConsentState.withConsentState(
            ConsentState.builder()
                .addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
                .addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(true).build())
                .setCCPAConsentState(CCPAConsent.builder(false).build())
                .build()
                .toString()
        )
        Assert.assertNotNull(state)
        Assert.assertEquals(2, state.build().gdprConsentState.size.toLong())
        Assert.assertNotNull(state.build().ccpaConsentState)
        Assert.assertEquals(
            false, state.build().gdprConsentState["foo-purpose-1"]?.isConsented
        )
        Assert.assertEquals(
            true, state.build().gdprConsentState["foo-purpose-2"]?.isConsented
        )
        Assert.assertEquals(false, state.build().ccpaConsentState?.isConsented)
    }

    @Test
    @Throws(Exception::class)
    fun testCopyMultipleConsent() {
        val state = ConsentState.withConsentState(
            ConsentState.builder()
                .addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
                .addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(true).build())
                .setCCPAConsentState(CCPAConsent.builder(true).build())
                .build()
        )
        Assert.assertNotNull(state)
        Assert.assertEquals(2, state.build().gdprConsentState.size.toLong())
        Assert.assertNotNull(state.build().ccpaConsentState)
        Assert.assertEquals(
            false, state.build().gdprConsentState["foo-purpose-1"]?.isConsented
        )
        Assert.assertEquals(
            true, state.build().gdprConsentState["foo-purpose-2"]?.isConsented
        )
        Assert.assertEquals(true, state.build().ccpaConsentState?.isConsented)
    }

    @Test
    @Throws(Exception::class)
    fun testSerializeWithAllValues() {
        val state = ConsentState.withConsentState(
            ConsentState.builder().addGDPRConsentState(
                "foo-purpose-1",
                GDPRConsent.builder(false)
                    .document("foo document")
                    .hardwareId("foo hardware id")
                    .location("foo location")
                    .timestamp(5L)
                    .build()
            )
                .setCCPAConsentState(
                    CCPAConsent.builder(true)
                        .document("bar document")
                        .hardwareId("bar hardware id")
                        .location("bar location")
                        .timestamp(6L)
                        .build()
                )
                .build().toString()
        )
        Assert.assertNotNull(state)
        Assert.assertEquals(1, state.build().gdprConsentState.size.toLong())
        Assert.assertEquals(
            false, state.build().gdprConsentState["foo-purpose-1"]?.isConsented
        )
        Assert.assertEquals(
            "foo document", state.build().gdprConsentState["foo-purpose-1"]?.document
        )
        Assert.assertEquals(
            "foo hardware id", state.build().gdprConsentState["foo-purpose-1"]?.hardwareId
        )
        Assert.assertEquals(
            "foo location", state.build().gdprConsentState["foo-purpose-1"]?.location
        )
        Assert.assertEquals(
            5,
            state.build().gdprConsentState["foo-purpose-1"]!!.timestamp
        )
        Assert.assertNotNull(state.build().ccpaConsentState)
        Assert.assertEquals(true, state.build().ccpaConsentState?.isConsented)
        Assert.assertEquals("bar document", state.build().ccpaConsentState?.document)
        Assert.assertEquals("bar hardware id", state.build().ccpaConsentState?.hardwareId)
        Assert.assertEquals("bar location", state.build().ccpaConsentState?.location)
        Assert.assertEquals(6, state.build().ccpaConsentState?.timestamp?.toInt())
    }

    @Test
    @Throws(Exception::class)
    fun testCopyWithAllValues() {
        val state = ConsentState.withConsentState(
            ConsentState.builder().addGDPRConsentState(
                "foo-purpose-1",
                GDPRConsent.builder(false)
                    .document("foo document")
                    .hardwareId("foo hardware id")
                    .location("foo location")
                    .timestamp(5L)
                    .build()
            )
                .setCCPAConsentState(
                    CCPAConsent.builder(true)
                        .document("bar document")
                        .hardwareId("bar hardware id")
                        .location("bar location")
                        .timestamp(6L)
                        .build()
                )
                .build().toString()
        )
        Assert.assertNotNull(state)
        Assert.assertEquals(1, state.build().gdprConsentState.size.toLong())
        Assert.assertEquals(
            false, state.build().gdprConsentState["foo-purpose-1"]?.isConsented
        )
        Assert.assertEquals(
            "foo document", state.build().gdprConsentState["foo-purpose-1"]?.document
        )
        Assert.assertEquals(
            "foo hardware id", state.build().gdprConsentState["foo-purpose-1"]?.hardwareId
        )
        Assert.assertEquals(
            "foo location", state.build().gdprConsentState["foo-purpose-1"]?.location
        )
        Assert.assertEquals(
            5,
            state.build().gdprConsentState["foo-purpose-1"]?.timestamp?.toInt()
        )
        Assert.assertNotNull(state.build().ccpaConsentState)
        Assert.assertEquals(true, state.build().ccpaConsentState?.isConsented)
        Assert.assertEquals("bar document", state.build().ccpaConsentState?.document)
        Assert.assertEquals("bar hardware id", state.build().ccpaConsentState?.hardwareId)
        Assert.assertEquals("bar location", state.build().ccpaConsentState?.location)
        Assert.assertEquals(6, state.build().ccpaConsentState?.timestamp?.toInt())
    }

    @Test
    @Throws(Exception::class)
    fun testTimestampAutomaticallySet() {
        val currentTime = System.currentTimeMillis()
        Assert.assertTrue(currentTime <= GDPRConsent.builder(false).build().timestamp)
        Assert.assertTrue(currentTime <= CCPAConsent.builder(false).build().timestamp)
    }
}