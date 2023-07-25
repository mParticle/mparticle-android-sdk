package com.mparticle.internal;

import android.util.Log;

import com.mparticle.MParticle.LogLevel;


public class Logger {
    public static final LogLevel DEFAULT_MIN_LOG_LEVEL = LogLevel.DEBUG;
    private static LogLevel sMinLogLevel = DEFAULT_MIN_LOG_LEVEL;
    private static boolean sExplicitlySet = false;
    private static AbstractLogHandler logHandler = new DefaultLogHandler();

    public static void setMinLogLevel(LogLevel minLogLevel) {
        setMinLogLevel(minLogLevel, null);
    }

    public static void setMinLogLevel(LogLevel minLogLevel, Boolean explicit) {
        if (explicit != null) {
            sExplicitlySet = explicit;
        }
        if (sExplicitlySet && explicit == null) {
            return;
        }
        Logger.sMinLogLevel = minLogLevel;
    }

    public static LogLevel getMinLogLevel() {
        return sMinLogLevel;
    }

    public static void verbose(String... messages) {
        verbose(null, messages);
    }

    public static void verbose(Throwable error, String... messages) {
        getLogHandler().log(LogLevel.VERBOSE, error, getMessage(messages));
    }

    public static void info(String... messages) {
        info(null, messages);
    }

    public static void info(Throwable error, String... messages) {
        getLogHandler().log(LogLevel.INFO, error, getMessage(messages));
    }

    public static void debug(String... messages) {
        debug(null, messages);
    }

    public static void debug(Throwable error, String... messages) {
        getLogHandler().log(LogLevel.DEBUG, error, getMessage(messages));
    }

    public static void warning(String... messages) {
        warning(null, messages);
    }

    public static void warning(Throwable error, String... messages) {
        getLogHandler().log(LogLevel.WARNING, error, getMessage(messages));
    }

    public static void error(String... messages) {
        error(null, messages);
    }

    public static void error(Throwable error, String... messages) {
        getLogHandler().log(LogLevel.ERROR, error, getMessage(messages));
    }

    private static String getMessage(String... messages) {
        StringBuilder logMessage = new StringBuilder();
        for (String m : messages) {
            logMessage.append(m);
        }
        return logMessage.toString();
    }


    /**
     * Testing method. Use this method to intercept Logs, or customize what happens when something is logged.
     * For example, you can use this method to throw an exception every time an "error" log is called.
     *
     * @param logListener
     */
    public static void setLogHandler(AbstractLogHandler logListener) {
        Logger.logHandler = logListener;
    }

    public static AbstractLogHandler getLogHandler() {
        if (logHandler == null) {
            logHandler = new DefaultLogHandler();
        }
        return logHandler;
    }

    public abstract static class AbstractLogHandler {

        public void log(LogLevel priority, Throwable error, String messages) {
            if (messages != null && isLoggable(priority.logLevel)) {
                switch (priority) {
                    case ERROR:
                        error(error, messages);
                        break;
                    case WARNING:
                        warning(error, messages);
                        break;
                    case DEBUG:
                        debug(error, messages);
                        break;
                    case VERBOSE:
                        verbose(error, messages);
                        break;
                    case INFO:
                        info(error, messages);
                }
            }
        }

        private boolean isLoggable(int logLevel) {
            boolean isAPILoggable = logLevel >= Logger.sMinLogLevel.logLevel;
            boolean isADBLoggable;

            //This block will catch the exception that is thrown during testing.
            try {
                isADBLoggable = isADBLoggable(Constants.LOG_TAG, logLevel);
            } catch (UnsatisfiedLinkError ex) {
                return false;
            } catch (RuntimeException ignored) {
                return false;
            }
            return isADBLoggable || (isAPILoggable && MPUtility.isDevEnv());
        }

        //Override this method during testing, otherwise this will throw an error and logs will not be printed.
        protected boolean isADBLoggable(String tag, int logLevel) {
            return Log.isLoggable(tag, logLevel);
        }

        public abstract void verbose(Throwable error, String message);

        public abstract void info(Throwable error, String message);

        public abstract void debug(Throwable error, String message);

        public abstract void warning(Throwable error, String message);

        public abstract void error(Throwable error, String message);
    }

    public static class DefaultLogHandler extends AbstractLogHandler {

        @Override
        public void verbose(Throwable error, String messages) {
            if (error != null) {
                Log.v(Constants.LOG_TAG, messages, error);
            } else {
                Log.v(Constants.LOG_TAG, messages);
            }
        }

        @Override
        public void info(Throwable error, String messages) {
            if (error != null) {
                Log.i(Constants.LOG_TAG, messages, error);
            } else {
                Log.i(Constants.LOG_TAG, messages);
            }
        }

        @Override
        public void debug(Throwable error, String messages) {
            if (error != null) {
                Log.d(Constants.LOG_TAG, messages, error);
            } else {
                Log.d(Constants.LOG_TAG, messages);
            }
        }

        @Override
        public void warning(Throwable error, String messages) {
            if (error != null) {
                Log.w(Constants.LOG_TAG, messages, error);
            } else {
                Log.w(Constants.LOG_TAG, messages);
            }
        }

        @Override
        public void error(Throwable error, String messages) {
            if (error != null) {
                Log.e(Constants.LOG_TAG, messages, error);
            } else {
                Log.e(Constants.LOG_TAG, messages);
            }
        }
    }

}
