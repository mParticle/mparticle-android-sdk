package com.mparticle.modernization.kit

import com.mparticle.modernization.core.MParticleComponent

internal abstract class MParticleKitManager : MParticleKit()

internal abstract class KitManagerInternal : MParticleKitManager(), MParticleComponent
