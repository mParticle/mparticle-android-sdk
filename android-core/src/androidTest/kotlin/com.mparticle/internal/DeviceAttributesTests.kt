package com.mparticle.internal

import androidx.test.platform.app.InstrumentationRegistry
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class DeviceAttributesTests : BaseCleanInstallEachTest() {
    @Test
    @Throws(Exception::class)
    fun testAndroidIDCollection() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val attributes = JSONObject()
        DeviceAttributes.addAndroidId(attributes, context)
        Assert.assertFalse(attributes.has(Constants.MessageKey.DEVICE_ANID))
        Assert.assertFalse(attributes.has(Constants.MessageKey.DEVICE_OPEN_UDID))
        Assert.assertFalse(attributes.has(Constants.MessageKey.DEVICE_ID))
        var options = MParticleOptions.builder(context)
            .androidIdEnabled(false)
            .credentials("key", "secret")
            .build()
        MParticle.start(options)
        var newAttributes = JSONObject()
        DeviceAttributes.addAndroidId(newAttributes, context)
        Assert.assertTrue(newAttributes.length() == 0)
        MParticle.setInstance(null)
        options = MParticleOptions.builder(context)
            .androidIdEnabled(true)
            .credentials("key", "secret")
            .build()
        MParticle.start(options)
        newAttributes = JSONObject()
        val androidId = MPUtility.getAndroidID(context)
        DeviceAttributes.addAndroidId(newAttributes, context)
        Assert.assertTrue(newAttributes.length() == 3)
        Assert.assertEquals(newAttributes.getString(Constants.MessageKey.DEVICE_ANID), androidId)
        Assert.assertTrue(
            newAttributes.getString(Constants.MessageKey.DEVICE_OPEN_UDID).isNotEmpty()
        )
        Assert.assertEquals(newAttributes.getString(Constants.MessageKey.DEVICE_ID), androidId)
    }
}