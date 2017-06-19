package com.mparticle.identity;


import com.mparticle.MParticle;
import com.mparticle.segmentation.SegmentListener;

import java.util.Map;

public final class MParticleUser {

    private Map<String, Object> userAttributes;
    private Map<MParticle.IdentityType, String> userIdentities;
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    public long getId() {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    void setUserAttributes(Map<String, Object> userAttributes) {
        this.userAttributes = userAttributes;
    }

    public Map<MParticle.IdentityType, String> getUserIdentities() {
        return userIdentities;
    }

    void setUserIdentities(Map<MParticle.IdentityType, String> userIdentities) {
        this.userIdentities = userIdentities;
    }

    public boolean setUserAttribute(String key, Object value) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public boolean setUserAttributeList(String key, Object value) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public boolean incrementUserAttribute(String key, Object value) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public boolean removeUserAttribute(String key) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public boolean setUserTag(String tag) {
        throw new UnsupportedOperationException("Not implemented yet...");
    }

    public void getSegments(long timeout, String endpointId, SegmentListener listener) {

    }
}