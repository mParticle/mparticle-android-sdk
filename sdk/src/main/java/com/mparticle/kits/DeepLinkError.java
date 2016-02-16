package com.mparticle.kits;

/**
 * Class representing the result of a deep link query to an integration partner.
 */
public class DeepLinkError {
    private String message;
    private int serviceProviderId;

    public DeepLinkError setMessage(String message) {
        this.message = message;
        return this;
    }

    public DeepLinkError setServiceProviderId(int id) {
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

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Deep Link Error:\n");
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
