package com.mparticle.modernization.kit.example

import android.util.Log
import com.mparticle.MPEvent
import com.mparticle.modernization.kit.EventListener
import com.mparticle.modernization.kit.KitConfiguration
import com.mparticle.modernization.kit.MParticleKitInternal

internal class XKit() : MParticleKitInternal(), EventListener {

    override fun getConfiguration(): KitConfiguration {
        TODO("Not yet implemented")
    }

    override fun eventLogged(event: MPEvent) {
       Log.d("MP_EVENT", event.toString())
    }

    override fun errorLogged(message: String, params: Map<String, String>?, exception: Exception?) {
        TODO("Not yet implemented")
    }

}
