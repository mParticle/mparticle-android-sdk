package com.mparticle.internal;

import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        //Set Logger to lowest level, so it prints everything by default.
        MParticle.setLogLevel(MParticle.LogLevel.VERBOSE);
        //Set environment to Development. Pretty hacky, but the easiest way.
        new ConfigManager(new MockContext(), MParticle.Environment.Development, null, null, null, null, null, null, null);

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
        public void verbose(Throwable error, String message) {
            called[0] = true;
        }

        @Override
        public void info(Throwable error, String message) {
            called[1] = true;
        }

        @Override
        public void debug(Throwable error, String message) {
            called[2] = true;
        }

        @Override
        public void warning(Throwable error, String message) {
            called[3] = true;
        }

        @Override
        public void error(Throwable error, String message) {
            called[4] = true;
        }
    }

}
