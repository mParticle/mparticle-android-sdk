package com.mparticle;

import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;


public class MParticleTest {

    ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Test
    public void testAndroidIdDisabled() throws Exception {
        assertFalse(MParticle.isAndroidIdDisabled());
        MParticle.setAndroidIdDisabled(true);
        assertTrue(MParticle.isAndroidIdDisabled());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                assertTrue(MParticle.isAndroidIdDisabled());
                MParticle.setAndroidIdDisabled(false);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        assertFalse(MParticle.isAndroidIdDisabled());
                    }
                });
            }
        });
    }
}