package com.mparticle.internal.database.services

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.messages.BaseMPMessage
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class BreadcrumbServiceTest {
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)
    private val context: Context = Mockito.mock(Context::class.java)
    private val message: BaseMPMessage = Mockito.mock(BaseMPMessage::class.java)

    @Test
    fun testInsertBreadcrumbWithNoMessageReturnsMinusOneAndTouchesNothing() {
        Assert.assertEquals(-1, BreadcrumbService.insertBreadcrumb(database, context, null, "key", 7L))

        Mockito.verifyNoInteractions(database)
    }

    @Test
    fun testInsertBreadcrumbStoresTheMessageAndDeletesTheOldestRowOverTheLimit() {
        stubMessage()
        Mockito.`when`(database.query("breadcrumbs", arrayOf<String?>("_id"), "mp_id = ?", arrayOf<String?>("7"), null, null, " _id asc")).thenReturn(cursor)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(cursor.getInt(0)).thenReturn(11)
        Mockito.`when`(cursor.count).thenReturn(3)
        Mockito.`when`(database.delete("breadcrumbs", " _id = ?", arrayOf<String?>("11"))).thenReturn(1)
        Mockito.mockStatic(ConfigManager::class.java).use {
            Mockito.`when`(ConfigManager.getBreadcrumbLimit(context, 7L)).thenReturn(2)
            Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
                Assert.assertEquals(1, BreadcrumbService.insertBreadcrumb(database, context, message, "key", 7L))

                Assert.assertEquals(1, contentValues.constructed().size)
                val values = contentValues.constructed()[0]
                Mockito.verify(values).put("mp_id", 7L)
                Mockito.verify(values).put("api_key", "key")
                Mockito.verify(values).put("breadcrumb_time", 100L)
                Mockito.verify(values).put("session_id", "session")
                Mockito.verify(values).put("message", MESSAGE_JSON)
                val inOrder = Mockito.inOrder(database)
                inOrder.verify(database).insert("breadcrumbs", null, values)
                inOrder.verify(database).query("breadcrumbs", arrayOf<String?>("_id"), "mp_id = ?", arrayOf<String?>("7"), null, null, " _id asc")
                inOrder.verify(database).delete("breadcrumbs", " _id = ?", arrayOf<String?>("11"))
            }
        }
    }

    @Test
    fun testInsertBreadcrumbAtTheLimitDeletesNothing() {
        stubMessage()
        Mockito.`when`(database.query("breadcrumbs", arrayOf<String?>("_id"), "mp_id = ?", arrayOf<String?>("7"), null, null, " _id asc")).thenReturn(cursor)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(cursor.count).thenReturn(2)
        Mockito.mockStatic(ConfigManager::class.java).use {
            Mockito.`when`(ConfigManager.getBreadcrumbLimit(context, 7L)).thenReturn(2)
            Mockito.mockConstruction(ContentValues::class.java).use {
                Assert.assertEquals(-1, BreadcrumbService.insertBreadcrumb(database, context, message, "key", 7L))

                Mockito.verify(database, Mockito.never()).delete(Mockito.any(), Mockito.any(), Mockito.any())
            }
        }
    }

    @Test
    fun testInsertBreadcrumbWithANullMpidStoresANullMpidAndQueriesForTheTextNull() {
        stubMessage()
        Mockito.`when`(database.query("breadcrumbs", arrayOf<String?>("_id"), "mp_id = ?", arrayOf<String?>("null"), null, null, " _id asc")).thenReturn(cursor)
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            Assert.assertEquals(-1, BreadcrumbService.insertBreadcrumb(database, context, message, "key", null))

            Mockito.verify(contentValues.constructed()[0]).put("mp_id", null as Long?)
            Mockito.verify(database).query("breadcrumbs", arrayOf<String?>("_id"), "mp_id = ?", arrayOf<String?>("null"), null, null, " _id asc")
        }
    }

    @Test
    fun testGetBreadcrumbsReadsTheNewestRowsUpToTheLimitAndClosesTheCursor() {
        Mockito
            .`when`(database.query("breadcrumbs", arrayOf<String?>("breadcrumb_time", "message"), "mp_id = ? ", arrayOf<String?>("7"), null, null, "breadcrumb_time desc limit 2"))
            .thenReturn(cursor)
        Mockito.`when`(cursor.count).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(cursor.getString(1)).thenReturn("{\"a\":1}", "{\"b\":2}")
        Mockito.mockStatic(ConfigManager::class.java).use {
            Mockito.`when`(ConfigManager.getBreadcrumbLimit(context, 7L)).thenReturn(2)

            val breadcrumbs = BreadcrumbService.getBreadcrumbs(database, context, 7L)

            Assert.assertEquals(2, breadcrumbs.length())
            Assert.assertEquals(1, breadcrumbs.getJSONObject(0).getInt("a"))
            Assert.assertEquals(2, breadcrumbs.getJSONObject(1).getInt("b"))
            Mockito.verify(cursor).close()
        }
    }

    @Test
    fun testGetBreadcrumbsWithNoRowsReturnsAnEmptyArray() {
        Mockito
            .`when`(database.query("breadcrumbs", arrayOf<String?>("breadcrumb_time", "message"), "mp_id = ? ", arrayOf<String?>("7"), null, null, "breadcrumb_time desc limit 2"))
            .thenReturn(cursor)
        Mockito.mockStatic(ConfigManager::class.java).use {
            Mockito.`when`(ConfigManager.getBreadcrumbLimit(context, 7L)).thenReturn(2)

            Assert.assertEquals(0, BreadcrumbService.getBreadcrumbs(database, context, 7L).length())
            Mockito.verify(cursor).close()
        }
    }

    @Test
    fun testGetBreadcrumbsWithANullMpidLogsTheFailureAndReturnsAnEmptyArray() {
        Mockito.mockStatic(Logger::class.java).use { logger ->
            Assert.assertEquals(0, BreadcrumbService.getBreadcrumbs(database, context, null).length())

            Mockito.verifyNoInteractions(database)
            logger.verify { Logger.debug(Mockito.startsWith("Error while appending breadcrumbs: java.lang.NullPointerException")) }
        }
    }

    @Test
    fun testGetBreadcrumbCountCountsTheRowsOfOneMpidAndClosesTheCursor() {
        Mockito.`when`(database.query("breadcrumbs", null, "mp_id = ? ", arrayOf<String?>("7"), null, null, null)).thenReturn(cursor)
        Mockito.`when`(cursor.count).thenReturn(3)

        Assert.assertEquals(3, BreadcrumbService.getBreadcrumbCount(database, 7L))
        Mockito.verify(cursor).close()
    }

    @Test
    fun testDeleteAllDeletesEveryRow() {
        BreadcrumbService.deleteAll(database)

        Mockito.verify(database).delete("breadcrumbs", null, null)
    }

    private fun stubMessage() {
        Mockito.`when`(message.getLong("ct")).thenReturn(100L)
        Mockito.`when`(message.sessionId).thenReturn("session")
        Mockito.`when`(message.toString()).thenReturn(MESSAGE_JSON)
    }

    private companion object {
        const val MESSAGE_JSON = "{\"dt\":\"bc\"}"
    }
}
