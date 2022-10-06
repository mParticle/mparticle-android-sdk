package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.mock.MockContext
import com.mparticle.internal.Logger.DefaultLogHandler
import com.mparticle.internal.Logger.AbstractLogHandler
import org.junit.Assert
import org.junit.Test
import java.lang.Exception

/**
 * Created by wpassidomo on 3/1/17.
 */
class LoggerTest {
    @Test
    @Throws(Exception::class)
    fun testSetLogLevel() {
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR, true)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
    }

    @Test
    @Throws(Exception::class)
    fun testSetExplicitLogLevel() {
        Logger.setMinLogLevel(MParticle.LogLevel.WARNING, false)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.WARNING)
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
        Logger.setMinLogLevel(MParticle.LogLevel.NONE)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.NONE)
        Logger.setMinLogLevel(MParticle.LogLevel.ERROR, true)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
        Logger.setMinLogLevel(MParticle.LogLevel.INFO)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR)
        Logger.setMinLogLevel(MParticle.LogLevel.NONE, true)
        Assert.assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.NONE)
    }

    @Test
    fun testLogHandler() {
        //Set Logger to lowest level, so it prints everything by default.
        MParticle.setLogLevel(MParticle.LogLevel.VERBOSE)
        //Set environment to Development. Pretty hacky, but the easiest way.
        ConfigManager(
            MockContext(),
            MParticle.Environment.Development,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
        Assert.assertNotNull(Logger.getLogHandler())
        Assert.assertTrue(Logger.getLogHandler() is DefaultLogHandler)
        val called = BooleanArray(6)
        val logHandlerTest = LogHandlerTest(called)
        Logger.setLogHandler(logHandlerTest)
        assertTrueUpTo(0, called)
        Logger.verbose("testMessage")
        assertTrueUpTo(1, called)
        Logger.info("testMessage")
        assertTrueUpTo(2, called)
        Logger.debug("testMessage")
        assertTrueUpTo(3, called)
        Logger.warning("testMessage")
        assertTrueUpTo(4, called)
        Logger.error("testMessage")
        assertTrueUpTo(5, called)
        Assert.assertEquals(logHandlerTest, Logger.getLogHandler())
        Logger.setLogHandler(null)
        Assert.assertNotNull(Logger.getLogHandler())
        Assert.assertTrue(Logger.getLogHandler() is DefaultLogHandler)
    }

    private fun assertTrueUpTo(limit: Int, called: BooleanArray) {
        for (i in called.indices) {
            if (i < limit) {
                Assert.assertTrue(called[i])
            } else {
                Assert.assertFalse(called[i])
            }
        }
    }

    internal inner class LogHandlerTest(private val called: BooleanArray) : AbstractLogHandler() {
        override fun isADBLoggable(tag: String, logLevel: Int): Boolean {
            return true
        }

        override fun verbose(error: Throwable?, message: String) {
            called[0] = true
        }

        override fun info(error: Throwable?, message: String) {
            called[1] = true
        }

        override fun debug(error: Throwable?, message: String) {
            called[2] = true
        }

        override fun warning(error: Throwable?, message: String) {
            called[3] = true
        }

        override fun error(error: Throwable?, message: String) {
            called[4] = true
        }
    }
}