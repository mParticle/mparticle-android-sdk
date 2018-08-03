package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.GrantPermissionRule;

import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.BaseAbstractTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MParticleOptionsTest extends BaseAbstractTest {
    Context mContext;
    Context mProductionContext;

    @Before
    public void before() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = InstrumentationRegistry.getContext();
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
            MParticleOptions.builder(mContext).build();
        }
        catch (IllegalArgumentException ex) {
            fail("MParticleOptions should build without credentials if there are stored credentials");
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
        assertEquals(MParticle.getInstance().getConfigManager().getApiKey(), key);
        assertEquals(MParticle.getInstance().getConfigManager().getApiSecret(), secret);
    }

    @Test
    public void testAndroidIdDisabled() throws Exception {
        MParticle.setInstance(null);
        startMParticle(MParticleOptions.builder(mContext)
                .androidIdDisabled(true));
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);
        startMParticle(MParticleOptions.builder(mContext)
                .androidIdDisabled(false));
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
        assertFalse(MParticle.getInstance().getConfigManager().getLogUnhandledExceptions());
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .enableUncaughtExceptionLogging(true));
        assertTrue(MParticle.getInstance().getConfigManager().getLogUnhandledExceptions());

        startMParticle(MParticleOptions.builder(mProductionContext)
                .enableUncaughtExceptionLogging(false));
        assertFalse(MParticle.getInstance().getConfigManager().getLogUnhandledExceptions());
        MParticle.setInstance(null);
    }

    @Test
    public void testSessionTimeout() throws Exception {
        startMParticle();
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .sessionTimeout(-123));
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .sessionTimeout(123));
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 123000);

        //make sure it resets if the session timeout is not specified
        startMParticle();
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);
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
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 10000);
        MParticle.setInstance(null);


        //default upload interval for production
        startMParticle(MParticleOptions.builder(mProductionContext));
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 600000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .uploadInterval(123));
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 123000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .uploadInterval(-123));
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 600000);
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
        assertEquals(MParticle.getInstance().getConfigManager().getIdentityConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        assertEquals(MParticle.getInstance().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(0));
        assertEquals(MParticle.getInstance().getConfigManager().getIdentityConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        assertEquals(MParticle.getInstance().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext)
                .identityConnectionTimeout(123));
        assertEquals(MParticle.getInstance().getConfigManager().getIdentityConnectionTimeout(), 123000);
        assertEquals(MParticle.getInstance().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        MParticle.setInstance(null);

        startMParticle(MParticleOptions.builder(mProductionContext));
        assertEquals(MParticle.getInstance().getConfigManager().getIdentityConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
        assertEquals(MParticle.getInstance().getConfigManager().getConnectionTimeout(), ConfigManager.DEFAULT_CONNECTION_TIMEOUT_SECONDS * 1000);
    }

    @Test
    public void testNetworkOptions() {
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        assertTrue(com.mparticle.networking.AccessUtils.equals(options.getNetworkOptions(), com.mparticle.networking.AccessUtils.getDefaultNetworkOptions()));
    }

}
