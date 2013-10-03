package com.mparticle.hello;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;

public class SplashActivity extends Activity {

	TextView mTimeToLoad;
	Button mRebootButton;
	float mCountDown;
	
	Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0) {
				if (mCountDown <= 0.0f) {
					// advance to next screen
					Intent intent = new Intent(SplashActivity.this, AnimationActivity.class);
					startActivity(intent);
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
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		mTimeToLoad = (TextView)findViewById(R.id.txt_time_to_load);
		mCountDown = 5.0f; // 5 seconds
		handler.sendEmptyMessage(0);		
		mRebootButton = (Button)findViewById(R.id.btn_restart);
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
