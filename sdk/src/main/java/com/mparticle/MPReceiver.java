package com.mparticle;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * BroadcastReceiver that handles the following Intent actions:
 *
 * <ul>
 *     <li>{@code com.android.vending.INSTALL_REFERRER}</li>
 *     <li>{@code com.google.android.c2dm.intent.REGISTRATION}</li>
 *     <li>{@code com.google.android.c2dm.intent.RECEIVE}</li>
 *     <li>{@code com.google.android.c2dm.intent.UNREGISTER}</li>
 * </ul>
 *
 * This BroadcastReceiver should be specified within the {@code <application>} block of AndroidManifest.xml file:
 *
 * <pre>
 * {@code
 *  <receiver android:name="com.mparticle.MPReceiver"
 *      android:permission="com.google.android.c2dm.permission.SEND" >
 *      <intent-filter>
 *          <action android:name="com.android.vending.INSTALL_REFERRER"/>
 *          <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
 *          <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *          <action android:name="com.google.android.c2dm.intent.UNREGISTER"/>
 *          <!-- Use your application's package name as the category -->
 *          <category android:name="com.your.package.name" />
 *      </intent-filter>
 * </receiver> }</pre>
 *
 *
 */
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
