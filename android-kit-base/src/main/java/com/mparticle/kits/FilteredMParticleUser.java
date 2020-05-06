package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.UserAttributeListener;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.MParticleUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilteredMParticleUser implements MParticleUser {
    MParticleUser mpUser;
    KitIntegration provider;

    private FilteredMParticleUser(MParticleUser mpUserImpl, KitIntegration provider) {
        this.mpUser = mpUserImpl;
        this.provider = provider;
    }

    static FilteredMParticleUser getInstance(MParticleUser user, KitIntegration provider) {
        if (user != null) {
            return new FilteredMParticleUser(user, provider);
        }
        return null;
    }

    static FilteredMParticleUser getInstance(long mpid, KitIntegration provider) {
        MParticleUser user = MParticle.getInstance().Identity().getUser(mpid);
        if (user != null) {
            return new FilteredMParticleUser(user, provider);
        }
        return null;
    }

    @Override
    public long getId() {
        return mpUser.getId();
    }

    /**
     * Retrieve filtered user identities. User this method to retrieve user identities at any time.
     * To ensure that filtering is respected, kits must use this method rather than the public API.
     *
     * @return a Map of identity-types and identity-values
     */
    @Override
    public Map<String, Object> getUserAttributes() {
        return (Map<String, Object>)KitConfiguration.filterAttributes(
                provider.getConfiguration().getUserAttributeFilters(),
                mpUser.getUserAttributes()
        );
    }

    @Override
    public Map<String, Object> getUserAttributes(final UserAttributeListener listener) {
        return mpUser.getUserAttributes(new UserAttributeListener() {
            @Override
            public void onUserAttributesReceived(Map<String, String> userAttributes, Map<String, List<String>> userAttributeLists, Long mpid) {
                listener.onUserAttributesReceived((Map<String, String>)KitConfiguration.filterAttributes(
                        provider.getConfiguration().getUserAttributeFilters(),
                        userAttributes),
                        (Map<String, List<String>>)KitConfiguration.filterAttributes(
                                provider.getConfiguration().getUserAttributeFilters(),
                                userAttributeLists), mpid);
            }
        });
    }

    @Override
    public boolean setUserAttributes(Map<String, Object> userAttributes) {
        return false;
    }

    @Override
    public Map<MParticle.IdentityType, String> getUserIdentities() {
        Map<MParticle.IdentityType, String> identities = mpUser.getUserIdentities();
        Map<MParticle.IdentityType, String> filteredIdentities = new HashMap<MParticle.IdentityType, String>(identities.size());
        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            if (provider.getConfiguration().shouldSetIdentity(entry.getKey())) {
                filteredIdentities.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredIdentities;
    }

    @Override
    public boolean setUserAttribute(String key, Object value) {
        return false;
    }

    @Override
    public boolean setUserAttributeList(String key, Object value) {
        return false;
    }

    @Override
    public boolean incrementUserAttribute(String key, int value) {
        return false;
    }

    @Override
    public boolean removeUserAttribute(String key) {
        return false;
    }

    @Override
    public boolean setUserTag(String tag) {
        return false;
    }

    @Override
    public ConsentState getConsentState() {
        return null;
    }

    @Override
    public void setConsentState(ConsentState state) {

    }

    @Override
    public boolean isLoggedIn() {
        return mpUser.isLoggedIn();
    }

    @Override
    public long getFirstSeenTime() {
        return mpUser.getFirstSeenTime();
    }

    @Override
    public long getLastSeenTime() {
        return mpUser.getLastSeenTime();
    }
}