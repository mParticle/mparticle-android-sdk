package com.mparticle;

import com.mparticle.identity.IdentityApiResult;

public interface TaskSuccessListener {
    void onSuccess(IdentityApiResult result);
}
