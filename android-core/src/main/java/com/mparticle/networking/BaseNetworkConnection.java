package com.mparticle.networking;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import java.io.IOException;

public abstract class BaseNetworkConnection {
    private SharedPreferences mPreferences;

    public abstract MPConnection makeUrlRequest(MParticleBaseClientImpl.Endpoint endpoint, MPConnection connection, String payload, boolean identity) throws IOException;

    protected BaseNetworkConnection(Context context) {
        this.mPreferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    }

    protected BaseNetworkConnection(SharedPreferences sharedPreferences) {
        this.mPreferences = sharedPreferences;
    }

    public void setNextAllowedRequestTime(MPConnection connection, MParticleBaseClientImpl.Endpoint endpoint) {
        long throttle = NetworkConnection.DEFAULT_THROTTLE_MILLIS;
        if (connection != null) {
            //Most HttpUrlConnectionImpl's are case insensitive, but the interface
            //doesn't actually restrict it so let's be safe and check.
            String retryAfter = connection.getHeaderField("Retry-After");
            if (MPUtility.isEmpty(retryAfter)) {
                retryAfter = connection.getHeaderField("retry-after");
            }
            try {
                long parsedThrottle = Long.parseLong(retryAfter) * 1000;
                if (parsedThrottle > 0) {
                    throttle = Math.min(parsedThrottle, NetworkConnection.MAX_THROTTLE_MILLIS);
                }
            } catch (NumberFormatException nfe) {
                Logger.debug("Unable to parse retry-after header, using default.");
            }
        }

        long nextTime = System.currentTimeMillis() + throttle;
        setNextRequestTime(endpoint, nextTime);
    }

    public void setNextRequestTime(MParticleBaseClientImpl.Endpoint endpoint, long timeMillis) {
        mPreferences.edit().putLong(endpoint.name() + ":" + Constants.PrefKeys.NEXT_REQUEST_TIME, timeMillis).apply();
    }
}
