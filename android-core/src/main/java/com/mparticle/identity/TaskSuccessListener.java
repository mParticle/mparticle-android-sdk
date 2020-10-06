package com.mparticle.identity;

import androidx.annotation.NonNull;

public interface TaskSuccessListener {
    void onSuccess(@NonNull IdentityApiResult result);
}