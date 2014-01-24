package com.mparticle;

/**
 * Created by sdozor on 1/23/14.
 */
interface PushRegistrationListener {
    void onRegistered(String regId);
    void onCleared(String regId);
}
