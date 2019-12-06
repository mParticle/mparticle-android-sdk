package com.mparticle.consent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConsentStateTest {

    @Test
    public void removeAllGDPRConsentState() {
        ConsentState.Builder state = ConsentState.builder();
        assertEquals(0, state.build().getGDPRConsentState().size());
        state.addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build());
        state.addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(false).build());
        state.setCCPAConsent(CCPAConsent.builder(false).build());
        assertEquals(2, state.build().getGDPRConsentState().size());
        state.setGDPRConsentState(null);
        state.removeCCPAConsent();
        assertEquals(0, state.build().getGDPRConsentState().size());
        assertNull(state.build().getCCPAConsentState());
    }

    @Test
    public void addGDPRConsentState() {
        ConsentState.Builder state = ConsentState.builder();
        assertEquals(0, state.build().getGDPRConsentState().size());
        state.addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build());
        state.addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(false).build());
        assertEquals(2, state.build().getGDPRConsentState().size());
    }

    @Test
    public void removeGDPRConsentState() {
        ConsentState.Builder state = ConsentState.builder();
        state.addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build());
        state.addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(false).build());
        assertEquals(2, state.build().getGDPRConsentState().size());
        state.removeGDPRConsentState("foo-purpose-1");
        assertEquals(1, state.build().getGDPRConsentState().size());
        state.removeGDPRConsentState(null);
    }

    @Test
    public void addCCPAConsentState() {
        ConsentState.Builder state = ConsentState.builder();
        assertEquals(0, state.build().getGDPRConsentState().size());
        state.setCCPAConsent(CCPAConsent.builder(true).build());
        assertNotNull(state.build().getCCPAConsentState());
    }

    @Test
    public void addCCPAConsentStateMultiple() {
        ConsentState.Builder state = ConsentState.builder();
        assertEquals(0, state.build().getGDPRConsentState().size());
        state.setCCPAConsent(CCPAConsent.builder(true).build());
        state.setCCPAConsent(CCPAConsent.builder(false).build());
        assertNotNull(state.build().getCCPAConsentState());
        assertFalse(state.build().getCCPAConsentState().isConsented());
    }

    @Test
    public void removeCCPAConsentState() {
        ConsentState.Builder state = ConsentState.builder();
        assertEquals(0, state.build().getGDPRConsentState().size());
        state.setCCPAConsent(CCPAConsent.builder(true).build());
        assertNotNull(state.build().getCCPAConsentState());
        state.removeCCPAConsent();
        assertNull(state.build().getCCPAConsentState());
    }

    @Test
    public void testSerializeNullConsent() throws Exception {
        ConsentState state = ConsentState.withConsentState(ConsentState.builder().build().toString()).build();
        assertNotNull(state);
        assertEquals(0, state.getGDPRConsentState().size());
        assertNull(state.getCCPAConsentState());
    }

    @Test
    public void testSerializeMultipleConsent() throws Exception {
        ConsentState.Builder state = ConsentState.withConsentState(ConsentState.builder()
                .addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
                .addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(true).build())
                .setCCPAConsent(CCPAConsent.builder(false).build())
                .build()
                .toString()
        );
        assertNotNull(state);
        assertEquals(2, state.build().getGDPRConsentState().size());
        assertNotNull(state.build().getCCPAConsentState());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals(true, state.build().getGDPRConsentState().get("foo-purpose-2").isConsented());
        assertEquals(false, state.build().getCCPAConsentState().isConsented());
    }

    @Test
    public void testCopyMultipleConsent() throws Exception {
        ConsentState.Builder state = ConsentState.withConsentState(ConsentState.builder()
                .addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
                .addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(true).build())
                .setCCPAConsent(CCPAConsent.builder(true).build())
                .build()
        );
        assertNotNull(state);
        assertEquals(2, state.build().getGDPRConsentState().size());
        assertNotNull(state.build().getCCPAConsentState());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals(true, state.build().getGDPRConsentState().get("foo-purpose-2").isConsented());
        assertEquals(true, state.build().getCCPAConsentState().isConsented());
    }

    @Test
    public void testSerializeWithAllValues() throws Exception {
        ConsentState.Builder state = ConsentState.withConsentState(ConsentState.builder().addGDPRConsentState(
                "foo-purpose-1",
                GDPRConsent.builder(false)
                        .document("foo document")
                        .hardwareId("foo hardware id")
                        .location("foo location")
                        .timestamp(5L)
                        .build()
                )
                .setCCPAConsent(CCPAConsent.builder(true)
                        .document("bar document")
                        .hardwareId("bar hardware id")
                        .location("bar location")
                        .timestamp(6L)
                        .build())
                .build().toString()
        );
        assertNotNull(state);
        assertEquals(1, state.build().getGDPRConsentState().size());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals("foo document", state.build().getGDPRConsentState().get("foo-purpose-1").getDocument());
        assertEquals("foo hardware id", state.build().getGDPRConsentState().get("foo-purpose-1").getHardwareId());
        assertEquals("foo location", state.build().getGDPRConsentState().get("foo-purpose-1").getLocation());
        assertEquals(5, (long)state.build().getGDPRConsentState().get("foo-purpose-1").getTimestamp());

        assertNotNull(state.build().getCCPAConsentState());
        assertEquals(true, state.build().getCCPAConsentState().isConsented());
        assertEquals("bar document", state.build().getCCPAConsentState().getDocument());
        assertEquals("bar hardware id", state.build().getCCPAConsentState().getHardwareId());
        assertEquals("bar location", state.build().getCCPAConsentState().getLocation());
        assertEquals(6, (long)state.build().getCCPAConsentState().getTimestamp());
    }

    @Test
    public void testCopyWithAllValues() throws Exception {
        ConsentState.Builder state = ConsentState.withConsentState(ConsentState.builder().addGDPRConsentState(
                "foo-purpose-1",
                GDPRConsent.builder(false)
                        .document("foo document")
                        .hardwareId("foo hardware id")
                        .location("foo location")
                        .timestamp(5L)
                        .build()
                )
                .setCCPAConsent(CCPAConsent.builder(true)
                        .document("bar document")
                        .hardwareId("bar hardware id")
                        .location("bar location")
                        .timestamp(6L)
                        .build())
                .build().toString()
        );
        assertNotNull(state);
        assertEquals(1, state.build().getGDPRConsentState().size());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals("foo document", state.build().getGDPRConsentState().get("foo-purpose-1").getDocument());
        assertEquals("foo hardware id", state.build().getGDPRConsentState().get("foo-purpose-1").getHardwareId());
        assertEquals("foo location", state.build().getGDPRConsentState().get("foo-purpose-1").getLocation());
        assertEquals(5, (long)state.build().getGDPRConsentState().get("foo-purpose-1").getTimestamp());

        assertNotNull(state.build().getCCPAConsentState());
        assertEquals(true, state.build().getCCPAConsentState().isConsented());
        assertEquals("bar document", state.build().getCCPAConsentState().getDocument());
        assertEquals("bar hardware id", state.build().getCCPAConsentState().getHardwareId());
        assertEquals("bar location", state.build().getCCPAConsentState().getLocation());
        assertEquals(6, (long)state.build().getCCPAConsentState().getTimestamp());
    }

    @Test
    public void testTimestampAutomaticallySet() throws Exception {
        long currentTime = System.currentTimeMillis();
        assertTrue(currentTime <= GDPRConsent.builder(false).build().getTimestamp());
        assertTrue(currentTime <= CCPAConsent.builder(false).build().getTimestamp());
    }

}