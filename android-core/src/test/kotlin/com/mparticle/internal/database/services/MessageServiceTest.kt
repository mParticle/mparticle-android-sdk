package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import org.mockito.AdditionalMatchers.aryEq
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

class MessageServiceTest {
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)
    private val message: BaseMPMessage = Mockito.mock(BaseMPMessage::class.java)
    private val limit = Constants.maxMessagePerBatch.toString()

    @Test
    fun testGetSessionIdsReadsEachDistinctSessionIdAndClosesTheCursor() {
        Mockito.`when`(database.rawQuery("SELECT DISTINCT session_id FROM messages")).thenReturn(cursor)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, true, false)
        Mockito.`when`(cursor.getString(0)).thenReturn("a", "b", "a")

        Assert.assertEquals(setOf("a", "b"), MessageService.getSessionIds(database))
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetSessionHistoryReadsTheUploadedMessagesOfOtherSessionsAndClosesTheCursor() {
        Mockito
            .`when`(database.query("messages", HISTORY_COLUMNS, "(upload_status = 3) and (session_id != ?) and (mp_id != ?)", arrayOf<String?>("current", "0"), null, null, "_id asc", limit))
            .thenReturn(cursor)
        stubMessageRows()

        val messages = MessageService.getSessionHistory(database, "current")

        Assert.assertEquals(listOf(7L, 8L), messages.map { it.mpid })
        Assert.assertEquals(listOf("s1", "s2"), messages.map { it.sessionId })
        Assert.assertEquals(listOf(11, 12), messages.map { it.messageId })
        Assert.assertEquals(listOf("m1", "m2"), messages.map { it.message })
        Assert.assertEquals(listOf("plan", null), messages.map { it.dataplanId })
        Assert.assertEquals(listOf(2, null), messages.map { it.dataplanVersion })
        Mockito.verify(cursor, Mockito.times(1)).getInt(5)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetSessionHistoryOfOneMpidSelectsThatMpid() {
        Mockito
            .`when`(database.query("messages", HISTORY_COLUMNS, "(upload_status = 3) and (session_id != ?) and (mp_id  = ?)", arrayOf<String?>("current", "7"), null, null, "_id asc", limit))
            .thenReturn(cursor)

        Assert.assertTrue(MessageService.getSessionHistory(database, "current", true, 7L).isEmpty())
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetSessionHistoryThrowsAMissingColumnAndClosesTheCursor() {
        Mockito
            .`when`(database.query("messages", HISTORY_COLUMNS, "(upload_status = 3) and (session_id != ?) and (mp_id != ?)", arrayOf<String?>("current", "0"), null, null, "_id asc", limit))
            .thenReturn(cursor)
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenThrow(IllegalArgumentException("no _id"))

        Assert.assertThrows(IllegalArgumentException::class.java) { MessageService.getSessionHistory(database, "current") }
        Mockito.verify(cursor).close()
    }

    @Test
    fun testDeleteOldMessagesDeletesTheUploadedMessagesOfOtherSessions() {
        Mockito.`when`(database.delete("messages", "(upload_status = 3) and (session_id != ?) and (mp_id != ?)", arrayOf<String?>("current", "0"))).thenReturn(5)

        Assert.assertEquals(5, MessageService.deleteOldMessages(database, "current"))
    }

    @Test
    fun testDeleteMessagesOlderThanDeletesByCreationTime() {
        Mockito.`when`(database.delete("messages", "message_time < ?", arrayOf<String?>("1000"))).thenReturn(4)

        Assert.assertEquals(4, MessageService.deleteMessagesOlderThan(database, 1000L))
    }

    @Test
    fun testHasMessagesForUploadCountsTheMessagesNotYetUploadedAndClosesTheCursor() {
        Mockito
            .`when`(database.query(Mockito.eq("messages"), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc")))
            .thenReturn(cursor)
        Mockito.`when`(cursor.count).thenReturn(1)
        val before = System.currentTimeMillis()

        Assert.assertTrue(MessageService.hasMessagesForUpload(database))

        val after = System.currentTimeMillis()
        val selection = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(database).query(Mockito.eq("messages"), aryEq(arrayOf<String?>("_id")), selection.capture(), aryEq(arrayOf<String?>("3", "0")), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc"))
        assertUploadSelection(selection.value, " != ?", before, after)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testHasMessagesForUploadWithNoRowsIsFalse() {
        Mockito
            .`when`(database.query(Mockito.eq("messages"), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc")))
            .thenReturn(cursor)

        Assert.assertFalse(MessageService.hasMessagesForUpload(database))
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetMessagesForUploadReadsTheMessagesNotYetUploadedOldestFirstAndClosesTheCursor() {
        Mockito
            .`when`(database.query(Mockito.eq("messages"), Mockito.isNull(), Mockito.anyString(), Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc"), Mockito.eq(limit)))
            .thenReturn(cursor)
        stubMessageRows()
        val before = System.currentTimeMillis()

        val messages = MessageService.getMessagesForUpload(database)

        val after = System.currentTimeMillis()
        val selection = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(database).query(Mockito.eq("messages"), Mockito.isNull(), selection.capture(), aryEq(arrayOf<String?>("3", "0")), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc"), Mockito.eq(limit))
        assertUploadSelection(selection.value, " != ?", before, after)
        Assert.assertEquals(listOf(11, 12), messages.map { it.messageId })
        Assert.assertEquals(listOf(2, null), messages.map { it.dataplanVersion })
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetMessagesForUploadOfOneMpidSelectsThatMpid() {
        Mockito
            .`when`(database.query(Mockito.eq("messages"), Mockito.isNull(), Mockito.anyString(), Mockito.any(), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc"), Mockito.eq(limit)))
            .thenReturn(cursor)

        Assert.assertTrue(MessageService.getMessagesForUpload(database, true, 7L).isEmpty())

        val selection = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(database).query(Mockito.eq("messages"), Mockito.isNull(), selection.capture(), aryEq(arrayOf<String?>("3", "7")), Mockito.isNull(), Mockito.isNull(), Mockito.eq("_id asc"), Mockito.eq(limit))
        assertUploadSelection(selection.value, " = ?", 0L, Long.MAX_VALUE)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testCleanupMessagesDeletesMessagesOverTheSizeLimit() {
        Mockito.`when`(database.delete("messages", "length(message) > 102400", null)).thenReturn(3)

        Assert.assertEquals(3, MessageService.cleanupMessages(database))
    }

    @Test
    fun testMarkMessagesAsUploadedUpdatesEveryMessageUpToTheId() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            Mockito.`when`(database.update(Mockito.eq("messages"), Mockito.any(), Mockito.eq("_id <= ? and mp_id != ? "), aryEq(arrayOf<String?>("5", "0")))).thenReturn(6)

            Assert.assertEquals(6, MessageService.markMessagesAsUploaded(database, 5))

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("upload_status", 3)
            Mockito.verify(database).update("messages", values, "_id <= ? and mp_id != ? ", arrayOf<String?>("5", "0"))
        }
    }

    @Test
    fun testDeleteMessagesDeletesEveryMessageUpToTheId() {
        Mockito.`when`(database.delete("messages", "_id <= ? and mp_id != ?", arrayOf<String?>("5", "0"))).thenReturn(6)

        Assert.assertEquals(6, MessageService.deleteMessages(database, 5))
    }

    @Test
    fun testInsertMessageStoresEveryColumnAsReady() {
        stubMessage("s1", "e")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            MessageService.insertMessage(database, "key", message, 7L, "plan", 2)

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("api_key", "key")
            Mockito.verify(values).put("message_time", 100L)
            Mockito.verify(values).put("session_id", "s1")
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("dataplan_id", "plan")
            Mockito.verify(values).put("dataplan_version", 2)
            Mockito.verify(values).put("message", MESSAGE_JSON)
            Mockito.verify(values).put("upload_status", 1)
            Mockito.verify(message, Mockito.never()).remove(Mockito.anyString())
            Mockito.verify(database).insert("messages", null, values)
        }
    }

    @Test
    fun testInsertMessageStoresAFirstRunMessageAsBatchReady() {
        stubMessage("s1", "fr")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            MessageService.insertMessage(database, "key", message, 7L, null, null)

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("dataplan_id", null as String?)
            Mockito.verify(values).put("dataplan_version", null as Int?)
            Mockito.verify(values).put("upload_status", 2)
            Mockito.verify(database).insert("messages", null, values)
        }
    }

    @Test
    fun testInsertMessageRemovesTheSessionIdOfAMessageWithNoSession() {
        stubMessage("NO-SESSION", "e")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            MessageService.insertMessage(database, "key", message, 7L, null, null)

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("session_id", "NO-SESSION")
            Mockito.verify(message).remove("sid")
            Mockito.verify(database).insert("messages", null, values)
        }
    }

    @Test
    fun testInsertMessageOverTheSizeLimitLogsAnErrorAndStoresNothing() {
        stubMessage("s1", "e")
        Mockito.`when`(message.toString()).thenReturn("x".repeat(102401))
        Mockito.mockStatic(Logger::class.java).use { logger ->
            Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
                MessageService.insertMessage(database, "key", message, 7L, null, null)

                Mockito.verify(contentValues.constructed()[0], Mockito.never()).put(Mockito.eq("message"), Mockito.anyString())
                Mockito.verifyNoInteractions(database)
                logger.verify { Logger.error("Message logged of size 102401 that exceeds maximum safe size of 102400 bytes.") }
            }
        }
    }

    @Test
    fun testInsertMessageWithNoTimestampThrowsAndStoresNothing() {
        Mockito.`when`(message.getLong("ct")).thenThrow(JSONException("no timestamp"))
        Mockito.mockConstruction(ContentValues::class.java).use {
            Assert.assertThrows(JSONException::class.java) { MessageService.insertMessage(database, "key", message, 7L, null, null) }

            Mockito.verifyNoInteractions(database)
        }
    }

    @Test
    fun testDeleteAllDeletesEveryRow() {
        MessageService.deleteAll(database)

        Mockito.verify(database).delete("messages", null, null)
    }

    @Test
    fun testANullDatabaseThrowsNullPointerException() {
        Assert.assertThrows(NullPointerException::class.java) { MessageService.cleanupMessages(null) }
    }

    @Test
    fun testTheConstructorIsPublic() {
        Assert.assertNotNull(MessageService())
    }

    private fun stubMessageRows() {
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("session_id")).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("mp_id")).thenReturn(3)
        Mockito.`when`(cursor.getColumnIndexOrThrow("dataplan_id")).thenReturn(4)
        Mockito.`when`(cursor.getColumnIndexOrThrow("dataplan_version")).thenReturn(5)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(cursor.getInt(0)).thenReturn(11, 12)
        Mockito.`when`(cursor.getString(1)).thenReturn("m1", "m2")
        Mockito.`when`(cursor.getString(2)).thenReturn("s1", "s2")
        Mockito.`when`(cursor.getLong(3)).thenReturn(7L, 8L)
        Mockito.`when`(cursor.getString(4)).thenReturn("plan", null)
        Mockito.`when`(cursor.isNull(5)).thenReturn(false, true)
        Mockito.`when`(cursor.getInt(5)).thenReturn(2)
    }

    private fun stubMessage(sessionId: String, type: String) {
        Mockito.`when`(message.getLong("ct")).thenReturn(100L)
        Mockito.`when`(message.sessionId).thenReturn(sessionId)
        Mockito.`when`(message.getString("dt")).thenReturn(type)
        Mockito.`when`(message.toString()).thenReturn(MESSAGE_JSON)
    }

    private fun assertUploadSelection(selection: String, mpidClause: String, before: Long, after: Long) {
        val match = Regex("upload_status != \\? and message_time < (\\d+) and mp_id(.*)").matchEntire(selection)
        Assert.assertNotNull(selection, match)
        Assert.assertTrue(selection, match!!.groupValues[1].toLong() in before..after)
        Assert.assertEquals(mpidClause, match.groupValues[2])
    }

    private companion object {
        const val MESSAGE_JSON = "{\"dt\":\"e\"}"
        val HISTORY_COLUMNS = arrayOf<String?>("_id", "message", "message_time", "upload_status", "session_id", "mp_id", "dataplan_id", "dataplan_version")
    }
}
