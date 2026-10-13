package com.mparticle.internal.database.services

import android.os.Handler
import android.os.Looper
import com.mparticle.MParticle
import com.mparticle.TypedUserAttributeListener
import com.mparticle.identity.UserAttributeListenerWrapper
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeRemoval
import com.mparticle.internal.database.services.MParticleDBManager.UserAttributeResponse
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.networking.NetworkOptions
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanInstallEachTest
import com.mparticle.testutils.MPLatch
import org.junit.Assert
import org.junit.Test
import java.util.TreeMap
import java.util.UUID

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
        val manager: MParticleDBManager =
            object : MParticleDBManager() {
                override fun getUserAttributeSingles(mpId: Long): Map<String, Any>? {
                    dbAccessThread.value = Thread.currentThread()
                    return null
                }

                override fun getUserAttributeLists(mpId: Long): TreeMap<String, List<String>>? = null
            }
        val latch = AndroidUtils.Mutable(MPLatch(1))
        val callbackThread = AndroidUtils.Mutable<Thread?>(null)

        // when not on the main thread, it should callback on the current thread, and access the DB on the same thread
        Assert.assertNotEquals("main", Thread.currentThread().name)

        val listener: TypedUserAttributeListener =
            object : TypedUserAttributeListener {
                override fun onUserAttributesReceived(
                    userAttributes: Map<String, Any?>,
                    userAttributeLists: Map<String, List<String?>?>,
                    mpid: Long,
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
        val listener1: TypedUserAttributeListener =
            object : TypedUserAttributeListener {
                override fun onUserAttributesReceived(
                    userAttributes: Map<String, Any?>,
                    userAttributeLists: Map<String, List<String?>?>,
                    mpid: Long,
                ) {
                    callbackThread.value = Thread.currentThread()
                    latch.value.countDown()
                }
            }
        Handler(Looper.getMainLooper()).post {
            manager.getUserAttributes(
                UserAttributeListenerWrapper(listener),
                1,
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

    @Test
    @Throws(Exception::class)
    fun testUploadBatchSizeLimitCountsTheBatchHeader() {
        startMParticle()
        val instance = MParticle.getInstance()!!
        val manager = MParticleDBManager(mContext)

        // User attributes large enough that the batch header alone takes most of the upload size limit.
        val attributes = UserAttributeResponse()
        attributes.mpId = mStartingMpid
        attributes.attributeSingles = HashMap()
        for (i in 0 until 40) {
            attributes.attributeSingles["attribute-$i"] = "a".repeat(4000)
        }
        manager.setUserAttribute(attributes)

        // Three messages whose bodies alone fit in one batch, but not beside that header.
        val sessionId = UUID.randomUUID().toString()
        val session = InternalSession()
        session.mSessionID = sessionId
        for (i in 0 until 3) {
            val message =
                BaseMPMessage
                    .Builder("event-$i-" + "b".repeat(30000))
                    .timestamp(System.currentTimeMillis() - 1000L)
                    .build(session, null, mStartingMpid)
            manager.insertMessage("apiKey", message, null, null)
        }

        manager.createMessagesForUploadMessage(
            instance.Internal().configManager,
            instance.Internal().messageManager.deviceAttributes,
            UUID.randomUUID().toString(),
            UploadSettings("apiKey", "secret", NetworkOptions.builder().build(), "", ""),
        )

        // Every stored upload fits the limit, so the cleanup that runs before an upload removes none of them.
        val uploads = manager.readyUploads
        Assert.assertTrue(uploads.isNotEmpty())
        for (upload in uploads) {
            Assert.assertTrue(upload.message.length <= Constants.LIMIT_MAX_UPLOAD_SIZE)
        }
        manager.cleanupUploadMessages()
        Assert.assertEquals(uploads.size, manager.readyUploads.size)

        // The messages that did not fit beside the header stay queued for the next round instead of being lost.
        val remaining = MessageService.getMessagesForUpload(manager.database).filter { it.sessionId == sessionId }
        Assert.assertEquals(2, remaining.size)
    }
}
