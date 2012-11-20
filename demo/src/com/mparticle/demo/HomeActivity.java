package com.mparticle.demo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mparticle.DemoDeviceProperties;
import com.mparticle.MParticleAPI;

public class HomeActivity extends Activity implements OnItemSelectedListener {

    private static final String TAG = "mParticleDemo";

    private MParticleAPI mParticleAPI;

    private SharedPreferences mPreferences;

    private static final String PREFS_EXCEPTION = "exceptions_mode";
    private static final String PREFS_DEBUG = "debug_mode";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mPreferences = getSharedPreferences("mParticleDemoPrefs", MODE_PRIVATE);

        Spinner spinnerApiKey = (Spinner) findViewById(R.id.spinnerApiKey);
        spinnerApiKey.setOnItemSelectedListener(this);
        ArrayAdapter<String> apiKeysAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        apiKeysAdapter.add("TestAppKey/secret");
        apiKeysAdapter.add("NoSdkKey/NoSdkSecret");
        apiKeysAdapter.add("ApsalarKey/ApsalarSecret");
        apiKeysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerApiKey.setAdapter(apiKeysAdapter);

        Spinner locationProviderSpinner = (Spinner) findViewById(R.id.spinLocationProvider);
        ArrayAdapter<String> locProviderAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        locProviderAdapter.add(LocationManager.NETWORK_PROVIDER);
        locProviderAdapter.add(LocationManager.GPS_PROVIDER);
        locProviderAdapter.add(LocationManager.PASSIVE_PROVIDER);
        locProviderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationProviderSpinner.setAdapter(locProviderAdapter);

        setupApiInstance("TestAppKey", "secret", 60);

        collectDeviceProperties();
    }

    private void setupApiInstance(String apiKey, String secret, int uploadInterval) {
        mParticleAPI = MParticleAPI.getInstance(this, apiKey, secret, uploadInterval);
        // for testing, the timeout is 1 minute
        mParticleAPI.setSessionTimeout(60*1000);

        boolean exceptionsMode = mPreferences.getBoolean(PREFS_EXCEPTION, true);
        CheckBox exceptionsModeCheckBox = (CheckBox) findViewById(R.id.checkBoxExceptionsMode);
        exceptionsModeCheckBox.setChecked(exceptionsMode);
        setupExceptionsLogging(exceptionsMode);

        boolean debugMode = mPreferences.getBoolean(PREFS_DEBUG, true);
        mParticleAPI.setDebug(debugMode);
        CheckBox debugModeCheckBox = (CheckBox) findViewById(R.id.checkBoxDebugMode);
        debugModeCheckBox.setChecked(debugMode);

        CheckBox optOutCheckBox = (CheckBox) findViewById(R.id.checkBoxOptOut);
        optOutCheckBox.setChecked(mParticleAPI.getOptOut());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menuProxy:
            // String proxyIp = "192.168.1.100";
            String proxyIp = "192.168.1.16";
            mParticleAPI.setConnectionProxy(proxyIp, 8080);
            Toast.makeText(this, "Now proxying requests to " + proxyIp + " port 8080", Toast.LENGTH_LONG).show();
            return true;
        case R.id.menuClose:
            this.finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void collectDeviceProperties() {
        StringBuffer diagnosticMessage=new StringBuffer();
        JSONObject appInfo = DemoDeviceProperties.collectAppInfo(this.getApplicationContext());
        try {
            if (appInfo.length() > 0) {
                Iterator<?> deviceKeys = appInfo.keys();
                while( deviceKeys.hasNext() ){
                    String key = (String)deviceKeys.next();
                    diagnosticMessage.append(key + "=" + appInfo.get(key)+"\n");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error parsing app info JSON");
        }
        JSONObject deviceInfo = DemoDeviceProperties.collectDeviceInfo(this.getApplicationContext());
        try {
            if (deviceInfo.length() > 0) {
                Iterator<?> deviceKeys = deviceInfo.keys();
                while( deviceKeys.hasNext() ){
                    String key = (String)deviceKeys.next();
                    diagnosticMessage.append(key + "=" + deviceInfo.get(key)+"\n");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error parsing device info JSON");
        }
        TextView diagnosticsTextView = (TextView) findViewById(R.id.textDiagnostics);
        diagnosticsTextView.setText(diagnosticMessage.toString());
    }

    public void pressEventButton(View view) throws JSONException {
        switch (view.getId()) {
        case R.id.buttonA:
            mParticleAPI.logEvent("ButtonAPressed");
            break;
        case R.id.buttonB:
            mParticleAPI.logEvent("ButtonBPressed");
            mParticleAPI.setSessionProperty("testSessionProp1", "testValue1");
            mParticleAPI.setSessionProperty("testSessionProp2", "testValue1");
            break;
        case R.id.buttonC: {
            boolean on = ((ToggleButton) view).isChecked();
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("button_state", on ? "on":"off");
            mParticleAPI.logEvent("ButtonCPressed", eventData);
            break;
        }
        case R.id.viewA:
            mParticleAPI.logScreenView("View A");
            break;
        case R.id.viewB: {
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("key1", "value1");
            eventData.put("key2", "value2");
            mParticleAPI.logScreenView("View B", eventData);
            break;
        }
        case R.id.eventEndUpload: {
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("key1", "value1");
            eventData.put("key2", "value2");
            mParticleAPI.logEvent("TestEvent", eventData);
            mParticleAPI.endSession();
            mParticleAPI.upload();
            break;
        }
        case R.id.buttonError:
            mParticleAPI.logErrorEvent("Test Error Occurred");
            break;
        case R.id.buttonException:
            mParticleAPI.logErrorEvent(new Exception("Test Exception Occurred"));
            break;
        }
    }

    public void pressDataButton(View view) {
        switch (view.getId()) {
        case R.id.buttonListSessions:
            startActivity(new Intent(this, SessionsListActivity.class));
            break;
        case R.id.buttonListMessages:
            startActivity(new Intent(this, MessagesListActivity.class));
            break;
        case R.id.buttonListUploads:
            startActivity(new Intent(this, UploadsListActivity.class));
            break;
        case R.id.buttonListCommands:
            startActivity(new Intent(this, CommandsListActivity.class));
            break;
        }
    }

    public void pressSessionButton(View view) {
        switch (view.getId()) {
        case R.id.buttonStartSession:
            mParticleAPI.start();
            break;
        case R.id.buttonStopSession:
            mParticleAPI.stop();
            break;
        case R.id.buttonNewSession:
            mParticleAPI.newSession();
            break;
        case R.id.buttonEndSession:
            mParticleAPI.endSession();
            break;
        case R.id.buttonUpload:
            mParticleAPI.upload();
            break;
        case R.id.buttonCrash:
            throw new Error("Intentionally crashing demo app");
        case R.id.buttonUpdateLocation:
            Random r = new Random();
            Location location = new Location("user");
            location.setLatitude( 360.0 * r.nextDouble() - 180.0 );
            location.setLongitude( 360.0 * r.nextDouble() - 180.0 );
            location.setAccuracy( 50.0f * r.nextFloat() );
            mParticleAPI.setLocation(location);
            break;
        }
    }

    public void pressSetVariable(View view) {
        switch (view.getId()) {
        case R.id.buttonSetUserVar:
            TextView editUserView = (TextView) findViewById(R.id.editUserVar);
            String userVar = editUserView.getText().toString();
            mParticleAPI.setUserProperty("user_var", userVar);
            break;
        case R.id.buttonSetSessionVar:
            TextView editSessionView = (TextView) findViewById(R.id.editSessionVar);
            String sessionVar = editSessionView.getText().toString();
            mParticleAPI.setSessionProperty("session_var", sessionVar);
            break;
        }
    }

    public void pressToggleOption(View view) {
        boolean optionValue = ((CheckBox) view).isChecked();
        switch (view.getId()) {
        case R.id.checkBoxOptOut:
            mParticleAPI.setOptOut(optionValue);
            break;
        case R.id.checkBoxDebugMode:
            mPreferences.edit().putBoolean(PREFS_DEBUG, optionValue).commit();
            mParticleAPI.setDebug(optionValue);
            break;
        case R.id.checkBoxExceptionsMode:
            mPreferences.edit().putBoolean(PREFS_EXCEPTION, optionValue).commit();
            setupExceptionsLogging(optionValue);
            break;
        case R.id.checkBoxPushRegistration:
            if (optionValue) {
                mParticleAPI.setPushRegistrationId("TOKEN");
            } else {
                mParticleAPI.clearPushRegistrationId();
            }
            break;
        }
    }

    public void pressToggleLocationTracking(View view) {
        if (((ToggleButton) view).isChecked()) {
            Spinner locationProviderSpinner = (Spinner) findViewById(R.id.spinLocationProvider);
            TextView editLocMinTime = (TextView) findViewById(R.id.editLocMinTime);
            TextView editLocMinDistance = (TextView) findViewById(R.id.editLocMinDistance);

            String selectedProvider = ((TextView) locationProviderSpinner.getSelectedView()).getText().toString();
            String minTime = editLocMinTime.getText().toString();
            String minDistance = editLocMinDistance.getText().toString();

            mParticleAPI.enableLocationTracking(selectedProvider, 1000*Integer.parseInt(minTime), Integer.parseInt(minDistance));
        } else {
            mParticleAPI.disableLocationTracking();
        }
    }

    private void setupExceptionsLogging(boolean exceptionsMode) {
        if (exceptionsMode) {
            mParticleAPI.enableUncaughtExceptionLogging();
        } else {
            mParticleAPI.disableUncaughtExceptionLogging();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String keySelection = (String) parent.getItemAtPosition(pos);
        String[] keyParts = keySelection.split("/");
        setupApiInstance(keyParts[0], keyParts[1], 60);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // no-op
    }

}
