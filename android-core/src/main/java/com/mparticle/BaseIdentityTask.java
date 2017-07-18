package com.mparticle;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;
import com.mparticle.identity.MParticleUser;

import java.util.HashSet;
import java.util.Set;

public final class BaseIdentityTask extends MParticleTask<IdentityApiResult> {
    boolean isCompleted;
    boolean isSuccessful;
    IdentityApiResult result;
    Set<TaskSuccessListener> successListeners = new HashSet<TaskSuccessListener>();
    Set<TaskFailureListener> failureListeners = new HashSet<TaskFailureListener>();

    public void setFailed(final IdentityHttpResponse errorResponse) {
        isCompleted = true;
        isSuccessful = false;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for(TaskFailureListener listener: failureListeners) {
                    listener.onFailure(errorResponse);
                }
            }
        });
    }

    public void setSuccessful(final IdentityApiResult result) {
        isCompleted = true;
        isSuccessful = true;
        this.result = result;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (TaskSuccessListener listener: successListeners) {
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
    public IdentityApiResult getResult() {
        return result;
    }

    @NonNull
    @Override
    public MParticleTask addSuccessListener(@NonNull TaskSuccessListener listener) {
        if (listener != null) {
            successListeners.add(listener);
        }
        return this;
    }

    @NonNull
    @Override
    public MParticleTask addFailureListener(@NonNull TaskFailureListener listener) {
        if (listener != null) {
            failureListeners.add(listener);
        }
        return this;
    }
}