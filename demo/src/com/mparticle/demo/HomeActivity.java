package com.mparticle.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import android.app.Activity;
import android.content.Intent;
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

        mParticleAPI = MParticleAPI.getInstance(this, "TestAppKey", "secret");
        // for testing, the timeout is 1 minute
        mParticleAPI.setSessionTimeout(60*1000);
    }

    @Override
    public void onStart() {
      super.onStart();
      mParticleAPI.start();
      mParticleAPI.logScreenView("Screen1");
      refreshDiagnostics();
    }

    @Override
    public void onStop() {
      super.onStop();
      mParticleAPI.stop();
      refreshDiagnostics();
    }

    private void refreshDiagnostics() {
        Map<String, Object> props = mParticleAPI.collectDeviceProperties();
        StringBuffer diagnosticMessage=new StringBuffer();
        for (Entry<String, Object> entry : props.entrySet()) {
            diagnosticMessage.append(entry.getKey() + "=" + entry.getValue()+"\n");
        }
        diagnosticsTextView.setText(diagnosticMessage.toString());
    }

    public void pressButtonA(View view) {
        mParticleAPI.logEvent("ButtonAPressed");
        refreshDiagnostics();
    }
    public void pressButtonB(View view) {
        mParticleAPI.logEvent("ButtonBPressed");
        refreshDiagnostics();
    }
    public void pressButtonC(View view) {
        boolean on = ((ToggleButton) view).isChecked();
        Map<String, String> eventData=new HashMap<String, String>();
        eventData.put("button_state", on ? "on":"off");
        mParticleAPI.logEvent("ButtonCPressed", eventData);
        refreshDiagnostics();
    }

    public void pressSetUserId(View view) {
        TextView editView = (TextView) findViewById(R.id.editUserId);
        String userId = editView.getText().toString();
        mParticleAPI.identifyUser(userId);
        refreshDiagnostics();
    }
    public void pressSetUserVar(View view) {
        TextView editView = (TextView) findViewById(R.id.editUserVar);
        String userVar = editView.getText().toString();
        mParticleAPI.setUserProperty("user_var", userVar);
        refreshDiagnostics();
    }
    public void pressSetSessionVar(View view) {
        TextView editView = (TextView) findViewById(R.id.editSessionVar);
        String sessionVar = editView.getText().toString();
        mParticleAPI.setUserProperty("session_var", sessionVar);
        refreshDiagnostics();
    }

    public void pressStartSession(View view) {
        mParticleAPI.start();
        refreshDiagnostics();
    }
    public void pressEndSession(View view) {
        mParticleAPI.endSession();
        refreshDiagnostics();
    }
    public void pressUpload(View view) {
        mParticleAPI.upload();
        refreshDiagnostics();
    }
    public void pressCrash(View view) {
        mParticleAPI.logErrorEvent("ErrorOccurred");
        throw new Error("Intentionally crashing demo app");
    }
    public void pressGetUserSegment(View view) {
        String userSegment = mParticleAPI.getUserSegment();
        Toast.makeText(view.getContext(), "Got User Segment: " + userSegment, Toast.LENGTH_SHORT).show();
    }
    public void pressShowPendingMessages(View view) {
        Intent intent = new Intent(this, PendingMessagesActivity.class);
        startActivity(intent);
    }
    public void pressUpdateLocation(View view) {
        Random r = new Random();
        mParticleAPI.setLocation(r.nextLong(), r.nextLong());
        refreshDiagnostics();
    }

    public void pressOptOut(View view) {
        boolean optOut = ((CheckBox) view).isChecked();
        mParticleAPI.setOptOut(optOut);
        refreshDiagnostics();
    }
    public void pressDebug(View view) {
        boolean debugMode = ((CheckBox) view).isChecked();
        mParticleAPI.setDebug(debugMode);
        refreshDiagnostics();
    }
    public void pressPushRegistration(View view) {
        boolean pushRegistration = ((CheckBox) view).isChecked();
        if (pushRegistration) {
            mParticleAPI.setPushRegistrationId("TOKEN");
        } else {
            mParticleAPI.clearPushRegistrationId();
        }
        refreshDiagnostics();
    }

    public void pressRefreshDiagnostics(View view) {
        this.refreshDiagnostics();
    }

}
