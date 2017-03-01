package com.mparticle.internal;

import com.mparticle.MParticle;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by wpassidomo on 3/1/17.
 */

public class LoggerTest {

    @Test
    public void testSetLogLevel() throws Exception {
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR);
    }

    @Test
    public void testSetExplicitLogLevel() throws Exception {
        Logger.setMinLogLevel(MParticle.LogLevel.WARNING, false);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.WARNING);
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR);
        Logger.setMinLogLevel(MParticle.LogLevel.NONE);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.NONE);
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR, true);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR);
        Logger.setMinLogLevel(MParticle.LogLevel.INFO);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR);
        Logger.setMinLogLevel(MParticle.LogLevel.NONE, true);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.NONE);
    }

}
