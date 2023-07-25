package com.mparticle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * Class representing the result of an attribution query to an integration partner.
 */
public class AttributionResult {
    private JSONObject parameters;
    private int serviceProviderId;
    private String linkUrl = null;

    @NonNull
    public AttributionResult setParameters(@Nullable JSONObject parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Parameters of an attribution. Use these parameters to programmatically route your users
     * and customize your interface.
     *
     * @return returns a JSONObject, may be null if the integration does not support attribution parameters.
     */
    @Nullable
    public JSONObject getParameters() {
        return parameters;
    }

    @NonNull
    public AttributionResult setLink(@Nullable String linkUrl) {
        this.linkUrl = linkUrl;
        return this;
    }

    @Nullable
    public String getLink() {
        return this.linkUrl;
    }

    @NonNull
    public AttributionResult setServiceProviderId(int id) {
        serviceProviderId = id;
        return this;
    }

    /**
     * Get the service provider or integration id associated with this result.
     *
     * @return the id of the associated integration
     * @see com.mparticle.MParticle.ServiceProviders
     */
    public int getServiceProviderId() {
        return serviceProviderId;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder("Attribution Result:\n");
        boolean empty = true;
        if (serviceProviderId > 0) {
            builder.append("Service provider ID:\n").append(serviceProviderId).append("\n");
            empty = false;
        }
        if (linkUrl != null) {
            builder.append("Link URL:\n").append(linkUrl).append("\n");
            empty = false;
        }
        if (parameters != null) {
            builder.append("Link parameters:\n").append(parameters.toString()).append("\n");
            empty = false;
        }
        if (empty) {
            builder.append("Empty");
        }
        return builder.toString();
    }
}
