package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.commerce.Cart;

import java.util.Map;

public interface MParticleUser {

    /**
     * query the MPID of the User
     *
     * @return the mpid
     */
    long getId();

    /**
     * query the Cart of the User
     *
     * @return the cart
     *
     * @see Cart
     */
    Cart getCart();

    /**
     * query the attributes of the User
     *
     * @return the User's attributes
     */
    Map<String, Object> getUserAttributes();

    /**
     * query the attributes of the User asynchronously
     *
     * @param listener a callback for querying User's attributes
     *
     * @return
     */
    Map<String, Object> getUserAttributes(final UserAttributeListener listener);

    /**
     * assign attributes to the User in bulk
     *
     * @param userAttributes the attributes to be set
     *
     * @return whether the attributes where successfully set
     */
    boolean setUserAttributes(Map<String, Object> userAttributes);

    /**
     * query the Identities of the User
     *
     * @return the User's Identities
     *
     */
    Map<MParticle.IdentityType, String> getUserIdentities();

    /**
     * set a single attribute for the user
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     *
     * @return whether the attributes where successfully set
     */
    boolean setUserAttribute(String key, Object value);

    /**
     * set a single attribute for the user whos value is an Object, not just a String
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     *
     * @return whether the attributes where successfully set
     */
    boolean setUserAttributeList(String key, Object value);

    /**
     * increment an attribute for the user
     *
     * @param key the key of the attribute
     * @param value the value of the attribute
     *
     * @return whether the attributes where successfully set
     */
    boolean incrementUserAttribute(String key, int value);

    /**
     * remove an attribute for the user
     *
     * @param key the key of the attribute which is to be removed
     *
     * @return whether the attributes where successfully removed
     */
    boolean removeUserAttribute(String key);

    /**
     * set a tag for a User. A tag is represented by a key and a value of "null"
     *
     * @param tag the tag to be set for the user
     *
     * @return whether the tag was successfully set
     */
    boolean setUserTag(String tag);
}