package com.mparticle;

import java.util.ArrayList;
import java.util.Properties;

import com.mparticle.Constants.ConfigKeys;

import android.content.Context;
import android.test.AndroidTestCase;

public class APISetupTests extends AndroidTestCase {

    private static final Properties sEmptySettings = new Properties();
    private Properties mRestoreSettings;

    @Override
    protected void setUp() throws Exception {
        mRestoreSettings = MParticleAPI.sDefaultSettings;
        MParticleAPI.sDefaultSettings = sEmptySettings;
    }

    @Override
    protected void tearDown() throws Exception {
        if (null!=mRestoreSettings) {
            MParticleAPI.sDefaultSettings = mRestoreSettings;
        }
    }

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
        assertNotNull("Exception expected for null context", expected);
        assertTrue(expected instanceof IllegalArgumentException);
    }

    public void testInvalidKey() {
        Exception expected = null;
        try {
            MParticleAPI.getInstance(getContext(), null, "secret");
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull("Exception expected for null API key", expected);
        assertTrue(expected instanceof IllegalArgumentException);
    }

    public void testInvalidSecret() {
        Exception expected = null;
        try {
            MParticleAPI.getInstance(getContext(), "invalidSecret", null);
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull("Exception expected for null secret", expected);
        assertTrue(expected instanceof IllegalArgumentException);
    }

    public void testConfigFromSettings() {
        Properties testSettings = new Properties();
        testSettings.put(ConfigKeys.API_KEY, "test-api-key");
        testSettings.put(ConfigKeys.API_SECRET, "test-api-secret");
        MParticleAPI.sDefaultSettings = testSettings;

        MParticleAPI api1 = MParticleAPI.getInstance(getContext());
        MParticleAPI api2 = MParticleAPI.getInstance(getContext(),
                testSettings.getProperty(ConfigKeys.API_KEY),
                testSettings.getProperty(ConfigKeys.API_SECRET));
        assertSame(api1, api2);
    }

    public void testConfigMissingApiKey() {
        Exception expected = null;
        try {
            MParticleAPI.getInstance(getContext());
        } catch (Exception e) {
            expected = e;
        }
        assertNotNull("Exception expected for missing API Key", expected);
        assertTrue(expected instanceof IllegalArgumentException);
    }

    public void testMultithreadGetInstance() {
        int THREAD_COUNT = 10;
        Thread[] threads = new Thread[THREAD_COUNT];
        final ArrayList<MParticleAPI> apiInstances = new ArrayList<MParticleAPI>(THREAD_COUNT);
        final Context context = getContext();
        final String testApiKey = "TestMT" + System.currentTimeMillis();
        for( int i = 0; i < threads.length; i++ ) {
            threads[i] = new Thread( new Runnable() {
                public void run() {
                    apiInstances.add(MParticleAPI.getInstance(context, testApiKey, "secret"));
                }
            });
        }
        for( Thread thread : threads ) {
            thread.start();
        }
        boolean done = false;
        while( !done ) {
            done = true;
            for( Thread thread : threads ) {
                if( thread.isAlive() ) {
                    done = false;
                }
            }
        }
        for( int i = 0; i < apiInstances.size() - 1; i++ ) {
            assertSame(apiInstances.get(i),apiInstances.get(i+1));
        }
    }
}
