package com.mparticle.sizereport

import android.app.Application
import com.mparticle.MParticle
import com.mparticle.MParticleOptions

/**
 * Holds a live reference to the Core SDK's public entry point so R8 keeps it reachable.
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
    }
}
