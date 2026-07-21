package com.rokt.sdkplus;

/**
 * Entry-point metadata for the Rokt SDK+ umbrella artifact.
 *
 * <p>Rokt SDK+ ({@code com.rokt:rokt-sdk-plus}) is a single dependency that bundles the mParticle
 * core SDK, the mParticle Rokt kit, and the Rokt Payment Extension (Shoppable Ads). It contains no
 * runtime logic of its own: initialize mParticle and use the Rokt kit APIs
 * ({@code com.mparticle.kits.MParticleRokt.Rokt()}) exactly as you would without the umbrella.
 */
public final class RoktSdkPlus {

    private RoktSdkPlus() {
    }

    /** The Rokt SDK+ umbrella version, aligned with the mParticle SDK release line. */
    public static final String VERSION = BuildConfig.VERSION_NAME;
}
