package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.MockContext;
import com.mparticle.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;


public class DeviceAttributesTest {

    @Test
    public void testCollectAppInfo() throws Exception {
        JSONObject appInfo = DeviceAttributes.collectAppInfo(new MockContext());
        assertTrue(appInfo.getString(Constants.MessageKey.APP_PACKAGE_NAME).equals("com.mparticle.test"));
        assertTrue(appInfo.getString(Constants.MessageKey.APP_VERSION_CODE).equals("42"));
        assertTrue(appInfo.getString(Constants.MessageKey.APP_INSTALLER_NAME).equals("com.mparticle.test.installer"));
        assertTrue(appInfo.getString(Constants.MessageKey.APP_NAME).equals("test label"));
        assertTrue(appInfo.getString(Constants.MessageKey.APP_VERSION).equals("42"));
        assertTrue(!MPUtility.isEmpty(appInfo.getString(Constants.MessageKey.BUILD_ID)));
        assertNotNull(appInfo.getBoolean(Constants.MessageKey.APP_DEBUG_SIGNING));
        assertNotNull(appInfo.getBoolean(Constants.MessageKey.APP_PIRATED));
        assertNotNull(appInfo.getLong(Constants.MessageKey.MPARTICLE_INSTALL_TIME));
        assertNotNull(appInfo.getInt(Constants.MessageKey.LAUNCH_COUNT));
        assertNotNull(appInfo.getLong(Constants.MessageKey.LAST_USE_DATE));
        assertNotNull(appInfo.getInt(Constants.MessageKey.LAUNCH_COUNT_SINCE_UPGRADE));
        assertNotNull(appInfo.getLong(Constants.MessageKey.UPGRADE_DATE));
        assertNotNull(appInfo.getBoolean(Constants.MessageKey.FIRST_SEEN_INSTALL));
    }

    @Test
    public void testAppInfoInstallTime() throws Exception {
        MockContext context = new MockContext();
        MockSharedPreferences prefs = (MockSharedPreferences) context.getSharedPreferences(null, 0);
        long now = System.currentTimeMillis();
        prefs.putLong(Constants.PrefKeys.INSTALL_TIME, now);
        JSONObject appInfo = DeviceAttributes.collectAppInfo(context);
        assertTrue(appInfo.getLong(Constants.MessageKey.MPARTICLE_INSTALL_TIME) == now);

        JSONObject appInfo2 = DeviceAttributes.collectAppInfo(context);
        assertTrue(appInfo2.getLong(Constants.MessageKey.MPARTICLE_INSTALL_TIME) == now);
    }

    @Test
    public void testAppInfoLaunchCount() throws Exception {
        Context context = new MockContext();
        JSONObject appInfo = null;
        int launchCount = 20;
        for (int i = 0; i < 20; i++) {
            appInfo = DeviceAttributes.collectAppInfo(context);
        }

        assertEquals(launchCount, appInfo.getInt(Constants.MessageKey.LAUNCH_COUNT));
    }
}