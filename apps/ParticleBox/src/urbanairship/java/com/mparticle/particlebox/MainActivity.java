package com.mparticle.particlebox;

import android.app.Application;
import android.os.Bundle;

import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;

/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UAirship.takeOff((Application) this.getApplicationContext(), null);
        PushManager.enablePush();
        PushManager.shared().setIntentReceiver(PushReceiver.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        UAirship.shared().getAnalytics().activityStarted(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        UAirship.shared().getAnalytics().activityStopped(this);
    }
}
