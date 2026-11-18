package com.mparticle.identity;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.MParticleTask;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class BaseIdentityTask extends MParticleTask<IdentityApiResult> {
    boolean isCompleted;
    boolean isSuccessful;
    IdentityApiResult result;
    private IdentityHttpResponse errorResponse;
    Set<TaskSuccessListener> successListeners = new HashSet<TaskSuccessListener>();
    Set<TaskFailureListener> failureListeners = new HashSet<TaskFailureListener>();

    public void setFailed(@Nullable final IdentityHttpResponse errorResponse) {
        Set<TaskFailureListener> listeners;
        synchronized (this) {
            isCompleted = true;
            isSuccessful = false;
            this.errorResponse = errorResponse;
            listeners = new HashSet<TaskFailureListener>(failureListeners);
        }
        notifyFailure(listeners, errorResponse);
    }

    public void setSuccessful(@NonNull final IdentityApiResult result) {
        Set<TaskSuccessListener> listeners;
        synchronized (this) {
            isCompleted = true;
            isSuccessful = true;
            this.result = result;
            listeners = new HashSet<TaskSuccessListener>(successListeners);
        }
        notifySuccess(listeners, result);
    }

    @Override
    public synchronized boolean isComplete() {
        return isCompleted;
    }

    @Override
    public synchronized boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    @Nullable
    public synchronized IdentityApiResult getResult() {
        return result;
    }

    /**
     * Adds a listener for a successful result. A listener added after the task has already succeeded is called
     * with that result, and stays registered for any later completion. Listeners are called on the main thread.
     */
    @NonNull
    @Override
    public BaseIdentityTask addSuccessListener(@NonNull TaskSuccessListener listener) {
        if (listener == null) {
            return this;
        }
        boolean notifyNow;
        IdentityApiResult completedResult;
        synchronized (this) {
            // A listener already registered was in the last completion's snapshot, so it is not notified again.
            notifyNow = successListeners.add(listener) && isCompleted && isSuccessful;
            completedResult = result;
        }
        if (notifyNow) {
            notifySuccess(Collections.singleton(listener), completedResult);
        }
        return this;
    }

    /**
     * Adds a listener for a failed request. A listener added after the task has already failed is called with that
     * error, and stays registered for any later completion. Listeners are called on the main thread.
     */
    @NonNull
    @Override
    public BaseIdentityTask addFailureListener(@NonNull TaskFailureListener listener) {
        if (listener == null) {
            return this;
        }
        boolean notifyNow;
        IdentityHttpResponse completedError;
        synchronized (this) {
            notifyNow = failureListeners.add(listener) && isCompleted && !isSuccessful;
            completedError = errorResponse;
        }
        if (notifyNow) {
            notifyFailure(Collections.singleton(listener), completedError);
        }
        return this;
    }

    private static void notifySuccess(final Set<TaskSuccessListener> listeners, final IdentityApiResult result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (TaskSuccessListener listener : listeners) {
                    listener.onSuccess(result);
                }
            }
        });
    }

    private static void notifyFailure(final Set<TaskFailureListener> listeners, @Nullable final IdentityHttpResponse errorResponse) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (TaskFailureListener listener : listeners) {
                    listener.onFailure(errorResponse);
                }
            }
        });
    }
}
