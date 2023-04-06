package com.mparticle.modernization.kit.example

import android.util.Log
import com.mparticle.BaseEvent
import com.mparticle.modernization.kit.MParticleKit

internal class CustomKit : MParticleKit() {
    override fun logEvent(event: BaseEvent) {
        Log.d("CUSTOM_LOG", event.toString())
    }
}
