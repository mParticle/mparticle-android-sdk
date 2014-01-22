package com.mparticle;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GcmBroadcastReceiver extends BroadcastReceiver {

    public GcmBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("GcmBroadcastReceiver", "Received message with action: " + intent.getAction());
        GcmIntentService.runIntentInService(context, intent);
        setResult(Activity.RESULT_OK, null, null);
    }

}
