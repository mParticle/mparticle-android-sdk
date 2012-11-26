package com.mparticle;

import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;

/* package-private */ class ExceptionHandler implements UncaughtExceptionHandler {

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
            mMessageManager.logErrorEvent(null, 0, System.currentTimeMillis(), null, ex);
            if (null!=mOriginalUncaughtExceptionHandler) {
                mOriginalUncaughtExceptionHandler.uncaughtException(thread, ex);
            } else {
                Thread.getDefaultUncaughtExceptionHandler().uncaughtException(thread, ex);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to log error event for uncaught exception", t);
            // we tried. don't make things worse.
        }
    }

    public UncaughtExceptionHandler getOriginalExceptionHandler() {
        return mOriginalUncaughtExceptionHandler;
    }

}
