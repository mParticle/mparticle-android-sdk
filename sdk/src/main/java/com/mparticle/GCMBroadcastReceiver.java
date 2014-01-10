package com.mparticle;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GCMBroadcastReceiver extends BroadcastReceiver {

    public GCMBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.i("GCMBroadcastReceiver", "Received message with action: "+intent.getAction());
        GCMIntentService.runIntentInService(context, intent);
        setResult(Activity.RESULT_OK, null, null);
    }

}
