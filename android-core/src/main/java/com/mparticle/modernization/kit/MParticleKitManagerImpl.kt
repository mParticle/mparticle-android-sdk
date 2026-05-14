package com.mparticle.modernization.kit

internal class MParticleKitManagerImpl(
    private val kits: MutableList<MParticleKit>
) : KitManagerInternal() {

    override fun leaveBreadcrumb(breadcrumb: String) {
        kits.forEach { it.leaveBreadcrumb(breadcrumb) }
    }
}
