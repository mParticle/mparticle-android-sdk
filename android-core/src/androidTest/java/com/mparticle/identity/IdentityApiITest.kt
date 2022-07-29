package com.mparticle.identity

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.MParticleTask
import com.mparticle.internal.ConfigManager
import com.mparticle.messages.IdentityResponseMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mockserver.SuccessResponse
import com.mparticle.utils.randomIdentities
import com.mparticle.utils.setUserIdentity
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class IdentityApiITest : BaseStartedTest() {
    var mConfigManager: ConfigManager? = null
    var handler: Handler? = null
    var mpid1: Long = 0
    var mpid2: Long = 0
    var mpid3: Long = 0
    @Before
    fun before() {
        mConfigManager = MParticle.getInstance()!!.Internal().configManager
        handler = Handler()
        mpid1 = Random.Default.nextLong()
        mpid2 = Random.Default.nextLong()
        mpid3 = Random.Default.nextLong()
    }

    /**
     * test that when we receive a new MParticleUser from and IdentityApi server call, the correct
     * MParticleUser object is passed to all the possible callbacks
     * - IdentityStateListener
     * - MParticleTask<IdentityApiResult>
     * - MParticle.getInstance().Identity().getCurrentUser()
     </IdentityApiResult> */
    @Test
    @Throws(JSONException::class, InterruptedException::class)
    fun testUserChangeCallbackAccuracy() {
        val identities: MutableMap<MParticle.IdentityType, String> = HashMap()
        identities[MParticle.IdentityType.Facebook] = "facebooker.me"
        identities[MParticle.IdentityType.Email] = "tester@mparticle.gov"
        identities[MParticle.IdentityType.Google] = "hello@googlemail.com"
        val identities2: MutableMap<MParticle.IdentityType, String> = HashMap()
        identities2[MParticle.IdentityType.CustomerId] = "12345"
        identities2[MParticle.IdentityType.Microsoft] = "microsoftUser"
        val userAttributes: MutableMap<String, Any> = HashMap()
        userAttributes["field1"] = JSONObject("{jsonField1:\"value\", json2:3}")
        userAttributes["number2"] = "HelloWorld"
        userAttributes["third"] = 123
        val isLoggedIn: Boolean = Random.Default.nextBoolean()
        Server.endpoint(EndpointType.Identity_Login).addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
            SuccessResponse {
                this.responseObject = IdentityResponseMessage(mpid1, isLoggedIn = isLoggedIn)
            }
        }
        val latch: CountDownLatch = FailureLatch()
        var count = 0
        MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
            if (user.id == mpid1) {
                try {
                    com.mparticle.internal.AccessUtils.awaitMessageHandler()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                assertMParticleUserEquals(user, mpid1, identities, null, isLoggedIn)
                count++
                if (count == 2)
                    latch.countDown()
            }
        }
        val request = IdentityApiRequest.withEmptyUser().userIdentities(identities).build()
        val result: MParticleTask<IdentityApiResult> =
            MParticle.getInstance()!!.Identity().login(request)

        // test that change actually took place
        result.addSuccessListener { identityApiResult ->
            assertMParticleUserEquals(identityApiResult.user, mpid1, identities, null, isLoggedIn)
            assertEquals(identityApiResult.previousUser!!.id, mStartingMpid)
            latch.countDown()
        }
        latch.await()
    }

    /**
     * happy case, tests that IdentityChangedListener works when added, and stays there
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testIdentityChangedListenerAdd() {
        Server
            .endpoint(EndpointType.Identity_Identify)
            .addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid1, isLoggedIn = false)
                }
            }
            .addResponseLogic({ it.body.previousMpid == mpid1 }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid2, isLoggedIn = true)
                }
            }
        val user1Called: Mutable<Boolean> = Mutable<Boolean>(false)
        val user2Called: Mutable<Boolean> = Mutable<Boolean>(false)
        val user3Called: Mutable<Boolean> = Mutable<Boolean>(false)
        var count = 0
        val latch: CountDownLatch = FailureLatch()
        MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
            if (user.id == mpid1) {
                user1Called.value = true
                count++
            }
            if (user1Called.value && user.id == mpid2) {
                user2Called.value = true
                count++
            }
            if (count == 3) {
                latch.countDown()
            }
        }
        var request = IdentityApiRequest.withEmptyUser().build()
        var result: MParticleTask<IdentityApiResult?> =
            MParticle.getInstance()!!.Identity().identify(request)

        // test that change actually took place
        result.addSuccessListener(
            TaskSuccessListener { identityApiResult ->
                assertEquals(identityApiResult.user.id, mpid1)
                assertEquals(identityApiResult.previousUser!!.id, mStartingMpid)
            }
        )
        com.mparticle.internal.AccessUtils.awaitUploadHandler()
        request = IdentityApiRequest.withEmptyUser().build()
        result = MParticle.getInstance()!!.Identity().identify(request)
        result.addSuccessListener(
            TaskSuccessListener { identityApiResult ->
                assertEquals(identityApiResult.user.id, mpid2)
                assertEquals(
                    identityApiResult.user.id,
                    MParticle.getInstance()!!
                        .Identity().currentUser!!.id
                )
                assertEquals(identityApiResult.previousUser!!.id, mpid1)
                latch.countDown()
                user3Called.value = true
            }
        )
        latch.await()
        Assert.assertTrue(user1Called.value)
        Assert.assertTrue(user2Called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testAddMultipleIdentityStateListeners() {
        Server
            .endpoint(EndpointType.Identity_Identify)
            .addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid1)
                }
            }
        var count = 0
        val latch: CountDownLatch = FailureLatch()
        fun countDown() {
            count++
            if (count == 6) {
                latch.countDown()
            }
        }
        MParticle.getInstance()!!.Identity()
            .addIdentityStateListener { user, previousUser -> countDown() }
        MParticle.getInstance()!!.Identity()
            .addIdentityStateListener { user, previousUser -> countDown() }
        MParticle.getInstance()!!.Identity()
            .addIdentityStateListener { user, previousUser -> countDown() }
        val request = IdentityApiRequest.withUser(
            MParticle.getInstance()!!.Identity().getUser(mpid1)
        ).build()
        val result: MParticleTask<IdentityApiResult> =
            MParticle.getInstance()!!.Identity().identify(request)
        result
            .addSuccessListener { countDown() }
            .addSuccessListener { countDown() }
            .addSuccessListener { countDown() }
        latch.await()
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveIdentityStateListeners() {
        Server.endpoint(EndpointType.Identity_Identify)
            .addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid1)
                }
            }
            .addResponseLogic({ it.body.previousMpid == mpid1 }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid2)
                }
            }
        val mpid1Latch: CountDownLatch = FailureLatch()
        val mpid2Latch: CountDownLatch = FailureLatch()
        val keptIdStateListener = IdentityStateListener { user, previousUser ->
            if (user.id == mpid1 && previousUser!!.id == mStartingMpid) {
                mpid1Latch.countDown()
            }
            if (user.id == mpid2 && previousUser!!.id == mpid1) {
                mpid2Latch.countDown()
            }
        }
        val removeIdStateListener1 = IdentityStateListener { user, previousUser ->
            if (user.id != mpid1 || previousUser!!.id != mStartingMpid) {
                fail("IdentityStateListener failed to be removed")
            }
        }
        val removeIdStateListener2 = IdentityStateListener { user, previousUser ->
            if (user.id != mpid1 || previousUser!!.id != mStartingMpid) {
                fail("IdentityStateListener failed to be removed")
            }
        }
        val removeIdStateListener3 = IdentityStateListener { user, previousUser ->
            if (user.id != mpid1 || previousUser!!.id != mStartingMpid) {
                junit.framework.Assert.fail("IdentityStateListener failed to be removed")
            }
        }
        MParticle.getInstance()!!.Identity().addIdentityStateListener(keptIdStateListener)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(removeIdStateListener1)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(removeIdStateListener2)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(removeIdStateListener3)
        MParticle.getInstance()!!
            .Identity().identify(IdentityApiRequest.withEmptyUser().build())
        mpid1Latch.await()
        MParticle.getInstance()!!.Identity().removeIdentityStateListener(removeIdStateListener1)
        MParticle.getInstance()!!.Identity().removeIdentityStateListener(removeIdStateListener2)
        MParticle.getInstance()!!.Identity().removeIdentityStateListener(removeIdStateListener3)
        MParticle.getInstance()!!
            .Identity().identify(IdentityApiRequest.withEmptyUser().build())
        mpid2Latch.await()
    }

    /**
     * Make sure that the [IdentityStateListener] callbacks are occuring on the Main Thread.
     * This is important so that the KitManagerImpl, which will only instantiate kits on the MainThread,
     * will instantiate kits synchronously
     * @throws InterruptedException
     */
    @Test
    @Throws(InterruptedException::class)
    fun testIdentityStateListenerThread() {
        val called: Mutable<Boolean> = Mutable<Boolean>(false)
        val latch: CountDownLatch = FailureLatch()
        val backgroundThread = HandlerThread(RandomUtils.getAlphaNumericString(8))
        backgroundThread.start()
        Handler(backgroundThread.getLooper()).post(
            Runnable {
                MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
                    assertEquals(Looper.getMainLooper(), Looper.myLooper())
                    assertEquals(
                        user.id,
                        MParticle.getInstance()!!
                            .Identity().currentUser!!.id
                    )
                    called.value = true
                    latch.countDown()
                }
                MParticle.getInstance()!!
                    .Internal().configManager.setMpid(mpid1, Random.Default.nextBoolean())
            }
        )
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIdentityTransitionListener() {
        Server
            .endpoint(EndpointType.Identity_Login)
            .addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid1)
                }
            }
        val latch: CountDownLatch = FailureLatch()
        val called: Mutable<Boolean> = Mutable(false)
        MParticle.getInstance()!!.Identity().addIdentityStateListener { newUser, previousUser ->
            assertEquals(mStartingMpid, previousUser!!.id)
            assertEquals(mpid1, newUser.id)
            called.value = true
            latch.countDown()
        }
        MParticle.getInstance()!!.Identity().login()
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testCallbacksVsCurrentUser() {
        Server
            .endpoint(EndpointType.Identity_Login)
            .addResponseLogic({ it.body.previousMpid == mStartingMpid }) {
                SuccessResponse {
                    responseObject = IdentityResponseMessage(mpid1)
                }
            }
        var count = 0
        val latch: CountDownLatch = FailureLatch()
        val called1: Mutable<Boolean> = Mutable<Boolean>(false)
        val called2: Mutable<Boolean> = Mutable<Boolean>(false)
        MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
            assertEquals(
                mpid1,
                MParticle.getInstance()!!.Identity().currentUser!!
                    .id
            )
            assertEquals(mpid1, user.id)
            assertEquals(mStartingMpid, previousUser!!.id)
            called1.value = true
            count++
            if (count == 2) {
                latch.countDown()
            }
        }
        MParticle.getInstance()!!.Identity().login()
            .addSuccessListener { result ->
                assertEquals(
                    mpid1,
                    MParticle.getInstance()!!.Identity().currentUser!!
                        .id
                )
                assertEquals(mpid1, result.user.id)
                assertEquals(mStartingMpid, result.previousUser!!.id)
                called2.value = true
                count++
                if (count == 2) {
                    latch.countDown()
                }
            }
        latch.await()
        Assert.assertTrue(called1.value)
        Assert.assertTrue(called2.value)
    }

    private fun assertMParticleUserEquals(
        dto1: MParticleUser?,
        mpid: Long,
        identityTypes: Map<MParticle.IdentityType, String>,
        userAttributes: Map<String?, Any?>?,
        isLoggedIn: Boolean
    ) {
        Assert.assertTrue(dto1!!.id == mpid)
        if (userAttributes != null) {
            if (dto1.userAttributes != null) {
                assertEquals(dto1.userAttributes.size.toLong(), userAttributes.size.toLong())
                for ((key, value) in dto1.userAttributes) {
                    if (value == null) {
                        Assert.assertNull(userAttributes[key])
                    } else {
                        assertEquals(value.toString(), userAttributes[key].toString())
                    }
                }
            }
        } else {
            assertEquals(dto1.userAttributes.size.toLong(), 0)
        }
        assertEquals(dto1.userIdentities.size.toLong(), identityTypes.size.toLong())
        for ((key, value) in dto1.userIdentities) {
            assertEquals(value, identityTypes[key])
        }
        assertEquals(isLoggedIn, dto1.isLoggedIn)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUser() {
        val identity = MParticle.getInstance()!!
            .Identity()
        Assert.assertNotNull(identity.currentUser)
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid1, true)
        assertEquals(identity.currentUser!!.id, mpid1)
        val mpid1UserAttributes =
            RandomUtils.getRandomAttributes(3).toMutableMap()
        val mpid1UserIdentites: Map<MParticle.IdentityType, String> =
            randomIdentities(2)
        identity.currentUser!!.userAttributes = mpid1UserAttributes.toMap()
        AccessUtils.setUserIdentities(mpid1UserIdentites.toMap(), identity.currentUser!!.id)
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid2, false)
        val mpid2UserAttributes =
            RandomUtils.getRandomAttributes(3).toMutableMap()
        val mpid2UserIdentites: Map<MParticle.IdentityType, String> =
            randomIdentities(3)
        identity.currentUser!!.userAttributes = mpid2UserAttributes.toMap()
        AccessUtils.setUserIdentities(mpid2UserIdentites.toMap(), identity.currentUser!!.id)
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid3, true)
        val mpid3UserAttributes =
            RandomUtils.getRandomAttributes(3).toMutableMap()
        val mpid3UserIdentities: Map<MParticle.IdentityType, String> =
            randomIdentities(2)
        identity.currentUser!!.userAttributes = mpid3UserAttributes.toMap()
        AccessUtils.setUserIdentities(mpid3UserIdentities.toMap(), identity.currentUser!!.id)
        mpid1UserAttributes.remove(null)
        mpid2UserAttributes.remove(null)
        mpid3UserAttributes.remove(null)
        com.mparticle.internal.AccessUtils.awaitMessageHandler()

        // should return null for mpid = 0
        Assert.assertNull(identity.getUser(0L))

        // should return an MParticleUser with the correct mpid, userIdentities, and userAttributes for
        // previously seen users
        assertMParticleUserEquals(
            identity.getUser(mpid1),
            mpid1,
            mpid1UserIdentites,
            mpid1UserAttributes,
            true
        )
        assertMParticleUserEquals(
            identity.getUser(mpid2),
            mpid2,
            mpid2UserIdentites,
            mpid2UserAttributes,
            false
        )
        assertMParticleUserEquals(
            identity.getUser(mpid3),
            mpid3,
            mpid3UserIdentities,
            mpid3UserAttributes,
            true
        )

        // should return null for unseen mpid's
        Assert.assertNull(identity.getUser(RandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE)))
        Assert.assertNull(identity.getUser(RandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE)))
        Assert.assertNull(identity.getUser(RandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE)))
    }

    /**
     * this simulates the scenerio in which you make a modify() call, but the current MParticleUser
     * changes between the time you build the request and the time you make the call
     */
    @Test
    @Throws(Exception::class)
    fun testModifyConcurrentCalls() {
        assertEquals(
            mStartingMpid,
            MParticle.getInstance()!!
                .Identity().currentUser!!.id
        )
        val userIdentities: Map<MParticle.IdentityType, String> =
            randomIdentities()
        for (identity in userIdentities.keys) {
            setUserIdentity(userIdentities[identity], identity, mStartingMpid)
        }
        val request = IdentityApiRequest.withUser(
            MParticle.getInstance()!!.Identity().currentUser
        ).customerId(RandomUtils.getAlphaNumericString(24)).build()
        Server.endpoint(EndpointType.Identity_Modify)
            .assertWillReceive { it.body.identityChanges?.size == 1 }
            .after { MParticle.getInstance()!!.Identity().modify(request) }
            .blockUntilFinished()

        // change the mpid;
        // behind the scenes, this call will take place before the (above) modify request goes out, since
        // the modify request will be passed to a background thread before it is executed
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid2, Random.Default.nextBoolean())
    }

    @Test
    fun testGetUsersApi() {
        // test that by default there is only the starting user
        assertEquals(MParticle.getInstance()!!.Identity().users.size.toLong(), 1)
        assertEquals(MParticle.getInstance()!!.Identity().users[0].id, mStartingMpid)

        // add 5 Users
        val mpids: MutableList<Long> = ArrayList()
        mpids.add(mStartingMpid)
        for (i in 0..4) {
            mpids.add(Random.Default.nextLong())
        }
        for (mpid in mpids) {
            MParticle.getInstance()!!
                .Internal().configManager.setMpid(mpid, Random.Default.nextBoolean())
        }

        // test that there are now 6 users present in the getUsers() endpoint
        assertEquals(
            MParticle.getInstance()!!.Identity().users.size.toLong(),
            mpids.size.toLong()
        )

        // test that they are the same users we added before
        for (
            mParticleUser in MParticle.getInstance()!!
                .Identity().users
        ) {
            Assert.assertTrue(mpids.contains(mParticleUser.id))
        }

        // remove 2 users
        for (i in 0..1) {
            MParticle.getInstance()!!.Internal().configManager.deleteUserStorage(mpids.removeAt(i))
        }

        // test that there are now 4 remaining users
        assertEquals(MParticle.getInstance()!!.Identity().users.size.toLong(), 4)

        // test that they are the correct users
        for (
            mParticleUser in MParticle.getInstance()!!
                .Identity().users
        ) {
            Assert.assertTrue(mpids.contains(mParticleUser.id))
        }
    }

    /**
     * make sure that there is no way for an MParticleUser with MPID == 0 to be returned from the
     * IdentityAPI
     */
    @Test
    fun testNoZeroMpidUser() {
        Assert.assertNull(MParticle.getInstance()!!.Identity().getUser(0L))
        for (
            user in MParticle.getInstance()!!
                .Identity().users
        ) {
            junit.framework.Assert.assertNotSame(0, user.id)
        }
        MParticle.getInstance()!!.Internal().configManager.setMpid(0L, Random.Default.nextBoolean())
        Assert.assertNull(MParticle.getInstance()!!.Identity().getUser(0L))
        for (
            user in MParticle.getInstance()!!
                .Identity().users
        ) {
            junit.framework.Assert.assertNotSame(0, user.id)
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testGetDeviceApplicationStamp() {
        val dasLength = UUID.randomUUID().toString().length
        val currentDas = MParticle.getInstance()!!.Identity().deviceApplicationStamp
        assertEquals(dasLength.toLong(), currentDas.length.toLong())
        assertEquals(currentDas, MParticle.getInstance()!!.Identity().deviceApplicationStamp)
        MParticle.reset(context)
        startMParticle()
        val newDas = MParticle.getInstance()!!.Identity().deviceApplicationStamp
        Assert.assertNotNull(newDas)
        junit.framework.Assert.assertNotSame(currentDas, newDas)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModifyWhenIdentityAddedConcurrently() {
        val latch: CountDownLatch = FailureLatch()
        val currentUser = MParticle.getInstance()!!
            .Identity().currentUser
        val identityApi = MParticle.getInstance()!!
            .Identity()
        val modifyRequest = IdentityApiRequest.withUser(currentUser)
            .pushToken("new push", "old_push")
            .build()
        val taskSuccessListener = TaskSuccessListener {

            Server
                .endpoint(EndpointType.Identity_Modify)
                .assertWillReceive {
                    it.body.identityChanges?.let {
                        assertEquals(1, it.size)
                        it[0].apply {
                            identityType == "push_token"
                        }
                        true
                    } ?: false
                }
                .after {
                    identityApi.modify(modifyRequest)
                }
                .blockUntilFinished()
            latch.countDown()
        }
        val loginRequest = IdentityApiRequest.withUser(currentUser)
            .customerId("my Id")
            .build()
        identityApi
            .login(loginRequest)
            .addSuccessListener(taskSuccessListener)
        latch.await()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModifyWhenIdentityChangesConcurrently() {
        val latch: CountDownLatch = FailureLatch()
        val currentUser = MParticle.getInstance()!!
            .Identity().currentUser
        val identityApi = MParticle.getInstance()!!
            .Identity()
        val modifyRequest = IdentityApiRequest.withUser(currentUser)
            .customerId("new customer ID")
            .build()
        val taskSuccessListener = TaskSuccessListener {
            identityApi.modify(modifyRequest)
            Server
                .endpoint(EndpointType.Identity_Modify)
                .assertWillReceive {
                    it.body.identityChanges?.let {
                        assertEquals(1, it.size)
                        it[0].apply {
                            assertEquals("customerid", identityType)
                            assertEquals("new customer ID", newValue)
                            assertEquals("old customer ID", oldValue)
                        }
                        true
                    } ?: false
                }
                .blockUntilFinished()
            latch.countDown()
        }
        val loginRequest = IdentityApiRequest.withUser(currentUser)
            .customerId("old customer ID")
            .build()
        identityApi
            .login(loginRequest)
            .addSuccessListener(taskSuccessListener)
        latch.await()
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModifyDoesntMissIdentitiesSetNull() {
        setUserIdentity("customer Id", MParticle.IdentityType.CustomerId, mStartingMpid)
        setUserIdentity("facebook Id", MParticle.IdentityType.Facebook, mStartingMpid)
        setUserIdentity("other Id", MParticle.IdentityType.Other2, mStartingMpid)

        Server
            .endpoint(EndpointType.Identity_Modify)
            .assertWillReceive {
                it.body.identityChanges?.let {
                    assertEquals(3, it.size)
                    it
                        .first { it.identityType == "customerid" }
                        .apply {
                            assertEquals("customer Id", oldValue)
                            assertEquals(null, newValue)
                        }
                    it
                        .first { it.identityType == "facebook" }
                        .apply {
                            assertEquals("facebook Id", oldValue)
                            assertEquals(null, newValue)
                        }
                    it
                        .first { it.identityType == "other2" }
                        .apply {
                            assertEquals("other Id", oldValue)
                            assertEquals(null, newValue)
                        }
                    true
                } ?: false
            }
            .after {
                val request = IdentityApiRequest.withUser(
                    MParticle.getInstance()!!.Identity().currentUser
                )
                    .customerId(null)
                    .userIdentity(MParticle.IdentityType.Facebook, null)
                    .userIdentity(MParticle.IdentityType.Other2, null)
                    .build()
                MParticle.getInstance()!!.Identity().modify(request)
            }
            .blockUntilFinished()
    }
}
