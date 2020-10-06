package com.mparticle.identity;

import androidx.annotation.Nullable;

public interface TaskFailureListener {
    void onFailure(@Nullable IdentityHttpResponse result);
}