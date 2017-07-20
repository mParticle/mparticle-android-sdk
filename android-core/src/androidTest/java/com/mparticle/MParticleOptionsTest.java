package com.mparticle;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.ApplicationContextWrapper;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.utils.AndroidUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.*;

public class MParticleOptionsTest {
    Context mContext;
    Context mProductionContext;

    @BeforeClass
    public static void preConditions() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
        mProductionContext = AndroidUtils.getInstance().getProductionContext(mContext);
        MParticle.setInstance(null);
        assertNull(MParticle.getInstance());
    }

    @Test
    public void testCrashOnNoCredentials() throws Exception {
        boolean thrown = false;
        try {
            MParticleOptions.builder(mContext).build();
        }
        catch (IllegalArgumentException ex) {
            thrown = true;
        }
        assertTrue(thrown);

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
                .setAndroidIdDisabled(true)
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
                .setAndroidIdDisabled(false)
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
                .setDevicePerformanceMetricsDisabled(false)
                .build();
        MParticle.start(options);
        assertFalse(MParticle.getInstance().isDevicePerformanceMetricsDisabled());
        MParticle.setInstance(null);

        options = MParticleOptions.builder(InstrumentationRegistry.getContext())
                .credentials("key", "secret")
                .setDevicePerformanceMetricsDisabled(true)
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
                .setLogLevel(MParticle.LogLevel.VERBOSE)
                .build();
        MParticle.setInstance(null);
        MParticle.start(options);
        assertEquals(Logger.getMinLogLevel(), MParticle.LogLevel.VERBOSE);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .setLogLevel(MParticle.LogLevel.ERROR)
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
                .setSessionTimeout(-123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getSessionTimeout(), 60000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .setSessionTimeout(123)
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
                .setUploadInterval(123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 123000);
        MParticle.setInstance(null);

        options = MParticleOptions.builder(mProductionContext)
                .credentials("key", "secret")
                .setUploadInterval(-123)
                .build();
        MParticle.start(options);
        assertEquals(MParticle.getInstance().getConfigManager().getUploadInterval(), 600000);
        MParticle.setInstance(null);
    }

}
