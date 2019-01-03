package com.mparticle.identity;

import android.support.annotation.NonNull;

public interface TaskSuccessListener {
    void onSuccess(@NonNull IdentityApiResult result);
}