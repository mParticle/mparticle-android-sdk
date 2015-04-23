package com.mparticle;

import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;

/**
 * Class used to capture and log uncaught exceptions.
 */
/* package-private */public class ExceptionHandler implements UncaughtExceptionHandler {

    private static final String TAG = Constants.LOG_TAG;
    private UncaughtExceptionHandler mOriginalUncaughtExceptionHandler = null;

    public ExceptionHandler(UncaughtExceptionHandler originalUncaughtExceptionHandler) {
        if (originalUncaughtExceptionHandler != null && !(originalUncaughtExceptionHandler instanceof ExceptionHandler)) {
            mOriginalUncaughtExceptionHandler = originalUncaughtExceptionHandler;
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        try {
            MParticle.getInstance().logUnhandledError(ex);

            if (mOriginalUncaughtExceptionHandler != null) {
                mOriginalUncaughtExceptionHandler.uncaughtException(thread, ex);
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
