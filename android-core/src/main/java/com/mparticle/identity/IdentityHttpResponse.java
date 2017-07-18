package com.mparticle.identity;

import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class IdentityHttpResponse {
    private ArrayList<Error> errors = new ArrayList<Error>();
    private long mpId;
    private String context;
    private int httpCode;

    private IdentityHttpResponse() {}

    public IdentityHttpResponse(int code, String errorString) {
        this.httpCode = code;
        this.errors.add(new Error("UNKNOWN", errorString));
    }

    public IdentityHttpResponse(int httpCode, JSONObject jsonObject) throws JSONException {
        this.httpCode = httpCode;
        if (jsonObject != null) {
            if (jsonObject.has("mpid")) {
                this.mpId = jsonObject.getLong("mpid");
                this.context = jsonObject.optString("context");
            } else if (jsonObject.has("errors")) {
                JSONArray errorsArray = jsonObject.optJSONArray("errors");
                if (!MPUtility.isEmpty(errorsArray)) {
                    for (int i = 0; i < errorsArray.length(); i++) {
                        try {
                            JSONObject object = errorsArray.getJSONObject(i);
                            String code = object.optString("code");
                            String message = object.optString("message");
                            this.errors.add(new Error(code, message));
                        } catch (JSONException ignore) {
                        }
                    }
                }
            } else {
                String code = jsonObject.optString("code");
                String message = jsonObject.optString("message");
                this.errors.add(new Error(code, message));
            }
        }

    }

    public boolean isSuccessful() {
        return httpCode == 200;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public long getMpId() {
        return mpId;
    }

    public String getContext() {
        return context;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public static class Error {
        public final String message;
        public final String code;

        public Error(String errorCode, String message) {
            this.code = errorCode;
            this.message = message;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Identity Response:\n");
        builder.append("Identity Response Code: " + httpCode + "\n");
        if (isSuccessful()) {
            builder.append("MPID: " + mpId + "\n");
            builder.append("Context: " + context + "\n");
        } else {
            for (Error error : errors) {
                builder.append("Code: " + error.code + "\n");
                builder.append("Message: " + error.message + "\n");
            }
        }
        return builder.toString();
    }
}