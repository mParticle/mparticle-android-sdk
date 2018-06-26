package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by wpassidomo on 3/1/17.
 */

public class LoggerTest {

    @Test
    public void testSetLogLevel() throws Exception {
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR, true);
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

    @Test
    public void testLogHandler() {
        //set Logger to lowest level, so it prints everything by default
        MParticle.setLogLevel(MParticle.LogLevel.VERBOSE);
        //set environment to Development. Pretty hacky, but the easiest way
        new ConfigManager(new MockContext(), MParticle.Environment.Development, null, null);

        assertNotNull(Logger.getLogHandler());
        assertTrue(Logger.getLogHandler() instanceof Logger.DefaultLogHandler);

        final boolean[] called = new boolean[6];
        LogHandlerTest logHandlerTest = new LogHandlerTest(called);
        Logger.setLogHandler(logHandlerTest);

        assertTrueUpTo(0, called);
        Logger.verbose("testMessage");
        assertTrueUpTo(1, called);
        Logger.info("testMessage");
        assertTrueUpTo(2, called);
        Logger.debug("testMessage");
        assertTrueUpTo(3, called);
        Logger.warning("testMessage");
        assertTrueUpTo(4, called);
        Logger.error("testMessage");
        assertTrueUpTo(5, called);
        assertEquals(logHandlerTest, Logger.getLogHandler());
        Logger.setLogHandler(null);
        assertNotNull(Logger.getLogHandler());
        assertTrue(Logger.getLogHandler() instanceof Logger.DefaultLogHandler);
    }

    private void assertTrueUpTo(int limit, boolean[] called) {
        for (int i = 0; i < called.length; i++) {
            if (i < limit) {
                assertTrue(called[i]);
            } else {
                assertFalse(called[i]);
            }
        }
    }

    class LogHandlerTest extends Logger.AbstractLogHandler {
        private boolean[] called;

        @Override
        protected boolean isADBLoggable(String tag, int logLevel) {
            return true;
        }

        public LogHandlerTest (boolean[] called){
            this.called = called;
        }

        @Override
        void verbose(Throwable error, String message) {
            called[0] = true;
        }

        @Override
        void info(Throwable error, String message) {
            called[1] = true;
        }

        @Override
        void debug(Throwable error, String message) {
            called[2] = true;
        }

        @Override
        void warning(Throwable error, String message) {
            called[3] = true;
        }

        @Override
        void error(Throwable error, String message) {
            called[4] = true;
        }
    }

}
