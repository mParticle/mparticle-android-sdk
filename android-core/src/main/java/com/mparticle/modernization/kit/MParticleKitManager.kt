package com.mparticle.modernization.kit

import com.mparticle.modernization.core.MParticleComponent

abstract class MParticleKitManager : MParticleKit()

abstract class KitManagerInternal : MParticleKitManager(), MParticleComponent
