package com.mparticle.modernization.kit

import com.mparticle.modernization.core.MParticleComponent
import com.mparticle.modernization.eventlogging.MParticleEventLogging

internal abstract class MParticleKitManager : MParticleComponent

internal abstract class KitManagerInternal : MParticleKitManager(),
    MParticleEventLogging {
}
