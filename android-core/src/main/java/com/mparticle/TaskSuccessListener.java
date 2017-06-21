package com.mparticle;

public interface TaskSuccessListener<TResult> {
    void onSuccess(TResult result);
}
