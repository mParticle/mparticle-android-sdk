package com.mparticle.particlebox;

import android.os.Bundle;
import android.provider.Settings;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

/**
 * Created by sdozor on 2/7/14.
 */
public class MainActivity extends ParticleActivity {

    private MixpanelAPI mixpanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mixpanel =  MixpanelAPI.getInstance(this, "b66214bab597da0085dfc3bcc1e44929");

      /*  mixpanel.getPeople().identify(Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID));
*/

        mixpanel.getPeople().initPushHandling("217134478361");
    }

    @Override
    protected void onDestroy() {
        mixpanel.flush();
        super.onDestroy();
    }
}
