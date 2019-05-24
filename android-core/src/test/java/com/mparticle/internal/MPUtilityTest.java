package com.mparticle.internal;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import com.mparticle.mock.MockContext;
import com.mparticle.mock.utils.RandomUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
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
        for (int i = 0; i < Constants.LIMIT_ATTR_VALUE + 1; i++) {
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
        for (int i = 0; i < Constants.LIMIT_ATTR_VALUE + 1; i++) {
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
    public void testMapToJson() throws Exception {
        assertNull(MPUtility.mapToJson(null));
        for (int i = 0; i < 10; i++) {
            Map<String, String> testMap = new HashMap<String, String>();
            JSONObject testJson = new JSONObject();
            for (int j = 0; j < 10; j++) {
                String key = RandomUtils.getInstance().getAlphaNumericString(10);
                String value = RandomUtils.getInstance().getAlphaNumericString(18);
                testMap.put(key, value);
                testJson.put(key, value);
            }
            assertUnorderedJsonEqual(testJson, MPUtility.mapToJson(testMap));
        }
    }

    private void assertUnorderedJsonEqual(JSONObject object1, JSONObject object2) {
        if (object1 == object2) {
            return;
        }
        assertEquals(object1.length(), object2.length());
        Iterator<String> keys = object1.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                Object obj1Val = object1.get(key);
                Object obj2Val = object2.get(key);
                //Dealing with nested JSONObjects, not going to deal with nested JSONArray's.
                if (obj1Val instanceof JSONObject && obj2Val instanceof JSONObject) {
                    assertUnorderedJsonEqual((JSONObject)obj1Val, (JSONObject)obj2Val);
                } else {
                    assertEquals(obj1Val, obj2Val);
                }
            }
            catch (JSONException jse) {
                fail(jse.getMessage());
            }
        }
    }
}