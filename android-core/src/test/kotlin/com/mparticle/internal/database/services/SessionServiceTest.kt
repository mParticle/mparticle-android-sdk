package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import com.mparticle.internal.BatchId
import com.mparticle.internal.MessageBatch
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class SessionServiceTest {
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val cursor: Cursor = Mockito.mock(Cursor::class.java)
    private val message: BaseMPMessage = Mockito.mock(BaseMPMessage::class.java)

    @Test
    fun testReadyMessagesHoldsTheUploadedStatus() {
        Assert.assertArrayEquals(arrayOf("3"), SessionService.readyMessages)
    }

    @Test
    fun testDeleteSessionsDeletesEveryOtherSession() {
        Mockito.`when`(database.delete("sessions", "session_id!=? ", arrayOf<String?>("current"))).thenReturn(2)

        Assert.assertEquals(2, SessionService.deleteSessions(database, "current"))
    }

    @Test
    fun testDeleteSessionsOlderThanDeletesByEndTime() {
        Mockito.`when`(database.delete("sessions", "end_time < ?", arrayOf<String?>("1000"))).thenReturn(4)

        Assert.assertEquals(4, SessionService.deleteSessionsOlderThan(database, 1000L))
    }

    @Test
    fun testDeleteSessionsExceptASetQuotesEachIdIntoTheClause() {
        Mockito.`when`(database.delete("sessions", "session_id NOT IN ('a','b')", null)).thenReturn(3)
        Mockito.mockStatic(TextUtils::class.java).use {
            stubJoin()

            Assert.assertEquals(3, SessionService.deleteSessions(database, linkedSetOf("a", "b")))
        }
    }

    @Test
    fun testDeleteSessionsExceptAnEmptySetWritesAnEmptyList() {
        Mockito.mockStatic(TextUtils::class.java).use {
            stubJoin()

            SessionService.deleteSessions(database, emptySet<String>())

            Mockito.verify(database).delete("sessions", "session_id NOT IN ()", null)
        }
    }

    @Test
    fun testGetSessionsQueriesEveryRowAndLeavesTheCursorOpen() {
        Mockito.`when`(database.query("sessions", null, null, null, null, null, null)).thenReturn(cursor)

        Assert.assertSame(cursor, SessionService.getSessions(database))
        Mockito.verify(cursor, Mockito.never()).close()
    }

    @Test
    fun testUpdateSessionEndTimeStoresTheForegroundLengthWhenItIsPositive() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            SessionService.updateSessionEndTime(database, "session", 100L, 20L)

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("end_time", 100L)
            Mockito.verify(values).put("session_length", 20L)
            Mockito.verify(database).update("sessions", values, "session_id=?", arrayOf<String?>("session"))
        }
    }

    @Test
    fun testUpdateSessionEndTimeSkipsAZeroForegroundLength() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            SessionService.updateSessionEndTime(database, "session", 100L, 0L)

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("end_time", 100L)
            Mockito.verifyNoMoreInteractions(values)
            Mockito.verify(database).update("sessions", values, "session_id=?", arrayOf<String?>("session"))
        }
    }

    @Test
    fun testUpdateSessionAttributesStoresTheAttributes() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            SessionService.updateSessionAttributes(database, "session", "{\"a\":1}")

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("attributes", "{\"a\":1}")
            Mockito.verify(database).update("sessions", values, "session_id=?", arrayOf<String?>("session"))
        }
    }

    @Test
    fun testUpdateSessionStatusStoresTheStatusInTheStatusColumn() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            SessionService.updateSessionStatus(database, "session", "1")

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("cfuuid", "1")
            Mockito.verify(database).update("sessions", values, "session_id=?", arrayOf<String?>("session"))
        }
    }

    @Test
    fun testGetSessionForSessionEndMessageSelectsAnOpenSessionAndLeavesTheCursorOpen() {
        Mockito
            .`when`(database.query("sessions", arrayOf<String?>("start_time", "end_time", "session_length", "attributes"), "session_id=? and cfuuid IS NULL", arrayOf<String?>("session"), null, null, null))
            .thenReturn(cursor)

        Assert.assertSame(cursor, SessionService.getSessionForSessionEndMessage(database, "session"))
        Mockito.verify(cursor, Mockito.never()).close()
    }

    @Test
    fun testGetOrphanSessionIdsReadsEveryOpenSessionOfTheApiKeyAndClosesTheCursor() {
        Mockito.`when`(database.query("sessions", arrayOf<String?>("session_id"), "api_key= ? and cfuuid IS NULL", arrayOf<String?>("key"), null, null, null)).thenReturn(cursor)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(cursor.getString(0)).thenReturn("a", "b")

        Assert.assertEquals(listOf("a", "b"), SessionService.getOrphanSessionIds(database, "key"))
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetOrphanSessionIdsThrowsAFailedQuery() {
        val failure = IllegalStateException("query failed")
        Mockito.`when`(database.query("sessions", arrayOf<String?>("session_id"), "api_key= ? and cfuuid IS NULL", arrayOf<String?>("key"), null, null, null)).thenThrow(failure)

        val thrown = Assert.assertThrows(IllegalStateException::class.java) { SessionService.getOrphanSessionIds(database, "key") }
        Assert.assertSame(failure, thrown)
    }

    @Test
    fun testInsertSessionStoresEveryColumn() {
        Mockito.`when`(message.getLong("ct")).thenReturn(100L)
        Mockito.`when`(message.sessionId).thenReturn("session")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            SessionService.insertSession(database, message, "key", "{\"app\":1}", "{\"device\":1}", 7L)

            Assert.assertEquals(1, contentValues.constructed().size)
            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("api_key", "key")
            Mockito.verify(values).put("session_id", "session")
            Mockito.verify(values).put("start_time", 100L)
            Mockito.verify(values).put("end_time", 100L)
            Mockito.verify(values).put("session_length", 0)
            Mockito.verify(values).put("app_info", "{\"app\":1}")
            Mockito.verify(values).put("device_info", "{\"device\":1}")
            Mockito.verify(database).insert("sessions", null, values)
        }
    }

    @Test
    fun testInsertSessionWithNoTimestampThrowsAndInsertsNothing() {
        Mockito.`when`(message.getLong("ct")).thenThrow(JSONException("no timestamp"))
        Mockito.mockConstruction(ContentValues::class.java).use {
            Assert.assertThrows(JSONException::class.java) { SessionService.insertSession(database, message, "key", "{}", "{}", 7L) }

            Mockito.verifyNoInteractions(database)
        }
    }

    @Test
    fun testProcessSessionsSetsTheAppAndDeviceInfoOnTheBatchesOfEachStoredSession() {
        val first = Mockito.mock(MessageBatch::class.java)
        val second = Mockito.mock(MessageBatch::class.java)
        val other = Mockito.mock(MessageBatch::class.java)
        val batches =
            hashMapOf(
                BatchId(1L, "s1", null, null) to first,
                BatchId(2L, "s1", null, null) to second,
                BatchId(1L, "s3", null, null) to other,
            )
        stubSessionRows(listOf("s1", "s2"), listOf("{\"app\":1}", "{\"app\":2}"), listOf("{\"device\":1}", "{\"device\":2}"))

        val deviceInfos = SessionService.processSessions(database, batches)

        Assert.assertEquals(listOf("{\"device\":1}"), deviceInfos.map { it.toString() })
        for (batch in listOf(first, second)) {
            Mockito.verify(batch).setAppInfo(Mockito.argThat<JSONObject> { it.toString() == "{\"app\":1}" })
            Mockito.verify(batch).setDeviceInfo(Mockito.argThat<JSONObject> { it.toString() == "{\"device\":1}" })
        }
        Mockito.verifyNoInteractions(other)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testProcessSessionsSkipsASessionWhoseInfoIsNotJson() {
        val batch = Mockito.mock(MessageBatch::class.java)
        stubSessionRows(listOf("s1"), listOf("not json"), listOf("{\"device\":1}"))

        val deviceInfos = SessionService.processSessions(database, hashMapOf(BatchId(1L, "s1", null, null) to batch))

        Assert.assertTrue(deviceInfos.isEmpty())
        Mockito.verifyNoInteractions(batch)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testUpdateSessionInstallReferrerStoresTheAppInfoText() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            SessionService.updateSessionInstallReferrer(database, JSONObject().put("referrer", "r"), "session")

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("app_info", "{\"referrer\":\"r\"}")
            Mockito.verify(database).update("sessions", values, "session_id=?", arrayOf<String?>("session"))
        }
    }

    @Test
    fun testFlattenBySessionIdGroupsTheBatchesOfEachSession() {
        val first = Mockito.mock(MessageBatch::class.java)
        val second = Mockito.mock(MessageBatch::class.java)
        val other = Mockito.mock(MessageBatch::class.java)
        val batches =
            hashMapOf(
                BatchId(1L, "s1", null, null) to first,
                BatchId(2L, "s1", "plan", 1) to second,
                BatchId(1L, "s2", null, null) to other,
            )

        val bySessionId = SessionService.flattenBySessionId(batches)

        Assert.assertEquals(setOf("s1", "s2"), bySessionId.keys)
        Assert.assertEquals(setOf(first, second), bySessionId["s1"]!!.toSet())
        Assert.assertEquals(2, bySessionId["s1"]!!.size)
        Assert.assertEquals(listOf(other), bySessionId["s2"])
    }

    @Test
    fun testDeleteAllDeletesEveryRow() {
        SessionService.deleteAll(database)

        Mockito.verify(database).delete("sessions", null, null)
    }

    @Test
    fun testANullDatabaseThrowsNullPointerException() {
        Assert.assertThrows(NullPointerException::class.java) { SessionService.deleteAll(null) }
    }

    private fun stubJoin() {
        Mockito
            .`when`(TextUtils.join(Mockito.any(), Mockito.any<Array<Any>>()))
            .thenAnswer { invocation -> invocation.getArgument<Array<Any>>(1).joinToString(invocation.getArgument<CharSequence>(0)) }
    }

    private fun stubSessionRows(sessionIds: List<String>, appInfos: List<String>, deviceInfos: List<String>) {
        Mockito.`when`(database.query("sessions", null, null, null, null, null, null)).thenReturn(cursor)
        Mockito.`when`(cursor.getColumnIndexOrThrow("session_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("app_info")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("device_info")).thenReturn(2)
        val moves = sessionIds.map { true } + false
        Mockito.`when`(cursor.moveToNext()).thenReturn(moves[0], *moves.drop(1).toTypedArray())
        Mockito.`when`(cursor.getString(0)).thenReturn(sessionIds[0], *sessionIds.drop(1).toTypedArray())
        Mockito.`when`(cursor.getString(1)).thenReturn(appInfos[0], *appInfos.drop(1).toTypedArray())
        Mockito.`when`(cursor.getString(2)).thenReturn(deviceInfos[0], *deviceInfos.drop(1).toTypedArray())
    }
}
