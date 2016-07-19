package com.mparticle.internal;


import android.content.Context;
import android.content.pm.ApplicationInfo;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}