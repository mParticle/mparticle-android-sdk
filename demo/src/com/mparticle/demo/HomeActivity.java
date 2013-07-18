package com.mparticle.demo;

import java.util.HashMap;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mparticle.Attributes;
import com.mparticle.DemoDeviceAttributes;
import com.mparticle.MParticleAPI;
import com.mparticle.MParticleAPI.EventType;
import com.mparticle.MParticleJSInterface;

public class HomeActivity extends Activity implements OnItemSelectedListener {

    private static final String TAG = "mParticleDemo";

    private MParticleAPI mParticleAPI;

    private SharedPreferences mPreferences;

    public static String SENDER_ID = "31005546127";
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
        apiKeysAdapter.add("abc123/secret");
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

        setupApiInstance("abc123", "secret");
        updatePushControls();
        
        WebView myWebView = (WebView)this.findViewById(R.id.webView1);
        myWebView.setWebChromeClient(new WebChromeClient() {
        	public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        		Log.d("MParticle.JS", message);
        	}
        });
        
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        
        myWebView.loadUrl("http://10.0.0.11");
        
        MParticleJSInterface jsInstance = new MParticleJSInterface(this, mParticleAPI);
        myWebView.addJavascriptInterface(jsInstance, "mParticleAndroid");
    }

    private void setupApiInstance(String apiKey, String secret) {
        mParticleAPI = MParticleAPI.getInstance(this, apiKey, secret);

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

    private void updatePushControls() {
        String pushId = mParticleAPI.getPushRegistrationId();
        TextView pushIdView = (TextView) findViewById(R.id.editPushRegistrationId);
        pushIdView.setText(pushId);
        ((Button) findViewById(R.id.buttonPushAutoRegister)).setEnabled(null==pushId);
        ((Button) findViewById(R.id.buttonPushAutoUnregister)).setEnabled(null!=pushId);
        ((Button) findViewById(R.id.buttonPushManualRegister)).setEnabled(null==pushId);
        ((Button) findViewById(R.id.buttonPushManualUnregister)).setEnabled(null!=pushId);
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
            String proxyIp = "192.168.1.100";
            mParticleAPI.setConnectionProxy(proxyIp, 8080);
            Toast.makeText(this, "Now proxying requests to " + proxyIp + " port 8080", Toast.LENGTH_LONG).show();
            break;
        case R.id.menuDeviceInfo:
            try {
                JSONObject info = DemoDeviceAttributes.collectDeviceInfo(this.getApplicationContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Device Info").setMessage(info.toString(2)).show();
            } catch (JSONException e) {
                Toast.makeText(this, "Failed to parse device info JSON", Toast.LENGTH_LONG).show();
            }
            break;
        case R.id.menuAppInfo:
            try {
                JSONObject info = DemoDeviceAttributes.collectAppInfo(this.getApplicationContext());
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("App Info").setMessage(info.toString(2)).show();
            } catch (JSONException e) {
                Toast.makeText(this, "Failed to parse device info JSON", Toast.LENGTH_LONG).show();
            }
            break;
        case R.id.menuClose:
            this.finish();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void pressEventButton(View view) throws JSONException {
        switch (view.getId()) {
        case R.id.buttonA:
            mParticleAPI.logEvent("ButtonAPressed", EventType.ACTION);
            break;
        case R.id.buttonB:
            mParticleAPI.logEvent("ButtonBPressed", EventType.NAVIGATION);
            mParticleAPI.setSessionAttribute("testSessionAttr1", "testValue1");
            mParticleAPI.setSessionAttribute(Attributes.GENDER, "male");
            break;
        case R.id.buttonC: {
            boolean on = ((ToggleButton) view).isChecked();
            HashMap<String, String> eventData= new HashMap<String, String>();
            eventData.put("button_state", on ? "on":"off");
            mParticleAPI.logEvent("ButtonCPressed", EventType.OTHER, eventData);
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
            mParticleAPI.logEvent("TestEvent", EventType.ACTION, eventData);
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
            mParticleAPI.startActivity();
            break;
        case R.id.buttonStopSession:
            mParticleAPI.stopActivity();
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
            mParticleAPI.setUserAttribute("user_var", userVar);
            break;
        case R.id.buttonSetSessionVar:
            TextView editSessionView = (TextView) findViewById(R.id.editSessionVar);
            String sessionVar = editSessionView.getText().toString();
            mParticleAPI.setSessionAttribute("session_var", sessionVar);
            break;
        }
    }

    public void pressNotifyButton(View view) {
        switch (view.getId()) {
        case R.id.buttonPushAutoRegister:
            mParticleAPI.enablePushNotifications(SENDER_ID);
            new PushRegChangeDetectorTask().execute();
            break;
        case R.id.buttonPushAutoUnregister:
            mParticleAPI.clearPushNotifications();
            new PushRegChangeDetectorTask().execute();
            break;
        case R.id.buttonPushManualRegister:
            TextView pushIdView = (TextView) findViewById(R.id.editPushRegistrationId);
            mParticleAPI.setPushRegistrationId(pushIdView.getText().toString());
            updatePushControls();
            break;
        case R.id.buttonPushManualUnregister:
            mParticleAPI.clearPushRegistrationId();
            updatePushControls();
            break;
        case R.id.buttonPushRefresh:
            updatePushControls();
            break;
        }
    }

    public void pressBrowserButton(View view) {
        String url = "http://www.gynn.org/mp/";
        String mp_token = "unknown";
        // NOTE: we could expose a getMParticleToken() method from the API
        JSONObject info = DemoDeviceAttributes.collectDeviceInfo(this.getApplicationContext());
        try {
            mp_token = info.getString("duid");
        } catch (JSONException e) {
        }

        switch (view.getId()) {
        case R.id.buttonBrowserStatus:
            url += "mp_client.php";
            break;
        case R.id.buttonBrowserTrackInstall:
            url += "mp.php?mp_token=" + mp_token + "&mp_location=mparticledemo://test/abc?def=ghi";
            break;
        case R.id.buttonBrowserTrackInstallHttp:
            url += "mp.php?mp_token=" + mp_token + "&mp_location=http://demo.mparticle.com/testreferrer/abc?def=ghi";
            break;
        }
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    // this async task polls the registration id for up to 5 seconds to see if
    // it changes and updates the controls
    private class PushRegChangeDetectorTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String initialPushId = mParticleAPI.getPushRegistrationId();
                for(int i=0; i<5; i++) {
                    Thread.sleep(1000);
                    String currentPushId = mParticleAPI.getPushRegistrationId();
                    if (currentPushId!=initialPushId) {
                        return true;
                    }
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Push reg checker died");
            }
            return false;
        }
        protected void onPostExecute(Boolean regUpdated) {
            if (regUpdated) {
                updatePushControls();
            }
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
        setupApiInstance(keyParts[0], keyParts[1]);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // no-op
    }

}
