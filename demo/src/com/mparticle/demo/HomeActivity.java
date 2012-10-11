package com.mparticle.demo;

import java.util.HashMap;

import java.util.Map;
import java.util.Random;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mparticle.MParticleAPI;

public class HomeActivity extends Activity {

    private MParticleAPI mParticleAPI;
    private TextView diagnosticsTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        diagnosticsTextView = (TextView) findViewById(R.id.textDiagnostics);

        mParticleAPI = MParticleAPI.getInstance(this, "01234567890123456789012345678901", "secret");
    }

    @Override
    public void onStart() {
      super.onStart();
      mParticleAPI.startSession();
      mParticleAPI.logScreenView("Screen1");
      refreshDiagnostics();
    }

    @Override
    public void onStop() {
      super.onStop();
      mParticleAPI.endSession();
    }

    private void refreshDiagnostics() {
        Map<String, Object> props = mParticleAPI.collectDeviceProperties();
        diagnosticsTextView.setText(props.toString());
    }

    public void pressButtonA(View view) {
        mParticleAPI.logEvent("ButtonAPressed");
    }
    public void pressButtonB(View view) {
        mParticleAPI.logEvent("ButtonBPressed");
    }
    public void pressButtonC(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        Map<String, String> eventData=new HashMap<String, String>();
        eventData.put("button_state", on ? "on":"off");
        mParticleAPI.logEvent("ButtonCPressed", eventData);
    }

    public void pressSetUserId(View view) {
        TextView editView = (TextView) findViewById(R.id.editUserId);
        String userId = editView.getText().toString();
        mParticleAPI.identifyUser(userId);
    }
    public void pressSetUserVar(View view) {
        TextView editView = (TextView) findViewById(R.id.editUserVar);
        String userVar = editView.getText().toString();
        mParticleAPI.setUserProperty("user_var", userVar);
    }
    public void pressSetSessionVar(View view) {
        TextView editView = (TextView) findViewById(R.id.editSessionVar);
        String sessionVar = editView.getText().toString();
        mParticleAPI.setUserProperty("session_var", sessionVar);
    }

    public void pressStartSession(View view) {
        mParticleAPI.startSession();
    }
    public void pressEndSession(View view) {
        mParticleAPI.endSession();
    }
    public void pressUpload(View view) {
        mParticleAPI.upload();
    }
    public void pressCrash(View view) {
        mParticleAPI.logErrorEvent("ErrorOccurred");
        throw new Error("Intentionally crashing demo app");
    }
    public void pressGetUserSegment(View view) {
        String userSegment = mParticleAPI.getUserSegment();
        Toast.makeText(view.getContext(), "Got User Segment: " + userSegment, Toast.LENGTH_SHORT).show();
    }
    public void pressUpdateLocation(View view) {
        Random r = new Random();
        mParticleAPI.setLocation(r.nextLong(), r.nextLong());
    }

    public void pressOptOut(View view) {
        boolean optOut = ((CheckBox) view).isChecked();
        mParticleAPI.setOptOut(optOut);
    }
    public void pressDebug(View view) {
        boolean debugMode = ((CheckBox) view).isChecked();
        mParticleAPI.setDebug(debugMode);
    }
    public void pressPushRegistration(View view) {
        boolean pushRegistration = ((CheckBox) view).isChecked();
        if (pushRegistration) {
            mParticleAPI.setPushRegistrationId("TOKEN");
        } else {
            mParticleAPI.clearPushRegistrationId();
        }
    }
}
