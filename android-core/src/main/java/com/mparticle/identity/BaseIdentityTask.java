package com.mparticle.identity;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.MParticleTask;

import java.util.HashSet;
import java.util.Set;

public final class BaseIdentityTask extends MParticleTask<IdentityApiResult> {
    boolean isCompleted;
    boolean isSuccessful;
    IdentityApiResult result;
    Set<TaskSuccessListener> successListeners = new HashSet<TaskSuccessListener>();
    Set<TaskFailureListener> failureListeners = new HashSet<TaskFailureListener>();

    public void setFailed(@Nullable final IdentityHttpResponse errorResponse) {
        isCompleted = true;
        isSuccessful = false;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (TaskFailureListener listener : failureListeners) {
                    listener.onFailure(errorResponse);
                }
            }
        });
    }


    public void setSuccessful(@NonNull final IdentityApiResult result) {
        isCompleted = true;
        isSuccessful = true;
        this.result = result;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (TaskSuccessListener listener : successListeners) {
                    listener.onSuccess(result);
                }
            }
        });
    }

    @Override
    public boolean isComplete() {
        return isCompleted;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    @Nullable
    public IdentityApiResult getResult() {
        return result;
    }

    @NonNull
    @Override
    public BaseIdentityTask addSuccessListener(@NonNull TaskSuccessListener listener) {
        if (listener != null) {
            successListeners.add(listener);
        }
        return this;
    }

    @NonNull
    @Override
    public BaseIdentityTask addFailureListener(@NonNull TaskFailureListener listener) {
        if (listener != null) {
            failureListeners.add(listener);
        }
        return this;
    }
}