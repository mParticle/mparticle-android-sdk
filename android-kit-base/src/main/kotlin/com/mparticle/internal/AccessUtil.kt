package com.mparticle.internal

import com.mparticle.MParticle
import com.mparticle.kits.KitManagerImpl

object AccessUtil {
    fun kitManager(): KitManagerImpl = (MParticle.getInstance()?.Internal()?.kitManager as KitFrameworkWrapper).mKitManager as KitManagerImpl
}
