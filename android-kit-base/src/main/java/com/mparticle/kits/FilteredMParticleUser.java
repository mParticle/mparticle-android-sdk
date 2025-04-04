package com.mparticle.kits;

import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;

import com.mparticle.MParticle;
import com.mparticle.TypedUserAttributeListener;
import com.mparticle.UserAttributeListener;
import com.mparticle.UserAttributeListenerType;
import com.mparticle.consent.ConsentState;
import com.mparticle.audience.AudienceResponse;
import com.mparticle.audience.AudienceTask;
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
        MParticle instance = MParticle.getInstance();
        if (instance != null) {
            MParticleUser user = instance.Identity().getUser(mpid);
            if (user != null) {
                return new FilteredMParticleUser(user, provider);
            }
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
        Map<String, Object> userAttributes = mpUser.getUserAttributes();
        KitManagerImpl kitManager = provider.getKitManager();
        if (kitManager != null) {
            userAttributes = kitManager.getDataplanFilter().transformUserAttributes(userAttributes);
        }
        return (Map<String, Object>) KitConfiguration.filterAttributes(
                provider.getConfiguration().getUserAttributeFilters(),
                userAttributes
        );
    }

    @Override
    public Map<String, Object> getUserAttributes(final UserAttributeListenerType listener) {
        return mpUser.getUserAttributes(new TypedUserAttributeListener() {
            @Override
            public void onUserAttributesReceived(@NonNull Map<String, ?> userAttributes, @NonNull Map<String, ? extends List<String>> userAttributeLists, long mpid) {
                KitManagerImpl kitManager = provider.getKitManager();
                if (kitManager != null) {
                    userAttributes = kitManager.getDataplanFilter().transformUserAttributes(userAttributes);
                    userAttributeLists = kitManager.getDataplanFilter().transformUserAttributes(userAttributeLists);
                }
                SparseBooleanArray filters = provider.getConfiguration().getUserAttributeFilters();
                if (userAttributes == null) {
                    userAttributes = new HashMap<>();
                }
                if (listener instanceof UserAttributeListener) {
                    Map<String, String> stringifiedAttributes = new HashMap<>();
                    for (Map.Entry<String, ?> entry : userAttributes.entrySet()) {
                        if (entry.getValue() != null) {
                            stringifiedAttributes.put(entry.getKey(), entry.getValue().toString());
                        } else {
                            stringifiedAttributes.put(entry.getKey(), null);
                        }
                    }
                    ((UserAttributeListener) listener).onUserAttributesReceived(
                            (Map<String, String>) KitConfiguration.filterAttributes(filters, stringifiedAttributes),
                            (Map<String, List<String>>) KitConfiguration.filterAttributes(filters, userAttributeLists),
                            mpid);
                }
                if (listener instanceof TypedUserAttributeListener) {
                    ((TypedUserAttributeListener) listener).onUserAttributesReceived(
                            KitConfiguration.filterAttributes(filters, userAttributes),
                            (Map<String, List<String>>) KitConfiguration.filterAttributes(filters, userAttributeLists),
                            mpid);
                }
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
        KitManagerImpl kitManager = provider.getKitManager();
        if (kitManager != null) {
            identities = kitManager.getDataplanFilter().transformIdentities(identities);
        }
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
    public boolean incrementUserAttribute(String key, Number value) {
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
        return mpUser.getConsentState();
    }

    @Override
    public void setConsentState(ConsentState state) {
        mpUser.setConsentState(state);
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

    @Override
    public AudienceTask<AudienceResponse> getUserAudiences() {
        return null;
    }
}