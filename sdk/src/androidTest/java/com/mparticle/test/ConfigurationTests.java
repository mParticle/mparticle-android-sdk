package com.mparticle.test;

import android.test.AndroidTestCase;

import com.mparticle.MParticle;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sdozor on 12/30/14.
 */
public class ConfigurationTests extends AndroidTestCase {


    private ConfigManager manager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        assertFalse(AppStateManager.mInitialized);
        MParticle.start(getContext());
        manager = MParticle.getInstance().internal().getConfigurationManager();
    }

    public void testApiCredentials(){
        assertEquals(manager.getApiKey(), getContext().getResources().getString(R.string.mp_key));
        assertEquals(manager.getApiSecret(), getContext().getResources().getString(R.string.mp_secret));
    }

    public void testCatchUnhandledExceptions(){
        assertFalse(manager.getLogUnhandledExceptions());
        try {
            JSONObject unhandleExceptions = new JSONObject();
            unhandleExceptions.put("cue", "forcecatch");
            manager.updateConfig(unhandleExceptions);
            assertTrue(manager.getLogUnhandledExceptions());

            unhandleExceptions.put("cue", "appdefined");
            manager.updateConfig(unhandleExceptions);
            assertFalse(manager.getLogUnhandledExceptions());

            MParticle.getInstance().enableUncaughtExceptionLogging();
            assertTrue(manager.getLogUnhandledExceptions());

            unhandleExceptions.put("cue", "forceignore");
            manager.updateConfig(unhandleExceptions);
            assertFalse(manager.getLogUnhandledExceptions());

            unhandleExceptions.put("cue", "appdefined");
            manager.updateConfig(unhandleExceptions);
            assertTrue(manager.getLogUnhandledExceptions());
            MParticle.getInstance().disableUncaughtExceptionLogging();
            assertFalse(manager.getLogUnhandledExceptions());

        }catch (JSONException jse){
            fail(jse.toString());
        }
    }

    public void testPushMessageKeys(){
        assertNotNull(ConfigManager.getPushKeys(getContext()));
        JSONObject json = new JSONObject();
        JSONArray keys = new JSONArray();
        keys.put("key1");
        keys.put("key2");
        try {
            json.put("pmk", keys);
            manager.updateConfig(json);
            assertTrue(ConfigManager.getPushKeys(getContext()).length() == 2);
        }catch (JSONException jse){
            fail(jse.toString());
        }
    }
    public void testCaptureNetworkPerformance(){
        assertFalse(manager.isNetworkPerformanceEnabled());
        /*
        try {
            JSONObject networkPerformance = new JSONObject();
            networkPerformance.put("cnp", "forcetrue");
            manager.updateConfig(networkPerformance);
            assertEquals("forcetrue", manager.mNetworkPerformance);

            networkPerformance.put("cnp", "appdefined");
            manager.updateConfig(networkPerformance);
            assertFalse(manager.isNetworkPerformanceEnabled());

            MParticle.getInstance().beginMeasuringNetworkPerformance();
            assertTrue(manager.isNetworkPerformanceEnabled());

            networkPerformance.put("cnp", "forcefalse");
            manager.updateConfig(networkPerformance);
            assertFalse(manager.isNetworkPerformanceEnabled());

            networkPerformance.put("cnp", "appdefined");
            manager.updateConfig(networkPerformance);
            assertTrue(manager.isNetworkPerformanceEnabled());
            MParticle.getInstance().endMeasuringNetworkPerformance();
            assertFalse(manager.isNetworkPerformanceEnabled());

        }catch (JSONException jse){
            fail(jse.toString());
        }*/
    }

    public void testRampPercentage(){
        assertEquals(-1, manager.getCurrentRampValue());
        int ramp = 12;
        JSONObject json = new JSONObject();
        try {
            json.put("rp", ramp);
            manager.updateConfig(json);
        }catch (JSONException jse){
            fail(jse.toString());
        }

        assertEquals(ramp, manager.getCurrentRampValue());
    }

    public void testTriggerItems(){
        try {
            assertNull(manager.getTriggerMessageHashes());
            assertNull(manager.getTriggerMessageMatches());
            JSONObject json = new JSONObject();
            json.put("tri", new JSONObject());
            json.getJSONObject("tri").put("mm", new JSONArray());
            JSONObject object = new JSONObject();
            object.put("dt", "x");
            object.put("eh", true);
            json.getJSONObject("tri").getJSONArray("mm").put(object);
            json.getJSONObject("tri").put("evts", new JSONArray());
            json.getJSONObject("tri").getJSONArray("evts").put(MPUtility.mpHash("test"));
            json.getJSONObject("tri").getJSONArray("evts").put(MPUtility.mpHash("test2"));
            json.getJSONObject("tri").getJSONArray("evts").put(MPUtility.mpHash("test3"));

            manager.updateConfig(json);
            assertNotNull(manager.getTriggerMessageHashes());
            assertNotNull(manager.getTriggerMessageMatches());

            manager.updateConfig(new JSONObject());
            assertNull(manager.getTriggerMessageHashes());
            assertNull(manager.getTriggerMessageMatches());

        }catch (JSONException jse){
            fail(jse.toString());
        }
    }
}
