package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.database.MPDatabase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class ReportingServiceTest {
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)

    @Test
    fun testInsertReportingMessageStoresEveryColumn() {
        val message = Mockito.mock(JsonReportingMessage::class.java)
        Mockito.`when`(message.timestamp).thenReturn(100L)
        Mockito.`when`(message.moduleId).thenReturn(64)
        Mockito.`when`(message.toJson()).thenReturn(JSONObject().put("a", 1))
        Mockito.`when`(message.sessionId).thenReturn("session")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            ReportingService.insertReportingMessage(database, message, 7L)

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("report_time", 100L)
            Mockito.verify(values).put("module_id", 64)
            Mockito.verify(values).put("message", "{\"a\":1}")
            Mockito.verify(values).put("session_id", "session")
            Mockito.verify(database).insert("reporting", null, values)
        }
    }

    @Test
    fun testGetReportingMessagesForUploadSkipsTheTemporaryMpidAndReadsEveryColumn() {
        Mockito.`when`(database.query("reporting", null, "mp_id != ?", arrayOf<String?>("0"), null, null, "_id asc")).thenReturn(cursor)
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("session_id")).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("mp_id")).thenReturn(3)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getString(1)).thenReturn("{\"a\":1}")
        Mockito.`when`(cursor.getString(2)).thenReturn("session")
        Mockito.`when`(cursor.getInt(0)).thenReturn(9)
        Mockito.`when`(cursor.getLong(3)).thenReturn(7L)

        val messages = ReportingService.getReportingMessagesForUpload(database)

        Assert.assertEquals(1, messages.size)
        Assert.assertEquals(1, messages[0].msgObject.getInt("a"))
        Assert.assertEquals("session", messages[0].sessionId)
        Assert.assertEquals(9, messages[0].reportingMessageId)
        Assert.assertEquals(7L, messages[0].mpid)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetReportingMessagesForUploadOfOneMpidSelectsThatMpid() {
        Mockito.`when`(database.query("reporting", null, "mp_id = ?", arrayOf<String?>("3"), null, null, "_id asc")).thenReturn(cursor)

        Assert.assertTrue(ReportingService.getReportingMessagesForUpload(database, true, 3L).isEmpty())
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetReportingMessagesForUploadThrowsOnAStoredMessageThatIsNotJsonAndClosesTheCursor() {
        Mockito.`when`(database.query("reporting", null, "mp_id != ?", arrayOf<String?>("0"), null, null, "_id asc")).thenReturn(cursor)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getString(1)).thenReturn("not json")

        Assert.assertThrows(JSONException::class.java) { ReportingService.getReportingMessagesForUpload(database) }
        Mockito.verify(cursor).close()
    }

    @Test
    fun testDeleteReportingMessageDeletesOneRowById() {
        ReportingService.deleteReportingMessage(database, 9)

        Mockito.verify(database).delete("reporting", "_id =?", arrayOf<String?>("9"))
    }

    @Test
    fun testDeleteAllDeletesEveryRow() {
        ReportingService.deleteAll(database)

        Mockito.verify(database).delete("reporting", null, null)
    }
}
