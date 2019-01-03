package com.mparticle.identity;

import android.support.annotation.Nullable;

public interface TaskFailureListener {
    void onFailure(@Nullable IdentityHttpResponse result);
}