package com.mparticle.networking;

import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DomainMapping {
    private Endpoint mType;
    private String mUrl;
    private List<Certificate> mCertificates = new LinkedList<Certificate>();
    private boolean overridesSubdirectory;
    private boolean eventsOnly;

    private DomainMapping(Builder builder) {
        this.mType = builder.type;
        this.mUrl = builder.newUrl;
        if (builder.certificates != null && builder.certificates.size() > 0) {
            this.mCertificates = new LinkedList<Certificate>(builder.certificates);
        } else {
            Logger.warning(String.format("Domain mapping for %s does not have any mCertificates, default mCertificates will be applied", builder.type.name()));
        }
        this.overridesSubdirectory = builder.overridesSubdirectory;
        this.eventsOnly = builder.eventsOnly;
    }

    /**
     * Override the url of outbound Config requests
     *
     * @param newUrl the new domain portion of the url
     * @return the Builder instance
     */
    @NonNull
    public static Builder configMapping(@Nullable String newUrl) {
        return configMapping(newUrl, false);
    }

    /**
     * Override the url of outbound Config requests. The subdirectory of the url will also be
     * overridden if {@param overridesSubdirectory} is true
     *
     * @param newUrl                the new domain portion of the url
     * @param overridesSubdirectory indicate whether the new domain includes the subdirectory
     * @return the Builder instance
     */
    public static Builder configMapping(@Nullable String newUrl, boolean overridesSubdirectory) {
        return new Builder(Endpoint.CONFIG, newUrl, overridesSubdirectory);
    }

    /**
     * Override the url of outbound Events requests, including Alias events
     *
     * @param newUrl the new domain portion of the url
     * @return the Builder instance
     */
    @NonNull
    public static Builder eventsMapping(@Nullable String newUrl) {
        return eventsMapping(newUrl, false);
    }

    /**
     * Override the url of outbound Events requests, including Alias events. The subdirectory of the url will also be
     * overridden if {@param overridesSubdirectory} is true
     *
     * @param newUrl                the new domain portion of the url
     * @param overridesSubdirectory indicate whether the new domain includes the subdirectory
     * @return the Builder instance
     */
    public static Builder eventsMapping(@Nullable String newUrl, boolean overridesSubdirectory) {
        return eventsMapping(newUrl, overridesSubdirectory, false);
    }

    /**
     * Override the url of outbound Events requests. The subdirectory of the url will also be
     * overridden if {@param overridesSubdirectory} is true. {@param eventsOnly} with indicate if
     * this mapping should only be applied to Events requests, and specifically not Alias events
     *
     * @param newUrl                the new domain portion of the url
     * @param overridesSubdirectory indicate whether the new domain includes the subdirectory
     * @param eventsOnly            indicates whether this mapping should only apply to core, non-alias events
     * @return the Builder instance
     */
    public static Builder eventsMapping(@Nullable String newUrl, boolean overridesSubdirectory, boolean eventsOnly) {
        return new Builder(Endpoint.EVENTS, newUrl, overridesSubdirectory)
                .setEventsOnly(eventsOnly);
    }

    /**
     * Override the url of outbound Alias events requests.
     *
     * @param newUrl the new domain portion of the url
     * @return the Builder instance
     */
    public static Builder aliasMapping(@Nullable String newUrl) {
        return aliasMapping(newUrl, false);
    }

    /**
     * Override the url of outbound Alias events requests. The subdirectory of the url will also be
     * overridden if {@param overridesSubdirectory} is true
     *
     * @param newUrl                the new domain portion of the url
     * @param overridesSubdirectory indicate whether the new domain includes the subdirectory
     * @return the Builder instance
     */
    public static Builder aliasMapping(@Nullable String newUrl, boolean overridesSubdirectory) {
        return new Builder(Endpoint.ALIAS, newUrl, overridesSubdirectory);
    }


    /**
     * Override the url of outbound Identity requests
     *
     * @param newUrl the new domain portion of the url
     * @return the Builder instance
     */
    @NonNull
    public static Builder identityMapping(@Nullable String newUrl) {
        return identityMapping(newUrl, false);
    }

    /**
     * Override the url of outbound Identity requests. The subdirectory of the url will also be
     * overridden if {@param overridesSubdirectory} is true
     *
     * @param newUrl                the new domain portion of the url
     * @param overridesSubdirectory indicate whether the new domain includes the subdirectory
     * @return the Builder instance
     */
    public static Builder identityMapping(@Nullable String newUrl, boolean overridesSubdirectory) {
        return new Builder(Endpoint.IDENTITY, newUrl, overridesSubdirectory);
    }

    /**
     * Override the url of outbound Audience requests
     *
     * @param newUrl the new domain portion of the url
     * @return the Builder instance
     */
    @NonNull
    public static Builder audienceMapping(@Nullable String newUrl) {
        return audienceMapping(newUrl, false);
    }

    /**
     * Override the url of outbound Audience requests. The subdirectory of the url will also be
     * overridden if {@param overridesSubdirectory} is true
     *
     * @param newUrl                the new domain portion of the url
     * @param overridesSubdirectory indicate whether the new domain includes the subdirectory
     * @return the Builder instance
     */
    public static Builder audienceMapping(@Nullable String newUrl, boolean overridesSubdirectory) {
        return new Builder(Endpoint.AUDIENCE, newUrl, overridesSubdirectory);
    }

    static Builder withEndpoint(Endpoint endpoint) {
        return new Builder(endpoint);
    }

    static Builder withDomainMapping(String jsonObject) {
        return Builder.withJson(jsonObject);
    }

    Endpoint getType() {
        return mType;
    }

    @Nullable
    public String getUrl() {
        return mUrl;
    }

    void setUrl(String url) {
        this.mUrl = url;
    }

    @NonNull
    public List<Certificate> getCertificates() {
        return new LinkedList<Certificate>(mCertificates);
    }

    void setCertificates(List<Certificate> certificates) {
        if (MPUtility.isEmpty(certificates)) {
            this.mCertificates = new ArrayList<Certificate>();
        } else {
            this.mCertificates = new ArrayList<Certificate>(certificates);
        }
    }

    public boolean isOverridesSubdirectory() {
        return overridesSubdirectory;
    }

    boolean isEventsOnly() {
        return eventsOnly;
    }

    @Override
    @NonNull
    public String toString() {
        return toJson().toString();
    }

    JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray certificatesJson = new JSONArray();
            jsonObject.put("mCertificates", certificatesJson);
            if (mCertificates != null) {
                for (Certificate certificate : mCertificates) {
                    certificatesJson.put(certificate.toJson());
                }
            }
            return new JSONObject()
                    .put("mType", mType.value)
                    .put("url", mUrl)
                    .put("mCertificates", certificatesJson);
        } catch (JSONException jse) {
            Logger.error(jse);
        }
        return jsonObject;
    }

    public static class Builder {
        Endpoint type;
        String newUrl;
        List<Certificate> certificates = new LinkedList<Certificate>();
        boolean overridesSubdirectory = false;
        boolean eventsOnly;

        private Builder(Endpoint type) {
            this.type = type;
            this.newUrl = NetworkOptionsManager.getDefaultUrl(type);
        }

        private Builder(Endpoint type, String newUrl) {
            this.type = type;
            this.newUrl = newUrl;
        }

        private Builder(Endpoint type, String newUrl, boolean overridesSubdirectory) {
            this.type = type;
            this.newUrl = newUrl;
            this.overridesSubdirectory = overridesSubdirectory;
        }


        @NonNull
        public Builder addCertificate(@NonNull Certificate certificate) {
            return addCertificate(certificate, null);
        }

        @NonNull
        public Builder addCertificate(@NonNull String alias, @NonNull String certificate) {
            Certificate certificateInstance = Certificate.with(alias, certificate);
            if (certificateInstance != null) {
                certificates.add(certificateInstance);
            }
            return this;
        }

        @NonNull
        public Builder addCertificate(@NonNull Certificate certificate, @Nullable Integer position) {
            if (certificate != null) {
                if (certificates == null) {
                    certificates = new LinkedList<Certificate>();
                }
                if (position != null) {
                    certificates.add(position, certificate);
                } else {
                    certificates.add(certificate);
                }
            } else {
                String message = "NetworkOptions issue: Certificate is null, cannot be added.";
                if (MPUtility.isDevEnv()) {
                    throw new IllegalArgumentException(message);
                } else {
                    Logger.error(message);
                }
            }
            return this;
        }

        @NonNull
        public Builder addCertificate(@NonNull String alias, @NonNull String certificate, @Nullable Integer position) {
            addCertificate(Certificate.with(alias, certificate), position);
            return this;
        }

        @NonNull
        public Builder setCertificates(@Nullable List<Certificate> certificates) {
            this.certificates = new ArrayList<Certificate>();
            for (Certificate certificate : certificates) {
                if (certificate != null) {
                    addCertificate(certificate);
                }
            }
            return this;
        }

        Builder setEventsOnly(boolean eventsOnly) {
            this.eventsOnly = eventsOnly;
            return this;
        }

        @NonNull
        public DomainMapping build() {
            return new DomainMapping(this);
        }

        private static Builder withJson(String jsonString) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                int type = jsonObject.getInt("mType");
                String newUrl = jsonObject.getString("url");
                Builder builder = new Builder(Endpoint.parseInt(type), newUrl);
                JSONArray certificatesJsonArray = jsonObject.getJSONArray("mCertificates");
                for (int i = 0; i < certificatesJsonArray.length(); i++) {
                    builder.addCertificate(Certificate.withCertificate(certificatesJsonArray.getJSONObject(i)));
                }
                return builder;
            } catch (JSONException jse) {
                Logger.error(jse);
            }
            return null;
        }
    }
}