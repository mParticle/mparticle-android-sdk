package com.mparticle.modernization.kit.example

import com.mparticle.BaseEvent
import com.mparticle.modernization.core.MParticleMediator
import com.mparticle.modernization.kit.MParticleKitInternal

internal class XKit(private val mediator: MParticleMediator) : MParticleKitInternal() {
    override fun logEvent(event: BaseEvent) {
        TODO("Not yet implemented")
    }
}
