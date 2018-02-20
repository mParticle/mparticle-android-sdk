package com.mparticle.networking;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

public class PinningTestNetworkOptionsDisabled extends PinningTest{

    @Override
    protected boolean shouldPin() {
        return true;
    }

    @Override
    protected MParticleOptions.Builder transformMParticleOptions(MParticleOptions.Builder builder) {
        return builder
                .environment(MParticle.Environment.Production)
                .networkOptions(NetworkOptions.builder()
                        .setPinningDisabledInDevelopment(true)
                        .build());
    }
}
