package com.mparticle.identity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticleTask;
import com.mparticle.TaskFailureListener;
import com.mparticle.TaskSuccessListener;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public abstract class BaseIdentityTask<T> extends MParticleTask<T> {
    boolean isCompleted;
    boolean isSuccessful;
    Exception mException;
    T result;
    Set<TaskSuccessListener<? super T>> successListeners = new HashSet<TaskSuccessListener<? super T>>();
    Set<TaskFailureListener> failureListeners = new HashSet<TaskFailureListener>();


    void setFailed(Exception exception) {
        isCompleted = true;
        isSuccessful = false;
        mException = exception;
        for(TaskFailureListener listener: failureListeners) {
            listener.onFailure(exception);
        }
    }

    void setSuccessful(Object object) {
        isCompleted = true;
        isSuccessful = true;
        T result = buildResult(object);

        for (TaskSuccessListener<? super T> listener: successListeners) {
            listener.onSuccess(this.result);
        }
    }

    abstract T buildResult(Object o);

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
