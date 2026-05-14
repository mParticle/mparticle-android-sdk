package com.mparticle.rokt

import com.mparticle.MParticle
import com.mparticle.kits.Rokt

/**
 * Java-friendly accessors for the legacy Rokt API object.
 */
object MParticleRokt {
    @Suppress("FunctionName")
    @JvmStatic
    fun Rokt(): Rokt {
        val mParticle = requireNotNull(MParticle.getInstance()) {
            "MParticle must be started before calling MParticleRokt.Rokt()"
        }
        return createRokt(mParticle)
    }
}

private fun createRokt(mParticle: MParticle): Rokt {
    val kitManager = mParticle.Internal().kitManager
    return Rokt(kitManager)
}
