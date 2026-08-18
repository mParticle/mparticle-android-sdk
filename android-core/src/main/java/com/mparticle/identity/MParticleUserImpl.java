package com.mparticle.identity;


import android.content.Context;

import androidx.annotation.NonNull;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListenerType;
import com.mparticle.consent.ConsentState;
import com.mparticle.audience.AudienceResponse;
import com.mparticle.audience.AudienceTask;
import com.mparticle.internal.listeners.ApiClass;

import java.util.Map;

/**
 * A class which represents a User.
 */
@ApiClass
public class MParticleUserImpl implements MParticleUser {
    private long mMpId;
    MParticleUserDelegate mUserDelegate;

    private MParticleUserImpl(Context context, long mpId, MParticleUserDelegate userDelegate) {
        this.mMpId = mpId;
        this.mUserDelegate = userDelegate;
    }

    //unit testing only
    protected MParticleUserImpl() {
    }

    static MParticleUser getInstance(Context context, long mpId, MParticleUserDelegate userDelegate) {
        return new MParticleUserImpl(context, mpId, userDelegate);
    }

    @Override
    public long getId() {
        return mMpId;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return mUserDelegate.getUserAttributes(getId());
    }

    /**
     * Query the attributes of the User asynchronously.
     *
     * @param listener a callback for querying User's attributes
     * @return
     */
    public Map<String, Object> getUserAttributes(final UserAttributeListenerType listener) {
        return mUserDelegate.getUserAttributes(listener, getId());
    }

    @Override
    public boolean setUserAttributes(Map<String, Object> userAttributes) {
        boolean success = true;
        if (userAttributes == null) {
            return false;
        }
        for (Map.Entry<String, Object> entry : userAttributes.entrySet()) {
            if (!setUserAttribute(entry.getKey(), entry.getValue())) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return mUserDelegate.getUserIdentities(getId());
    }

    void setUserIdentities(Map<MParticle.IdentityType, String> userIdentities) {
        if (userIdentities == null) {
            return;
        }
        for (Map.Entry<MParticle.IdentityType, String> entry : userIdentities.entrySet()) {
            mUserDelegate.setUserIdentity(entry.getValue(), entry.getKey(), getId());
        }
    }

    void setUserIdentity(MParticle.IdentityType identity, String value) {
        mUserDelegate.setUserIdentity(value, identity, getId());
    }

    @Override
    public boolean setUserAttribute(String key, Object value) {
        return mUserDelegate.setUserAttribute(key, value, getId());
    }

    @Override
    public boolean setUserAttributeList(String key, Object value) {
        MParticle.logRoktApiUsage("SET_USER_ATTRIBUTE_LIST");
        return mUserDelegate.setUserAttributeList(key, value, getId());
    }

    @Override
    public boolean incrementUserAttribute(String key, Number value) {
        MParticle.logRoktApiUsage("INCREMENT_USER_ATTRIBUTE");
        return mUserDelegate.incrementUserAttribute(key, value, getId());
    }

    @Override
    public boolean removeUserAttribute(String key) {
        MParticle.logRoktApiUsage("REMOVE_USER_ATTRIBUTE");
        return mUserDelegate.removeUserAttribute(key, getId());
    }

    @Override
    public boolean setUserTag(@NonNull String tag) {
        MParticle.logRoktApiUsage("SET_USER_TAG");
        return setUserAttribute(tag, null);
    }


    MParticleUser setUserDelegate(MParticleUserDelegate mParticleUserDelegate) {
        mUserDelegate = mParticleUserDelegate;
        return this;
    }

    @Override
    public ConsentState getConsentState() {
        MParticle.logRoktApiUsage("GET_CONSENT_STATE");
        return mUserDelegate.getConsentState(getId());
    }

    @Override
    public void setConsentState(ConsentState state) {
        MParticle.logRoktApiUsage("SET_CONSENT_STATE");
        mUserDelegate.setConsentState(state, getId());
    }

    @Override
    public boolean isLoggedIn() {
        return mUserDelegate.isLoggedIn(getId());
    }

    @Override
    public long getFirstSeenTime() {
        return mUserDelegate.getFirstSeenTime(getId());
    }

    @Override
    public long getLastSeenTime() {
        return mUserDelegate.getLastSeenTime(getId());
    }

    @Override
    public AudienceTask<AudienceResponse> getUserAudiences() {
        MParticle.logRoktApiUsage("GET_USER_AUDIENCES");
        return mUserDelegate.getUserAudiences(getId());
    }

}