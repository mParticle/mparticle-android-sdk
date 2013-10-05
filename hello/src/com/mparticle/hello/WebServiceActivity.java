package com.mparticle.hello;

import com.mparticle.MParticleAPI.EventType;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WebServiceActivity extends BaseActivity {

	Button mStartStop;
	
	boolean mRunning;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webservice);
		mStartStop = (Button)findViewById(R.id.btn_start_stop);
		mStartStop.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View vw) {
				if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
					mParticleAPI.logEvent("SDK Start/Stop Pressed", EventType.ACTION);
				mRunning = !mRunning;
				initializeMParticleAPI(); // make sure the api is initialized
				if (mRunning) {
					mStartStop.setText(R.string.btn_stop);
					smMParticleAPIEnabled = Boolean.valueOf(true);
				} else {
					mStartStop.setText(R.string.btn_start);
					smMParticleAPIEnabled = Boolean.valueOf(false);
				}
			}
		});
		mRunning = true;
		mStartStop.setText(R.string.btn_stop);
		
		if ((mParticleAPI != null) && (smMParticleAPIEnabled != null) && smMParticleAPIEnabled) 
        	mParticleAPI.logScreenView("WebServiceActivity");
	}
}
