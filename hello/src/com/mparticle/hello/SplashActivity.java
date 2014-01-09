package com.mparticle.hello;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mparticle.MParticleAPI.EventType;

public class SplashActivity extends BaseActivity {

	TextView mTimeToLoad;
	Button mRebootButton;
	float mCountDown;
	
	static final float smSplashScreenTime = 5.0f;
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (isFinishing()) return;
			
			if (msg.what == 0) {
				if (mCountDown <= 0.0f) {
					// advance to next screen
					Intent intent = new Intent(SplashActivity.this, AnimationActivity.class);
					startActivity(intent);
					if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
						mParticleAPI.logEvent("Splash Timeout", EventType.Navigation);
				} else {
					String ttlFormat = getString(R.string.time_to_load);
					String text = String.format(ttlFormat, mCountDown);
					mTimeToLoad.setText(text);
					mCountDown -= 0.1f;
					this.sendEmptyMessageDelayed(0, 100);
				}
			}
		}
	};
	
  	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// was this launched by another app?  test before calling baseactivity which needs this data for instantiating mParticleSDK
		Intent intent = getIntent();
		Uri data = intent.getData();
		if (data != null) {
			String scheme = data.getScheme(); // mparticlehello:
			String host = data.getHost(); // hello.mparticle.com
			List<String> params = data.getPathSegments();
			if (params.size() > 0) {
			}
		}
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		mTimeToLoad = (TextView)findViewById(R.id.txt_time_to_load);
		mCountDown = smSplashScreenTime; // 5 seconds
		handler.sendEmptyMessage(0);		
		mRebootButton = (Button)findViewById(R.id.btn_restart);
        if (smMParticleAPIEnabled) {
        	mRebootButton.setText(R.string.restart_without_sdk);
        } else {
        	mRebootButton.setText(R.string.restart_with_sdk);
        }
        mRebootButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("Reboot Pressed", EventType.Navigation);
		        
		        SharedPreferences p = getApplicationContext().getSharedPreferences(getApplicationContext().getPackageName(), MODE_PRIVATE );
		        Editor e = p.edit();
		        e.putBoolean( getString(R.string.particleAPIEnabledKey), new Boolean( !smMParticleAPIEnabled )).commit();
		        // reboot
		        Intent i = getBaseContext().getPackageManager()
		                .getLaunchIntentForPackage( getBaseContext().getPackageName() );
			   i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			   startActivity(i);
			   finish();
			   
			   smMParticleAPIEnabled = null;
			}
        });
        
//		if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
//        	mParticleAPI.logScreenView("SplashActivity");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.splash, menu);
		return true;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (!isFinishing()) {
			finish(); // kill this one for good
		}
	}

}
