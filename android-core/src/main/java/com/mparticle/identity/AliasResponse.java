package com.mparticle.identity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.internal.MParticleApiClient;

/**
 * Object describing the results of the most recent Alias request, including success indicators,
 * and any error messages.
 */
public class AliasResponse {

    private int responseCode;
    private AliasRequest requestMessage;
    private String requestId;
    private boolean willRetry;
    private String errorResponse;

    public AliasResponse(@NonNull MParticleApiClient.AliasNetworkResponse response, @NonNull AliasRequest originalRequst, @NonNull String requestId, boolean willRetry) {
        this.responseCode = response.getResponseCode();
        this.requestId = requestId;
        this.willRetry = willRetry;
        this.requestMessage = originalRequst;
        this.errorResponse = response.getErrorMessage();
    }

    /**
     * The HTTP response code for the Alias network request
     *
     * @return
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * The internal ID for the Alias request. Each {@link AliasRequest} has a unique, consistent request id
     *
     * @return
     */
    @NonNull
    public String getRequestId() {
        return requestId;
    }

    /**
     * A copy of the {@link AliasRequest} object passed into {@link IdentityApi#aliasUsers(AliasRequest)}
     * that this {@link AliasResponse} is describing
     *
     * @return
     */
    @NonNull
    public AliasRequest getRequest() {
        return requestMessage;
    }

    /**
     * Whether or not the request was successful
     *
     * @return
     */
    public boolean isSuccessful() {
        return responseCode >= 200 && responseCode < 300;
    }

    /**
     * Whether or not this request will be retried. Retries are handled by the SDK, so this does not
     * indicate that any action needs to be taken. Requests that result in recoverable errors, such as
     * rate limiting server error will be retried, while authentication errors, malformed requests and
     * others will not be retried
     *
     * @return
     */
    public boolean willRetry() {
        return willRetry;
    }

    /**
     * The error response message returned by the server, if there was one
     *
     * @return
     */
    @Nullable
    public String getErrorResponse() {
        return errorResponse;
    }
}
