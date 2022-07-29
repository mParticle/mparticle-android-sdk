package com.mparticle.identity

import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.MPUtility
import com.mparticle.testing.BaseTest
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.utils.randomIdentities
import com.mparticle.utils.setUserIdentity
import com.mparticle.utils.startMParticle
import junit.framework.TestCase
import org.json.JSONException
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class IdentityApiStartTest : BaseTest() {

    @Test
    @Throws(Exception::class)
    fun testInitialIdentitiesPresentWithAndroidId() {
        val identities: Map<MParticle.IdentityType, String> = randomIdentities()
        val request = IdentityApiRequest.withEmptyUser()
            .userIdentities(identities)
            .build()
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdEnabled(true)
                .identify(request)
        )
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, identities, true)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInitialIdentitiesPresentWithoutAndroidId() {
        val identities: Map<MParticle.IdentityType, String> = randomIdentities()
        val request = IdentityApiRequest.withEmptyUser()
            .userIdentities(identities)
            .build()
        startMParticle(
            MParticleOptions.builder(context)
                .androidIdEnabled(false)
                .identify(request)
        )
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, identities, false)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testNoInitialIdentityNoStoredIdentity() {
        startMParticle()
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(1, it.size)
            assertIdentitiesMatch(it[0].request.body.knownIdentities, mapOf(), false)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testNoInitialIdentity() {
        val currentMpid: Long = Random.Default.nextLong()
        val identities: Map<MParticle.IdentityType, String> = randomIdentities()
        startMParticle()
        MParticle.getInstance()!!
            .Internal().configManager.setMpid(currentMpid, Random.Default.nextBoolean())
        for ((key, value) in identities) {
            setUserIdentity(value, key, currentMpid)
        }
        com.mparticle.internal.AccessUtils.awaitMessageHandler()
        startMParticle()
        Server.endpoint(EndpointType.Identity_Identify).requests.let {
            assertEquals(2, it.size)
            assertIdentitiesMatch(it[1].request.body.knownIdentities, identities, false)
        }
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
        TestCase.assertNull(ConfigManager.getInstance(context).getPushInstanceId())
        val instanceId: String = RandomUtils.getAlphaNumericString(10)
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(
            context,
            instanceId,
            RandomUtils.getAlphaNumericString(15)
        )
        /**
         * This tests that a modify request is sent when the previous Push InstanceId is empty, with the value of "null"
         */
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive {
                it.body.identityChanges?.let {
                    assertEquals(1, it.size)
                    it[0].apply {
                        assertEquals(instanceId, newValue)
                        assertNull(oldValue)
                        assertEquals("push_token", identityType)
                    }
                    true
                } ?: false
            }
            .after { startMParticle() }
            .blockUntilFinished()

        MParticle.setInstance(null)
        val newInstanceId: String = RandomUtils.getAlphaNumericString(15)
        com.mparticle.internal.AccessUtils.setPushInPushRegistrationHelper(
            context,
            newInstanceId,
            RandomUtils.getAlphaNumericString(15)
        )
        /**
         * tests that the modify request was made with the correct value for the instanceId set while
         * the SDK was stopped
         */
        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive {
                it.body.identityChanges?.let {
                    assertEquals(1, it.size)
                    it[0].apply {
                        assertEquals(instanceId, oldValue)
                        assertEquals(newInstanceId, newValue)
                        assertEquals("push_token", identityType)
                    }
                    true
                } ?: false
            }
            .after { startMParticle() }
            .blockUntilFinished()
    }

    @Throws(Exception::class)
    private fun assertIdentitiesMatch(
        receivedIdentities: Map<String, String?>?,
        testIdentities: Map<MParticle.IdentityType, String>,
        androidIdEnabled: Boolean
    ) {
        val knownIdentitiesCopy = receivedIdentities!!.toMutableMap()
        if (androidIdEnabled) {
            assertNotNull(knownIdentitiesCopy.remove("android_uuid"))
        } else {
            assertFalse(knownIdentitiesCopy.containsKey("android_uuid"))
        }
        assertNotNull(knownIdentitiesCopy.remove("device_application_stamp"))
        assertEquals(testIdentities.size, knownIdentitiesCopy.size)
        receivedIdentities.forEach {
            assertEquals("${it.key} identity type does not match", knownIdentitiesCopy[it.key], testIdentities[MParticleIdentityClientImpl.getIdentityType(it.key)])
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
        var pushRegistration: String? = null
        MParticle.start(MParticleOptions.builder(context).credentials("key", "secret").build())
        (0 until 4).forEach {
            MParticle.getInstance()!!
                .logPushRegistration(
                    RandomUtils.getAlphaString(12).also { pushRegistration = it },
                    "senderId"
                )
        }
        MParticle.getInstance()?.Identity()!!.let {
            assertNull(it.currentUser)
            it.addIdentityStateListener { _, _ ->
                Server
                    .endpoint(EndpointType.Identity_Modify)
                    .assertNextRequest {
                        it.url.contains(mStartingMpid.toString()) &&
                            it.body.identityChanges?.let { changes ->
                                changes.size == 0 &&
                                    changes[0].identityType == "push_token" &&
                                    changes[0].newValue == pushRegistration
                            } ?: false
                    }
                    .after {}
                    .blockUntilFinished()
            }
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testOperatingSystemSetProperly() {
        Server
            .endpoint(EndpointType.Identity_Identify)
            .assertWillReceive { it.body.clientSdk?.platform == "fire" }
            .after {
                MParticle.start(
                    MParticleOptions.builder(context)
                        .credentials("key", "secret")
                        .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                        .build()
                )
            }
            .blockUntilFinished()
        MParticle.setInstance(null)
        Server
            .endpoint(EndpointType.Identity_Identify)
            .assertWillReceive { it.body.clientSdk?.platform == "fire" }
            .after {
                MParticle.start(
                    MParticleOptions.builder(context)
                        .credentials("key", "secret")
                        .operatingSystem(MParticle.OperatingSystem.FIRE_OS)
                        .build()
                )
            }
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
                    .builder(context)
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
