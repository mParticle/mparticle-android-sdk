package com.mparticle.identity

import android.os.Handler
import com.mparticle.MParticle
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.MPUtility
import com.mparticle.networking.MPConnection
import com.mparticle.networking.MPConnectionMockImpl
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.utils.randomIdentities
import junit.framework.Assert
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class MParticleIdentityClientImplITest : BaseStartedTest() {
    private var mConfigManager: ConfigManager? = null
    private var mApiClient: MParticleIdentityClientImpl? = null
    @Before
    @Throws(Exception::class)
    fun before() {
        mConfigManager = MParticle.getInstance()!!.Internal().configManager
    }

    @Test
    @Throws(Exception::class)
    fun testModifySameData() {
        val latch: CountDownLatch = FailureLatch(count = 2)
        val handler = Handler()
        handler.postDelayed({ Assert.fail("modify did not complete") }, (10 * 1000).toLong())
        val called: Mutable<Boolean> = Mutable<Boolean>(false)
        MParticle.getInstance()!!
            .Identity().modify(IdentityApiRequest.withEmptyUser().build())
            .addSuccessListener {
                latch.countDown()
                MParticle.getInstance()!!
                    .Identity().modify(IdentityApiRequest.withEmptyUser().build())
                    .addSuccessListener {
                        val currentModifyRequestCount: Int = Server.endpoint(EndpointType.Identity_Modify).requests.size
                        // make sure we made 1 or 0 modify requests. It could go either way for the first modify request,
                        // it may have changes, it may not depending on state. The second request though, should not have
                        // changes, and therefore it should not take place, so less than 2 requests is a good condition
                        org.junit.Assert.assertTrue(2 > currentModifyRequestCount)
                        handler.removeCallbacksAndMessages(null)
                        called.value = true
                        latch.countDown()
                    }
                    .addFailureListener { Assert.fail("task failed") }
            }
            .addFailureListener { Assert.fail("task failed") }
        latch.await()
        org.junit.Assert.assertTrue(called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testIdentifyMessage() {
        val iterations = 5
        for (i in 0 until iterations) {
            val userIdentities: Map<MParticle.IdentityType?, String?> = randomIdentities().toMap()
            val latch = CountDownLatch(1)
            setApiClient(object : MockIdentityApiClient {
                @Throws(IOException::class, JSONException::class)
                override fun makeUrlRequest(
                    connection: MPConnection,
                    payload: String?,
                    mparticle: Boolean
                ) {
                    if (connection.url.toString().contains("/identify")) {
                        val jsonObject = JSONObject(payload)
                        val knownIdentities =
                            jsonObject.getJSONObject(MParticleIdentityClientImpl.KNOWN_IDENTITIES)
                        org.junit.Assert.assertNotNull(knownIdentities)
                        checkStaticsAndRemove(knownIdentities)
                        if (knownIdentities.length() != userIdentities.size) {
                            org.junit.Assert.assertEquals(
                                knownIdentities.length().toLong(),
                                userIdentities.size.toLong()
                            )
                        }
                        for ((key, value1) in userIdentities) {
                            val value = knownIdentities.getString(
                                MParticleIdentityClientImpl.getStringValue(key)
                            )
                            org.junit.Assert.assertEquals(value, value1)
                        }
                        setApiClient(null)
                        latch.countDown()
                    }
                }
            })
            mApiClient!!.identify(
                IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build()
            )
            latch.await()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLoginMessage() {
        val iterations = 5
        for (i in 0 until iterations) {
            val latch: CountDownLatch = FailureLatch()
            val userIdentities: Map<MParticle.IdentityType, String> = randomIdentities()
            setApiClient(object : MockIdentityApiClient {
                @Throws(IOException::class, JSONException::class)
                override fun makeUrlRequest(
                    connection: MPConnection,
                    payload: String?,
                    mparticle: Boolean
                ) {
                    if (connection.url.toString().contains("/login")) {
                        val jsonObject = JSONObject(payload)
                        val knownIdentities =
                            jsonObject.getJSONObject(MParticleIdentityClientImpl.KNOWN_IDENTITIES)
                        org.junit.Assert.assertNotNull(knownIdentities)
                        checkStaticsAndRemove(knownIdentities)
                        org.junit.Assert.assertEquals(
                            knownIdentities.length().toLong(),
                            userIdentities.size.toLong()
                        )
                        for ((key, value1) in userIdentities) {
                            val value = knownIdentities.getString(
                                MParticleIdentityClientImpl.getStringValue(key)
                            )
                            org.junit.Assert.assertEquals(value, value1)
                        }
                        latch.countDown()
                    }
                }
            })
            mApiClient!!.login(
                IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build()
            )
//            latch.await()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLogoutMessage() {
        val iterations = 5
        for (i in 0 until iterations) {
            val userIdentities: Map<MParticle.IdentityType, String> = randomIdentities()
            val latch: CountDownLatch = FailureLatch()
            val checked = Mutable(false)
            setApiClient(object : MockIdentityApiClient {
                @Throws(IOException::class, JSONException::class)
                override fun makeUrlRequest(
                    connection: MPConnection,
                    payload: String?,
                    mparticle: Boolean
                ) {
                    if (connection.url.toString().contains("/logout")) {
                        val jsonObject = JSONObject(payload)
                        val knownIdentities =
                            jsonObject.getJSONObject(MParticleIdentityClientImpl.KNOWN_IDENTITIES)
                        org.junit.Assert.assertNotNull(knownIdentities)
                        checkStaticsAndRemove(knownIdentities)
                        org.junit.Assert.assertEquals(
                            knownIdentities.length().toLong(),
                            userIdentities.size.toLong()
                        )
                        for ((key, value1) in userIdentities) {
                            val value = knownIdentities.getString(
                                MParticleIdentityClientImpl.getStringValue(key)
                            )
                            org.junit.Assert.assertEquals(value, value1)
                        }
                        checked.value = true
                        latch.countDown()
                    }
                }
            })
            mApiClient!!.logout(
                IdentityApiRequest.withEmptyUser()
                    .userIdentities(userIdentities)
                    .build()
            )
            latch.await()
            org.junit.Assert.assertTrue(checked.value)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testModifyMessage() {
        val iterations = 5
        for (i in 1..iterations) {
            mConfigManager?.setMpid(i.toLong(), Random.Default.nextBoolean())
            val oldUserIdentities: Map<MParticle.IdentityType, String> = randomIdentities()
            val newUserIdentities: Map<MParticle.IdentityType, String> = randomIdentities()
            MParticle.getInstance()?.Identity()?.currentUser
                .let { it as MParticleUserImpl }
                .userIdentities = oldUserIdentities
            val latch: CountDownLatch = FailureLatch()
            val checked = Mutable(false)
            setApiClient(object : MockIdentityApiClient {
                @Throws(IOException::class, JSONException::class)
                override fun makeUrlRequest(
                    connection: MPConnection,
                    payload: String?,
                    identityRequest: Boolean
                ) {
                    if (connection.url.toString()
                        .contains(MParticleIdentityClientImpl.MODIFY_PATH)
                    ) {
                        val jsonObject = JSONObject(payload)
                        val changedIdentities =
                            jsonObject.getJSONArray(MParticleIdentityClientImpl.IDENTITY_CHANGES)
                        for (i in 0 until changedIdentities.length()) {
                            val changeJson = changedIdentities.getJSONObject(i)
                            val newValue: Any =
                                changeJson.getString(MParticleIdentityClientImpl.NEW_VALUE)
                            val oldValue: Any =
                                changeJson.getString(MParticleIdentityClientImpl.OLD_VALUE)
                            val identityType = MParticleIdentityClientImpl.getIdentityType(
                                changeJson.getString(MParticleIdentityClientImpl.IDENTITY_TYPE)
                            )
                            val nullString = JSONObject.NULL.toString()
                            if (oldUserIdentities[identityType] == null) {
                                if (oldValue != JSONObject.NULL.toString()) {
                                    Assert.fail()
                                }
                            } else {
                                org.junit.Assert.assertEquals(
                                    oldValue,
                                    oldUserIdentities[identityType]
                                )
                            }
                            if (newUserIdentities[identityType] == null) {
                                if (newValue != nullString) {
                                    Assert.fail()
                                }
                            } else {
                                org.junit.Assert.assertEquals(
                                    newValue,
                                    newUserIdentities[identityType]
                                )
                            }
                        }
                        setApiClient(null)
                        checked.value = true
                        latch.countDown()
                    }
                }
            })
            mApiClient!!.modify(
                IdentityApiRequest.withEmptyUser()
                    .userIdentities(newUserIdentities)
                    .build()
            )
            latch.await()
            org.junit.Assert.assertTrue(checked.value)
        }
    }

    private fun setApiClient(identityClient: MockIdentityApiClient?) {
        mApiClient = object : MParticleIdentityClientImpl(
            context,
            mConfigManager,
            MParticle.OperatingSystem.ANDROID
        ) {
            @Throws(IOException::class)
            override fun makeUrlRequest(
                endpoint: Endpoint,
                connection: MPConnection,
                payload: String,
                identity: Boolean
            ): MPConnection {
                try {
                    identityClient!!.makeUrlRequest(connection, payload, identity)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Assert.fail(e.message)
                }
                (connection as MPConnectionMockImpl).connection.setResponseCode(202)
                return super.makeUrlRequest(endpoint, connection, payload, identity)
            }
        }
        MParticle.getInstance()!!.Identity().apiClient = mApiClient
    }

    @Throws(JSONException::class)
    private fun checkStaticsAndRemove(knowIdentites: JSONObject) {
        if (knowIdentites.has(MParticleIdentityClientImpl.ANDROID_AAID)) {
            org.junit.Assert.assertEquals(
                MPUtility.getAdIdInfo(context).id,
                knowIdentites.getString(MParticleIdentityClientImpl.ANDROID_AAID)
            )
            knowIdentites.remove(MParticleIdentityClientImpl.ANDROID_AAID)
        } else {
            org.junit.Assert.assertTrue(
                MPUtility.getAdIdInfo(context) == null || MPUtility.isEmpty(
                    MPUtility.getAdIdInfo(context).id
                )
            )
        }
        if (knowIdentites.has(MParticleIdentityClientImpl.ANDROID_UUID)) {
            org.junit.Assert.assertEquals(
                MPUtility.getAndroidID(context),
                knowIdentites.getString(MParticleIdentityClientImpl.ANDROID_UUID)
            )
            knowIdentites.remove(MParticleIdentityClientImpl.ANDROID_UUID)
        } else {
            org.junit.Assert.assertTrue(MPUtility.isEmpty(MPUtility.getAndroidID(context)))
        }
        if (knowIdentites.has(MParticleIdentityClientImpl.PUSH_TOKEN)) {
            org.junit.Assert.assertEquals(
                mConfigManager?.pushInstanceId,
                knowIdentites.getString(MParticleIdentityClientImpl.PUSH_TOKEN)
            )
            knowIdentites.remove(MParticleIdentityClientImpl.PUSH_TOKEN)
        } else {
            org.junit.Assert.assertNull(mConfigManager?.pushInstanceId)
        }
        org.junit.Assert.assertTrue(knowIdentites.has(MParticleIdentityClientImpl.DEVICE_APPLICATION_STAMP))
        org.junit.Assert.assertEquals(
            mConfigManager?.deviceApplicationStamp,
            knowIdentites[MParticleIdentityClientImpl.DEVICE_APPLICATION_STAMP]
        )
        knowIdentites.remove(MParticleIdentityClientImpl.DEVICE_APPLICATION_STAMP)
    }

    internal interface MockIdentityApiClient {
        @Throws(IOException::class, JSONException::class)
        fun makeUrlRequest(connection: MPConnection, payload: String?, mparticle: Boolean)
    }
}
