package com.mparticle.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.audience.BaseAudienceTask;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.networking.MParticleBaseClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * API client interface primarily needed for mocking/testing.
 */
public interface MParticleApiClient extends MParticleBaseClient {
    void fetchConfig() throws IOException, MParticleApiClientImpl.MPConfigException;

    void fetchConfig(boolean force) throws IOException, MParticleApiClientImpl.MPConfigException;

    int sendMessageBatch(@NonNull String message, @NonNull UploadSettings uploadSettings) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException;

    void fetchUserAudience(BaseAudienceTask task,long mpId);

    JSONObject getCookies();

    @NonNull
    AliasNetworkResponse sendAliasRequest(@NonNull String message, @NonNull UploadSettings uploadSettings) throws JSONException, IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException;

    class AliasNetworkResponse {
        private int responseCode;
        private String errorMessage;

        public AliasNetworkResponse(int responseCode) {
            this(responseCode, null);
        }

        AliasNetworkResponse(int responseCode, String errorMessage) {
            this.responseCode = responseCode;
            this.errorMessage = errorMessage;
        }

        public int getResponseCode() {
            return responseCode;
        }

        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }

        void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}