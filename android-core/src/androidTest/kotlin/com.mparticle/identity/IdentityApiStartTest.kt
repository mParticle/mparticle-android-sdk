package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.MPUtility
import com.mparticle.networking.IdentityRequest
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer
import com.mparticle.networking.MockServer.JSONMatch
import com.mparticle.networking.Request
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import junit.framework.TestCase
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch

class IdentityApiStartTest : BaseCleanInstallEachTest() {
    @Test
    @Throws(Exception::class)
    fun testInitialIdentitiesPresentWithAndroidId() {
        val identities = mRandomUtils.randomUserIdentities
        val request = IdentityApiRequest.withEmptyUser()
            .userIdentities(identities)
            .build()
        startMParticle(
            MParticleOptions.builder(mContext)
                .androidIdEnabled(true)
                .identify(request)
        )
        Assert.assertTrue(mServer.Requests().identify.size == 1)
        assertIdentitiesMatch(mServer.Requests().identify[0], identities, true)
    }

    @Test
    @Throws(Exception::class)
    fun testInitialIdentitiesPresentWithoutAndroidId() {
        val identities = mRandomUtils.randomUserIdentities
        val request = IdentityApiRequest.withEmptyUser()
            .userIdentities(identities)
            .build()
        startMParticle(
            MParticleOptions.builder(mContext)
                .androidIdEnabled(false)
                .identify(request)
        )
        Assert.assertTrue(mServer.Requests().identify.size == 1)
        assertIdentitiesMatch(mServer.Requests().identify[0], identities, false)
    }

    @Test
    @Throws(Exception::class)
    fun testNoInitialIdentityNoStoredIdentity() {
        startMParticle()
        TestCase.assertEquals(mServer.Requests().identify.size, 1)
        assertIdentitiesMatch(mServer.Requests().identify[0], HashMap(), false)
    }

    @Test
    @Throws(Exception::class)
    fun testNoInitialIdentity() {
        val currentMpid = ran.nextLong()
        val identities = mRandomUtils.randomUserIdentities
        startMParticle()
        MParticle.getInstance()?.Internal()?.configManager?.setMpid(currentMpid, ran.nextBoolean())
        for ((key, value) in identities) {
            AccessUtils.setUserIdentity(value, key, currentMpid)
        }
        com.mparticle.internal.AccessUtils.awaitMessageHandler()
        mServer = MockServer.getNewInstance(mContext)
        startMParticle()
        TestCase.assertEquals(mServer.Requests().identify.size, 1)
        assertIdentitiesMatch(mServer.Requests().identify[0], identities, false)
    }

    /**
     * This asserts that when the SDK receives a new Push InstanceId in the background, it will send
     * a modify request with the background change when the SDK starts up, unless there is a pushRegistration
     * included in the startup object. Make sure the Push InstanceId logged in the background is deleted
     * after it is used in the modify() request
     */
    @Test
    @Throws(InterruptedException::class)
    fun testLogNotificationBackgroundTest() {
        TestCase.assertNull(ConfigManager.getInstance(mContext).pushInstanceId)
        val instanceId = mRandomUtils.getAlphaNumericString(10)
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(
            mContext,
            instanceId,
            mRandomUtils.getAlphaNumericString(15)
        )
        val called = AndroidUtils.Mutable(false)
        var latch: CountDownLatch = MPLatch(1)
        /**
         * This tests that a modify request is sent when the previous Push InstanceId is empty, with the value of "null"
         */
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)).bodyMatch(
                JSONMatch { jsonObject ->
                    if (jsonObject.has("identity_changes")) {
                        try {
                            val identityChanges = jsonObject.getJSONArray("identity_changes")
                            TestCase.assertEquals(1, identityChanges.length())
                            val identityChange = identityChanges.getJSONObject(0)
                            TestCase.assertEquals("null", identityChange.getString("old_value"))
                            TestCase.assertEquals(instanceId, identityChange.getString("new_value"))
                            TestCase.assertEquals(
                                "push_token",
                                identityChange.getString("identity_type")
                            )
                            called.value = true
                        } catch (jse: JSONException) {
                            jse.toString()
                        }
                        return@JSONMatch true
                    }
                    false
                }), latch
        )
        startMParticle()
        latch.await()
        Assert.assertTrue(called.value)
        MParticle.setInstance(null)
        called.value = false
        val newInstanceId = mRandomUtils.getAlphaNumericString(15)
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(
            mContext,
            newInstanceId,
            mRandomUtils.getAlphaNumericString(15)
        )
        latch = CountDownLatch(1)
        /**
         * tests that the modify request was made with the correct value for the instanceId set while
         * the SDK was stopped
         */
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid)).bodyMatch(
                JSONMatch { jsonObject ->
                    if (jsonObject.has("identity_changes")) {
                        try {
                            val identityChanges = jsonObject.getJSONArray("identity_changes")
                            TestCase.assertEquals(1, identityChanges.length())
                            val identityChange = identityChanges.getJSONObject(0)
                            TestCase.assertEquals(instanceId, identityChange.getString("old_value"))
                            TestCase.assertEquals(
                                newInstanceId,
                                identityChange.getString("new_value")
                            )
                            TestCase.assertEquals(
                                "push_token",
                                identityChange.getString("identity_type")
                            )
                            called.value = true
                        } catch (jse: JSONException) {
                            jse.toString()
                        }
                        return@JSONMatch true
                    }
                    false
                }), latch
        )
        startMParticle()
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Throws(Exception::class)
    private fun assertIdentitiesMatch(
        request: Request,
        identities: Map<IdentityType, String>,
        androidIdEnabled: Boolean
    ) {
        Assert.assertTrue(request is IdentityRequest)
        val identityRequest = request.asIdentityRequest()
        Assert.assertNotNull(identityRequest)
        val knownIdentities = identityRequest.body.known_identities
        Assert.assertNotNull(knownIdentities)
        if (androidIdEnabled) {
            Assert.assertNotNull(knownIdentities.remove("android_uuid"))
        } else {
            TestCase.assertFalse(knownIdentities.has("android_uuid"))
        }
        Assert.assertNotNull(knownIdentities.remove("device_application_stamp"))
        TestCase.assertEquals(knownIdentities.length(), identities.size)
        val keys = knownIdentities.keys()
        val copy: Map<IdentityType, String> = HashMap(identities)
        while (keys.hasNext()) {
            val key = keys.next()
            TestCase.assertEquals(
                copy[MParticleIdentityClientImpl.getIdentityType(key)],
                knownIdentities.getString(key)
            )
        }
    }

    /**
     * In this scenario, a logPushRegistration's modify request is made when the current MPID is 0. Previously
     * the method's modify request would failed when a valid MPID wasn't present, but currently we will
     * defer the request until a valid MPID is present.
     *
     * Additionally, this tests that if the logPushRegistration method is called multiple times (for whatever reason)
     * before a valid MPID is present, we will ignore the previous values, and only send the most recent request.
     * This would be good in a case where the device is offline for a period of time, and logPushNotification
     * request back up.
     * @throws InterruptedException
     */
    @Test
    @Throws(InterruptedException::class, JSONException::class)
    fun testPushRegistrationModifyRequest() {
        val startingMpid = ran.nextLong()
        mServer.setupHappyIdentify(startingMpid, 200)
        val logPushRegistrationCalled = AndroidUtils.Mutable(false)
        val identifyLatch: CountDownLatch = MPLatch(1)
        val modifyLatch: CountDownLatch = MPLatch(1)
        MParticle.start(MParticleOptions.builder(mContext).credentials("key", "value").build())
        MParticle.getInstance()?.Identity()
            ?.addIdentityStateListener(object : IdentityStateListener {
                override fun onUserIdentified(user: MParticleUser, previousUser: MParticleUser?) {
                    Assert.assertTrue(logPushRegistrationCalled.value)
                    identifyLatch.countDown()
                    MParticle.getInstance()?.Identity()?.removeIdentityStateListener(this)
                }
            })
        mServer.waitForVerify(Matcher(mServer.Endpoints().getModifyUrl(startingMpid)), modifyLatch)
        var pushRegistration: String? = null
        for (i in 0..4) {
            MParticle.getInstance()
                ?.logPushRegistration(
                    mRandomUtils.getAlphaString(12).also { pushRegistration = it },
                    "senderId"
                )
        }
        logPushRegistrationCalled.value = true
        identifyLatch.await()
        modifyLatch.await()
        val modifyRequests = mServer.Requests().modify
        TestCase.assertEquals(1, modifyRequests.size)
        val body = modifyRequests[0].bodyJson
        val identityChanges = body.getJSONArray("identity_changes")
        TestCase.assertEquals(1, identityChanges.length())
        val identityChange = identityChanges.getJSONObject(0)
        TestCase.assertEquals(pushRegistration, identityChange.getString("new_value"))
        TestCase.assertEquals("push_token", identityChange.getString("identity_type"))

        //make sure the mDeferredModifyPushRegistrationListener was successfully removed from the IdentityApi
        TestCase.assertEquals(0, AccessUtils.getIdentityStateListeners().size)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testOperatingSystemSetProperly() {
        val called = AndroidUtils.Mutable(false)
        val latch: CountDownLatch = MPLatch(1)
        mServer.waitForVerify(Matcher(mServer.Endpoints().identifyUrl)) { request ->
            TestCase.assertEquals("fire", request.asIdentityRequest().body.clientSdk.platform)
            called.value = true
            latch.countDown()
        }
        MParticle.start(
            MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                .build()
        )
        latch.await()
        Assert.assertTrue(called.value)
        MParticle.setInstance(null)
        called.value = false
        val latch1: CountDownLatch = MPLatch(1)
        mServer.waitForVerify(Matcher(mServer.Endpoints().identifyUrl)) { request ->
            TestCase.assertEquals("fire", request.asIdentityRequest().body.clientSdk.platform)
            called.value = true
            latch1.countDown()
        }
        MParticle.start(
            MParticleOptions.builder(mContext)
                .credentials("key", "secret")
                .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                .build()
        )
        latch1.await()
        Assert.assertTrue(called.value)
    }

    /**
     * This builds on the previous test. The common scenario where we send a modify() request
     * when a valid MPID is not present, is when a client sets a pushRegistration in MParticleOptions
     * on the applications initial install
     */
    @Test
    fun testPushRegistrationInMParticleOptions() {
        var ex: Exception? = null
        try {
            startMParticle(
                MParticleOptions
                    .builder(mContext)
                    .pushRegistration("instanceId", "senderId")
                    .environment(MParticle.Environment.Development)
            )
            Assert.assertTrue(MPUtility.isDevEnv())
        } catch (e: Exception) {
            ex = e
        }
        TestCase.assertNull(ex)
    }
}