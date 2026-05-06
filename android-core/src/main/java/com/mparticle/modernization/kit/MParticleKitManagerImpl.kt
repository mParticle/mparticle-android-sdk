package com.mparticle.modernization.kit

import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.commerce.CommerceEvent
import com.mparticle.modernization.kit.example.MpKit

internal class MParticleKitManagerImpl(
    private val kits: MutableList<MParticleKit>
) : KitManagerInternal() {

    private fun doInKits(action: (kit: MParticleKit) -> Unit) {
        kits.forEach { action.invoke(it) }
    }

    private fun CommerceEvent.processEvent() {
        doInKits{ if (it is CommerceListener) { it.commerceEventLogged(this) } }
    }
    private fun MPEvent.processEvent() {
        doInKits{ if (it is EventListener) { it.eventLogged(this) } }
    }


    override fun logEvent(event: BaseEvent) {
        when (event) {
            is CommerceEvent -> event.processEvent()
            is MPEvent -> event.processEvent()
            else -> {}
        }
    }

    override fun logError(
        message: String,
        params: Map<String, String>?,
        exception: Exception?
    ) {
        doInKits {  if (it is EventListener) { it.errorLogged(message, params, exception) } }
    }
}
