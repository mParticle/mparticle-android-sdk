package com.mparticle.activity;

import android.os.Bundle;

import com.mparticle.MParticle;
import com.unity3d.player.UnityPlayerNativeActivity;

/**
 * Created by sdozor on 5/6/14.
 */
public class MPUnityNativeActivity extends UnityPlayerNativeActivity {

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MParticle.start(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MParticle.getInstance().activityStarted(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        MParticle.getInstance().activityStopped(this);
    }
}