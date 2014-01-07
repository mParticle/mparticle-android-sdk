package com.mparticle.testbatch;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.widget.TextView;

import com.mparticle.MParticleAPI;
import com.mparticle.MParticleAPI.EventType;

public class Screen2Activity extends Activity {
    protected MParticleAPI mParticleAPI;
    
    static final String PARTICLE_APP_KEY = "1615d52f0d2ba44fb1125051c61ea87d";
    static final String PARTICLE_APP_SECRET = "Ay8AtMxEjUQB07xSwA270rWMX_CLGSP6l0zFJikZ71ccRZXmZp1PHeDvzgAGJOnF";
    static final String PARTICLE_BASE_URL = "api-qa.mparticle.com"; //"sdk.dev.aws.mparticle.com";

    static final int EVENT_COUNT = 200;
    private int mCurrent = EVENT_COUNT;
    private TextView mText;

    Handler handler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
			if (mParticleAPI != null) {
	    		if (mCurrent-- > 0) {
    				mParticleAPI.logEvent("BatchScreen2Message"+mCurrent, EventType.UserContent);
    				mText.setText( String.format(getString(R.string.screenmessage), 2, mCurrent, "batched"));
    				handler.sendEmptyMessageDelayed(0, 10);
	    		} else {
		    		mParticleAPI.endSession();
		    		Intent intent = new Intent(Screen2Activity.this, Screen3Activity.class);
		    		startActivity(intent);
		    		finish();
	    		}
			}
    	}
    };
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_screen2);
		mText = (TextView)findViewById(R.id.text);
		mText.setText( String.format(getString(R.string.screenmessage), 2, EVENT_COUNT, "batched"));
        initializeMParticleAPI();        
        mCurrent = EVENT_COUNT;
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
		mParticleAPI.newSession();	// explicitly start a new session in onStart
		mParticleAPI.startActivity();
		// set batch mode
		mParticleAPI.setDebug(false); // by taking out of debug mode
		handler.sendEmptyMessageDelayed(0, 1000L);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	// if rebooting, flag may have been set to null
    	if (mParticleAPI != null) {
    		mParticleAPI.stopActivity();
        }
    }
    
    protected void initializeMParticleAPI() {
    	// is there a config file?
    	try {
    		getResources().getAssets().open("mparticle.properties").close();
        	// first time
        	mParticleAPI = MParticleAPI.getInstance(this);
    	} catch(Exception e) {
    		// if failed, then use explicit getinstance
        	// first time
        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
    	}
    	try {
    		getResources().getAssets().open("mparticle.properties").close();
        	mParticleAPI = MParticleAPI.getInstance(this);
    	} catch(Exception e) {
    		// if failed, then use explicit getinstance
        	mParticleAPI = MParticleAPI.getInstance(this, PARTICLE_APP_KEY, PARTICLE_APP_SECRET);
    	}
	    if (mParticleAPI != null) {
        	try {
        		// force the debug endpoint
	        	Method privateStringMethod = mParticleAPI.getClass().getDeclaredMethod("setServiceHost", String.class);
	        	privateStringMethod.setAccessible(true);
	        	privateStringMethod.invoke(mParticleAPI, PARTICLE_BASE_URL);
//		    	Method privateBooleanMethod = mParticleAPI.getClass().getDeclaredMethod("setSecureTransport", boolean.class);
//		    	privateBooleanMethod.setAccessible(true);
//		    	privateBooleanMethod.invoke(mParticleAPI, false);
        	} catch( Exception e ) {
        		e.printStackTrace();
        		mParticleAPI = null;
        	}
	    }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
