package com.mparticle;

import org.json.JSONObject;

/**
 * Class representing the result of a deep link query to an integration partner.
 */
public class DeepLinkResult {
    private JSONObject parameters;
    private int serviceProviderId;
    private String linkUrl = null;

    public DeepLinkResult setParameters(JSONObject parameters) {
        this.parameters = parameters;
        return this;
    }

    /**
     * Parameters of a deep link. Use these parameters to programmatically route your users
     * and customize your interface.
     *
     * @return returns a JSONObject, may be null if the integration does not support deep link parameters.
     */
    public JSONObject getParameters() {
        return parameters;
    }


    public DeepLinkResult setLink(String linkUrl) {
        this.linkUrl = linkUrl;
        return this;
    }

    public String getLink() {
        return this.linkUrl;
    }

    public DeepLinkResult setServiceProviderId(int id) {
        serviceProviderId = id;
        return this;
    }

    /**
     * Get the service provider or integration id associated with this result.
     *
     * @see com.mparticle.MParticle.ServiceProviders
     *
     * @return the id of the associated integration
     */
    public int getServiceProviderId() {
        return serviceProviderId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Deep Link Result:\n");
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
