package com.mparticle.internal;

import android.content.Context;

import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;


public class DeviceAttributesTest {

    @Test
    public void testCollectAppInfo() throws Exception {
        JSONObject appInfo = DeviceAttributes.collectAppInfo(new MockContext());
        assertTrue(appInfo.getString("apn").equals("com.mparticle.test"));
        assertTrue(appInfo.getString("abn").equals("42"));
        assertTrue(appInfo.getString("ain").equals("com.mparticle.test.installer"));
        assertTrue(appInfo.getString("an").equals("test label"));
        assertTrue(appInfo.getString("av").equals("42"));
        assertTrue(!MPUtility.isEmpty(appInfo.getString("bid")));
        assertNotNull(appInfo.getBoolean("dbg"));
        assertNotNull(appInfo.getBoolean("pir"));
        assertNotNull(appInfo.getLong("ict"));
        assertNotNull(appInfo.getInt("lc"));
        assertNotNull(appInfo.getLong("lud"));
        assertNotNull(appInfo.getInt("lcu"));
        assertNotNull(appInfo.getLong("ud"));
        assertNotNull(appInfo.getBoolean("fi"));
    }

    @Test
    public void testAppInfoInstallTime() throws Exception {
        MockContext context = new MockContext();
        MockSharedPreferences prefs = (MockSharedPreferences) context.getSharedPreferences(null, 0);
        long now = 10012;
        prefs.putLong("mp::ict", now).commit();
        JSONObject appInfo = DeviceAttributes.collectAppInfo(context);
        assertEquals(now, appInfo.getLong("ict"));

        JSONObject appInfo2 = DeviceAttributes.collectAppInfo(context);
        assertEquals(now, appInfo2.getLong("ict"));
    }

    @Test
    public void testAppInfoLaunchCount() throws Exception {
        Context context = new MockContext();
        JSONObject appInfo = null;
        int launchCount = 20;
        for (int i = 0; i < 20; i++) {
            appInfo = DeviceAttributes.collectAppInfo(context);
        }

        assertEquals(launchCount, appInfo.getInt("lc"));
    }
}