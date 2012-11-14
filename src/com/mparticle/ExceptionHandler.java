package com.mparticle;

import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;

public class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String TAG = Constants.LOG_TAG;
    private UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;
    private MessageManager mMessageManager;

    public ExceptionHandler(MessageManager messageManager, UncaughtExceptionHandler defaultUncaughtExceptionHandler) {
        mMessageManager = messageManager;
        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            Log.e(TAG, "Caught uncaught exception", ex);
            long now = System.currentTimeMillis();
            mMessageManager.logErrorEvent(null, now, now, null, ex);
            mDefaultUncaughtExceptionHandler.uncaughtException(thread, ex);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to log uncaught exception", t);
            // we tried. don't make things worse.
        }
    }

}
