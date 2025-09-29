package com.mparticle.networking

import com.mparticle.MParticle
import com.mparticle.MParticleOptions

class PinningTestNetworkOptionsDisabled : PinningTest() {
    override fun shouldPin(): Boolean = true

    override fun transformMParticleOptions(builder: MParticleOptions.Builder): MParticleOptions.Builder =
        builder
            .environment(MParticle.Environment.Production)
            .networkOptions(
                NetworkOptions
                    .builder()
                    .setPinningDisabledInDevelopment(true)
                    .build(),
            )
}
