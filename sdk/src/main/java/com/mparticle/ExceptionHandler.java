package com.mparticle;

import android.util.Log;

import java.lang.Thread.UncaughtExceptionHandler;

/* package-private */class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String TAG = Constants.LOG_TAG;
    private final UncaughtExceptionHandler mOriginalUncaughtExceptionHandler;
    private final MessageManager mMessageManager;

    public ExceptionHandler(MessageManager messageManager, UncaughtExceptionHandler originalUncaughtExceptionHandler) {
        mMessageManager = messageManager;
        mOriginalUncaughtExceptionHandler = originalUncaughtExceptionHandler;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
                MParticle.getInstance().logUnhandledError(ex);

            if (null != mOriginalUncaughtExceptionHandler) {
                mOriginalUncaughtExceptionHandler.uncaughtException(thread, ex);
            } else {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, ex);
            }
        } catch (Exception t) {
            Log.e(TAG, "Failed to log error event for uncaught exception", t);
            // we tried. don't make things worse.
        }
    }

    public UncaughtExceptionHandler getOriginalExceptionHandler() {
        return mOriginalUncaughtExceptionHandler;
    }

}
