package com.mparticle.networking;

import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.ALIAS;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.CONFIG;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.EVENTS;
import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint.IDENTITY;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkOptions {

    Map<Endpoint, DomainMapping> domainMappings = new HashMap<Endpoint, DomainMapping>();
    boolean pinningDisabledInDevelopment = false;

    private NetworkOptions() {
    }

    private NetworkOptions(Builder builder) {
        if (builder.domainMappings != null) {
            domainMappings = builder.domainMappings;
        }
        if (builder.pinningDisabledInDevelopment != null) {
            pinningDisabledInDevelopment = builder.pinningDisabledInDevelopment;
        }
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String podPrefix, boolean enable) {
        return new Builder(podPrefix, enable);
    }

    @Nullable
    public static NetworkOptions withNetworkOptions(@Nullable String jsonString) {
        if (MPUtility.isEmpty(jsonString)) {
            return null;
        }
        Builder builder = new Builder();
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            builder.setPinningDisabledInDevelopment(jsonObject.optBoolean("disableDevPinning", false));
            JSONArray domainMappingsJson = jsonObject.getJSONArray("domainMappings");
            for (int i = 0; i < domainMappingsJson.length(); i++) {
                builder.addDomainMapping(DomainMapping
                        .withDomainMapping(domainMappingsJson.getString(i))
                        .build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    @Nullable
    public DomainMapping getConfigDomain() {
        return domainMappings.get(CONFIG);
    }

    @Nullable
    public DomainMapping getEventsDomain() {
        return domainMappings.get(EVENTS);
    }

    @Nullable
    public DomainMapping getIdentityDomain() {
        return domainMappings.get(IDENTITY);
    }

    @Nullable
    public DomainMapping getAliasDomain() {
        return domainMappings.get(ALIAS);
    }

    @NonNull
    public List<DomainMapping> getDomainMappings() {
        return new ArrayList<DomainMapping>(domainMappings.values());
    }

    public boolean isPinningDisabledInDevelopment() {
        return pinningDisabledInDevelopment;
    }

    DomainMapping getDomain(Endpoint endpoint) {
        return domainMappings.get(endpoint);
    }

    @Override
    @NonNull
    public String toString() {
        return toJson().toString();
    }


    private JSONObject toJson() {
        JSONObject networkOptions = new JSONObject();
        try {
            JSONArray domainMappingsJson = new JSONArray();
            networkOptions.put("disableDevPinning", pinningDisabledInDevelopment);
            networkOptions.put("domainMappings", domainMappingsJson);
            for (DomainMapping domainMapping : domainMappings.values()) {
                domainMappingsJson.put(domainMapping.toString());
            }
        } catch (JSONException jse) {
            Logger.error(jse);
        }
        return networkOptions;
    }

    public static class Builder {
        private Map<Endpoint, DomainMapping> domainMappings = new HashMap<Endpoint, DomainMapping>();
        private Boolean pinningDisabledInDevelopment;
        private String podPrefix = "us1";
        private boolean enablePodRedirection = true;

        private Builder() {
        }

        private Builder(String podPrefix, boolean enable){
            this.podPrefix = podPrefix;
            this.enablePodRedirection = enable;
        }

        @NonNull
        public Builder addDomainMapping(@Nullable DomainMapping domain) {
            if (domainMappings == null) {
                domainMappings = new HashMap<Endpoint, DomainMapping>();
            }
            if (domainMappings.containsKey(domain.getType())) {
                try {
                    Logger.warning("Duplicate DomainMapping submitted, DomainMapping:\n" + domain.toJson().toString(4) + "\n will overwrite DomainMapping:\n" + domain.toJson().toString(4));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (domain.getType() == EVENTS && !domain.isEventsOnly() && !domainMappings.containsKey(ALIAS)) {
                domainMappings.put(ALIAS, domain);
            }
            domainMappings.put(domain.getType(), domain);
            return this;
        }

        @NonNull
        public Builder setDomainMappings(@Nullable List<DomainMapping> domainMappingsList) {
            if (domainMappingsList == null) {
                domainMappings = new HashMap<Endpoint, DomainMapping>();
                return this;
            }
            for (DomainMapping domainMapping : domainMappingsList) {
                addDomainMapping(domainMapping);
            }
            return this;
        }

        @NonNull
        public Builder setPinningDisabledInDevelopment(boolean disabledInDevelopment) {
            this.pinningDisabledInDevelopment = disabledInDevelopment;
            return this;
        }

        @NonNull
        public NetworkOptions build() {
            return new NetworkOptions(this);
        }

    }
}
