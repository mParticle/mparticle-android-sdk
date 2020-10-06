package com.mparticle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.identity.TaskFailureListener;
import com.mparticle.identity.TaskSuccessListener;


public abstract class MParticleTask<MParticleTaskResult> {
    public MParticleTask() {
    }

    public abstract boolean isComplete();

    public abstract boolean isSuccessful();

    @Nullable
    public abstract MParticleTaskResult getResult();

    @NonNull
    public abstract MParticleTask<MParticleTaskResult> addSuccessListener(@NonNull TaskSuccessListener listener);

    @NonNull
    public abstract MParticleTask<MParticleTaskResult> addFailureListener(@NonNull TaskFailureListener listener);
}