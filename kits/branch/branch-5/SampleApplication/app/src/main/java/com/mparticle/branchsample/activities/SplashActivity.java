package com.mparticle.branchsample.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.branchsample.R;
import com.mparticle.branchsample.SampleApplication;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sojanpr on 4/18/18.
 * <p>
 * Splash Activity for MParticle-Branch Kit integration
 * </p>
 */

public class SplashActivity extends AppCompatActivity implements SampleApplication.IBranchEvents {

    private static final int SPLASH_DELAY = 1500;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        SampleApplication.setBranchEventCallback(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


            }
        }, SPLASH_DELAY);
    }

    /**
     * Note : Branch needs the new intents to be set to the activity if the launch mode is `SingleTask`
     **/
    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    @Override
    public void onBranchInitialised(JSONObject params) {
        final Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
        if (params.optBoolean("+clicked_branch_link")) {
            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("Referred Link", params.optString("~referring_link"));

            MPEvent event = new MPEvent.Builder("Referred Session", MParticle.EventType.UserContent)
                    .duration(300)
                    .info(infoMap)
                    .category("Session").build();
            MParticle.getInstance().logEvent(event);
            intent.putExtra(HomeActivity.BRANCH_PARAMS, params.toString());
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SplashActivity.this.startActivity(intent);
                SplashActivity.this.finish();
            }
        }, 1500);
    }

}
