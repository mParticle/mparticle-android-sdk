package com.mparticle.identity;

public interface TaskFailureListener {
    void onFailure(IdentityHttpResponse result);
}