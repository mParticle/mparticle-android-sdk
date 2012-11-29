package com.mparticle;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

public class GCMIntentService extends IntentService {

    public GCMIntentService() {
        super("com.mparticle.GCMIntentService");
    }

    private static final String TAG = Constants.LOG_TAG;

    private static PowerManager.WakeLock sWakeLock;
    private static final Object LOCK = GCMIntentService.class;

    static void runIntentInService(Context context, Intent intent) {
        synchronized(LOCK) {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "my_wakelock");
            }
        }
        sWakeLock.acquire();
        intent.setClassName(context, GCMIntentService.class.getName());
        context.startService(intent);
    }

    @Override
    public final void onHandleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                handleRegistration(intent);
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                handleMessage(intent);
            }
        } finally {
            synchronized(LOCK) {
                sWakeLock.release();
            }
        }
    }

    private void handleRegistration(Intent intent) {
        String registrationId = intent.getStringExtra("registration_id");
        String error = intent.getStringExtra("error");
        String unregistered = intent.getStringExtra("unregistered");

        MParticleAPI mParticleAPI;
        try {
            mParticleAPI = MParticleAPI.getInstance(getApplicationContext());
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }
        // registration succeeded
        if (registrationId != null) {
            mParticleAPI.setPushRegistrationId(registrationId);
        }

        // unregistration succeeded
        if (unregistered != null) {
            mParticleAPI.clearPushRegistrationId();
        }

        // last operation (registration or unregistration) returned an error;
        if (error != null) {
            if ("SERVICE_NOT_AVAILABLE".equals(error)) {
               // optionally retry using exponential back-off
               // (see Advanced Topics)
            } else {
                // Unrecoverable error, log it
                Log.i(TAG, "GCM registration error: " + error);
            }
        }
    }

    // TODO: implement better notification stuff here
    private void handleMessage(Intent intent) {
        Bundle extras = intent.getExtras();
        String message = extras.getString("mparticle_message_key1");
        Log.i(TAG, "Received GCM message: " + message);
    }

}
