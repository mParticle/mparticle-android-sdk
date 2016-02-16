package com.mparticle.kits;

import org.json.JSONObject;

public class DeeplinkResult {
    private JSONObject parameters;
    private int serviceProviderId;
    private String linkUrl = null;

    void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }

    public JSONObject getParameter() {
        return parameters;
    }

    void setLink(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkUrl() {
        return this.linkUrl;
    }

    void setServiceProviderId(int id) {
        serviceProviderId = id;
    }

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
