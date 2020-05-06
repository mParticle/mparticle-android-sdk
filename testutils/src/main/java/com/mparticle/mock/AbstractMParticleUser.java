package com.mparticle.mock;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

import java.util.Map;

public class AbstractMParticleUser implements MParticleUser {

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return null;
    }

    @Nullable
    @Override
    public Map<String, Object> getUserAttributes(@Nullable UserAttributeListener listener) {
        return null;
    }

    @Override
    public boolean setUserAttributes(@NonNull Map<String, Object> userAttributes) {
        return false;
    }

    @Override
    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return null;
    }

    @Override
    public boolean setUserAttribute(@NonNull String key, @NonNull Object value) {
        return false;
    }

    @Override
    public boolean setUserAttributeList(@NonNull String key, @NonNull Object value) {
        return false;
    }

    @Override
    public boolean incrementUserAttribute(@NonNull String key, int value) {
        return false;
    }

    @Override
    public boolean removeUserAttribute(@NonNull String key) {
        return false;
    }

    @Override
    public boolean setUserTag(@NonNull String tag) {
        return false;
    }

    @Override
    public ConsentState getConsentState() {
        return null;
    }

    @Override
    public void setConsentState(@Nullable ConsentState state) {
        //do nothing
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public long getFirstSeenTime() {
        return 0;
    }

    @Override
    public long getLastSeenTime() {
        return 0;
    }
}
