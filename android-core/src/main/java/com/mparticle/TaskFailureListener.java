package com.mparticle;

import com.mparticle.identity.IdentityHttpResponse;

public interface TaskFailureListener {
    void onFailure(IdentityHttpResponse result);
}
