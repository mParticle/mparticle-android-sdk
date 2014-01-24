package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.mparticle.Constants.PrefKeys;

public class InstallReferrerTracker extends BroadcastReceiver {

    public InstallReferrerTracker() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
            String referrer = intent.getStringExtra("referrer");
            SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            preferences.edit().putString(PrefKeys.INSTALL_REFERRER, referrer).commit();
        }
    }

}
