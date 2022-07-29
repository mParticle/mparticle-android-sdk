package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.toMParticleOptions

class PinningTestNetworkOptionsEnabled : PinningTest() {
    override fun shouldPin(): Boolean {
        return false
    }

    override fun transformMParticleOptions(builder: com.mparticle.api.MParticleOptions): com.mparticle.api.MParticleOptions {
        return builder.builder
            .environment(MParticle.Environment.Development)
            .networkOptions(
                NetworkOptions.builder()
                    .setPinningDisabledInDevelopment(true)
                    .build()
            )
            .toMParticleOptions()
    }
}
