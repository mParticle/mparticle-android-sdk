package com.mparticle.consent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
        assertEquals(2, state.build().getGDPRConsentState().size());
        state.setGDPRConsentState(null);
        assertEquals(0, state.build().getGDPRConsentState().size());
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
    public void testSerializeNullConsent() throws Exception {
        ConsentState state = ConsentState.withConsentState(ConsentState.builder().build().toString()).build();
        assertNotNull(state);
        assertEquals(0, state.getGDPRConsentState().size());
    }

    @Test
    public void testSerializeMultipleConsent() throws Exception {
        ConsentState.Builder state = ConsentState.withConsentState(ConsentState.builder()
                .addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
                .addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(true).build())
                .build()
                .toString()
        );
        assertNotNull(state);
        assertEquals(2, state.build().getGDPRConsentState().size());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals(true, state.build().getGDPRConsentState().get("foo-purpose-2").isConsented());
    }

    @Test
    public void testCopyMultipleConsent() throws Exception {
        ConsentState.Builder state = ConsentState.withConsentState(ConsentState.builder()
                .addGDPRConsentState("foo-purpose-1", GDPRConsent.builder(false).build())
                .addGDPRConsentState("foo-purpose-2", GDPRConsent.builder(true).build())
                .build()
        );
        assertNotNull(state);
        assertEquals(2, state.build().getGDPRConsentState().size());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals(true, state.build().getGDPRConsentState().get("foo-purpose-2").isConsented());
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
                ).build().toString()
        );
        assertNotNull(state);
        assertEquals(1, state.build().getGDPRConsentState().size());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals("foo document", state.build().getGDPRConsentState().get("foo-purpose-1").getDocument());
        assertEquals("foo hardware id", state.build().getGDPRConsentState().get("foo-purpose-1").getHardwareId());
        assertEquals("foo location", state.build().getGDPRConsentState().get("foo-purpose-1").getLocation());
        assertEquals(5, (long)state.build().getGDPRConsentState().get("foo-purpose-1").getTimestamp());
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
                ).build().toString()
        );
        assertNotNull(state);
        assertEquals(1, state.build().getGDPRConsentState().size());
        assertEquals(false, state.build().getGDPRConsentState().get("foo-purpose-1").isConsented());
        assertEquals("foo document", state.build().getGDPRConsentState().get("foo-purpose-1").getDocument());
        assertEquals("foo hardware id", state.build().getGDPRConsentState().get("foo-purpose-1").getHardwareId());
        assertEquals("foo location", state.build().getGDPRConsentState().get("foo-purpose-1").getLocation());
        assertEquals(5, (long)state.build().getGDPRConsentState().get("foo-purpose-1").getTimestamp());
    }

    @Test
    public void testTimestampAutomaticallySet() throws Exception {
        long currentTime = System.currentTimeMillis();
        assertTrue(currentTime <= GDPRConsent.builder(false).build().getTimestamp());
    }

}