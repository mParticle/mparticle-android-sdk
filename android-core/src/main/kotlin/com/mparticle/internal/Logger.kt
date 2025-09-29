package com.mparticle.internal

import android.util.Log
import com.mparticle.MParticle.LogLevel

object Logger {
    val DEFAULT_MIN_LOG_LEVEL: LogLevel = LogLevel.DEBUG
    private var sMinLogLevel = DEFAULT_MIN_LOG_LEVEL
    private var sExplicitlySet = false
    private var internalLogHandler: AbstractLogHandler? = null

    /**
     * Testing method. Use this method to intercept Logs, or customize what happens when something is logged.
     * For example, you can use this method to throw an exception every time an "error" log is called.
     *
     * @param logListener
     */
    @JvmStatic
    var logHandler: AbstractLogHandler? = null
        get() {
            if (field == null) {
                field = DefaultLogHandler()
            }
            return field
        }

    @JvmStatic
    fun setMinLogLevel(minLogLevel: LogLevel) {
        setMinLogLevel(minLogLevel, null)
    }

    @JvmStatic
    fun setMinLogLevel(minLogLevel: LogLevel, explicit: Boolean?) {
        explicit?.let {
            sExplicitlySet = it
        }
        if (sExplicitlySet && explicit == null) {
            return
        }
        sMinLogLevel = minLogLevel
    }

    fun getMinLogLevel(): LogLevel {
        return sMinLogLevel
    }

    @JvmStatic
    fun verbose(vararg messages: String) {
        verbose(error = null, *messages)
    }

    @JvmStatic
    fun verbose(error: Throwable?, vararg messages: String) {
        logHandler!!.log(LogLevel.VERBOSE, error, getMessage(*messages))
    }

    @JvmStatic
    fun info(vararg messages: String) {
        info(error = null, *messages)
    }

    fun info(error: Throwable?, vararg messages: String) {
        logHandler!!.log(LogLevel.INFO, error, getMessage(*messages))
    }

    @JvmStatic
    fun debug(vararg messages: String) {
        debug(error = null, *messages)
    }

    @JvmStatic
    fun debug(error: Throwable?, vararg messages: String) {
        logHandler!!.log(LogLevel.DEBUG, error, getMessage(*messages))
    }

    @JvmStatic
    fun warning(vararg messages: String) {
        warning(error = null, *messages)
    }

    @JvmStatic
    fun warning(error: Throwable?, vararg messages: String) {
        logHandler!!.log(LogLevel.WARNING, error, getMessage(*messages))
    }

    @JvmStatic
    fun error(vararg messages: String) {
        error(error = null, *messages)
    }

    @JvmStatic
    fun error(error: Throwable?, vararg messages: String) {
        logHandler!!.log(LogLevel.ERROR, error, getMessage(*messages))
    }

    private fun getMessage(vararg messages: String): String {
        val logMessage = StringBuilder()
        for (m in messages) {
            logMessage.append(m)
        }
        return logMessage.toString()
    }


    abstract class AbstractLogHandler {
        open fun log(priority: LogLevel, error: Throwable?, messages: String) {
            if (isLoggable(priority.logLevel)) {
                when (priority) {
                    LogLevel.ERROR -> error(error, messages)
                    LogLevel.WARNING -> warning(error, messages)
                    LogLevel.DEBUG -> debug(error, messages)
                    LogLevel.VERBOSE -> verbose(error, messages)
                    LogLevel.INFO -> info(error, messages)
                    else -> {
                        debug(error, messages)
                    }
                }
            }
        }

        private fun isLoggable(logLevel: Int): Boolean {
            val isAPILoggable = logLevel >= sMinLogLevel.logLevel
            val isADBLoggable: Boolean

            // This block will catch the exception that is thrown during testing.
            try {
                isADBLoggable = isADBLoggable(Constants.LOG_TAG, logLevel)
            } catch (ex: UnsatisfiedLinkError) {
                return false
            } catch (ignored: RuntimeException) {
                return false
            }
            return isADBLoggable || (isAPILoggable && MPUtility.isDevEnv())
        }

        // Override this method during testing, otherwise this will throw an error and logs will not be printed.
        protected open fun isADBLoggable(tag: String, logLevel: Int): Boolean {
            return Log.isLoggable(tag, logLevel)
        }

        abstract fun verbose(error: Throwable?, message: String)

        abstract fun info(error: Throwable?, message: String)

        abstract fun debug(error: Throwable?, message: String)

        abstract fun warning(error: Throwable?, message: String)

        abstract fun error(error: Throwable?, message: String)
    }

    open class DefaultLogHandler : AbstractLogHandler() {
        override fun verbose(error: Throwable?, messages: String) {
            if (error != null) {
                Log.v(Constants.LOG_TAG, messages, error)
            } else {
                Log.v(Constants.LOG_TAG, messages)
            }
        }

        override fun info(error: Throwable?, messages: String) {
            if (error != null) {
                Log.i(Constants.LOG_TAG, messages, error)
            } else {
                Log.i(Constants.LOG_TAG, messages)
            }
        }

        override fun debug(error: Throwable?, messages: String) {
            if (error != null) {
                Log.d(Constants.LOG_TAG, messages, error)
            } else {
                Log.d(Constants.LOG_TAG, messages)
            }
        }

        override fun warning(error: Throwable?, messages: String) {
            if (error != null) {
                Log.w(Constants.LOG_TAG, messages, error)
            } else {
                Log.w(Constants.LOG_TAG, messages)
            }
        }

        override fun error(error: Throwable?, messages: String) {
            if (error != null) {
                Log.e(Constants.LOG_TAG, messages, error)
            } else {
                Log.e(Constants.LOG_TAG, messages)
            }
        }
    }
}
