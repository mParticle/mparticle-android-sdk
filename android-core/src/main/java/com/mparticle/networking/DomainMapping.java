package com.mparticle.networking;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint;

public class DomainMapping {
    private Endpoint mType;
    private String mUrl;
    private List<Certificate> mCertificates = new LinkedList<Certificate>();

    private DomainMapping(Builder builder) {
        this.mType = builder.type;
        this.mUrl = builder.newUrl;
        if (builder.certificates != null && builder.certificates.size() > 0) {
            this.mCertificates = new LinkedList<Certificate>(builder.certificates);
        } else {
            Logger.warning(String.format("Domain mapping for %s does not have any mCertificates, default mCertificates will be applied", builder.type.name()));
        }
    }

    @NonNull
    public static Builder configMapping(@Nullable String newUrl) {
        return new Builder(Endpoint.CONFIG, newUrl);
    }

    @NonNull
    public static Builder eventsMapping(@Nullable String newUrl) {
        return new Builder(Endpoint.EVENTS, newUrl);
    }

    @NonNull
    public static Builder identityMapping(@Nullable String newUrl) {
        return new Builder(Endpoint.IDENTITY, newUrl);
    }

    @NonNull
    public static Builder audienceMapping(@Nullable String newUrl) {
        return new Builder(Endpoint.AUDIENCE, newUrl);
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
        }
        catch (JSONException jse) {
            Logger.error(jse);
        }
        return jsonObject;
    }

    public static class Builder {
        Endpoint type;
        String newUrl;
        List<Certificate> certificates = new LinkedList<Certificate>();

        private Builder(Endpoint type) {
            this.type = type;
            this.newUrl = NetworkOptionsManager.getDefaultUrl(type);
        }

        private Builder(Endpoint type, String newUrl) {
            this.type = type;
            this.newUrl = newUrl;
        }

        @NonNull
        public Builder addCertificate(@NonNull Certificate certificate) {
            return addCertificate(certificate, null);
        }

        @NonNull
        public Builder addCertificate(@NonNull String alias, @NonNull String pin) {
            Certificate certificate = Certificate.with(alias, pin);
            if (certificate != null) {
                certificates.add(certificate);
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
        public Builder addCertificate(@NonNull String alias, @NonNull String pin, @Nullable Integer position) {
            addCertificate(Certificate.with(alias, pin), position);
            return this;
        }

        @NonNull
        public Builder setCertificates(@Nullable List<Certificate> certificates) {
            this.certificates = certificates;
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
            }
            catch (JSONException jse) {
                Logger.error(jse);
            }
            return null;
        }
    }
}