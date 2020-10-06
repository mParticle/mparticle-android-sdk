package com.mparticle.consent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class ConsentInstance {
    static final String SERIALIZED_KEY_CONSENTED = "consented";
    static final String SERIALIZED_KEY_TIMESTAMP = "timestamp";
    static final String SERIALIZED_KEY_DOCUMENT = "document";
    static final String SERIALIZED_KEY_LOCATION = "location";
    static final String SERIALIZED_KEY_HARDWARE_ID = "hardware_id";
    boolean mConsented;
    String mDocument;
    Long mTimestamp;
    String mLocation;
    String mHardwareId;

    public boolean isConsented() {
        return mConsented;
    }

    @Nullable
    public String getDocument() {
        return mDocument;
    }

    @NonNull
    public Long getTimestamp() {
        return mTimestamp;
    }

    @Nullable
    public String getLocation() {
        return mLocation;
    }

    @Nullable
    public String getHardwareId() {
        return mHardwareId;
    }

    @Override
    @NonNull
    public String toString() {
        JSONObject gdprConsentJsonObject = new JSONObject();
        try {
            gdprConsentJsonObject.put(SERIALIZED_KEY_CONSENTED, isConsented());
            if (getTimestamp() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_TIMESTAMP, getTimestamp());
            }
            if (getDocument() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_DOCUMENT, getDocument());
            }
            if (getLocation() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_LOCATION, getLocation());
            }
            if (getHardwareId() != null) {
                gdprConsentJsonObject.put(SERIALIZED_KEY_HARDWARE_ID, getHardwareId());
            }
        } catch (JSONException ignored) {

        }
        return gdprConsentJsonObject.toString();
    }
}
