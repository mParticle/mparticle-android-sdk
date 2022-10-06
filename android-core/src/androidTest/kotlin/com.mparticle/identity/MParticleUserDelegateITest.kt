package com.mparticle.identity

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.UserAttributeListener
import com.mparticle.consent.CCPAConsent
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.internal.AccessUtils
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanStartedEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.CountDownLatch

class MParticleUserDelegateITest : BaseCleanStartedEachTest() {
    private lateinit var mUserDelegate: MParticleUserDelegate
    @Before
    @Throws(Exception::class)
    fun before() {
        mUserDelegate = MParticle.getInstance()?.Identity()?.mUserDelegate!!
    }

    @Test
    @Throws(Exception::class)
    fun testSetGetUserIdentities() {
        val attributes = HashMap<Long, Map<IdentityType, String>>()
        for (i in 0..4) {
            val mpid = ran.nextLong()
            val pairs= HashMap<IdentityType, String> ()
            attributes[mpid] = pairs
            for (j in 0..2) {
                val identityType =
                    IdentityType.parseInt(mRandomUtils.randomInt(0, IdentityType.values().size))
                val value = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 25))
                Assert.assertTrue(mUserDelegate.setUserIdentity(value, identityType, mpid))
                pairs[identityType] = value
            }
        }
        AccessUtils.awaitMessageHandler()
        val storedUsersTemp: MutableMap<Long, Map<IdentityType, String>> = HashMap()
        for ((key, value) in attributes) {
            val storedUserAttributes = mUserDelegate.getUserIdentities(key)
            storedUsersTemp[key] = storedUserAttributes
            for ((key1, value1) in value) {
                val currentAttribute: Any? = storedUserAttributes[key1]
                if (currentAttribute == null) {
                    Log.e("Stuff", "more stuff")
                }
                Assert.assertEquals(storedUserAttributes[key1], value1)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testInsertRetrieveDeleteUserAttributes() {
        // create and store
        val attributes: MutableMap<Long, Map<String, String>> = HashMap()
        for (i in 0..4) {
            val mpid = ran.nextLong()
            val pairs: MutableMap<String, String> = HashMap()
            attributes[mpid] = pairs
            for (j in 0..2) {
                val key =
                    mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55)).uppercase(
                        Locale.getDefault()
                    )
                val value = mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55))
                Assert.assertTrue(mUserDelegate.setUserAttribute(key, value, mpid, false))
                pairs[key] = value
            }
        }
        AccessUtils.awaitMessageHandler()

        // retrieve and compare
        for ((key, value) in attributes) {
            val storedUserAttributes = mUserDelegate.getUserAttributes(key)
            for ((key1, value1) in value) {
                if (storedUserAttributes[key1] == null) {
                    Assert.assertNull(value1)
                } else {
                    Assert.assertEquals(storedUserAttributes[key1].toString(), value1)
                }
            }
        }

        // delete
        for ((key, value) in attributes) {
            for ((key1) in value) {
                Assert.assertTrue(mUserDelegate.removeUserAttribute(key1, key))
            }
        }
        AccessUtils.awaitMessageHandler()
        for ((key, value) in attributes) {
            val storedUserAttributes = mUserDelegate.getUserAttributes(key)
            for ((key1) in value) {
                Assert.assertNull(storedUserAttributes[key1])
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSetConsentState() {
        val mpid = ran.nextLong()
        val mpid2 = ran.nextLong()
        val state = mUserDelegate.getConsentState(mpid)
        Assert.assertNotNull(state)
        Assert.assertNotNull(state.gdprConsentState)
        Assert.assertEquals(0, state.gdprConsentState.size.toLong())
        val builder = ConsentState.builder()
        builder.addGDPRConsentState("foo", GDPRConsent.builder(true).build())
        mUserDelegate.setConsentState(builder.build(), mpid)
        builder.addGDPRConsentState("foo2", GDPRConsent.builder(true).build())
        mUserDelegate.setConsentState(builder.build(), mpid2)
        builder.setCCPAConsentState(CCPAConsent.builder(false).build())
        mUserDelegate.setConsentState(builder.build(), mpid2)
        Assert.assertEquals(1, mUserDelegate.getConsentState(mpid).gdprConsentState.size.toLong())
        Assert.assertTrue(mUserDelegate.getConsentState(mpid).gdprConsentState.containsKey("foo"))
        Assert.assertNull(mUserDelegate.getConsentState(mpid).ccpaConsentState)
        Assert.assertEquals(
            2,
            mUserDelegate.getConsentState(mpid2).gdprConsentState.size.toLong()
        )
        Assert.assertTrue(mUserDelegate.getConsentState(mpid2).gdprConsentState.containsKey("foo"))
        Assert.assertTrue(mUserDelegate.getConsentState(mpid2).gdprConsentState.containsKey("foo2"))
        Assert.assertNotNull(mUserDelegate.getConsentState(mpid2).ccpaConsentState)
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveConsentState() {
        val mpid = ran.nextLong()
        val state = mUserDelegate.getConsentState(mpid)
        Assert.assertNotNull(state)
        Assert.assertNotNull(state.gdprConsentState)
        Assert.assertEquals(0, state.gdprConsentState.size.toLong())
        val builder = ConsentState.builder()
        builder.addGDPRConsentState("foo", GDPRConsent.builder(true).build())
        builder.setCCPAConsentState(CCPAConsent.builder(true).build())
        mUserDelegate.setConsentState(builder.build(), mpid)
        Assert.assertEquals(1, mUserDelegate.getConsentState(mpid).gdprConsentState.size.toLong())
        Assert.assertNotNull(mUserDelegate.getConsentState(mpid).ccpaConsentState)
        Assert.assertTrue(mUserDelegate.getConsentState(mpid).gdprConsentState.containsKey("foo"))
        mUserDelegate.setConsentState(null, mpid)
        Assert.assertEquals(0, mUserDelegate.getConsentState(mpid).gdprConsentState.size.toLong())
        Assert.assertNull(mUserDelegate.getConsentState(mpid).ccpaConsentState)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testGetUserAttributesListener() {
        val attributeSingles = mRandomUtils.getRandomAttributes(5, false)
        val attributeLists: MutableMap<String, List<String>> = HashMap()
        for ((key, value) in attributeSingles) {
            attributeLists[key + value] = listOf(value + key)
        }
        for ((key, value) in attributeLists) {
            mUserDelegate.setUserAttributeList(key, value, mStartingMpid)
        }
        for ((key, value) in attributeSingles) {
            mUserDelegate.setUserAttribute(key, value, mStartingMpid)
        }
        AccessUtils.awaitMessageHandler()
        val userAttributesResults = AndroidUtils.Mutable<Map<String, String>?>(null)
        val userAttributeListResults = AndroidUtils.Mutable<Map<String, List<String>>?>(null)

        //fetch on the current (non-main) thread
        mUserDelegate.getUserAttributes(UserAttributeListener { userAttributes, userAttributeLists, mpid ->
            userAttributesResults.value = userAttributes
            userAttributeListResults.value = userAttributeLists
        }, mStartingMpid)
        assertMapEquals(attributeSingles, userAttributesResults.value)
        assertMapEquals(attributeLists, userAttributeListResults.value)
        userAttributesResults.value = null
        userAttributeListResults.value = null

        //fetch on the main thread (seperate code path)
        val latch: CountDownLatch = MPLatch(1)
        Handler(Looper.getMainLooper()).post {
            mUserDelegate.getUserAttributes(UserAttributeListener { userAttributes, userAttributeLists, mpid ->
                userAttributesResults.value = userAttributes
                userAttributeListResults.value = userAttributeLists
                latch.countDown()
            }, mStartingMpid)
        }
        latch.await()
        assertMapEquals(attributeSingles, userAttributesResults.value)
        assertMapEquals(attributeLists, userAttributeListResults.value)
    }

    private fun assertMapEquals(map1: Map<*, *>, map2: Map<*, *>?) {
        Assert.assertEquals(
            """
    $map1
    
    vs${map2.toString()}
    """.trimIndent(), map1.size.toLong(), map2?.size?.toLong()
        )
        for (obj in map1.entries) {
            val (key, value) = obj
            Assert.assertEquals(value, map2?.get(key))
        }
    }
}