package com.mparticle.kits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.appsflyer.AppsFlyerLib;

public class AppsFlyerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AppsFlyerLib.getInstance().onReceive(context, intent);
    }
}
