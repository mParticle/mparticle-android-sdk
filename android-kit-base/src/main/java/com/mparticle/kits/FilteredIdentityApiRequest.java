package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;

import java.util.HashMap;
import java.util.Map;

public class FilteredIdentityApiRequest {
    KitIntegration provider;
    Map<MParticle.IdentityType, String> oldIdentities = new HashMap<>();
    Map<MParticle.IdentityType, String> newIdentities = new HashMap<>();
    
    FilteredIdentityApiRequest(IdentityApiRequest identityApiRequest, KitIntegration provider) {
        if (identityApiRequest != null) {
            oldIdentities = new HashMap<>(identityApiRequest.getOldIdentities());
            newIdentities = new HashMap<>(identityApiRequest.getUserIdentities());
        }
        this.provider = provider;
    }

    public Map<MParticle.IdentityType, String> getOldIdentities() {
        Map<MParticle.IdentityType, String> identities = oldIdentities;
        Map<MParticle.IdentityType, String> filteredIdentities = new HashMap<MParticle.IdentityType, String>(identities.size());
        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            if (provider.getConfiguration().shouldSetIdentity(entry.getKey())) {
                filteredIdentities.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredIdentities;
    }

    public Map<MParticle.IdentityType, String> getNewIdentities() {
        Map<MParticle.IdentityType, String> identities = newIdentities;
        Map<MParticle.IdentityType, String> filteredIdentities = new HashMap<MParticle.IdentityType, String>(identities.size());
        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            if (provider.getConfiguration().shouldSetIdentity(entry.getKey())) {
                filteredIdentities.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredIdentities;
    }
}
