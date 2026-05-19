package com.mparticle.kits

import com.mparticle.MParticle

/**
 * Java-friendly accessors for the legacy Rokt API object.
 */
object MParticleRokt {
    @Volatile
    private var rokt: Rokt? = null

    @Suppress("FunctionName")
    @JvmStatic
    fun Rokt(): Rokt {
        val mParticle = requireNotNull(MParticle.getInstance()) {
            "MParticle must be started before calling MParticleRokt.Rokt()"
        }

        synchronized(this) {
            rokt?.let { return it }

            return createRokt(mParticle).also {
                rokt = it
            }
        }
    }
}

private fun createRokt(mParticle: MParticle): Rokt {
    val kitManager = mParticle.Internal().kitManager
    return Rokt(kitManager)
}
