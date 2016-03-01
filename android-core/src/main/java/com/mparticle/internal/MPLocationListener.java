package com.mparticle.internal;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.mparticle.MParticle;

/**
 * Created by sdozor on 3/9/15.
 */
public final class MPLocationListener implements LocationListener {
    private final MParticle mParticle;

    public MPLocationListener(MParticle mParticle) {
        this.mParticle = mParticle;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mParticle != null) {
            mParticle.setLocation(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}
