package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions

class PinningTestNetworkOptionsEnabled : PinningTest() {
    override fun shouldPin(): Boolean {
        return false
    }

    override fun transformMParticleOptions(builder: MParticleOptions.Builder): MParticleOptions.Builder {
        return builder
            .environment(MParticle.Environment.Development)
            .networkOptions(
                NetworkOptions.builder()
                    .setPinningDisabledInDevelopment(true)
                    .build()
            )
    }
}
