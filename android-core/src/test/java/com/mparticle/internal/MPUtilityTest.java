package com.mparticle.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import com.mparticle.mock.MockContext;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

public class MPUtilityTest {

    @Test
    public void testSetCheckedAttribute() throws Exception {
        JSONObject attributes = new JSONObject();

        MPUtility.setCheckedAttribute(attributes, "some key", "some value", false, true);
        assertEquals("some value", attributes.getString("some key"));


        MPUtility.setCheckedAttribute(attributes, "some key 2", "some value 2", false, false);
        assertEquals("some value 2", attributes.getString("some key 2"));

    }

    @Test
    public void testSetKeyThatsTooLong() throws Exception {
        JSONObject attributes = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 257; i++) {
            builder.append("a");
        }
        String keyThatsTooLong = builder.toString();
        MPUtility.setCheckedAttribute(attributes, keyThatsTooLong, "some value 2", false, true);

        assertFalse(attributes.has(keyThatsTooLong));

    }

    @Test
    public void testGetBuildUUID() {
        UUID.fromString(MPUtility.getBuildUUID(null));
        assertTrue("UUIDs should have been the same", MPUtility.getBuildUUID("12345678").equals(MPUtility.getBuildUUID("12345678")));
        assertFalse("UUIDs should have been different", MPUtility.getBuildUUID("1234567").equals(MPUtility.getBuildUUID("12345678")));
    }

    @Test
    public void testSetValueThatsTooLong() throws Exception {
        JSONObject attributes = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 257; i++) {
            builder.append("a");
        }
        String valueThatsTooLong = builder.toString();
        MPUtility.setCheckedAttribute(attributes, "mykey", valueThatsTooLong, false, false);

        assertFalse(attributes.has("mykey"));

    }

    @Test
    public void testSetUserValueThatsTooLong() throws Exception {
        JSONObject attributes = new JSONObject();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4097; i++) {
            builder.append("a");
        }
        String valueThatsTooLong = builder.toString();
        MPUtility.setCheckedAttribute(attributes, "mykey", valueThatsTooLong, false, true);

        assertFalse(attributes.has("mykey"));
    }

    @Test
    public void getGoogleAdIdInfoWithoutPlayServicesAvailable() throws Exception{
        assertNull(MPUtility.getGoogleAdIdInfo(new MockContext()));
    }

    @Test
    public void testIsAppDebuggableTrue() throws Exception {
        Context context = Mockito.mock(Context.class);
        ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        applicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE;
        Mockito.when(context.getApplicationInfo()).thenReturn(applicationInfo);
        assertTrue(MPUtility.isAppDebuggable(context));
    }

    @Test
    public void testIsAppDebuggableFalse() throws Exception {
        Context context = Mockito.mock(Context.class);
        ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        applicationInfo.flags = 0;
        Mockito.when(context.getApplicationInfo()).thenReturn(applicationInfo);
        assertFalse(MPUtility.isAppDebuggable(context));
    }

    @Test
    public void testIsAppDebuggableDoesNotModify() throws Exception {
        Context context = Mockito.mock(Context.class);
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.flags = 5;
        Mockito.when(context.getApplicationInfo()).thenReturn(applicationInfo);
        MPUtility.isAppDebuggable(context);
        assertEquals(5, applicationInfo.flags);
    }

    @Test
    public void testNullMapKey() throws Exception {
        Map map = new HashMap();
        map.put("key1", "val1");
        map.put("key2", "val2");
        assertFalse(MPUtility.containsNullKey(map));
        map.put(null, "val3");
        assertTrue(MPUtility.containsNullKey(map));

        map = new Hashtable();
        map.put("key1", "val1");
        map.put("key2", "val2");
        assertFalse(MPUtility.containsNullKey(map));

        map = new TreeMap(map);
        assertFalse(MPUtility.containsNullKey(map));

        map = new LinkedHashMap(map);
        assertFalse(MPUtility.containsNullKey(map));
    }
}