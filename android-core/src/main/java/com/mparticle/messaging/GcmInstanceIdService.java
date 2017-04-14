package com.mparticle.messaging;

import android.app.Service;
import android.content.Context;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.mparticle.MParticle;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.PushRegistrationHelper;
import com.mparticle.messaging.InstanceIdService.WrappedService;

class GcmInstanceIdService extends InstanceIDListenerService implements WrappedService {
    private Service parent;

    GcmInstanceIdService(Service parent) {
        this.parent = parent;
    }

    public void onTokenRefresh() {
        super.onTokenRefresh();
        try {
            PushRegistrationHelper.requestInstanceId(getApplicationContext());
        } catch (Exception e) {
            Logger.error("Error refreshing Instance ID: " + e.getMessage());
        }
        parent.stopSelf();
    }

    @Override
    public void setBaseContext(Context context) {
        attachBaseContext(context);
    }
}