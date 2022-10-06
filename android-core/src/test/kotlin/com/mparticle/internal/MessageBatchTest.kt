package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.mock.MockContext
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.consent.CCPAConsent
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.lang.Exception

class MessageBatchTest {
    @Test
    @Throws(Exception::class)
    fun testCreate() {
        val mockMp = Mockito.mock(MParticle::class.java)
        Mockito.`when`(mockMp.environment).thenReturn(MParticle.Environment.Development)
        MParticle.setInstance(mockMp)
        val manager = ConfigManager(
            MockContext(),
            MParticle.Environment.Production,
            "some api key",
            "some api secret",
            null,
            null,
            null,
            null,
            null
        )
        var sessionHistory = true
        val batchId = BatchId(manager.mpid, null, null, null)
        var batch = MessageBatch.create(sessionHistory, manager, JSONObject(), batchId)
        Assert.assertNotNull(batch.getString("dt"))
        Assert.assertNotNull(batch.getString("id"))
        Assert.assertNotNull(batch.getDouble("ct"))
        Assert.assertNotNull(batch.getString("sdk"))
        Assert.assertNotNull(batch.getBoolean("oo"))
        Assert.assertNotNull(batch.getDouble("uitl"))
        Assert.assertNotNull(batch.getDouble("stl"))
        Assert.assertNotNull(batch.getJSONObject("ck"))
        if (manager.providerPersistence != null) {
            Assert.assertNotNull(batch.getJSONObject("cms"))
        }
        sessionHistory = false
        batch = MessageBatch.create(sessionHistory, manager, JSONObject(), batchId)
        Assert.assertNotNull(batch.getString("dt"))
        Assert.assertNotNull(batch.getString("id"))
        Assert.assertNotNull(batch.getDouble("ct"))
        Assert.assertNotNull(batch.getString("sdk"))
        Assert.assertNotNull(batch.getBoolean("oo"))
        Assert.assertNotNull(batch.getDouble("uitl"))
        Assert.assertNotNull(batch.getDouble("stl"))
        Assert.assertNotNull(batch.getJSONObject("ck"))
        if (manager.providerPersistence != null) {
            Assert.assertNotNull(batch.getJSONObject("cms"))
        }
        batch = MessageBatch.create(sessionHistory, manager, JSONObject(), batchId)
        Assert.assertFalse(batch.has("pb"))
    }

    @Test
    @Throws(Exception::class)
    fun testAddConsentState() {
        val mockMp = Mockito.mock(MParticle::class.java)
        Mockito.`when`(mockMp.environment).thenReturn(MParticle.Environment.Development)
        MParticle.setInstance(mockMp)
        val manager = ConfigManager(
            MockContext(),
            MParticle.Environment.Production,
            "some api key",
            "some api secret",
            null,
            null,
            null,
            null,
            null
        )
        val sessionHistory = true
        val batchId = BatchId(manager.mpid, null, null, null)
        val batch = MessageBatch.create(sessionHistory, manager, JSONObject(), batchId)
        batch.addConsentState(null)
        batch.addConsentState(ConsentState.builder().build())
        var consent = batch.optJSONObject("con")
        Assert.assertNotNull(consent)
        batch.addConsentState(
            ConsentState.builder().addGDPRConsentState(
                "foo purpose",
                GDPRConsent.builder(true)
                    .timestamp(10L)
                    .location("foo location")
                    .hardwareId("foo hardware id")
                    .document("foo document")
                    .build()
            )
                .setCCPAConsentState(
                    CCPAConsent.builder(true)
                        .timestamp(20L)
                        .location("bar location")
                        .hardwareId("bar hardware id")
                        .document("bar document")
                        .build()
                )
                .build()
        )
        val consentJSON = batch.optJSONObject(Constants.MessageKey.CONSENT_STATE)
        Assert.assertNotNull(consent)
        consent = consentJSON?.optJSONObject(Constants.MessageKey.CONSENT_STATE_GDPR)
        Assert.assertNotNull(consent)
        consent = consent?.getJSONObject("foo purpose")
        Assert.assertNotNull(consent)
        Assert.assertEquals(true, consent?.getBoolean(Constants.MessageKey.CONSENT_STATE_CONSENTED))
        Assert.assertEquals(10L, consent?.getLong(Constants.MessageKey.CONSENT_STATE_TIMESTAMP))
        Assert.assertEquals(
            "foo location",
            consent?.getString(Constants.MessageKey.CONSENT_STATE_LOCATION)
        )
        Assert.assertEquals(
            "foo hardware id",
            consent?.getString(Constants.MessageKey.CONSENT_STATE_HARDWARE_ID)
        )
        Assert.assertEquals(
            "foo document",
            consent?.getString(Constants.MessageKey.CONSENT_STATE_DOCUMENT)
        )
        consent = consentJSON?.optJSONObject(Constants.MessageKey.CONSENT_STATE_CCPA)
        Assert.assertNotNull(consent)
        consent = consent?.getJSONObject(Constants.MessageKey.CCPA_CONSENT_KEY)
        Assert.assertNotNull(consent)
        Assert.assertEquals(true, consent?.getBoolean(Constants.MessageKey.CONSENT_STATE_CONSENTED))
        Assert.assertEquals(20L, consent?.getLong(Constants.MessageKey.CONSENT_STATE_TIMESTAMP))
        Assert.assertEquals(
            "bar location",
            consent?.getString(Constants.MessageKey.CONSENT_STATE_LOCATION)
        )
        Assert.assertEquals(
            "bar hardware id",
            consent?.getString(Constants.MessageKey.CONSENT_STATE_HARDWARE_ID)
        )
        Assert.assertEquals(
            "bar document",
            consent?.getString(Constants.MessageKey.CONSENT_STATE_DOCUMENT)
        )
    }
}