package com.mparticle.integrationtest;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mparticle.MParticle;
import com.mparticle.MPEvent;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Main activity that sends test events to WireMock for integration testing.
 * 
 * This activity sends a series of test events that match the iOS integration
 * test suite to ensure feature parity.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "IntegrationTestApp";
    private TextView statusText;
    private Handler handler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Get API credentials from intent extras if provided
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String apiKey = extras.getString("MPARTICLE_API_KEY");
            String apiSecret = extras.getString("MPARTICLE_API_SECRET");
            if (apiKey != null && apiSecret != null) {
                // Write to credentials file for Application class to read
                try {
                    java.io.FileWriter writer = new java.io.FileWriter(
                        new java.io.File(getFilesDir(), "mparticle_creds.txt"));
                    writer.write("MPARTICLE_API_KEY=" + apiKey + "\n");
                    writer.write("MPARTICLE_API_SECRET=" + apiSecret + "\n");
                    writer.close();
                    log("API credentials written from intent extras");
                } catch (Exception e) {
                    log("Failed to write credentials: " + e.getMessage());
                }
            }
        }
        
        // Create a simple text view to show status
        statusText = new TextView(this);
        statusText.setText("Running integration tests...\n");
        statusText.setPadding(20, 20, 20, 20);
        setContentView(statusText);
        
        handler = new Handler(Looper.getMainLooper());
        
        // Wait for SDK to initialize, then run tests
        handler.postDelayed(this::runIntegrationTests, 2000);
    }
    
    private void runIntegrationTests() {
        log("Starting integration tests...");
        
        // Test 1: Set User Attribute
        testSetUserAttribute();
        handler.postDelayed(this::testIncrementUserAttribute, 1000);
        handler.postDelayed(this::testLogEvent, 2000);
        handler.postDelayed(this::testLogCommerceEvent, 3000);
        handler.postDelayed(this::testSetSessionAttribute, 4000);
        handler.postDelayed(this::testLogError, 5000);
        handler.postDelayed(this::testLogTimedEvent, 6000);
        handler.postDelayed(this::testIdentity, 7000);
        
        // Wait for events to upload, then finish
        // Give extra time for SDK to upload events (uploadInterval is 5 seconds)
        handler.postDelayed(() -> {
            log("\nAll tests completed. Waiting for events to upload...");
            // Force upload if possible
            try {
                MParticle.getInstance().upload();
            } catch (Exception e) {
                log("Could not force upload: " + e.getMessage());
            }
            // Wait a bit more for upload
            handler.postDelayed(() -> {
                log("Check WireMock for captured requests.");
                finish();
            }, 5000);
        }, 15000);
    }
    
    private void testSetUserAttribute() {
        log("Test 1: Setting user attribute");
        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("Age", 25);
        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("Gender", "Male");
        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("Achieved Level", 5);
    }
    
    private void testIncrementUserAttribute() {
        log("Test 2: Incrementing user attribute");
        MParticle.getInstance().Identity().getCurrentUser().setUserAttribute("Score", 100);
        MParticle.getInstance().Identity().getCurrentUser().incrementUserAttribute("Score", 10);
    }
    
    private void testLogEvent() {
        log("Test 3: Logging custom event");
        Map<String, String> eventInfo = new HashMap<>();
        eventInfo.put("category", "test");
        eventInfo.put("action", "integration_test");
        MPEvent event = new MPEvent.Builder("Test Event", MParticle.EventType.Other)
                .customAttributes(eventInfo)
                .build();
        MParticle.getInstance().logEvent(event);
    }
    
    private void testLogCommerceEvent() {
        log("Test 4: Logging commerce event");
        Product product = new Product.Builder("Test Product", "test-sku", 9.99)
                .quantity(1)
                .build();
        CommerceEvent event = new CommerceEvent.Builder(Product.PURCHASE, product)
                .build();
        MParticle.getInstance().logEvent(event);
    }
    
    private void testSetSessionAttribute() {
        log("Test 5: Setting session attribute");
        MParticle.getInstance().setSessionAttribute("Station", "Classic Rock");
    }
    
    private void testLogError() {
        log("Test 6: Logging error");
        Map<String, String> errorInfo = new HashMap<>();
        errorInfo.put("cause", "slippery floor");
        MParticle.getInstance().logError("Test Error", errorInfo);
    }
    
    private void testLogTimedEvent() {
        log("Test 7: Logging timed event");
        String eventName = "Timed Test Event";
        
        // Log timed event with fixed 2 second duration for deterministic testing
        MPEvent timedEvent = new MPEvent.Builder(eventName, MParticle.EventType.Other)
                .duration(2000.0) // 2 seconds in milliseconds
                .build();
        MParticle.getInstance().logEvent(timedEvent);
    }
    
    private void testIdentity() {
        log("Test 8: Testing identity operations");
        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .email("test@example.com")
                .customerId("test-customer-123")
                .build();
        
        MParticle.getInstance().Identity().identify(request);
    }
    
    private void log(String message) {
        Log.d(TAG, message);
        if (statusText != null) {
            statusText.append(message + "\n");
        }
    }
}


