package com.mparticle.internal.database.services

import com.mparticle.internal.Constants
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.testutils.TestingUtils
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import java.util.Collections

class ReportingServiceTest : BaseMPServiceTest() {
    @Test
    @Throws(JSONException::class)
    fun testInsertReportingMessage() {
        val reportingMessages = getNReportingMessages(20, "123")
        for (reportingMessage in reportingMessages) {
            ReportingService.insertReportingMessage(database, reportingMessage, 2L)
        }
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                2L
            ).size.toLong(),
            20
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(database).size.toLong(),
            20
        )
    }

    @Test
    @Throws(JSONException::class)
    fun testInsertReportingMessageMpIdSpecific() {
        for (reportingMessage in getNReportingMessages(2, "123")) {
            ReportingService.insertReportingMessage(database, reportingMessage, 2L)
        }
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                2L
            ).size.toLong(),
            2
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                3L
            ).size.toLong(),
            0
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                4L
            ).size.toLong(),
            0
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(database).size.toLong(),
            2
        )
        for (reportingMessage in getNReportingMessages(3, "123")) {
            ReportingService.insertReportingMessage(database, reportingMessage, 3L)
        }
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                2L
            ).size.toLong(),
            2
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                3L
            ).size.toLong(),
            3
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                false,
                4L
            ).size.toLong(),
            5
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                4L
            ).size.toLong(),
            0
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(database).size.toLong(),
            5
        )
        for (reportingMessage in getNReportingMessages(3, "123")) {
            ReportingService.insertReportingMessage(
                database,
                reportingMessage,
                Constants.TEMPORARY_MPID
            )
        }
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                2L
            ).size.toLong(),
            2
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                3L
            ).size.toLong(),
            3
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                true,
                Constants.TEMPORARY_MPID
            ).size.toLong(),
            3
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(
                database,
                false,
                4L
            ).size.toLong(),
            8
        )
        Assert.assertEquals(
            ReportingService.getReportingMessagesForUpload(database).size.toLong(),
            5
        )
    }

    @Test
    @Throws(JSONException::class)
    fun testDeleteReportingMessages() {
        for (reportingMessage in getNReportingMessages(2)) {
            ReportingService.insertReportingMessage(database, reportingMessage, 2L)
        }
        for (reportingMessage in getNReportingMessages(1)) {
            ReportingService.insertReportingMessage(database, reportingMessage, 3L)
        }
        var messagesFor2 = ReportingService.getReportingMessagesForUpload(database, true, 2L)
        var messagesFor3 = ReportingService.getReportingMessagesForUpload(database, true, 3L)
        Assert.assertEquals(messagesFor2.size.toLong(), 2)
        Assert.assertEquals(messagesFor3.size.toLong(), 1)
        ReportingService.deleteReportingMessage(database, messagesFor2[0].reportingMessageId)
        messagesFor2 = ReportingService.getReportingMessagesForUpload(database, true, 2L)
        messagesFor3 = ReportingService.getReportingMessagesForUpload(database, true, 3L)
        Assert.assertEquals(messagesFor2.size.toLong(), 1)
        Assert.assertEquals(messagesFor3.size.toLong(), 1)
        ReportingService.deleteReportingMessage(database, messagesFor3[0].reportingMessageId)
        messagesFor2 = ReportingService.getReportingMessagesForUpload(database, true, 2L)
        messagesFor3 = ReportingService.getReportingMessagesForUpload(database, true, 3L)
        Assert.assertEquals(messagesFor2.size.toLong(), 1)
        Assert.assertEquals(messagesFor3.size.toLong(), 0)
    }

    @Test
    @Throws(JSONException::class)
    fun testEntryIntegrity() {
        val jsonReportingMessages = getNReportingMessages(2)
        for (reportingMessage in jsonReportingMessages) {
            ReportingService.insertReportingMessage(database, reportingMessage, 1L)
        }
        val reportingMessages = ReportingService.getReportingMessagesForUpload(database, true, 1L)
        Collections.sort(
            reportingMessages,
            Comparator { o1, o2 ->
                try {
                    return@Comparator o1.msgObject.getInt("a random Number")
                        .compareTo(o2.msgObject.getInt("a random Number"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                -1
            }
        )
        Collections.sort(
            jsonReportingMessages,
            Comparator { o1, o2 ->
                try {
                    return@Comparator o1.toJson().getInt("a random Number")
                        .compareTo(o2.toJson().getInt("a random Number"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                -1
            }
        )
        Assert.assertEquals(jsonReportingMessages.size.toLong(), reportingMessages.size.toLong())
        var i = 0
        while (i < jsonReportingMessages.size && i < reportingMessages.size) {
            Assert.assertTrue(equals(jsonReportingMessages[i], reportingMessages[i]))
            i++
        }
    }

    private fun getNReportingMessages(n: Int): List<JsonReportingMessage> {
        return getNReportingMessages(n, null)
    }

    private fun getNReportingMessages(n: Int, sessionId: String?): List<JsonReportingMessage> {
        val reportingMessages: MutableList<JsonReportingMessage> = ArrayList()
        for (i in 0 until n) {
            reportingMessages.add(
                TestingUtils.getInstance()
                    .getRandomReportingMessage(sessionId ?: ran.nextInt().toString())
            )
        }
        return reportingMessages
    }

    private fun equals(
        jsonReportingMessage: JsonReportingMessage,
        reportingMessage: ReportingService.ReportingMessage
    ): Boolean {
        val reportingString = reportingMessage.msgObject.toString()
        val origString = jsonReportingMessage.toJson().toString()
        return reportingMessage.sessionId == jsonReportingMessage.sessionId && reportingString == origString
    }
}
