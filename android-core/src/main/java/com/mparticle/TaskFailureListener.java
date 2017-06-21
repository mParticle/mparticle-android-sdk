package com.mparticle;

public interface TaskFailureListener<TResult> {
    void onFailure(TResult result);
}
