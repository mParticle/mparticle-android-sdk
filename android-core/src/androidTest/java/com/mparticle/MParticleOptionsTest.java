package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.networking.Matcher;
import com.mparticle.networking.MockServer;
import com.mparticle.networking.Request;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseAbstractTest;
import com.mparticle.testutils.MPLatch;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;

public class MParticleOptionsTest extends BaseAbstractTest {
    Context mContext;
    Context mProductionContext;

    @Before
    public void before() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mProductionContext = new AndroidUtils().getProductionContext(mContext);
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
    }

    @Test
    public void testCrashOnNoCredentials() throws Exception {
        boolean thrown = false;
        clearStoredPreferences();
        try {
            MParticleOptions.builder(mContext).build();
        }
        catch (IllegalArgumentException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        clearStoredPreferences();
        thrown = false;
        try {
            MParticleOptions.builder(mContext)
                    .credentials(null, null)
                    .build();
        }
        catch (IllegalArgumentException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        clearStoredPreferences();
        thrown = false;
        try {
            MParticleOptions.builder(mContext)
                    .credentials("key", null)
                    .build();
        }
        catch (IllegalArgumentException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        clearStoredPreferences();
        thrown = false;
        try {
            MParticleOptions.builder(mContext)
                    .credentials(null, "key")
                    .build();
        }
        catch (IllegalArgumentException ex) {
            thrown = true;
        }
        assertTrue(thrown);

        setStoredPreference("key", "secret");
        try {
            MParticleOptions.builder(mContext).buildForInternalRestart();
        }
        catch (IllegalArgumentException ex) {
            fail("MParticleOptions should build without credentials if the internal build function is used");
        }

        try {
            MParticleOptions.builder(mProductionContext).build();
        }
        catch (IllegalArgumentException ex) {
            fail("MParticleOptions should build without credentials in a Production environment");
        }
        try {
            MParticleOptions.builder(mProductionContext)
                    .credentials(null, null)
                    .build();
        }
        catch (IllegalArgumentException ex) {
            fail("MParticleOptions should build without credentials in a Production environment");
        }
    }

    private void clearStoredPreferences() {
        getCredentialsPreferences()
                .edit()
                .remove(Constants.PrefKeys.API_KEY)
                .remove(Constants.PrefKeys.API_SECRET)
                .commit();
    }

    private void setStoredPreference(String apiKey, String apiSecret) {
        getCredentialsPreferences()
                .edit()
                .putString(Constants.PrefKeys.API_KEY, apiKey)
                .putString(Constants.PrefKeys.API_SECRET, apiSecret)
                .commit();
    }

    private SharedPreferences getCredentialsPreferences() {
        return mContext.getSharedPreferences("mp_preferences", Context.MODE_PRIVATE);
    }

    @Test
    public void testSetCredentials() throws Exception {
        String key = UUID.randomUUID().toString();
        String secret = UUID.randomUUID().toString();
        startMParticle(MParticleOptions.builder(mProductionContext)
                .credentials(key, secret));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getApiKey(), key);
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getApiSecret(), secret);
    }

    @Test
    public void testAndroidIdDisabled() throws Exception {
        //test defaults
        assertFalse(MParticle.isAndroidIdEnabled());
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);
        startMParticle(MParticleOptions.builder(mContext));
        assertFalse(MParticle.isAndroidIdEnabled());
        assertTrue(MParticle.isAndroidIdDisabled());

        //test androidIdDisabled == true
        MParticle.setInstance(null);
        startMParticle(
                MParticleOptions.builder(mContext)
                        .androidIdDisabled(true)
        );
        assertFalse(MParticle.isAndroidIdEnabled());
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);

        //test androidIdEnabled == false
        MParticle.setInstance(null);
        startMParticle(
                MParticleOptions.builder(mContext)
                        .androidIdEnabled(false)
        );
        assertFalse(MParticle.isAndroidIdEnabled());
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);

        //test androidIdDisabled == false
        startMParticle(
                MParticleOptions.builder(mContext)
                        .androidIdDisabled(false)
        );
        assertTrue(MParticle.isAndroidIdEnabled());
        assertFalse(MParticle.isAndroidIdDisabled());

        //test androidIdEnabled == true
        startMParticle(
                MParticleOptions.builder(mContext)
                        .androidIdEnabled(true)
        );
        assertTrue(MParticle.isAndroidIdEnabled());
        assertFalse(MParticle.isAndroidIdDisabled());
    }

    @Test
    public void testDevicePerformanceMetricsDisabled() throws Exception {
        startMParticle();
        assertFalse(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mContext)
                .devicePerformanceMetricsDisabled(false));
        assertFalse(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mContext)
                .devicePerformanceMetricsDisabled(true));
        assertTrue(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);


    }

    @Test
    public void testLogLevel() throws Exception {
        startMParticle();
        assertEquals(Logger.getMinLogLevel(), Logger.DEFAULT_MIN_LOG_LEVEL);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .logLevel(MParticle.LogLevel.VERBOSE));
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.VERBOSE);

        startMParticle(MParticleOptions.builder(mProductionContext).logLevel(MParticle.LogLevel.ERROR));

        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR);
    }

    @Test
    public void testEnvironment() throws Exception {
        startMParticle();
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Development);

        startMParticle(MParticleOptions.builder(mProductionContext).environment(MParticle.Environment.Production));
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Production);
        MParticle.setInstance(null);


        Context productionContext = mProductionContext;


        Boolean debuggable = MPUtility.isAppDebuggable(productionContext);
        assertFalse(debuggable);

        startMParticle(MParticleOptions.builder(productionContext)
                .environment(MParticle.Environment.AutoDetect));

        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Production);
        MParticle.setInstance(null);
    }

    @Test
    public void testEnableUncaughtExceptionLogging() throws Exception {
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().Internal().getConfigManager().getLogUnhandledExceptions());
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .enableUncaughtExceptionLogging(true));
        assertTrue(MParticle.getInstance().Internal().getConfigManager().getLogUnhandledExceptions());

        startMParticle(MParticleOptions.builder(mProductionContext)
                .enableUncaughtExceptionLogging(false));
        assertFalse(MParticle.getInstance().Internal().getConfigManager().getLogUnhandledExceptions());
        MParticle.setInstance(null);
    }

    @Test
    public void testSessionTimeout() throws Exception {
        startMParticle();
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getSessionTimeout(), 60000);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .sessionTimeout(-123));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .sessionTimeout(123));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getSessionTimeout(), 123000);

        //make sure it resets if the session timeout is not specified
        startMParticle();
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

    }

    @Test
    public void testInstallType() throws Exception {
        startMParticle();
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.AutoDetect);
        MParticle.setInstance(null);
        startMParticle(MParticleOptions.builder(mProductionContext)
                .installType(MParticle.InstallType.KnownInstall));
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.KnownInstall);
        startMParticle(MParticleOptions.builder(mProductionContext)
                .installType(MParticle.InstallType.KnownUpgrade));
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.KnownUpgrade);
        startMParticle(MParticleOptions.builder(mProductionContext)
                .installType(MParticle.InstallType.AutoDetect));
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.AutoDetect);
        MParticle.setInstance(null);
    }

    @Test
    public void testUploadInterval() throws Exception {
        //default upload interval for production
        startMParticle();
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 10000);
        MParticle.setInstance(null);


        //default upload interval for production
        startMParticle(MParticleOptions.builder(mProductionContext));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 600000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .uploadInterval(123));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 123000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .uploadInterval(-123));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getUploadInterval(), 600000);
        MParticle.setInstance(null);
    }

    @Test
    public void testAttributionListener() throws Exception {
        startMParticle();
        assertNull(MParticle.getInstance().getAttributionListener());

        startMParticle(MParticleOptions.builder(mContext)
                .attributionListener(new AttributionListener() {
                    @Override
                    public void onResult(AttributionResult result) {

                    }

                    @Override
                    public void onError(AttributionError error) {

                    }
                }));
        assertNotNull(MParticle.getInstance().getAttributionListener());
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mContext)
                .attributionListener(null));
        assertNull(MParticle.getInstance().getAttributionListener());
    }

    @Test
    public void setOperatingSystemTest() throws InterruptedException {
        final Mutable<Boolean> called = new Mutable<Boolean>(false);
        final CountDownLatch latch = new MPLatch(1);
        startMParticle(MParticleOptions.builder(mContext)
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS));
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()), new MockServer.RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                assertEquals("FireTV", request.getBodyJson().optJSONObject("di").optString("dp"));
                called.value = true;
                latch.countDown();
            }
        });

        MParticle.getInstance().logEvent(new MPEvent.Builder("event name", MParticle.EventType.Location).build());
        MParticle.getInstance().upload();

        latch.await();
        assertTrue(called.value);
    }
    @Test
    public void setOperatingSystemDefault() throws InterruptedException {
        final Mutable<Boolean> called = new Mutable<Boolean>(false);
        final CountDownLatch latch1 = new MPLatch(1);

        startMParticle(MParticleOptions.builder(mContext));
        mServer.waitForVerify(new Matcher(mServer.Endpoints().getEventsUrl()), new MockServer.RequestReceivedCallback() {
            @Override
            public void onRequestReceived(Request request) {
                assertEquals("Android", request.getBodyJson().optJSONObject("di").optString("dp"));
                called.value = true;
                latch1.countDown();
            }
        });

        MParticle.getInstance().logEvent(new MPEvent.Builder("event name", MParticle.EventType.Location).build());
        MParticle.getInstance().upload();

        latch1.await();
        assertTrue(called.value);
    }

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void testLocationTracking() throws InterruptedException {
        startMParticle(MParticleOptions.builder(mContext)
                .locationTrackingDisabled());
        assertFalse(MParticle.getInstance().isLocationTrackingEnabled());

        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());


        startMParticle(MParticleOptions.builder(mContext)
                .locationTrackingEnabled("passive", 100, 20));
        assertTrue(MParticle.getInstance().isLocationTrackingEnabled());

        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());

        startMParticle();
        assertFalse(MParticle.getInstance().isLocationTrackingEnabled());
    }

    @Test
    public void testTimeout() throws InterruptedException {

        startMParticle(MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(-123));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(0));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(123));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout(), 123000);
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext));
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getIdentityConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        assertEquals(MParticle.getInstance().Internal().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
    }

    @Test
    public void testNetworkOptions() {
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        assertTrue(com.mparticle.networking.AccessUtils.equals(options.getNetworkOptions(), com.mparticle.networking.AccessUtils.getDefaultNetworkOptions()));
    }

    @Test
    public void testConfigStaleness() {
        //nothing set, should return null
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build();
        assertNull(options.getConfigMaxAge());

        //0 should return 0
        options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .configMaxAgeSeconds(0)
                .build();
        assertEquals(0, options.getConfigMaxAge().intValue());

        //positive number should return positive number
        int testValue = Math.abs(ran.nextInt());
        options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .configMaxAgeSeconds(testValue)
                .build();
        assertEquals(testValue, options.getConfigMaxAge().intValue());

        //negative number should get thrown out and return null
        options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .configMaxAgeSeconds(-5)
                .build();
        assertNull(options.getConfigMaxAge());
    }

    @Test
    public void  testAndroidIdLogMessage() {
        List<String> infoLogs = new ArrayList();
        Logger.setLogHandler(new Logger.DefaultLogHandler() {
            @Override
            public void log(MParticle.LogLevel priority, Throwable error, String messages) {
                super.log(priority, error, messages);
                if (priority == MParticle.LogLevel.INFO) {
                    infoLogs.add(messages);
                }
            }
        });
        MParticleOptions.builder(mContext)
                .credentials("this", "that")
                .androidIdDisabled(true)
                .build();
        assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on MParticleOptions settings"));
        infoLogs.clear();

        MParticleOptions.builder(mContext)
                .credentials("this", "that")
                .androidIdDisabled(false)
                .build();
        assertTrue(infoLogs.contains("ANDROID_ID will be collected based on MParticleOptions settings"));
        infoLogs.clear();

        //test default
        MParticleOptions.builder(mContext)
                .credentials("this", "that")
                .build();

        assertTrue(infoLogs.contains("ANDROID_ID will not be collected based on default settings"));
        infoLogs.clear();
    }

    @Test
    public void testBatchCreationCallback() throws InterruptedException {
        MParticleOptions.BatchCreationListener listener = new MParticleOptions.BatchCreationListener() {
            @NonNull
            @Override
            public JSONObject onBatchCreated(@NonNull JSONObject batch) {
                return batch;
            }
        };
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .batchCreationListener(listener)
                .credentials("this", "that")
                .build();
        assertEquals(listener, options.getBatchCreationListener());

        options = MParticleOptions.builder(mProductionContext)
                .credentials("this", "that")
                .batchCreationListener(listener)
                .batchCreationListener(null)
                .build();
        assertNull(options.getBatchCreationListener());

        options = MParticleOptions.builder(mProductionContext)
                .credentials("this", "that")
                .build();
        assertNull(options.getBatchCreationListener());
    }
}
