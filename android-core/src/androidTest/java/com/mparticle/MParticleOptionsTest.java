package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.GrantPermissionRule;
import android.webkit.WebView;

import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.utils.AndroidUtils;
import com.mparticle.utils.TestingUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MParticleOptionsTest extends BaseTest {
    Context mContext;
    Context mProductionContext;

    @Override
    public void beforeClass() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Override
    public void before() {
        mContext = InstrumentationRegistry.getContext();
        mProductionContext = AndroidUtils.getInstance().getProductionContext(mContext);
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
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials(key, secret)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getApiKey(), key);
        assertEquals(MParticle.getInstance().getConfigManager().getApiSecret(), secret);
    }

    @Test
    public void testAndroidIdDisabled() throws Exception {
        assertFalse(MParticle.isAndroidIdDisabled());

        MParticleOptions options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .androidIdDisabled(true)
                .credentials("key", "secret")
                .build();
        try {
            MParticle.start(options);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(MParticle.isAndroidIdDisabled());
        MParticle.setInstance(null);
        options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .androidIdDisabled(false)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertFalse(MParticle.isAndroidIdDisabled());
    }

    @Test
    public void testDevicePerformanceMetricsDisabled() throws Exception {
        MParticleOptions options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);

        options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .credentials("key", "secret")
                .devicePerformanceMetricsDisabled(false)
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);

        options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .credentials("key", "secret")
                .devicePerformanceMetricsDisabled(true)
                .build();
        MParticle.start(options);
        assertTrue(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);


    }

    @Test
    public void testLogLevel() throws Exception {
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertEquals(Logger.getMinLogLevel(), Logger.DEFAULT_MIN_LOG_LEVEL);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .logLevel(MParticle.LogLevel.VERBOSE)
                .build();
        MParticle.setInstance(null);
        MParticle.start(options);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.VERBOSE);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .logLevel(MParticle.LogLevel.ERROR)
                .build();
        MParticle.setInstance(null);
        MParticle.start(options);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.ERROR);
    }

    @Test
    public void testEnvironment() throws Exception {
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Development);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .environment(MParticle.Environment.Production)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getEnvironment(), MParticle.Environment.Production);
        MParticle.setInstance(null);


        Context productionContext = mProductionContext;


        Boolean debuggable = MPUtility.isAppDebuggable(productionContext);
        assertFalse(debuggable);

        options = MParticleOptions.builder(productionContext)
                .credentials("key", "secret")
                .environment(MParticle.Environment.AutoDetect)
                .build();

        MParticle.start(options);
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

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .enableUncaughtExceptionLogging(true)
                .build();
        MParticle.start(options);
        assertTrue(MParticle.getInstance().getConfigManager().getLogUnhandledExceptions());
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .enableUncaughtExceptionLogging(false)
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().getConfigManager().getLogUnhandledExceptions());
        MParticle.setInstance(null);
    }

    @Test
    public void testSessionTimeout() throws Exception {
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .sessionTimeout(-123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .sessionTimeout(123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 123000);
    }

    @Test
    public void testInstallType() throws Exception {
        MParticleOptions options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.AutoDetect);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .installType(MParticle.InstallType.KnownInstall)
                .build();
        MParticle.start(options);
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.KnownInstall);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .installType(MParticle.InstallType.KnownUpgrade)
                .build();
        MParticle.start(options);
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.KnownUpgrade);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .installType(MParticle.InstallType.AutoDetect)
                .build();
        MParticle.start(options);
        assertEquals(AccessUtils.getInstallType(MParticle.getInstance().mMessageManager), MParticle.InstallType.AutoDetect);
        MParticle.setInstance(null);
    }

    @Test
    public void testUploadInterval() throws Exception {
        //default upload interval for production
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 10000);
        MParticle.setInstance(null);


        //default upload interval for production
        options = MParticleOptions.builder(mProductionContext)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 600000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .uploadInterval(123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 123000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .uploadInterval(-123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 600000);
        MParticle.setInstance(null);
    }

    @Test
    public void testAttributionListener() throws Exception {
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        assertNull(MParticle.getInstance().getAttributionListener());
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .attributionListener(new AttributionListener() {
                    @Override
                    public void onResult(AttributionResult result) {

                    }

                    @Override
                    public void onError(AttributionError error) {

                    }
                })
                .build();
        MParticle.start(options);
        assertNotNull(MParticle.getInstance().getAttributionListener());
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .attributionListener(null)
                .build();
        MParticle.start(options);
        assertNull(MParticle.getInstance().getAttributionListener());
    }

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void testLocationTracking() {
        MParticleOptions options = MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .locationTrackingDisabled()
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().isLocationTrackingEnabled());

        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());


        options = MParticleOptions.builder(mContext)
                .locationTrackingEnabled("passive", 100, 20)
                .build();
        MParticle.start(options);
        assertTrue(MParticle.getInstance().isLocationTrackingEnabled());

        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());

        options = MParticleOptions.builder(mContext)
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().isLocationTrackingEnabled());
    }

}
