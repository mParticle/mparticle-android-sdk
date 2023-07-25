package com.mparticle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Class representing the result of an attribution query to an integration partner.
 */
public class AttributionError {
    private String message;
    private int serviceProviderId;

    @NonNull
    public AttributionError setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    @NonNull
    public AttributionError setServiceProviderId(int id) {
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

    @Nullable
    public String getMessage() {
        return this.message;
    }

    @Override
    @NonNull
    public String toString() {
        StringBuilder builder = new StringBuilder("Attribution Error:\n");
        boolean empty = true;
        if (serviceProviderId > 0) {
            builder.append("Service provider ID:\n").append(serviceProviderId).append("\n");
            empty = false;
        }
        if (message != null) {
            builder.append("Message:\n").append(message).append("\n");
            empty = false;
        }

        if (empty) {
            builder.append("Empty error");
        }
        return builder.toString();
    }
}
