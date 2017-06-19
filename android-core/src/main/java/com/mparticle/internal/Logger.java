package com.mparticle.internal;

import android.util.Log;

import com.mparticle.MParticle.LogLevel;


public class Logger {
    private static LogLevel sMinLogLevel = LogLevel.DEBUG;
    private static boolean sExplicitlySet = false;

    public static void setMinLogLevel(LogLevel minLogLevel) {
        setMinLogLevel(minLogLevel, false);
    }

    public static void setMinLogLevel(LogLevel minLogLevel, boolean explicit) {
        if (explicit) {
            sExplicitlySet = true;
        } else {
            if (sExplicitlySet) {
                return;
            }
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
        log(LogLevel.VERBOSE, error, messages);
    }

    public static void info(String... messages) {
        info(null, messages);
    }

    public static void info(Throwable error, String... messages) {
        log(LogLevel.INFO, error, messages);
    }

    public static void debug(String... messages) {
        debug(null, messages);
    }

    public static void debug(Throwable error, String... messages) {
        log(LogLevel.DEBUG, error, messages);
    }

    public static void warning(String... messages) {
        warning(null, messages);
    }

    public static void warning(Throwable error, String... messages) {
        log(LogLevel.WARNING, error, messages);
    }

    public static void error(String... messages) {
        error(null, messages);
    }

    public static void error(Throwable error, String... messages) {
        log(LogLevel.ERROR, error, messages);
    }

    private static void log(LogLevel priority, String... messages) {
        log(priority, null, messages);
    }

    private static void log(LogLevel priority, Throwable error, String... messages){
        if (messages != null && isLoggable(priority.logLevel)) {
            StringBuilder logMessage = new StringBuilder();
            for (String m : messages){
                logMessage.append(m);
            }
            switch (priority){
                case ERROR:
                    if (error != null){
                        Log.e(Constants.LOG_TAG, logMessage.toString(), error);
                    } else {
                        Log.e(Constants.LOG_TAG, logMessage.toString());
                    }
                    break;
                case WARNING:
                    if (error != null){
                        Log.w(Constants.LOG_TAG, logMessage.toString(), error);
                    } else {
                        Log.w(Constants.LOG_TAG, logMessage.toString());
                    }
                    break;
                case DEBUG:
                    if (error != null){
                        Log.d(Constants.LOG_TAG, logMessage.toString(), error);
                    } else {
                        Log.d(Constants.LOG_TAG, logMessage.toString());
                    }
                    break;
                case VERBOSE:
                    if (error != null){
                        Log.v(Constants.LOG_TAG, logMessage.toString(), error);
                    } else {
                        Log.v(Constants.LOG_TAG, logMessage.toString());
                    }
                    break;
                case INFO:
                    if (error != null) {
                        Log.i(Constants.LOG_TAG, logMessage.toString(), error);
                    } else {
                        Log.i(Constants.LOG_TAG, logMessage.toString());
                    }
            }
        }
    }

    private static boolean isLoggable(int logLevel) {
        boolean isAPILoggable = logLevel >= Logger.sMinLogLevel.logLevel;
        boolean isADBLoggable;

        //this block will catch the exception that is thrown during testing
        try {
            isADBLoggable = Log.isLoggable(Constants.LOG_TAG, logLevel);
        }
        catch (UnsatisfiedLinkError ex) {
            return false;
        }
        return isADBLoggable || (isAPILoggable && MPUtility.isDevEnv());
    }
}
