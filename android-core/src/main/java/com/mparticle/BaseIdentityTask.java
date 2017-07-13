package com.mparticle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.identity.IdentityHttpResponse;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseIdentityTask<T> extends MParticleTask<T> {
    boolean isCompleted;
    boolean isSuccessful;
    Exception mException;
    T result;
    Set<TaskSuccessListener<? super T>> successListeners = new HashSet<TaskSuccessListener<? super T>>();
    Set<TaskFailureListener> failureListeners = new HashSet<TaskFailureListener>();

    public void setFailed(IdentityHttpResponse.Error error) {
        setFailed(new Exception(error.getErrorString()));
    }

    public void setFailed(Exception exception) {
        isCompleted = true;
        isSuccessful = false;
        mException = exception;
        for(TaskFailureListener listener: failureListeners) {
            listener.onFailure(exception);
        }
    }

    public void setSuccessful(Object object) {
        isCompleted = true;
        isSuccessful = true;
        result = buildResult(object);

        for (TaskSuccessListener<? super T> listener: successListeners) {
            listener.onSuccess(this.result);
        }
    }

    public abstract T buildResult(Object o);

    @Override
    public boolean isComplete() {
        return isCompleted;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Nullable
    @Override
    public Exception getException() {
        return mException;
    }

    @NonNull
    @Override
    public MParticleTask<T> addSuccessListener(@NonNull TaskSuccessListener<? super T> listener) {
        if (listener != null) {
            successListeners.add(listener);
        }
        return this;
    }

    @NonNull
    @Override
    public MParticleTask<T> addFailureListener(@NonNull TaskFailureListener listener) {
        if (listener != null) {
            failureListeners.add(listener);
        }
        return this;
    }
}
