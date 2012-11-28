package com.mparticle;

import java.util.ArrayList;

import android.content.Context;
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
