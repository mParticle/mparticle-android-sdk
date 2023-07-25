package com.mparticle.identity

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.internal.ConfigManager
import com.mparticle.networking.Matcher
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch

class IdentityApiTest : BaseCleanStartedEachTest() {
    var mConfigManager: ConfigManager? = null
    var handler: Handler? = null
    var mpid1: Long = 0
    var mpid2: Long = 0
    var mpid3: Long = 0

    @Before
    fun before() {
        mConfigManager = MParticle.getInstance()!!.Internal().configManager
        handler = Handler()
        mpid1 = ran.nextLong()
        mpid2 = ran.nextLong()
        mpid3 = ran.nextLong()
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
        val identities: MutableMap<IdentityType, String> = HashMap()
        identities[IdentityType.Facebook] = "facebooker.me"
        identities[IdentityType.Email] = "tester@mparticle.gov"
        identities[IdentityType.Google] = "hello@googlemail.com"
        val identities2: MutableMap<IdentityType, String> = HashMap()
        identities2[IdentityType.CustomerId] = "12345"
        identities2[IdentityType.Microsoft] = "microsoftUser"
        val userAttributes: MutableMap<String, Any> = HashMap()
        userAttributes["field1"] = JSONObject("{jsonField1:\"value\", json2:3}")
        userAttributes["number2"] = "HelloWorld"
        userAttributes["third"] = 123
        val isLoggedIn = ran.nextBoolean()
        mServer.addConditionalLoginResponse(mStartingMpid, mpid1, isLoggedIn)
        val latch: CountDownLatch = MPLatch(2)
        MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
            if (user.id == mpid1) {
                try {
                    com.mparticle.internal.AccessUtils.awaitMessageHandler()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                assertMParticleUserEquals(user, mpid1, identities, null, isLoggedIn)
                latch.countDown()
            }
        }
        val request = IdentityApiRequest.withEmptyUser().userIdentities(identities).build()
        val result = MParticle.getInstance()!!.Identity().login(request)

        // test that change actually took place
        result.addSuccessListener { identityApiResult ->
            assertMParticleUserEquals(identityApiResult.user, mpid1, identities, null, isLoggedIn)
            Assert.assertEquals(identityApiResult.previousUser!!.id, mStartingMpid.toLong())
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
        mServer
            .addConditionalIdentityResponse(mStartingMpid, mpid1, false)
            .addConditionalIdentityResponse(mpid1, mpid2, true)
        val user1Called = AndroidUtils.Mutable(false)
        val user2Called = AndroidUtils.Mutable(false)
        val user3Called = AndroidUtils.Mutable(false)
        val latch: CountDownLatch = MPLatch(3)
        MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
            if (user != null && user.id == mpid1) {
                user1Called.value = true
                latch.countDown()
            }
            if (user1Called.value && user.id == mpid2) {
                user2Called.value = true
                latch.countDown()
            }
        }
        var request = IdentityApiRequest.withEmptyUser().build()
        var result = MParticle.getInstance()!!.Identity().identify(request)

        // test that change actually took place
        result.addSuccessListener { identityApiResult ->
            Assert.assertEquals(identityApiResult.user.id, mpid1)
            Assert.assertEquals(identityApiResult.previousUser!!.id, mStartingMpid.toLong())
        }
        com.mparticle.internal.AccessUtils.awaitUploadHandler()
        request = IdentityApiRequest.withEmptyUser().build()
        result = MParticle.getInstance()!!.Identity().identify(request)
        result.addSuccessListener { identityApiResult ->
            Assert.assertEquals(identityApiResult.user.id, mpid2)
            Assert.assertEquals(
                identityApiResult.user.id,
                MParticle.getInstance()!!
                    .Identity().currentUser!!.id
            )
            Assert.assertEquals(identityApiResult.previousUser!!.id, mpid1)
            latch.countDown()
            user3Called.value = true
        }
        latch.await()
        Assert.assertTrue(user1Called.value)
        Assert.assertTrue(user2Called.value)
    }

    @Test
    @Throws(Exception::class)
    fun testAddMultipleIdentityStateListeners() {
        mServer.addConditionalIdentityResponse(mStartingMpid, mpid1)
        val latch: CountDownLatch = MPLatch(6)
        MParticle.getInstance()!!.Identity()
            .addIdentityStateListener { user, previousUser -> latch.countDown() }
        MParticle.getInstance()!!.Identity()
            .addIdentityStateListener { user, previousUser -> latch.countDown() }
        MParticle.getInstance()!!.Identity()
            .addIdentityStateListener { user, previousUser -> latch.countDown() }
        val request =
            IdentityApiRequest.withUser(MParticle.getInstance()!!.Identity().getUser(mpid1)).build()
        val result = MParticle.getInstance()!!.Identity().identify(request)
        result.addSuccessListener { latch.countDown() }
            .addSuccessListener { latch.countDown() }
            .addSuccessListener { latch.countDown() }
        latch.await()
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveIdentityStateListeners() {
        mServer.addConditionalIdentityResponse(mStartingMpid, mpid1)
            .addConditionalIdentityResponse(mpid1, mpid2)
        val mpid1Latch: CountDownLatch = MPLatch(1)
        val mpid2Latch: CountDownLatch = MPLatch(1)
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
                Assert.fail("IdentityStateListener failed to be removed")
            }
        }
        val removeIdStateListener2 = IdentityStateListener { user, previousUser ->
            if (user.id != mpid1 || previousUser!!.id != mStartingMpid) {
                Assert.fail("IdentityStateListener failed to be removed")
            }
        }
        val removeIdStateListener3 = IdentityStateListener { user, previousUser ->
            if (user.id != mpid1 || previousUser!!.id != mStartingMpid) {
                Assert.fail("IdentityStateListener failed to be removed")
            }
        }
        MParticle.getInstance()!!.Identity().addIdentityStateListener(keptIdStateListener)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(removeIdStateListener1)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(removeIdStateListener2)
        MParticle.getInstance()!!.Identity().addIdentityStateListener(removeIdStateListener3)
        MParticle.getInstance()!!.Identity().identify(IdentityApiRequest.withEmptyUser().build())
        mpid1Latch.await()
        MParticle.getInstance()!!.Identity().removeIdentityStateListener(removeIdStateListener1)
        MParticle.getInstance()!!.Identity().removeIdentityStateListener(removeIdStateListener2)
        MParticle.getInstance()!!.Identity().removeIdentityStateListener(removeIdStateListener3)
        MParticle.getInstance()!!.Identity().identify(IdentityApiRequest.withEmptyUser().build())
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
        val called = AndroidUtils.Mutable(false)
        val latch: CountDownLatch = MPLatch(1)
        val backgroundThread = HandlerThread(mRandomUtils.getAlphaNumericString(8))
        backgroundThread.start()
        Handler(backgroundThread.looper).post {
            MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
                Assert.assertEquals(Looper.getMainLooper(), Looper.myLooper())
                Assert.assertEquals(
                    user.id,
                    MParticle.getInstance()!!.Identity().currentUser!!
                        .id
                )
                called.value = true
                latch.countDown()
            }
            MParticle.getInstance()!!.Internal().configManager.setMpid(mpid1, ran.nextBoolean())
        }
        latch.await()
        Assert.assertTrue(called.value)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testIdentityTransitionListener() {
        mServer.addConditionalLoginResponse(mStartingMpid, mpid1)
        val latch: CountDownLatch = MPLatch(1)
        val called = AndroidUtils.Mutable(false)
        MParticle.getInstance()!!.Identity().addIdentityStateListener { newUser, previousUser ->
            Assert.assertEquals(mStartingMpid.toLong(), previousUser!!.id)
            Assert.assertEquals(mpid1, newUser.id)
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
        mServer.addConditionalLoginResponse(mStartingMpid, mpid1)
        val latch: CountDownLatch = MPLatch(2)
        val called1 = AndroidUtils.Mutable(false)
        val called2 = AndroidUtils.Mutable(false)
        MParticle.getInstance()!!.Identity().addIdentityStateListener { user, previousUser ->
            Assert.assertEquals(
                mpid1,
                MParticle.getInstance()!!
                    .Identity().currentUser!!.id
            )
            Assert.assertEquals(mpid1, user.id)
            Assert.assertEquals(mStartingMpid.toLong(), previousUser!!.id)
            called1.value = true
            latch.countDown()
        }
        MParticle.getInstance()!!.Identity().login()
            .addSuccessListener { result ->
                Assert.assertEquals(
                    mpid1,
                    MParticle.getInstance()!!
                        .Identity().currentUser!!.id
                )
                Assert.assertEquals(mpid1, result.user.id)
                Assert.assertEquals(
                    mStartingMpid.toLong(),
                    result.previousUser!!
                        .id
                )
                called2.value = true
                latch.countDown()
            }
        latch.await()
        Assert.assertTrue(called1.value)
        Assert.assertTrue(called2.value)
    }

    private fun assertMParticleUserEquals(
        dto1: MParticleUser?,
        mpid: Long,
        identityTypes: Map<IdentityType, String>,
        userAttributes: Map<String?, Any>?,
        isLoggedIn: Boolean
    ) {
        Assert.assertTrue(dto1!!.id == mpid)
        if (userAttributes != null) {
            if (dto1.userAttributes != null) {
                Assert.assertEquals(dto1.userAttributes.size.toLong(), userAttributes.size.toLong())
                for ((key, value) in dto1.userAttributes) {
                    if (value == null) {
                        Assert.assertNull(userAttributes[key])
                    } else {
                        Assert.assertEquals(value.toString(), userAttributes[key].toString())
                    }
                }
            }
        } else {
            Assert.assertEquals(dto1.userAttributes.size.toLong(), 0)
        }
        Assert.assertEquals(dto1.userIdentities.size.toLong(), identityTypes.size.toLong())
        for ((key, value) in dto1.userIdentities) {
            Assert.assertEquals(value, identityTypes[key])
        }
        Assert.assertEquals(isLoggedIn, dto1.isLoggedIn)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUser() {
        val identity = MParticle.getInstance()!!.Identity()
        Assert.assertNotNull(identity.currentUser)
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid1, true)
        Assert.assertEquals(identity.currentUser!!.id, mpid1)
        val mpid1UserAttributes: MutableMap<String?, Any> =
            HashMap(mRandomUtils.getRandomAttributes(3))
        val mpid1UserIdentites = mRandomUtils.getRandomUserIdentities(2)
        identity.currentUser!!.userAttributes = mpid1UserAttributes
        AccessUtils.setUserIdentities(
            mpid1UserIdentites,
            identity.currentUser!!
                .id
        )
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid2, false)
        val mpid2UserAttributes: MutableMap<String?, Any> =
            HashMap(mRandomUtils.getRandomAttributes(3))
        val mpid2UserIdentites = mRandomUtils.getRandomUserIdentities(3)
        identity.currentUser!!.userAttributes = mpid2UserAttributes
        AccessUtils.setUserIdentities(
            mpid2UserIdentites,
            identity.currentUser!!
                .id
        )
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid3, true)
        val mpid3UserAttributes: MutableMap<String?, Any> =
            HashMap(mRandomUtils.getRandomAttributes(3))
        val mpid3UserIdentities = mRandomUtils.getRandomUserIdentities(2)
        identity.currentUser!!.userAttributes = mpid3UserAttributes
        AccessUtils.setUserIdentities(
            mpid3UserIdentities,
            identity.currentUser!!
                .id
        )
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
        Assert.assertNull(identity.getUser(mRandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE)))
        Assert.assertNull(identity.getUser(mRandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE)))
        Assert.assertNull(identity.getUser(mRandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE)))
    }

    /**
     * this simulates the scenerio in which you make a modify() call, but the current MParticleUser
     * changes between the time you build the request and the time you make the call
     */
    @Test
    @Throws(Exception::class)
    fun testModifyConcurrentCalls() {
        Assert.assertEquals(
            mStartingMpid.toFloat(),
            MParticle.getInstance()!!
                .Identity().currentUser!!.id.toFloat(),
            0f
        )
        val userIdentities = mRandomUtils.randomUserIdentities
        for (identity in userIdentities.keys) {
            AccessUtils.setUserIdentity(userIdentities[identity], identity, mStartingMpid)
        }
        val request = IdentityApiRequest.withUser(MParticle.getInstance()!!.Identity().currentUser)
            .customerId(mRandomUtils.getAlphaNumericString(24)).build()
        val latch: CountDownLatch = MPLatch(1)
        mServer.waitForVerify(Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid))) { request ->
            Assert.assertEquals(1, request.asIdentityRequest().body.identity_changes.size.toLong())
            latch.countDown()
        }
        MParticle.getInstance()!!.Identity().modify(request)
        // change the mpid;
        // behind the scenes, this call will take place before the (above) modify request goes out, since
        // the modify request will be passed to a background thread before it is executed
        MParticle.getInstance()!!.Internal().configManager.setMpid(mpid2, ran.nextBoolean())
        latch.await()
    }

    @Test
    fun testGetUsersApi() {
        // test that by default there is only the starting user
        Assert.assertEquals(MParticle.getInstance()!!.Identity().users.size.toLong(), 1)
        Assert.assertEquals(MParticle.getInstance()!!.Identity().users[0].id, mStartingMpid)

        // add 5 Users
        val mpids: MutableList<Long> = ArrayList()
        mpids.add(mStartingMpid)
        for (i in 0..4) {
            mpids.add(ran.nextLong())
        }
        for (mpid in mpids) {
            MParticle.getInstance()!!.Internal().configManager.setMpid(mpid, ran.nextBoolean())
        }

        // test that there are now 6 users present in the getUsers() endpoint
        Assert.assertEquals(
            MParticle.getInstance()!!.Identity().users.size.toLong(),
            mpids.size.toLong()
        )

        // test that they are the same users we added before
        for (mParticleUser in MParticle.getInstance()!!.Identity().users) {
            Assert.assertTrue(mpids.contains(mParticleUser.id))
        }

        // remove 2 users
        for (i in 0..1) {
            MParticle.getInstance()!!.Internal().configManager.deleteUserStorage(mpids.removeAt(i))
        }

        // test that there are now 4 remaining users
        Assert.assertEquals(MParticle.getInstance()!!.Identity().users.size.toLong(), 4)

        // test that they are the correct users
        for (mParticleUser in MParticle.getInstance()!!.Identity().users) {
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
        for (user in MParticle.getInstance()!!.Identity().users) {
            Assert.assertNotSame(0, user.id)
        }
        MParticle.getInstance()!!.Internal().configManager.setMpid(0L, ran.nextBoolean())
        Assert.assertNull(MParticle.getInstance()!!.Identity().getUser(0L))
        for (user in MParticle.getInstance()!!.Identity().users) {
            Assert.assertNotSame(0, user.id)
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testGetDeviceApplicationStamp() {
        val dasLength = UUID.randomUUID().toString().length
        val currentDas = MParticle.getInstance()!!.Identity().deviceApplicationStamp
        Assert.assertEquals(dasLength.toLong(), currentDas.length.toLong())
        Assert.assertEquals(currentDas, MParticle.getInstance()!!.Identity().deviceApplicationStamp)
        MParticle.reset(mContext)
        startMParticle()
        val newDas = MParticle.getInstance()!!.Identity().deviceApplicationStamp
        Assert.assertNotNull(newDas)
        junit.framework.Assert.assertNotSame(currentDas, newDas)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testModifyWhenIdentityAddedConcurrently() {
        val latch: CountDownLatch = MPLatch(1)
        val currentUser = MParticle.getInstance()!!.Identity().currentUser
        val identityApi = MParticle.getInstance()!!.Identity()
        val modifyRequest = IdentityApiRequest.withUser(currentUser)
            .pushToken("new push", "old_push")
            .build()
        val taskSuccessListener = TaskSuccessListener {
            identityApi.modify(modifyRequest)
            mServer.waitForVerify(
                Matcher(mServer.Endpoints().getModifyUrl(currentUser!!.id))
            ) { request ->
                val identityChanges = request.asIdentityRequest().body.identity_changes
                Assert.assertEquals(1, identityChanges.size.toLong())
                // make sure the customerId didn't change. it should not be included in the IdentityApiRequest
                // since the request was built before customerId was set
                Assert.assertTrue("customerid" != identityChanges[0].optString("identity_type"))
                Assert.assertTrue("push_token" == identityChanges[0].optString("identity_type"))
                latch.countDown()
            }
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
        val latch: CountDownLatch = MPLatch(1)
        val currentUser = MParticle.getInstance()!!.Identity().currentUser
        val identityApi = MParticle.getInstance()!!.Identity()
        val modifyRequest = IdentityApiRequest.withUser(currentUser)
            .customerId("new customer ID")
            .build()
        val taskSuccessListener = TaskSuccessListener {
            identityApi.modify(modifyRequest)
            mServer.waitForVerify(
                Matcher(mServer.Endpoints().getModifyUrl(currentUser!!.id))
            ) { request ->
                val identityChanges = request.asIdentityRequest().body.identity_changes
                Assert.assertEquals(1, identityChanges.size.toLong())
                // make sure the customerId used the correct "old" value
                Assert.assertTrue("customerid" == identityChanges[0].optString("identity_type"))
                Assert.assertEquals("new customer ID", identityChanges[0].optString("new_value"))
                Assert.assertEquals("old customer ID", identityChanges[0].optString("old_value"))
                latch.countDown()
            }
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
        AccessUtils.setUserIdentity("customer Id", IdentityType.CustomerId, mStartingMpid)
        AccessUtils.setUserIdentity("facebook Id", IdentityType.Facebook, mStartingMpid)
        AccessUtils.setUserIdentity("other Id", IdentityType.Other2, mStartingMpid)
        val latch: CountDownLatch = MPLatch(1)
        val request = IdentityApiRequest.withUser(MParticle.getInstance()!!.Identity().currentUser)
            .customerId(null)
            .userIdentity(IdentityType.Facebook, null)
            .userIdentity(IdentityType.Other2, null)
            .build()
        MParticle.getInstance()!!.Identity().modify(request)
        mServer.waitForVerify(Matcher(mServer.Endpoints().getModifyUrl(mStartingMpid))) { request ->
            val identityChanges = request.asIdentityRequest().body.identity_changes
            Assert.assertEquals(3, identityChanges.size.toLong())
            identityChanges.sortWith(
                Comparator { jsonObject, t1 ->
                    jsonObject.optString("identity_type").compareTo(t1.optString("identity_type"))
                }
            )

            // make sure the existing values were set to null
            Assert.assertTrue("customerid" == identityChanges[0].optString("identity_type"))
            Assert.assertEquals("customer Id", identityChanges[0].optString("old_value"))
            Assert.assertEquals("null", identityChanges[0].optString("new_value"))
            Assert.assertTrue("facebook" == identityChanges[1].optString("identity_type"))
            Assert.assertEquals("facebook Id", identityChanges[1].optString("old_value"))
            Assert.assertEquals("null", identityChanges[1].optString("new_value"))
            Assert.assertTrue("other2" == identityChanges[2].optString("identity_type"))
            Assert.assertEquals("other Id", identityChanges[2].optString("old_value"))
            Assert.assertEquals("null", identityChanges[2].optString("new_value"))
            latch.countDown()
        }
        latch.await()
    }
}
