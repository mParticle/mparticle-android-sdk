package com.mparticle;

import android.test.AndroidTestCase;

public class APISetupTests extends AndroidTestCase {

    public void testGetSameInstance() {
        MParticleAPI api1 = MParticleAPI.getInstance(getContext(), "apiKey", "secret");
        MParticleAPI api2 = MParticleAPI.getInstance(getContext(), "apiKey", "secret");
        assertSame(api1, api2);
    }

    public void testGetDifferentInstance() {
        MParticleAPI api1 = MParticleAPI.getInstance(getContext(), "apiKey1", "secret");
        MParticleAPI api2 = MParticleAPI.getInstance(getContext(), "apiKey2", "secret");
        assertNotSame(api1, api2);
    }

    public void testInvalidContext() {
        Exception expected = null;
        try {
            MParticleAPI.getInstance(null, null, null);
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull("Excpetion expected for null context", expected);
    }

    public void testInvalidKey() {
        Exception expected = null;
        try {
            MParticleAPI.getInstance(getContext(), null, "secret");
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull("Excpetion expected for null context", expected);
    }

    public void testInvalidSecret() {
        Exception expected = null;
        try {
            MParticleAPI.getInstance(getContext(), "invalidSecret", null);
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull("Excpetion expected for null context", expected);
    }

}
