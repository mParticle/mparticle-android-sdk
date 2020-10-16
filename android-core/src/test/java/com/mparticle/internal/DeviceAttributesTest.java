package com.mparticle.internal;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.MockMParticle;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
public class DeviceAttributesTest {
    
    @Test
    public void testCollectAppInfo() throws Exception {
        MParticle.setInstance(new MockMParticle());
        MockContext context = new MockContext();
        context.getSharedPreferences(null, 0).edit().putString(Constants.PrefKeys.INSTALL_REFERRER, "install referrer").apply();
        JSONObject appInfo = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context);
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
        assertNotNull(appInfo.getInt("env"));
        assertEquals("install referrer", appInfo.getString("ir"));
        assertEquals(true, appInfo.getBoolean("fi"));
    }

    @Test
    public void testAppInfoInstallTime() throws Exception {
        MockContext context = new MockContext();
        MockSharedPreferences prefs = (MockSharedPreferences) context.getSharedPreferences(null, 0);
        long now = 10012;
        prefs.putLong("mp::ict", now).commit();
        JSONObject appInfo = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context);
        assertEquals(now, appInfo.getLong("ict"));

        JSONObject appInfo2 = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context);
        assertEquals(now, appInfo2.getLong("ict"));
    }

    @Test
    public void testAppInfoLaunchCount() throws Exception {
        Context context = new MockContext();
        // Clear out the stored data for the current user, so we don't get any launches from previous tests.
        new ConfigManager(context, null, null, null, null).deleteUserStorage(context, ConfigManager.getMpid(context));
        JSONObject appInfo = null;
        int launchCount = 20;
        for (int i = 0; i < 20; i++) {
            appInfo = new DeviceAttributes(MParticle.OperatingSystem.ANDROID).getAppInfo(context);
        }

        assertEquals(launchCount, appInfo.getInt("lc"));
    }

    @Test
    public void testOperatingSystemToString() {
        //make sure that all the cases are covered, default is not getting returned
        //if this test fails, it might be becuase you added a new OperatingSystem enum, but forgot
        //to update this method

        Set<String> osStringValues = new HashSet<String>();
        for (MParticle.OperatingSystem operatingSystem: MParticle.OperatingSystem.values()) {
            String osString = new DeviceAttributes(operatingSystem).getOperatingSystemString();
            assertFalse(osStringValues.contains(osString));
            osStringValues.add(osString);
        }
    }
}