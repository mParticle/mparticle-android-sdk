package com.mparticle;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class MPReceiver extends BroadcastReceiver {

    public MPReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
            String referrer = intent.getStringExtra("referrer");
            SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            preferences.edit().putString(Constants.PrefKeys.INSTALL_REFERRER, referrer).commit();
        }else{
            MParticle.start(context);
            MPService.runIntentInService(context, intent);
        }
        setResult(Activity.RESULT_OK, null, null);
    }
}
