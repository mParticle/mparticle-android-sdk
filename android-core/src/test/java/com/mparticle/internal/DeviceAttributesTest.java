package com.mparticle.internal;

import android.content.Context;

import com.mparticle.MParticle;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.MockSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
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
    @PrepareForTest({MPUtility.class})
    public void testAndroidIDCollection() throws Exception {
        PowerMockito.mockStatic(MPUtility.class);
        Context context = new MockContext();
        Mockito.when(MPUtility.getAndroidID(context)).thenReturn("the-android-id");
        Mockito.when(MPUtility.getOpenUDID(context)).thenReturn("the-open-android-id");
        JSONObject attributes = new JSONObject();
        DeviceAttributes.addAndroidId(attributes, context);
        assertEquals(attributes.getString(Constants.MessageKey.DEVICE_ANID),"the-android-id");
        assertTrue(attributes.getString(Constants.MessageKey.DEVICE_OPEN_UDID).length() > 0);
        assertEquals(attributes.getString(Constants.MessageKey.DEVICE_ID),"the-android-id");

        MParticle.setAndroidIdDisabled(true);
        JSONObject newAttributes = new JSONObject();
        DeviceAttributes.addAndroidId(attributes, context);
        assertTrue(newAttributes.length() == 0);
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