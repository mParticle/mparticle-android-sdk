package com.mparticle.kits.appsflyer.example.kotlin

import androidx.multidex.MultiDexApplication
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions

class ExampleApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        val options =
            MParticleOptions
                .builder(this)
                .credentials(
                    "REPLACE WITH YOUR MPARTICLE API KEY",
                    "REPLACE WITH YOUR MPARTICLE API SECRET",
                ).logLevel(MParticle.LogLevel.VERBOSE)
                .build()
        MParticle.start(options)
        MParticle.getInstance()?.logEvent(
            MPEvent.Builder("foo", MParticle.EventType.Other).build(),
        )
    }
}
