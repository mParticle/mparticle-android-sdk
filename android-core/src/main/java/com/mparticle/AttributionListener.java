package com.mparticle;

/**
 * Implement this interface and react to deep links
 */
public interface AttributionListener {
    void onResult(AttributionResult result);
    void onError(AttributionError error);
}