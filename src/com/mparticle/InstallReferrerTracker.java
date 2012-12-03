package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class InstallReferrerTracker extends BroadcastReceiver {

    public InstallReferrerTracker() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
            MParticleAPI mParticleAPI;
            try {
                mParticleAPI = MParticleAPI.getInstance(context);
                mParticleAPI.trackReferrer(intent);
            } catch (Throwable t) {
                // failure to instantiate mParticle likely means that the
                // mparticle.properties file is not correct
                // and a warning message will already have been logged
            }
        }

    }

}
