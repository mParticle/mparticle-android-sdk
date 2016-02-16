package com.mparticle.kits;

import com.mparticle.internal.MPUtility;

public class DeeplinkError {
    private String message;

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    @Override
    public String toString() {
        return MPUtility.isEmpty(message) ? "Unknown deeplink error." : message;
    }
}
