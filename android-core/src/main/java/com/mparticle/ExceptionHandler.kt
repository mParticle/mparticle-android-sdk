package com.mparticle

import com.mparticle.internal.Logger
import java.lang.Thread.UncaughtExceptionHandler

/**
 * Class used to capture and log uncaught exceptions.
 */
open class ExceptionHandler(originalUncaughtExceptionHandler: UncaughtExceptionHandler?) : UncaughtExceptionHandler {
    private var mOriginalUncaughtExceptionHandler: UncaughtExceptionHandler? = null

    init {
        if (originalUncaughtExceptionHandler != null && originalUncaughtExceptionHandler !is ExceptionHandler) {
            mOriginalUncaughtExceptionHandler = originalUncaughtExceptionHandler
        }
    }

    override fun uncaughtException(thread: Thread?, ex: Throwable?) {
        try {
            val instance = MParticle.getInstance()
            if (instance != null) {
                instance.logUnhandledError(ex)
            }
        } catch (t: Exception) {
            Logger.error(t, "Failed to log error event for uncaught exception.")
            // we tried. don't make things worse.
        }

        // Starting in O, if the default UncaughtExceptionHandler does not get called, the application
        // will not exit when an uncaught exception is thrown.
        try {
            if (mOriginalUncaughtExceptionHandler != null) {
                mOriginalUncaughtExceptionHandler!!.uncaughtException(thread, ex)
            }
        } catch (t: Exception) {
            Logger.error(t, "Failed to log error event for uncaught exception.")
            // We tried. don't make things worse.
        }
    }

    open fun getOriginalExceptionHandler(): UncaughtExceptionHandler? = mOriginalUncaughtExceptionHandler
}
