package com.mparticle.modernization.kit.example_impl

import android.util.Log
import com.mparticle.BaseEvent
import com.mparticle.modernization.kit.MParticleKit

class CustomKit : MParticleKit() {
    override fun logEvent(event: BaseEvent) {
        Log.d("CUSTOM_LOG", event.toString())
    }
}
