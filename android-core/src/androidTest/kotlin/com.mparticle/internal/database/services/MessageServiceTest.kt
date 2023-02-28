package com.mparticle.internal.database.services

import android.location.Location
import com.mparticle.internal.Constants
import com.mparticle.internal.InternalSession
import com.mparticle.internal.database.services.MessageService.ReadyMessage
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.properties.Delegates

class MessageServiceTest : BaseMPServiceTest() {
    var mpid1 by Delegates.notNull<Long>()
    var mpid2 by Delegates.notNull<Long>()
    var mpid3 by Delegates.notNull<Long>()

    @Before
    @Throws(Exception::class)
    fun before() {
        mpid1 = ran.nextLong()
        mpid2 = ran.nextLong()
        mpid3 = ran.nextLong()
    }

    @Test
    @Throws(Exception::class)
    fun testMessagesForUploadByMpid() {
        for (i in 0..19) {
            MessageService.insertMessage(
                database,
                "apiKey",
                mpMessage,
                mpid1,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid1
            ).size.toLong(),
            20
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(),
            0
        )
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 20)
        for (i in 0..29) {
            MessageService.insertMessage(
                database,
                "apiKey",
                mpMessage,
                Constants.TEMPORARY_MPID,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid1
            ).size.toLong(),
            20
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid2
            ).size.toLong(),
            0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(),
            30
        )
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 20)
        for (i in 0..34) {
            MessageService.insertMessage(
                database,
                "apiKey",
                mpMessage,
                mpid2,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid1
            ).size.toLong(),
            20
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid2
            ).size.toLong(),
            35
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(),
            30
        )
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 55)
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(database, Int.MAX_VALUE).toLong(),
            55
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid1
            ).size.toLong(),
            0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                mpid2
            ).size.toLong(),
            0
        )
        Assert.assertEquals(
            MessageService.getMessagesForUpload(
                database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(),
            30
        )
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 0)
    }

    @Test
    @Throws(Exception::class)
    fun testSessionHistoryByMpid() {
        val currentSession = UUID.randomUUID().toString()
        val previousSession = UUID.randomUUID().toString()
        for (i in 0..19) {
            MessageService.insertMessage(
                database,
                "apiKey",
                getMpMessage(currentSession),
                mpid1,
                null,
                null
            )
        }
        for (i in 0..29) {
            MessageService.insertMessage(
                database,
                "apiKey",
                getMpMessage(currentSession),
                Constants.TEMPORARY_MPID,
                null,
                null
            )
        }
        for (i in 0..34) {
            MessageService.insertMessage(
                database,
                "apiKey",
                getMpMessage(currentSession),
                mpid2,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(database, Int.MAX_VALUE).toLong(),
            55
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                database,
                previousSession
            ).size.toLong(),
            55
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                database,
                previousSession,
                true,
                mpid1
            ).size.toLong(),
            20
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                database,
                previousSession,
                true,
                mpid2
            ).size.toLong(),
            35
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                database,
                previousSession,
                false,
                mpid1
            ).size.toLong(),
            35
        )
        Assert.assertEquals(
            MessageService.getSessionHistory(
                database,
                previousSession,
                false,
                Constants.TEMPORARY_MPID
            ).size.toLong(),
            55
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSessionHistoryAccuracy() {
        val currentSession = UUID.randomUUID().toString()
        val previousSession = UUID.randomUUID().toString()
        var testMessage: BaseMPMessage
        val mpids = arrayOf(mpid1, mpid2, mpid3)
        var testMpid: Long?
        val testMessages: MutableMap<String, BaseMPMessage> = HashMap()
        for (i in 0..99) {
            testMpid = mpids[mRandomUtils.randomInt(0, 3)]
            testMessage = getMpMessage(currentSession, testMpid)
            testMessages[testMessage.toString()] = testMessage
            MessageService.insertMessage(
                database,
                "apiKey",
                testMessage,
                testMpid,
                null,
                null
            )
        }
        Assert.assertEquals(
            MessageService.markMessagesAsUploaded(database, Int.MAX_VALUE).toLong(),
            100
        )
        val readyMessages = MessageService.getSessionHistory(
            database,
            previousSession,
            false,
            Constants.TEMPORARY_MPID
        )
        Assert.assertEquals(readyMessages.size.toLong(), testMessages.size.toLong())
        for (readyMessage in readyMessages) {
            val message = testMessages[readyMessage.message]
            Assert.assertNotNull(message)
            Assert.assertEquals(readyMessage.mpid, message?.mpId)
            Assert.assertEquals(readyMessage.message, message.toString())
            Assert.assertEquals(readyMessage.sessionId, currentSession)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun testMessageFlow() {
        for (i in 0..9) {
            MessageService.insertMessage(database, "apiKey", mpMessage, 1, "dataplan1", 1)
        }
        val messageList = MessageService.getMessagesForUpload(database)
        Assert.assertEquals(messageList.size.toLong(), 10)
        Assert.assertEquals(MessageService.getSessionHistory(database, "123").size.toLong(), 0)
        val max = getMaxId(messageList)
        val numUpldated = MessageService.markMessagesAsUploaded(database, max)
        Assert.assertEquals(numUpldated.toLong(), 10)
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 0)
        Assert.assertEquals(MessageService.getSessionHistory(database, "").size.toLong(), 10)
    }

    @Test
    @Throws(JSONException::class)
    fun testMessageFlowMax() {
        for (i in 0..109) {
            MessageService.insertMessage(database, "apiKey", mpMessage, 1, null, null)
        }
        var messages = MessageService.getMessagesForUpload(database)
        Assert.assertEquals(messages.size.toLong(), 100)
        Assert.assertEquals(MessageService.getSessionHistory(database, "").size.toLong(), 0)
        var max = getMaxId(messages)
        var numUpdated = MessageService.markMessagesAsUploaded(database, max)
        Assert.assertEquals(numUpdated.toLong(), 100)
        Assert.assertEquals(MessageService.getSessionHistory(database, "").size.toLong(), 100)
        messages = MessageService.getMessagesForUpload(database)
        max = getMaxId(messages)
        numUpdated = MessageService.markMessagesAsUploaded(database, max)
        Assert.assertEquals(numUpdated.toLong(), 110)
        Assert.assertEquals(MessageService.getSessionHistory(database, "").size.toLong(), 100)
    }

    @Test
    @Throws(JSONException::class)
    fun testDeleteOldMessages() {
        val currentSession = UUID.randomUUID().toString()
        val newSession = UUID.randomUUID().toString()
        for (i in 0..9) {
            MessageService.insertMessage(
                database,
                "apiKey",
                getMpMessage(currentSession),
                1,
                null,
                null
            )
        }
        Assert.assertEquals(MessageService.markMessagesAsUploaded(database, 10).toLong(), 10)
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 0)
        MessageService.deleteOldMessages(database, currentSession)
        Assert.assertEquals(
            MessageService.getSessionHistory(database, newSession).size.toLong(),
            10
        )
        MessageService.deleteOldMessages(database, newSession)
        Assert.assertEquals(MessageService.getSessionHistory(database, newSession).size.toLong(), 0)
    }

    @Test
    @Throws(JSONException::class)
    fun testMessagesMaxSize() {
        for (i in 0..9) {
            MessageService.insertMessage(database, "apiKey", mpMessage, 1, "a", 1)
        }
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 10)
        val builder = StringBuilder()
        for (i in 0 until Constants.LIMIT_MAX_MESSAGE_SIZE) {
            builder.append("ab")
        }
        val message = BaseMPMessage.Builder(builder.toString())
            .build(InternalSession(), Location("New York City"), 1)
        MessageService.insertMessage(database, "apiKey", message, 1, "b", 2)
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 10)
        for (i in 0..9) {
            MessageService.insertMessage(database, "apiKey", mpMessage, 1, "c", 3)
        }
        Assert.assertEquals(MessageService.getMessagesForUpload(database).size.toLong(), 20)
    }

    private fun getMaxId(messages: List<ReadyMessage>): Int {
        var max = 0
        for (message in messages) {
            if (message.messageId > max) {
                max = message.messageId
            }
        }
        return max
    }
}
