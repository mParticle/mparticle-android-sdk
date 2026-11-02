package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.UploadSettings
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class UploadServiceTest {
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)
    private val uploadSettings: UploadSettings = Mockito.mock(UploadSettings::class.java)

    @Test
    fun testCleanupUploadMessagesDeletesUploadsOverTheSizeLimit() {
        Mockito.`when`(database.delete("uploads", "length(message) > ${Constants.LIMIT_MAX_UPLOAD_SIZE}", null)).thenReturn(3)

        Assert.assertEquals(3, UploadService.cleanupUploadMessages(database))
    }

    @Test
    fun testDeleteUploadsOlderThanDeletesByCreationTime() {
        Mockito.`when`(database.delete("uploads", "message_time < ?", arrayOf<String?>("1000"))).thenReturn(4)

        Assert.assertEquals(4, UploadService.deleteUploadsOlderThan(database, 1000L))
    }

    @Test
    fun testDeleteUploadDeletesOneRowById() {
        Mockito.`when`(database.delete("uploads", "_id=?", arrayOf<String?>("5"))).thenReturn(1)

        Assert.assertEquals(1, UploadService.deleteUpload(database, 5))
    }

    @Test
    fun testInsertUploadStoresABatchWithTheMessageTimestamp() {
        stubUploadSettings()
        val message = JSONObject().put(Constants.MessageKey.TIMESTAMP, 123L)
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            UploadService.insertUpload(database, message, uploadSettings)

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("api_key", "key")
            Mockito.verify(values).put("message_time", 123L)
            Mockito.verify(values).put("message", message.toString())
            Mockito.verify(values).put("cfuuid", "0")
            Mockito.verify(values).put("upload_settings", "settings")
            Mockito.verify(database).insert("uploads", null, values)
        }
    }

    @Test
    fun testInsertUploadUsesTheCurrentTimeWhenTheMessageHasNoTimestamp() {
        stubUploadSettings()
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            val before = System.currentTimeMillis()
            UploadService.insertUpload(database, JSONObject(), uploadSettings)
            val after = System.currentTimeMillis()

            Mockito.verify(contentValues.constructed()[0]).put(Mockito.eq("message_time"), Mockito.longThat { it in before..after })
        }
    }

    @Test
    fun testInsertAliasRequestStoresAnAliasRowAndReturnsItsId() {
        stubUploadSettings()
        val request = JSONObject().put("request_type", "alias")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            val before = System.currentTimeMillis()
            Mockito.`when`(database.insert(Mockito.eq("uploads"), Mockito.isNull(), Mockito.any())).thenReturn(8L)

            Assert.assertEquals(8L, UploadService.insertAliasRequest(database, request, uploadSettings))

            val after = System.currentTimeMillis()
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("api_key", "key")
            Mockito.verify(values).put(Mockito.eq("message_time"), Mockito.longThat { it in before..after })
            Mockito.verify(values).put("message", request.toString())
            Mockito.verify(values).put("cfuuid", "1")
            Mockito.verify(values).put("upload_settings", "settings")
            Mockito.verify(database).insert("uploads", null, values)
        }
    }

    @Test
    fun testGetReadyUploadsReadsEveryRowOldestFirstAndClosesTheCursor() {
        Mockito
            .`when`(database.query("uploads", arrayOf<String?>("_id", "message", "cfuuid", "upload_settings"), null, null, null, null, "message_time"))
            .thenReturn(cursor)
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("cfuuid")).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("upload_settings")).thenReturn(3)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(cursor.getInt(0)).thenReturn(4, 5)
        Mockito.`when`(cursor.getString(1)).thenReturn("batch", "alias")
        Mockito.`when`(cursor.getString(2)).thenReturn("0", "1")
        Mockito.`when`(cursor.getString(3)).thenReturn("{}")
        Mockito.mockStatic(Logger::class.java).use {
            val uploads = UploadService.getReadyUploads(database)

            Assert.assertEquals(listOf(4, 5), uploads.map { it.id })
            Assert.assertEquals(listOf("batch", "alias"), uploads.map { it.message })
            Assert.assertEquals(listOf(false, true), uploads.map { it.isAliasRequest })
            Assert.assertEquals(listOf(null, null), uploads.map { it.uploadSettings })
            Mockito.verify(cursor).close()
        }
    }

    @Test
    fun testGetReadyUploadsLogsAFailedQueryAndReturnsAnEmptyList() {
        val failure = IllegalStateException("query failed")
        Mockito
            .`when`(database.query("uploads", arrayOf<String?>("_id", "message", "cfuuid", "upload_settings"), null, null, null, null, "message_time"))
            .thenThrow(failure)
        Mockito.mockStatic(Logger::class.java).use { logger ->
            Assert.assertTrue(UploadService.getReadyUploads(database).isEmpty())

            logger.verify { Logger.error(failure, "Failed to get ready uploads") }
        }
    }

    private fun stubUploadSettings() {
        Mockito.`when`(uploadSettings.apiKey).thenReturn("key")
        Mockito.`when`(uploadSettings.toJson()).thenReturn("settings")
    }
}
