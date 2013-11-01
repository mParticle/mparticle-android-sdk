package com.mparticle.hello;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.mparticle.MParticleAPI;

public class BaseActivity extends Activity {
    protected MParticleAPI mParticleAPI;
    
    protected static Boolean smMParticleAPIEnabled = null;
    
    static final String PARTICLE_APP_KEY = "1615d52f0d2ba44fb1125051c61ea87d";
    static final String PARTICLE_APP_SECRET = "Ay8AtMxEjUQB07xSwA270rWMX_CLGSP6l0zFJikZ71ccRZXmZp1PHeDvzgAGJOnF";
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
        		mParticleAPI.newSession();	// explicitly start a new session in onStart
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
    	boolean hasPropFile = false;
	    if (smMParticleAPIEnabled == null) {
	        SharedPreferences p = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), MODE_PRIVATE );
	        smMParticleAPIEnabled = new Boolean(p.getBoolean(getString(R.string.particleAPIEnabledKey), true));
	        if (smMParticleAPIEnabled) {
	        	// is there a config file?
	        	try {
	        		getResources().getAssets().open("mparticle.properties").close();
		        	// first time
		        	mParticleAPI = MParticleAPI.getInstance(this);
		        	hasPropFile = true;
	        	} catch(Exception e) {
	        		// if failed, then use explicit getinstance
		        	// first time
		        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
	        	}
	        }
	    } else
	    if (smMParticleAPIEnabled) {
        	try {
        		getResources().getAssets().open("mparticle.properties").close();
	        	mParticleAPI = MParticleAPI.getInstance(this);
	        	hasPropFile = true;
        	} catch(Exception e) {
        		// if failed, then use explicit getinstance
	        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
        	}
	    }
	    if (mParticleAPI != null) {
        	try {
        		// force the debug endpoint
	        	Method privateStringMethod = mParticleAPI.getClass().getDeclaredMethod("setServiceHost", String.class);
	        	privateStringMethod.setAccessible(true);
	        	privateStringMethod.invoke(mParticleAPI, PARTICLE_BASE_URL);
		    	Method privateBooleanMethod = mParticleAPI.getClass().getDeclaredMethod("setSecureTransport", boolean.class);
		    	privateBooleanMethod.setAccessible(true);
		    	privateBooleanMethod.invoke(mParticleAPI, false);
        	} catch( Exception e ) {
        		e.printStackTrace();
        		mParticleAPI = null;
        		smMParticleAPIEnabled = false;
        	}
	    }
    }

}
