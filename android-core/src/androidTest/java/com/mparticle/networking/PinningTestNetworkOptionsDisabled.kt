package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.toMParticleOptions

class PinningTestNetworkOptionsDisabled : PinningTest() {
    override fun shouldPin(): Boolean {
        return true
    }

    override fun transformMParticleOptions(builder: com.mparticle.api.MParticleOptions): com.mparticle.api.MParticleOptions {
        return builder.builder
            .environment(MParticle.Environment.Production)
            .networkOptions(
                NetworkOptions.builder()
                    .setPinningDisabledInDevelopment(true)
                    .build()
            )
            .toMParticleOptions()
    }
}
