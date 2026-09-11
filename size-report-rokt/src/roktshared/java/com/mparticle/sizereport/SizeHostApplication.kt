package com.mparticle.sizereport

import android.app.Application
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.MParticleRokt

/**
 * Holds a live reference to the Core SDK and the Rokt kit so R8 keeps both reachable.
 *
 * Shared by the `kit` and `sdkplus` flavors on purpose: they must differ only in their
 * dependencies, or the delta between them would measure a source difference rather than the
 * cost of the payment extension.
 *
 * Never launched -- the fixture exists only to be measured.
 */
class SizeHostApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MParticle.start(
            MParticleOptions.builder(this)
                // Placeholders copied from the kit examples: anything shaped like
                // `xx1-<32 hex>` trips trunk's mparticle-api-key-check and fails the PR.
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
