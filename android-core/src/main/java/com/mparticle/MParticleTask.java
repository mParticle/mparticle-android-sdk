package com.mparticle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.identity.IdentityApiResult;
import com.mparticle.identity.IdentityHttpResponse;


public abstract class MParticleTask<MParticleTaskResult> {
    public MParticleTask() {
    }

    public abstract boolean isComplete();

    public abstract boolean isSuccessful();

    public abstract MParticleTaskResult getResult();

    @NonNull
    public abstract MParticleTask<MParticleTaskResult> addSuccessListener(@NonNull TaskSuccessListener listener);

    @NonNull
    public abstract MParticleTask<MParticleTaskResult> addFailureListener(@NonNull TaskFailureListener listener);
}