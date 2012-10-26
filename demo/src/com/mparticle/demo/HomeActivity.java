package com.mparticle.demo;

import java.util.Iterator;

import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mparticle.MParticleAPI;

public class HomeActivity extends Activity {

    private static final String TAG = "mParticleDemo";

    private MParticleAPI mParticleAPI;
    private TextView diagnosticsTextView;
    private CheckBox optOutCheckBox;
    private CheckBox debugModeCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        diagnosticsTextView = (TextView) findViewById(R.id.textDiagnostics);
        optOutCheckBox = (CheckBox) findViewById(R.id.checkBoxOptOut);
        debugModeCheckBox = (CheckBox) findViewById(R.id.checkBoxDebugMode);

        mParticleAPI = MParticleAPI.getInstance(this, "TestAppKey", "secret");
        // for testing, the timeout is 1 minute
        mParticleAPI.setSessionTimeout(60*1000);
        mParticleAPI.setDebug(true);

        debugModeCheckBox.setChecked(true);
        optOutCheckBox.setChecked(mParticleAPI.getOptOut());
        collectDeviceProperties();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void collectDeviceProperties() {
        StringBuffer diagnosticMessage=new StringBuffer();
        JSONObject mDeviceAttributes = MParticleAPI.collectDeviceProperties(this.getApplicationContext());
        try {
            if (mDeviceAttributes.length() > 0) {
                Iterator<?> deviceKeys = mDeviceAttributes.keys();
                while( deviceKeys.hasNext() ){
                    String key = (String)deviceKeys.next();
                    diagnosticMessage.append(key + "=" + mDeviceAttributes.get(key)+"\n");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error parsing device attributes JSON");
        }
        diagnosticsTextView.setText(diagnosticMessage.toString());
    }

    public void pressButtonA(View view) {
        mParticleAPI.logEvent("ButtonAPressed");
    }
    public void pressButtonB(View view) {
        mParticleAPI.logEvent("ButtonBPressed");
    }
    public void pressButtonC(View view) throws JSONException {
        boolean on = ((ToggleButton) view).isChecked();
        JSONObject eventData=new JSONObject();
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
        mParticleAPI.start();
    }
    public void pressStopSession(View view) {
        mParticleAPI.stop();
    }
    public void pressNewSession(View view) {
        mParticleAPI.newSession();
    }
    public void pressEndSession(View view) {
        mParticleAPI.endSession();
    }
    public void pressListSessions(View view) {
        Intent intent = new Intent(this, SessionsListActivity.class);
        startActivity(intent);
    }
    public void pressListUploads(View view) {
        Intent intent = new Intent(this, UploadsListActivity.class);
        startActivity(intent);
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
    public void pressShowPendingMessages(View view) {
        Intent intent = new Intent(this, PendingMessagesActivity.class);
        startActivity(intent);
    }
    public void pressUpdateLocation(View view) {
        Random r = new Random();
        mParticleAPI.setLocation((360.0*r.nextDouble()-180.0), (360.0*r.nextDouble()-180.0));
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
