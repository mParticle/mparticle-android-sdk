package com.mparticle.internal

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import com.mparticle.MParticle

class MPLocationListener(private val mParticle: MParticle) : LocationListener {
    override fun onLocationChanged(location: Location) {
        mParticle.setLocation(location)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
    }

    override fun onProviderEnabled(provider: String) {
    }

    override fun onProviderDisabled(provider: String) {
    }
}
