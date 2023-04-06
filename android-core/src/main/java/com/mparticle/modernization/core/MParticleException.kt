package com.mparticle.modernization.core

internal abstract class MParticleException(message: String, throwable: Throwable? = null) :
    RuntimeException()

internal class MParticleInitializationException(message: String = "MParticle must be started before obtaining an instance, please call MParticle.start() first") :
    MParticleException(message)
