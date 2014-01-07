package com.mparticle.hello;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.mparticle.MParticleAPI;

public class ReferrerReceiver extends BroadcastReceiver {

	final private String TAG = ReferrerReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Context ctx = context.getApplicationContext();
		String action = intent.getAction();
		Log.i(TAG, action);
		if (action.equals("com.android.vending.INSTALL_REFERRER")) {
			String data = intent.getDataString();
			if (data != null) Log.i(TAG, data);
			String intstr = intent.toString();
			if (intstr != null) Log.i(TAG, intstr);
			
			String referrer = intent.getStringExtra("referrer");
			if (referrer != null) {
				Log.i(TAG, referrer);
			
		        SharedPreferences p = ctx.getSharedPreferences(ctx.getPackageName(), Context.MODE_PRIVATE );
		        Editor e = p.edit();
		        e.putString( "referrer", referrer ).commit();
		        
				String [] strings = referrer.split("&");
				for (int i=0; i<strings.length; i++) {
					Log.i(TAG, strings[i]);
				}
			}
	        
	        MParticleAPI particleAPI = MParticleAPI.getInstance(ctx, BaseActivity.PARTICLE_APP_KEY, BaseActivity.PARTICLE_APP_SECRET);
	        particleAPI.setInstallReferrer(referrer);
		}
	}
}
