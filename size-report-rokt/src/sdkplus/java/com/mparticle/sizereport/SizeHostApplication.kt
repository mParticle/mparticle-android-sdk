package com.mparticle.sizereport

import android.app.Application
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.MParticleRokt

/**
 * The `core` fixture plus the Rokt kit surface, so R8 keeps the whole umbrella reachable:
 * android-rokt-kit, com.rokt:roktsdk and com.rokt:payment-extension.
 * Never launched -- the fixture exists only to be measured.
 */
class SizeHostApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MParticle.start(
            MParticleOptions.builder(this)
                .credentials(
                    "REPLACE WITH YOUR MPARTICLE API KEY",
                    "REPLACE WITH YOUR MPARTICLE API SECRET",
                )
                .logLevel(MParticle.LogLevel.VERBOSE)
                .build(),
        )
        MParticleRokt.Rokt().selectPlacements(
            identifier = "RoktExperience",
            attributes = mapOf("email" to "size-report@example.com"),
        )
    }
}
