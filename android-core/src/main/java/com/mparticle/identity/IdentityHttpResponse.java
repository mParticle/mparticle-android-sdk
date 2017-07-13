package com.mparticle.identity;

import org.json.JSONException;
import org.json.JSONObject;

public class IdentityHttpResponse {
    private long mpId;
    private String context;

    private IdentityHttpResponse() {}

    public IdentityHttpResponse(JSONObject jsonObject) throws JSONException {
        this.mpId = jsonObject.getLong("mpid");
        this.context = jsonObject.optString("context");
    }

    public boolean hasError() {
        return false;
    }

    public Error getError() {
        return null;
    }

    public long getMpId() {
        return mpId;
    }

    public String getContext() {
        return context;
    }

    public static class Error extends IdentityHttpResponse {
        private String error;

        public Error(String error) {
            this.error = error;
        }

        public String getErrorString() {
            return error;
        }

        @Override
        public boolean hasError() {
            return true;
        }

        @Override
        public Error getError() {
            return this;
        }
    }
}
