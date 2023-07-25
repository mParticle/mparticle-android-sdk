package com.mparticle.internal.database.services

import android.os.Handler
import android.os.Looper
import com.mparticle.TypedUserAttributeListener
import com.mparticle.identity.UserAttributeListenerWrapper
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeRemoval
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeResponse
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert
import org.junit.Test
import java.util.TreeMap

class MParticleDBManagerTest : BaseCleanInstallEachTest() {
    @Test
    @Throws(Exception::class)
    fun testRemoveUserAttributes() {
        val manager = MParticleDBManager(mContext)
        val removal = UserAttributeRemoval()
        removal.key = "foo"
        removal.mpId = 10L
        manager.removeUserAttribute(removal, null)
        var attributes = manager.getUserAttributes(10)
        Assert.assertNull(attributes["foo"])
        val newAttributes = UserAttributeResponse()
        newAttributes.mpId = 10L
        newAttributes.attributeLists = HashMap()
        val attributeList = ArrayList<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists["foo"] = attributeList
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        Assert.assertNotNull(attributes["foo"])
        manager.removeUserAttribute(removal, null)
        attributes = manager.getUserAttributes(10)
        Assert.assertNull(attributes["foo"])
    }

    @Test
    @Throws(Exception::class)
    fun testUserUserAttributeLists() {
        val manager = MParticleDBManager(mContext)
        val removal = UserAttributeRemoval()
        removal.key = "foo"
        removal.mpId = 10L
        manager.removeUserAttribute(removal, null)
        var attributes = manager.getUserAttributes(10)
        Assert.assertNull(attributes["foo"])
        val newAttributes = UserAttributeResponse()
        newAttributes.mpId = 10L
        newAttributes.attributeLists = HashMap()
        var attributeList = ArrayList<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists["foo"] = attributeList
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        Assert.assertNotNull(attributes["foo"])
        Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = ArrayList()
        attributeList.add("bar")
        attributeList.add("baz")
        attributeList.add("bar-2")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists["foo"] = attributeList
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        Assert.assertNotNull(attributes["foo"])
        Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = ArrayList()
        attributeList.add("bar-2")
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists["foo"] = attributeList
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        Assert.assertNotNull(attributes["foo"])
        Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = ArrayList()
        attributeList.add("bar")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists["foo"] = attributeList
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        Assert.assertNotNull(attributes["foo"])
        Assert.assertEquals(attributeList, attributes["foo"])
    }

    @Test
    @Throws(InterruptedException::class)
    fun testGetUserAttributesAsync() {
        startMParticle()
        val dbAccessThread = AndroidUtils.Mutable<Thread?>(null)
        val manager: MParticleDBManager = object : MParticleDBManager() {
            override fun getUserAttributeSingles(mpId: Long): Map<String, Any>? {
                dbAccessThread.value = Thread.currentThread()
                return null
            }

            override fun getUserAttributeLists(mpId: Long): TreeMap<String, List<String>>? {
                return null
            }
        }
        val latch = AndroidUtils.Mutable(MPLatch(1))
        val callbackThread = AndroidUtils.Mutable<Thread?>(null)

        // when not on the main thread, it should callback on the current thread, and access the DB on the same thread
        Assert.assertNotEquals("main", Thread.currentThread().name)

        val listener: TypedUserAttributeListener = object : TypedUserAttributeListener {

            override fun onUserAttributesReceived(
                userAttributes: Map<String, Any?>,
                userAttributeLists: Map<String, List<String?>?>,
                mpid: Long
            ) {
                callbackThread.value = Thread.currentThread()
                latch.value.countDown()
            }
        }
        manager.getUserAttributes(UserAttributeListenerWrapper(listener), 1)
        Assert.assertNotNull(callbackThread.value)
        Assert.assertEquals(Thread.currentThread().name, callbackThread.value?.name)
        Assert.assertEquals(Thread.currentThread().name, dbAccessThread.value?.name)
        callbackThread.value = null
        dbAccessThread.value = null
        latch.value = MPLatch(1)

        // when run from the main thread, it should be called back on the main thread, but NOT access the DB on the same thread
        val listener1: TypedUserAttributeListener = object : TypedUserAttributeListener {
            override fun onUserAttributesReceived(
                userAttributes: Map<String, Any?>,
                userAttributeLists: Map<String, List<String?>?>,
                mpid: Long
            ) {
                callbackThread.value = Thread.currentThread()
                latch.value.countDown()
            }
        }
        Handler(Looper.getMainLooper()).post {
            manager.getUserAttributes(
                UserAttributeListenerWrapper(listener),
                1
            )
        }
        latch.value.await()
        Assert.assertNotNull(callbackThread.value)
        Assert.assertEquals("main", callbackThread.value?.name)
        Assert.assertNotEquals("main", dbAccessThread.value?.name)
        // it's ok if this value changes in the future, if you know what you're doing. previously
        // this was being run on an AsyncTask, but it may have been leading to db locks, "messages"
        // thread is know to not be an issue w/db access
        Assert.assertEquals("mParticleMessageHandler", dbAccessThread.value?.name)
    }
}
