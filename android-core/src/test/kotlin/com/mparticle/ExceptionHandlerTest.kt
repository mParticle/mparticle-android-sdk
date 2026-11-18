package com.mparticle

import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.lang.Thread.UncaughtExceptionHandler

class ExceptionHandlerTest {
    @Test
    fun testStoresOriginalHandlerUnlessNestedExceptionHandler() {
        val original = UncaughtExceptionHandler { _, _ -> }
        val handler = ExceptionHandler(original)
        Assert.assertSame(original, handler.getOriginalExceptionHandler())

        val nested = ExceptionHandler(handler)
        Assert.assertNull(nested.getOriginalExceptionHandler())

        val nullHandler = ExceptionHandler(null)
        Assert.assertNull(nullHandler.getOriginalExceptionHandler())
    }

    @Test
    fun testUncaughtExceptionLogsAndForwardsToOriginal() {
        val mockMp = Mockito.mock(MParticle::class.java)
        MParticle.setInstance(mockMp)
        try {
            val original = Mockito.mock(UncaughtExceptionHandler::class.java)
            val handler = ExceptionHandler(original)
            val thread = Thread.currentThread()
            val error = RuntimeException("uncaught")

            handler.uncaughtException(thread, error)

            Mockito.verify(mockMp).logUnhandledError(error)
            Mockito.verify(original).uncaughtException(thread, error)
        } finally {
            MParticle.setInstance(null)
        }
    }

    @Test
    fun testUncaughtExceptionSwallowsMParticleAndOriginalExceptions() {
        val mockMp = Mockito.mock(MParticle::class.java)
        val error = RuntimeException("uncaught")
        Mockito.doThrow(RuntimeException("mp failure")).`when`(mockMp).logUnhandledError(error)
        MParticle.setInstance(mockMp)
        try {
            val original =
                UncaughtExceptionHandler { _, _ ->
                    throw RuntimeException("original failure")
                }
            val handler = ExceptionHandler(original)
            handler.uncaughtException(Thread.currentThread(), error)
            Mockito.verify(mockMp).logUnhandledError(error)
        } finally {
            MParticle.setInstance(null)
        }
    }
}
