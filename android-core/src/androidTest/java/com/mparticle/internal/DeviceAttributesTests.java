package com.mparticle.internal;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.testutils.BaseCleanInstallEachTest;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DeviceAttributesTests extends BaseCleanInstallEachTest {

    @Test
    public void testAndroidIDCollection() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        JSONObject attributes = new JSONObject();
        DeviceAttributes.addAndroidId(attributes, context);
        assertFalse(attributes.has(Constants.MessageKey.DEVICE_ANID));
        assertFalse(attributes.has(Constants.MessageKey.DEVICE_OPEN_UDID));
        assertFalse(attributes.has(Constants.MessageKey.DEVICE_ID));

        MParticleOptions options = MParticleOptions.builder(context)
                .androidIdEnabled(false)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        JSONObject newAttributes = new JSONObject();
        DeviceAttributes.addAndroidId(newAttributes, context);
        assertTrue(newAttributes.length() == 0);

        MParticle.setInstance(null);

        options = MParticleOptions.builder(context)
                .androidIdEnabled(true)
                .credentials("key", "secret")
                .build();
        MParticle.start(options);
        newAttributes = new JSONObject();
        String androidId = MPUtility.getAndroidID(context);
        DeviceAttributes.addAndroidId(newAttributes, context);
        assertTrue(newAttributes.length() == 3);
        assertEquals(newAttributes.getString(Constants.MessageKey.DEVICE_ANID),androidId);
        assertTrue(newAttributes.getString(Constants.MessageKey.DEVICE_OPEN_UDID).length() > 0);
        assertEquals(newAttributes.getString(Constants.MessageKey.DEVICE_ID),androidId);

    }
}
