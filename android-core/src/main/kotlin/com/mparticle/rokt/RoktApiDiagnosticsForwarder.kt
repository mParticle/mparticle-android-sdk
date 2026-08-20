package com.mparticle.rokt

/**
 * Implemented by the Rokt kit so mParticle core can forward a bounded, non-PII public-API-usage
 * diagnostic code into the Rokt SDK — without core depending on any kit types. Core resolves the
 * live Rokt kit via `getKitInstance` and calls this only when the kit is active.
 */
interface RoktApiDiagnosticsForwarder {
    fun onMParticleApiCall(code: String)
}
