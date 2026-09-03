package com.mparticle.integrationtest;

import android.app.Application;
import android.util.Log;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;
import com.mparticle.networking.DomainMapping;
import com.mparticle.networking.NetworkOptions;


/**
 * Application class for mParticle Android SDK integration testing.
 * 
 * This app initializes the mParticle SDK with WireMock endpoints and sends
 * test events for validation against WireMock recordings.
 */
public class IntegrationTestApplication extends Application {
    private static final String TAG = "IntegrationTestApp";
    
    // WireMock server runs on host machine, accessible via 10.0.2.2 from emulator
    // SDK uses HTTPS by default, so WireMock should run on HTTPS port (8443)
    // If WireMock runs on HTTP (8080), requests will fail unless you configure HTTPS proxy
    private static final String WIREMOCK_HOST = "10.0.2.2:8443"; // Use 8443 for HTTPS, or 8080 for HTTP
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Get API credentials from credentials file (set by test script)
        // If not found, the test will fail - credentials must be provided
        String apiKey = getEnvVar("MPARTICLE_API_KEY", null);
        String apiSecret = getEnvVar("MPARTICLE_API_SECRET", null);
        
        if (apiKey == null || apiSecret == null) {
            Log.e(TAG, "API credentials not found! Please provide MPARTICLE_API_KEY and MPARTICLE_API_SECRET.");
            Log.e(TAG, "The test script should prompt for these values.");
            throw new RuntimeException("API credentials are required but not provided");
        }
        
        Log.d(TAG, "Initializing mParticle SDK with API Key: " + apiKey);
        Log.d(TAG, "WireMock host: " + WIREMOCK_HOST);
        
        // Configure NetworkOptions to point to WireMock
        // Note: SDK will use HTTPS scheme by default, so WireMock needs to support HTTPS
        // OR we need to configure a proxy/redirect
        NetworkOptions networkOptions = NetworkOptions.builder()
                .addDomainMapping(DomainMapping.configMapping(WIREMOCK_HOST, true).build())
                .addDomainMapping(DomainMapping.eventsMapping(WIREMOCK_HOST, true).build())
                .addDomainMapping(DomainMapping.identityMapping(WIREMOCK_HOST, true).build())
                .addDomainMapping(DomainMapping.audienceMapping(WIREMOCK_HOST, true).build())
                .setPinningDisabled(true) // Disable SSL pinning for local testing
                .build();
        
        MParticleOptions options = MParticleOptions.builder(this)
                .credentials(apiKey, apiSecret)
                .networkOptions(networkOptions)
                .logLevel(MParticle.LogLevel.VERBOSE)
                .environment(MParticle.Environment.Development)
                .uploadInterval(5) // 5 seconds between uploads for faster testing
                .build();
        
        MParticle.start(options);
        
        Log.d(TAG, "mParticle SDK initialized");
    }
    
    /**
     * Get environment variable value.
     * Tries multiple methods:
     * 1. Read from credentials file in app's files directory
     * 2. Read from system property (if set)
     * 3. Use default value (if provided)
     */
    private String getEnvVar(String key, String defaultValue) {
        // Try reading from credentials file first
        try {
            java.io.File credsFile = new java.io.File(getFilesDir(), "mparticle_creds.txt");
            if (credsFile.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(credsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(key + "=")) {
                        String value = line.substring(key.length() + 1);
                        reader.close();
                        Log.d(TAG, "Read " + key + " from credentials file");
                        return value;
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not read credentials file: " + e.getMessage());
        }
        
        // Try reading from system property as fallback
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = systemProperties.getMethod("get", String.class, String.class);
            String value = (String) get.invoke(null, key, "");
            if (value != null && !value.isEmpty()) {
                Log.d(TAG, "Read " + key + " from system property");
                return value;
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not read system property " + key + ": " + e.getMessage());
        }
        
        if (defaultValue != null) {
            Log.d(TAG, "Using default value for " + key);
            return defaultValue;
        } else {
            Log.w(TAG, "No value found for " + key + " and no default provided");
            return null;
        }
    }
}

