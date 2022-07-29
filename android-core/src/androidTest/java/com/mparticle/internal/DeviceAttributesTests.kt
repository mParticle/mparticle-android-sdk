package com.mparticle.internal

import com.mparticle.testing.BaseTest
class DeviceAttributesTests : BaseTest() {
    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testAndroidIDCollection() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context
        val attributes = org.json.JSONObject()
        DeviceAttributes.addAndroidId(attributes, context)
        org.junit.Assert.assertFalse(attributes.has(Constants.MessageKey.DEVICE_ANID))
        org.junit.Assert.assertFalse(attributes.has(Constants.MessageKey.DEVICE_OPEN_UDID))
        org.junit.Assert.assertFalse(attributes.has(Constants.MessageKey.DEVICE_ID))
        var options = com.mparticle.MParticleOptions.builder(context)
            .androidIdEnabled(false)
            .credentials("key", "secret")
            .build()
        com.mparticle.MParticle.start(options)
        var newAttributes = org.json.JSONObject()
        DeviceAttributes.addAndroidId(newAttributes, context)
        org.junit.Assert.assertTrue(newAttributes.length() == 0)
        com.mparticle.MParticle.setInstance(null)
        options = com.mparticle.MParticleOptions.builder(context)
            .androidIdEnabled(true)
            .credentials("key", "secret")
            .build()
        com.mparticle.MParticle.start(options)
        newAttributes = org.json.JSONObject()
        val androidId = MPUtility.getAndroidID(context)
        DeviceAttributes.addAndroidId(newAttributes, context)
        org.junit.Assert.assertTrue(newAttributes.length() == 3)
        org.junit.Assert.assertEquals(
            newAttributes.getString(Constants.MessageKey.DEVICE_ANID),
            androidId
        )
        org.junit.Assert.assertTrue(
            newAttributes.getString(Constants.MessageKey.DEVICE_OPEN_UDID).isNotEmpty()
        )
        org.junit.Assert.assertEquals(
            newAttributes.getString(Constants.MessageKey.DEVICE_ID),
            androidId
        )
    }
}
