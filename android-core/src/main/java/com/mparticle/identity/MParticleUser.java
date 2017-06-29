package com.mparticle.identity;


import com.mparticle.MParticle;
import com.mparticle.segmentation.SegmentListener;

import java.util.Map;

public final class MParticleUser {

    private long mMpId;
    MParticleUserDelegate mUserDelegate;

    MParticleUser(long mpId, MParticleUserDelegate userDelegate) {
        mMpId = mpId;
        mUserDelegate = userDelegate;
    }

    public long getId() {
        return mMpId;
    }

    public Map<String, Object> getUserAttributes() {
        return mUserDelegate.getUserAttributes(getId());
    }

    void setUserAttributes(Map<String, Object> userAttributes) {
        for(Map.Entry<String, Object> entry: userAttributes.entrySet()) {
            setUserAttribute(entry.getKey(), entry.getValue());
        }
    }

    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return mUserDelegate.getUserIdentities(getId());
    }

    void setUserIdentities(Map<MParticle.IdentityType, String> userIdentities) {
        for(Map.Entry<MParticle.IdentityType, String> entry: userIdentities.entrySet()) {
            mUserDelegate.setUserIdentity(entry.getValue(), entry.getKey(), getId());
        }
    }

    public boolean setUserAttribute(String key, Object value) {
        return mUserDelegate.setUserAttribute(key, value, getId());
    }

    public boolean setUserAttributeList(String key, Object value) {
        return mUserDelegate.setUserAttributeList(key, value, getId());
    }

    public boolean incrementUserAttribute(String key, int value) {
        return mUserDelegate.incrementUserAttribute(key, value, getId());
    }

    public boolean removeUserAttribute(String key) {
        return mUserDelegate.removeUserAttribute(key, getId());
    }

    public boolean setUserTag(String tag) {
        return setUserAttribute(tag, null);
    }

    public void getSegments(long timeout, String endpointId, SegmentListener listener) {
        mUserDelegate.getSegments(timeout, endpointId, listener);
    }

    MParticleUser setUserDelegate(MParticleUserDelegate mParticleUserDelegate) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }
}