package com.mparticle.internal;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeviceAttributesTests {

    @BeforeClass
    public static void setUp() {
        Looper.prepare();
    }

    @Test
    public void testAndroidIDCollection() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        String androidId = MPUtility.getAndroidID(context);
        JSONObject attributes = new JSONObject();
        DeviceAttributes.addAndroidId(attributes, context);
        assertEquals(attributes.getString(Constants.MessageKey.DEVICE_ANID),androidId);
        assertTrue(attributes.getString(Constants.MessageKey.DEVICE_OPEN_UDID).length() > 0);
        assertEquals(attributes.getString(Constants.MessageKey.DEVICE_ID),androidId);

        MParticleOptions options = MParticleOptions.builder(context)
                .setAndroidIdDisabled(true)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        JSONObject newAttributes = new JSONObject();
        DeviceAttributes.addAndroidId(attributes, context);
        assertTrue(newAttributes.length() == 0);
    }

}
