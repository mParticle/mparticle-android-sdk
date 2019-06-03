package com.mparticle.identity;


import android.content.Context;
import android.support.annotation.NonNull;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;
import com.mparticle.consent.ConsentState;
import com.mparticle.internal.listeners.ApiClass;
import com.mparticle.internal.listeners.InternalListenerManager;
import com.mparticle.segmentation.SegmentListener;

import java.util.Map;

/**
 * a class which represents a User
 */
@ApiClass
public class MParticleUserImpl implements MParticleUser {
    private long mMpId;
    MParticleUserDelegate mUserDelegate;
    Cart cart;

    private MParticleUserImpl(Context context, long mpId, MParticleUserDelegate userDelegate) {
        this.mMpId = mpId;
        this.mUserDelegate = userDelegate;
        this.cart = new Cart(context, mMpId);
    }

    //unit testing only
    protected MParticleUserImpl() {}

    static MParticleUser getInstance(Context context, long mpId, MParticleUserDelegate userDelegate) {
        return new MParticleUserImpl(context, mpId, userDelegate);
    }

    @Override
    public long getId() {
        return mMpId;
    }

    @Override
    public Cart getCart() {
        return cart;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return mUserDelegate.getUserAttributes(getId());
    }

    /**
     * query the attributes of the User asynchronously
     *
     * @param listener a callback for querying User's attributes
     *
     * @return
     */
    public Map<String, Object> getUserAttributes(final UserAttributeListener listener) {
        return mUserDelegate.getUserAttributes(listener, getId());
    }

    @Override
    public boolean setUserAttributes(Map<String, Object> userAttributes) {
        boolean success = true;
        if (userAttributes == null) {
            return false;
        }
        for(Map.Entry<String, Object> entry: userAttributes.entrySet()) {
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
        for(Map.Entry<MParticle.IdentityType, String> entry: userIdentities.entrySet()) {
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
        return mUserDelegate.setUserAttributeList(key, value, getId());
    }

    @Override
    public boolean incrementUserAttribute(String key, int value) {
        return mUserDelegate.incrementUserAttribute(key, value, getId());
    }

    @Override
    public boolean removeUserAttribute(String key) {
        return mUserDelegate.removeUserAttribute(key, getId());
    }

    @Override
    public boolean setUserTag(@NonNull String tag) {
        return setUserAttribute(tag, null);
    }

    public void getSegments(long timeout, String endpointId, SegmentListener listener) {
        mUserDelegate.getSegments(timeout, endpointId, listener);
    }

    MParticleUser setUserDelegate(MParticleUserDelegate mParticleUserDelegate) {
        mUserDelegate = mParticleUserDelegate;
        return this;
    }

    @Override
    public ConsentState getConsentState() {
        return mUserDelegate.getConsentState(getId());
    }

    @Override
    public void setConsentState(ConsentState state) {
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

}