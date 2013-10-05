package com.mparticle.hello;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.mparticle.MParticleAPI;

public class BaseActivity extends Activity {
    protected MParticleAPI mParticleAPI;
    
    protected static Boolean smMParticleAPIEnabled = null;
    
    static final String PARTICLE_APP_KEY = "ec7567d384f20949ace17e3a819c5e1d";
    static final String PARTICLE_APP_SECRET = "JBN--4eC80P74B4WrHRidEGkfcohLC27aEVOIitXbEgJlJpx4kwn5PzSF3dtMxUH";
    static final String PARTICLE_BASE_URL = "sdk.dev.aws.mparticle.com";
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeMParticleAPI();
        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
        if (smMParticleAPIEnabled) {
        	if (mParticleAPI != null) {
        		mParticleAPI.startActivity();
        	}
        }
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	// if rebooting, flag may have been set to null
        if ((smMParticleAPIEnabled != null) && smMParticleAPIEnabled) {
        	if (mParticleAPI != null) {
        		mParticleAPI.stopActivity();
        	}
        }
    }
    
    protected void initializeMParticleAPI() {
	    if (smMParticleAPIEnabled == null) {
	        SharedPreferences p = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), MODE_PRIVATE );
	        smMParticleAPIEnabled = new Boolean(p.getBoolean(getString(R.string.particleAPIEnabledKey), true));
	        if (smMParticleAPIEnabled) {
	        	// first time
	        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
	        	mParticleAPI.setServiceHost(PARTICLE_BASE_URL);
	        	mParticleAPI.setDebug(true);
	        }
	    } else
	    if (smMParticleAPIEnabled) {
	    	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
	    }
    }

}
