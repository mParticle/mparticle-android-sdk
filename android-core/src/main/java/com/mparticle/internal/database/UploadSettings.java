package com.mparticle.internal.database;
import androidx.annotation.NonNull;

import com.mparticle.internal.Logger;
import com.mparticle.networking.NetworkOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class UploadSettings {
    private final String mApiKey;
    private final String mSecret;
    private final NetworkOptions mNetworkOptions;
    private final String mActiveKits;
    private final String mSupportedKits;

    public UploadSettings(@NonNull String apiKey, @NonNull String secret, @NonNull NetworkOptions networkOptions, @NonNull String activeKits, @NonNull String supportedKits) {
        mApiKey = apiKey;
        mSecret = secret;
        mNetworkOptions = networkOptions;
        mActiveKits = activeKits;
        mSupportedKits = supportedKits;
    }

    public String getApiKey() {
        return mApiKey;
    }

    public String getSecret() {
        return mSecret;
    }

    public NetworkOptions getNetworkOptions() {
        return mNetworkOptions;
    }

    public String getActiveKits() {
        return mActiveKits;
    }

    public String getSupportedKits() {
        return mSupportedKits;
    }

    public String toJson() {
        JSONObject uploadSettingsJson = new JSONObject();
        try {
            uploadSettingsJson.put("apiKey", mApiKey);
            uploadSettingsJson.put("secret", mSecret);
            uploadSettingsJson.put("networkOptions", mNetworkOptions.toJson());
            uploadSettingsJson.put("activeKits", mActiveKits);
            uploadSettingsJson.put("supportedKits", mSupportedKits);
        } catch (JSONException jse) {
            Logger.error(jse);
        }
        return uploadSettingsJson.toString();
    }

    public static UploadSettings withJson(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            String apiKey = jsonObject.getString("apiKey");
            String secret = jsonObject.getString("secret");
            JSONObject networkOptionsJsonObject = jsonObject.getJSONObject("networkOptions");
            NetworkOptions networkOptions = NetworkOptions.withNetworkOptions(networkOptionsJsonObject.toString());
            String activeKits = jsonObject.getString("activeKits");
            String supportedKits = jsonObject.getString("supportedKits");
            if (networkOptions != null) {
                return new UploadSettings(apiKey, secret, networkOptions, activeKits, supportedKits);
            }
        } catch (JSONException jse) {
            Logger.error(jse);
        }
        return null;
    }
}
