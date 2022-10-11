package com.mparticle.internal.database.services

object AccessUtils {
    fun setMessageStoredListener(listener: MParticleDBManager.MessageListener?) {
        MParticleDBManager.setMessageListener(listener)
    }
}
