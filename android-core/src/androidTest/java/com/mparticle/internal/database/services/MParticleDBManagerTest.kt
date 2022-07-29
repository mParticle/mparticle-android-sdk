package com.mparticle.internal.database.services

import android.os.Looper
import com.mparticle.TypedUserAttributeListener
import com.mparticle.identity.UserAttributeListenerWrapper
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeRemoval
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeResponse
import com.mparticle.testing.BaseTest
import com.mparticle.testing.FailureLatch
import com.mparticle.testing.Mutable
import com.mparticle.testing.context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull

class MParticleDBManagerTest : BaseTest() {
    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testRemoveUserAttributes() {
        val manager = MParticleDBManager(context)
        val removal = UserAttributeRemoval()
        removal.key = "foo"
        removal.mpId = 10L
        manager.removeUserAttribute(removal, null)
        var attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNull(attributes["foo"])
        val newAttributes = UserAttributeResponse()
        newAttributes.mpId = 10L
        newAttributes.attributeLists = java.util.HashMap<String, List<String>>()
        val attributeList = mutableListOf<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        manager.removeUserAttribute(removal, null)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNull(attributes["foo"])
    }

    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testUserUserAttributeLists() {
        val manager = MParticleDBManager(context)
        val removal = UserAttributeRemoval()
        removal.key = "foo"
        removal.mpId = 10L
        manager.removeUserAttribute(removal, null)
        var attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNull(attributes["foo"])
        val newAttributes = UserAttributeResponse()
        newAttributes.mpId = 10L
        newAttributes.attributeLists = mutableMapOf<String, List<String>>()
        var attributeList = mutableListOf<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = java.util.ArrayList<String>()
        attributeList.add("bar")
        attributeList.add("baz")
        attributeList.add("bar-2")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = java.util.ArrayList<String>()
        attributeList.add("bar-2")
        attributeList.add("bar")
        attributeList.add("baz")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
        attributeList = java.util.ArrayList<String>()
        attributeList.add("bar")
        newAttributes.attributeLists.clear()
        newAttributes.attributeLists.put("foo", attributeList)
        manager.setUserAttribute(newAttributes)
        attributes = manager.getUserAttributes(10)
        junit.framework.Assert.assertNotNull(attributes["foo"])
        junit.framework.Assert.assertEquals(attributeList, attributes["foo"])
    }

    @org.junit.Test
    @Throws(InterruptedException::class)
    fun testGetUserAttributesAsync() {
        startMParticle()
        val dbAccessThread = Mutable<Thread?>(null)
        val manager: MParticleDBManager = object : MParticleDBManager() {
            override fun getUserAttributeSingles(mpId: Long): java.util.TreeMap<String, String>? {
                dbAccessThread.value = Thread.currentThread()
                return null
            }

            override fun getUserAttributeLists(mpId: Long): java.util.TreeMap<String, List<String>>? {
                return null
            }
        }
        val latch = Mutable(FailureLatch())
        val callbackThread = Mutable<Thread?>(null)

        // when not on the main thread, it should callback on the current thread, and access the DB on the same thread
        assertNotEquals("main", Thread.currentThread().name)
        var listener: TypedUserAttributeListener = object : TypedUserAttributeListener {
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
        assertNotNull(callbackThread.value)
        assertEquals(Thread.currentThread().name, callbackThread.value?.name)
        assertEquals(Thread.currentThread().name, dbAccessThread.value?.name)
        callbackThread.value = null
        dbAccessThread.value = null
        latch.value = FailureLatch()

        listener = object : TypedUserAttributeListener {
            override fun onUserAttributesReceived(
                userAttributes: Map<String, Any?>,
                userAttributeLists: Map<String, List<String?>?>,
                mpid: Long
            ) {
                callbackThread.value = Thread.currentThread()
                latch.value.countDown()
            }
        }
        // when run from the main thread, it should be called back on the main thread, but NOT access the DB on the same thread
        android.os.Handler(Looper.getMainLooper()).post(
            Runnable {
                manager.getUserAttributes(UserAttributeListenerWrapper(listener), 1)
            }
        )
        latch.value.await()
        assertNotNull(callbackThread.value)
        assertEquals("main", callbackThread.value?.name)
        assertNotEquals("main", dbAccessThread.value?.name)
        // it's ok if this value changes in the future, if you know what you're doing. previously
        // this was being run on an AsyncTask, but it may have been leading to db locks, "messages"
        // thread is know to not be an issue w/db access
        assertEquals("mParticleMessageHandler", dbAccessThread.value?.name)
    }
}
