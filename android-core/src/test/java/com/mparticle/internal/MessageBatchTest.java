package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.consent.CCPAConsent;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.mock.MockContext;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class MessageBatchTest {


    @Test
    public void testCreate() throws Exception {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
        ConfigManager manager = new ConfigManager(new MockContext(), MParticle.Environment.Production, "some api key", "some api secret", null, null, null, null, null);
        boolean sessionHistory = true;
        BatchId batchId = new BatchId(manager.getMpid(), null, null, null);
        MessageBatch batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), batchId);
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }
        sessionHistory = false;
        batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), batchId);
        assertNotNull(batch.getString("dt"));
        assertNotNull(batch.getString("id"));
        assertNotNull(batch.getDouble("ct"));
        assertNotNull(batch.getString("sdk"));
        assertNotNull(batch.getBoolean("oo"));
        assertNotNull(batch.getDouble("uitl"));
        assertNotNull(batch.getDouble("stl"));
        assertNotNull(batch.getJSONObject("ck"));
        if (manager.getProviderPersistence() != null) {
            assertNotNull(batch.getJSONObject("cms"));
        }

        batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), batchId);
        assertFalse(batch.has("pb"));
    }

    @Test
    public void testAddConsentState() throws Exception {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
        ConfigManager manager = new ConfigManager(new MockContext(), MParticle.Environment.Production, "some api key", "some api secret", null, null, null, null, null);
        boolean sessionHistory = true;
        BatchId batchId = new BatchId(manager.getMpid(), null, null, null);
        MessageBatch batch = MessageBatch.create( sessionHistory, manager,new JSONObject(), batchId);
        batch.addConsentState(null);
        batch.addConsentState(ConsentState.builder().build());
        JSONObject consent = batch.optJSONObject("con");
        assertNotNull(consent);
        batch.addConsentState(
                ConsentState.builder().addGDPRConsentState("foo purpose",
                        GDPRConsent.builder(true)
                                .timestamp(10L)
                                .location("foo location")
                                .hardwareId("foo hardware id")
                                .document("foo document")
                                .build())
                        .setCCPAConsentState(CCPAConsent.builder(true)
                                .timestamp(20L)
                                .location("bar location")
                                .hardwareId("bar hardware id")
                                .document("bar document")
                                .build())
                        .build()
        );
        JSONObject consentJSON = batch.optJSONObject(Constants.MessageKey.CONSENT_STATE);
        assertNotNull(consent);
        consent = consentJSON.optJSONObject(Constants.MessageKey.CONSENT_STATE_GDPR);
        assertNotNull(consent);
        consent = consent.getJSONObject("foo purpose");
        assertNotNull(consent);
        assertEquals(true, consent.getBoolean(Constants.MessageKey.CONSENT_STATE_CONSENTED));
        assertEquals((long)10, consent.getLong(Constants.MessageKey.CONSENT_STATE_TIMESTAMP));
        assertEquals("foo location", consent.getString(Constants.MessageKey.CONSENT_STATE_LOCATION));
        assertEquals("foo hardware id", consent.getString(Constants.MessageKey.CONSENT_STATE_HARDWARE_ID));
        assertEquals("foo document", consent.getString(Constants.MessageKey.CONSENT_STATE_DOCUMENT));

        consent = consentJSON.optJSONObject(Constants.MessageKey.CONSENT_STATE_CCPA);
        assertNotNull(consent);
        consent = consent.getJSONObject(Constants.MessageKey.CCPA_CONSENT_KEY);
        assertNotNull(consent);

        assertEquals(true, consent.getBoolean(Constants.MessageKey.CONSENT_STATE_CONSENTED));
        assertEquals((long)20, consent.getLong(Constants.MessageKey.CONSENT_STATE_TIMESTAMP));
        assertEquals("bar location", consent.getString(Constants.MessageKey.CONSENT_STATE_LOCATION));
        assertEquals("bar hardware id", consent.getString(Constants.MessageKey.CONSENT_STATE_HARDWARE_ID));
        assertEquals("bar document", consent.getString(Constants.MessageKey.CONSENT_STATE_DOCUMENT));
    }
}