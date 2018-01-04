package com.mparticle.identity;


import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;
import com.mparticle.segmentation.SegmentListener;

import java.util.Map;

public class MParticleUser {
    private long mMpId;
    MParticleUserDelegate mUserDelegate;
    Cart cart;

    private MParticleUser(Context context, long mpId, MParticleUserDelegate userDelegate) {
        this.mMpId = mpId;
        this.mUserDelegate = userDelegate;
        this.cart = new Cart(context, mMpId);
    }

    static MParticleUser getInstance(Context context, long mpId, MParticleUserDelegate userDelegate) {
        return new MParticleUser(context, mpId, userDelegate);
    }

    public long getId() {
        return mMpId;
    }

    public Cart getCart() {
        return cart;
    }

    public Map<String, Object> getUserAttributes() {
        return mUserDelegate.getUserAttributes(getId());
    }

    public Map<String, Object> getUserAttributes(final UserAttributeListener listener) {
        return mUserDelegate.getUserAttributes(listener, getId());
    }

    public boolean setUserAttributes(Map<String, Object> userAttributes) {
        for(Map.Entry<String, Object> entry: userAttributes.entrySet()) {
            if (!setUserAttribute(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return mUserDelegate.getUserIdentities(getId());
    }

    void setUserIdentities(Map<MParticle.IdentityType, String> userIdentities) {
        for(Map.Entry<MParticle.IdentityType, String> entry: userIdentities.entrySet()) {
            mUserDelegate.setUserIdentity(entry.getValue(), entry.getKey(), getId());
        }
    }

    void setUserIdentity(MParticle.IdentityType identity, String value) {
        mUserDelegate.setUserIdentity(value, identity, getId());
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
        mUserDelegate = mParticleUserDelegate;
        return this;
    }
}