package com.mparticle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


public abstract class MParticleTask<MParticleTaskResult> {
    public MParticleTask() {
    }

    public abstract boolean isComplete();

    public abstract boolean isSuccessful();

    public abstract MParticleTaskResult getResult();

    @Nullable
    public abstract Exception getException();

    @NonNull
    public abstract MParticleTask<MParticleTaskResult> addSuccessListener(@NonNull TaskSuccessListener<? super MParticleTaskResult> listener);

    @NonNull
    public abstract MParticleTask<MParticleTaskResult> addFailureListener(@NonNull TaskFailureListener listener);
}