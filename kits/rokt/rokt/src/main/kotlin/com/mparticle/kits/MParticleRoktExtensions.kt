package com.mparticle.rokt

import com.mparticle.MParticle
import com.mparticle.Rokt

/**
 * Kotlin-friendly accessor for the legacy Rokt API object.
 */
fun MParticle.Rokt(): Rokt = createRokt(this)

/**
 * Java-friendly accessors for the legacy Rokt API object.
 */
object MParticleRokt {
    @JvmStatic
    fun Rokt(mParticle: MParticle?): Rokt? = mParticle?.let { createRokt(it) }

    @JvmStatic
    fun Rokt(): Rokt? = MParticle.getInstance()?.let { createRokt(it) }
}

private fun createRokt(mParticle: MParticle): Rokt {
    val configManager = mParticle.Internal().configManager
    val kitManager = mParticle.Internal().kitManager
    val constructor = Rokt::class.java.getDeclaredConstructor(configManager::class.java, kitManager::class.java)
    constructor.isAccessible = true
    return constructor.newInstance(configManager, kitManager)
}
