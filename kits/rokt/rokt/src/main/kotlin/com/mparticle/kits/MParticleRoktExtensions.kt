package com.mparticle.rokt

import com.mparticle.MParticle
import com.mparticle.kits.Rokt

/**
 * Kotlin-friendly accessor for the legacy Rokt API object.
 */
fun MParticle.Rokt(): Rokt = createRokt(this)

/**
 * Java-friendly accessors for the legacy Rokt API object.
 */
object MParticleRokt {
    @Suppress("FunctionName")
    @JvmStatic
    fun Rokt(mParticle: MParticle?): Rokt? = mParticle?.let { createRokt(it) }

    @Suppress("FunctionName")
    @JvmStatic
    fun Rokt(): Rokt? = MParticle.getInstance()?.let { createRokt(it) }
}

private fun createRokt(mParticle: MParticle): Rokt {
    val kitManager = mParticle.Internal().kitManager
    return Rokt(kitManager)
}
