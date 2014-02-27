package com.mparticle.particlebox;

import android.os.Bundle;

import com.kahuna.sdk.KahunaAnalytics;


/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    @Override
    protected void onStart() {
        super.onStart();
        KahunaAnalytics.start();
        // Your Code Here
    }

    @Override
    protected void onStop() {
        super.onStop();
        KahunaAnalytics.stop();
        // Your Code Here
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KahunaAnalytics.onAppCreate(this, "d3e09fd03a1041e7bffde7d902093426", "648167995857");
        String username = "Sam Dozor3"; //TODO: Load your app's username here (if any
        String email = "sdozor3@mparticle.com"; //TODO: Load your app's email here (if any)
        KahunaAnalytics.setUsernameAndEmail(username, email);
        KahunaAnalytics.setDebugMode(true);
    }


}
