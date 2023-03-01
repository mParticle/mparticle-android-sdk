package com.mparticle.modernization.core

abstract class MParticleException(message: String, throwable: Throwable? = null) :
    RuntimeException()

class MParticleInitializationException(message: String = "MParticle must be started before obtaining an instance, please call MParticle.start() first") :
    MParticleException(message)