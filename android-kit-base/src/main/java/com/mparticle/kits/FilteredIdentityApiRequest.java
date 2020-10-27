package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;

import java.util.HashMap;
import java.util.Map;

public class FilteredIdentityApiRequest {
    KitIntegration provider;
    Map<MParticle.IdentityType, String> userIdentities = new HashMap<>();
    
    FilteredIdentityApiRequest(IdentityApiRequest identityApiRequest, KitIntegration provider) {
        if (identityApiRequest != null) {
            userIdentities = new HashMap<>(identityApiRequest.getUserIdentities());
            if (provider.getKitManager()!= null) {
                userIdentities = provider.getKitManager().getDataplanFilter().transformIdentities(userIdentities);
            }
        }
        this.provider = provider;
    }

    @Deprecated
    public Map<MParticle.IdentityType, String> getNewIdentities() {
        return getUserIdentities();
    }

    public Map<MParticle.IdentityType, String> getUserIdentities() {
        Map<MParticle.IdentityType, String> identities = userIdentities;
        Map<MParticle.IdentityType, String> filteredIdentities = new HashMap<MParticle.IdentityType, String>(identities.size());
        for (Map.Entry<MParticle.IdentityType, String> entry : identities.entrySet()) {
            if (provider.getConfiguration().shouldSetIdentity(entry.getKey())) {
                filteredIdentities.put(entry.getKey(), entry.getValue());
            }
        }
        return filteredIdentities;
    }
}
