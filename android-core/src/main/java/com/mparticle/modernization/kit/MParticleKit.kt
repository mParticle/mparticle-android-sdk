package com.mparticle.modernization.kit

internal abstract class MParticleKit : MPLifecycle {
    abstract fun getConfiguration() :  KitConfiguration
}
internal abstract class MParticleKitInternal : MParticleKit() {
}

