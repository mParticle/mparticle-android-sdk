package com.mparticle.identity;


import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;
import com.mparticle.segmentation.SegmentListener;

import java.util.Map;

/**
 * a class which represents a User
 */
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

    /**
     * query the MPID of the User
     *
     * @return the mpid
     */
    public long getId() {
        return mMpId;
    }

    /**
     * query the Cart of the User
     *
     * @return the cart
     *
     * @see Cart
     */
    public Cart getCart() {
        return cart;
    }

    /**
     * query the attributes of the User
     *
     * @return the User's attributes
     */
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

    /**
     * assign attributes to the User in bulk
     *
     * @param userAttributes the attributes to be set
     *
     * @return whether the attributes where successfully set
     */
    public boolean setUserAttributes(Map<String, Object> userAttributes) {
        boolean success = true;
        for(Map.Entry<String, Object> entry: userAttributes.entrySet()) {
            if (!setUserAttribute(entry.getKey(), entry.getValue())) {
                success = false;
            }
        }
        return success;
    }

    /**
     * query the Identities of the User
     *
     * @return the User's Identities
     *
     */
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

    /**
     * set a single attribute for the user
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     *
     * @return whether the attributes where successfully set
     */
    public boolean setUserAttribute(String key, Object value) {
        return mUserDelegate.setUserAttribute(key, value, getId());
    }

    /**
     * set a single attribute for the user whos value is an Object, not just a String
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     *
     * @return whether the attributes where successfully set
     */
    public boolean setUserAttributeList(String key, Object value) {
        return mUserDelegate.setUserAttributeList(key, value, getId());
    }

    /**
     * increment an attribute for the user
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     *
     * @return whether the attributes where successfully set
     */
    public boolean incrementUserAttribute(String key, int value) {
        return mUserDelegate.incrementUserAttribute(key, value, getId());
    }

    /**
     * remove an attribute for the user
     *
     * @param key the key of the attribute which is to be removed
     *
     * @return whether the attributes where successfully removed
     */
    public boolean removeUserAttribute(String key) {
        return mUserDelegate.removeUserAttribute(key, getId());
    }

    /**
     * set a tag for a User. A tag is represented by a key and a value of "null"
     *
     * @param tag the tag to be set for the user
     *
     * @return whether the tag was sucessfully set
     */
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