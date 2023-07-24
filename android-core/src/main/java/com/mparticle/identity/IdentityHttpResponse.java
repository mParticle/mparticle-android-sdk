package com.mparticle.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private boolean loggedIn;

    @NonNull
    public static final String MPID = "mpid";
    @NonNull
    public static final String CONTEXT = "context";
    @NonNull
    public static final String ERRORS = "errors";
    @NonNull
    public static final String LOGGED_IN = "is_logged_in";
    @NonNull
    public static final String CODE = "code";
    @NonNull
    public static final String MESSAGE = "message";

    @NonNull
    public static final String UNKNOWN = "UNKNOWN";

    private IdentityHttpResponse() {
    }

    public IdentityHttpResponse(int code, long mpid, @Nullable String context, @Nullable ArrayList<Error> errors) {
        this.httpCode = code;
        this.mpId = mpid;
        this.context = context;
        this.errors = errors == null ? new ArrayList<Error>() : new ArrayList<Error>(errors);
    }

    public IdentityHttpResponse(int code, @NonNull String errorString) {
        this.httpCode = code;
        this.errors.add(new Error(UNKNOWN, errorString));
    }

    public IdentityHttpResponse(int httpCode, @Nullable JSONObject jsonObject) throws JSONException {
        this.httpCode = httpCode;
        if (!MPUtility.isEmpty(jsonObject)) {
            if (jsonObject.has(MPID)) {
                this.mpId = Long.valueOf(jsonObject.getString(MPID));
                this.context = jsonObject.optString(CONTEXT);
                this.loggedIn = jsonObject.optBoolean(LOGGED_IN);
            } else if (jsonObject.has(ERRORS)) {
                JSONArray errorsArray = jsonObject.optJSONArray(ERRORS);
                if (!MPUtility.isEmpty(errorsArray)) {
                    for (int i = 0; i < errorsArray.length(); i++) {
                        try {
                            JSONObject object = errorsArray.getJSONObject(i);
                            String code = object.optString(CODE);
                            String message = object.optString(MESSAGE);
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

    @NonNull
    public List<Error> getErrors() {
        return errors;
    }

    public long getMpId() {
        return mpId;
    }

    @Nullable
    public String getContext() {
        return context;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public static class Error {
        @NonNull
        public final String message;
        @NonNull
        public final String code;

        public Error(@NonNull String errorCode, @NonNull String message) {
            this.code = errorCode;
            this.message = message;
        }
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Identity Response:\n");
        builder.append("Identity Response Code: " + httpCode + "\n");
        if (isSuccessful()) {
            builder.append("MPID: " + mpId + "\n");
            builder.append("Context: " + context + "\n");
            builder.append("Is Logged In: " + loggedIn + "\n");
        } else {
            for (Error error : errors) {
                builder.append("Code: " + error.code + "\n");
                builder.append("Message: " + error.message + "\n");
            }
        }
        return builder.toString();
    }
}