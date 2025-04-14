package com.rokt.example

import android.app.Application
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.rokt.roktsdk.Rokt

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val options: MParticleOptions = MParticleOptions.builder(this)
            .credentials(
                "MockKey",
                "MockSecret"
            )
            .environment(MParticle.Environment.Development)
            .logLevel(MParticle.LogLevel.VERBOSE)
            .build()


        MParticle.start(options)
        Rokt.init("2754655826098840951", "1.0.0",this)
    }
}