package com.mparticle.kits

import com.mparticle.MParticle

/**
 * Java-friendly accessors for the legacy Rokt API object.
 */
object MParticleRokt {
    @Volatile
    private var rokt: Rokt? = null

    @Volatile
    private var roktInstance: MParticle? = null

    @Suppress("FunctionName")
    @JvmStatic
    fun Rokt(): Rokt {
        val mParticle = requireNotNull(MParticle.getInstance()) {
            "MParticle must be started before calling MParticleRokt.Rokt()"
        }

        return roktFor(mParticle)
    }

    internal fun roktFor(mParticle: MParticle): Rokt = synchronized(this) {
        val existing = rokt
        if (existing != null && roktInstance === mParticle) {
            return existing
        }

        return createRokt(mParticle).also {
            rokt = it
            roktInstance = mParticle
        }
    }
}

/**
 * Returns the Rokt Kit facade bound to this mParticle instance.
 *
 * Kotlin consumers can use this property to call Rokt Kit APIs through
 * `MParticle.getInstance()?.rokt`. Java consumers should continue to use
 * [MParticleRokt.Rokt].
 */
val MParticle.rokt: Rokt
    get() = MParticleRokt.roktFor(this)

private fun createRokt(mParticle: MParticle): Rokt {
    val kitManager = mParticle.Internal().kitManager
    return Rokt(kitManager)
}
