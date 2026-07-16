package com.mparticle.internal.database.services

import android.database.Cursor
import com.mparticle.internal.BatchId
import com.mparticle.internal.MessageBatch
import com.mparticle.internal.database.tables.SessionTable
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class SessionServiceTest : BaseMPServiceTest() {
    @Test
    @Throws(Exception::class)
    fun testUpdateSessionInstallReferrer() {
        var fooObject = JSONObject()
        val sessionId = UUID.randomUUID().toString()
        fooObject.put("foo", "bar")
        val mpMessage = getMpMessage(sessionId)
        SessionService.insertSession(
            database,
            mpMessage,
            "foo-app-key",
            fooObject.toString(),
            fooObject.toString(),
            1,
        )
        fooObject = JSONObject()
        val randomId = UUID.randomUUID().toString()
        fooObject.put("foo", randomId)
        SessionService.updateSessionInstallReferrer(database, fooObject, sessionId)
        var cursor: Cursor? = null
        try {
            cursor = SessionService.getSessions(database)
            while (cursor.moveToNext()) {
                val currentSessionId =
                    cursor.getString(cursor.getColumnIndexOrThrow(SessionTable.SessionTableColumns.SESSION_ID))
                val appInfo =
                    cursor.getString(cursor.getColumnIndexOrThrow(SessionTable.SessionTableColumns.APP_INFO))
                if (sessionId == currentSessionId) {
                    val appInfoObject = JSONObject(appInfo)
                    assertEquals(randomId, appInfoObject.getString("foo"))
                    return
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        Assert.fail("Failed to find updated app customAttributes object.")
    }

    @Test
    fun flattenMessagesByBatchIdTest() {
        val batchMap: MutableMap<BatchId, MessageBatch> = HashMap()
        batchMap[BatchId(ran.nextLong(), "1", "a", 1)] = MockMessageBatch(1)
        batchMap[BatchId(ran.nextLong(), "2", null, null)] = MockMessageBatch(2)
        batchMap[BatchId(ran.nextLong(), "1", "a", 2)] = MockMessageBatch(3)
        batchMap[BatchId(ran.nextLong(), "1", "ab", null)] = MockMessageBatch(4)
        batchMap[BatchId(ran.nextLong(), "2", null, 3)] = MockMessageBatch(5)
        batchMap[BatchId(ran.nextLong(), "3", null, 3)] = MockMessageBatch(6)
        batchMap[BatchId(ran.nextLong(), "1", null, 3)] = MockMessageBatch(7)
        val batchBySessionId = SessionService.flattenBySessionId(batchMap)
        assertEquals(4, batchBySessionId["1"]?.size)
        assertEquals(2, batchBySessionId["2"]?.size)
        assertEquals(1, batchBySessionId["3"]?.size)

        // make sure the elements in the list are unique..no inadvertent copies
        val session1Batches = ArrayList(batchBySessionId["1"])
        var size = session1Batches.size
        for (messageBatch in session1Batches) {
            batchBySessionId["1"]?.remove(messageBatch)
            assertEquals(--size, batchBySessionId["1"]?.size)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun testDeleteSessionsOlderThan() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24L * 60L * 60L * 1000L
        val oldEndTime = now - 10L * oneDayMillis
        val recentEndTime = now - 60L * 60L * 1000L

        // Insert 5 sessions whose END_TIME is 10 days ago and 5 whose END_TIME is 1 hour ago.
        // insertSession seeds END_TIME = START_TIME, so we call updateSessionEndTime to model
        // the production flow where subsequent events advance END_TIME independently.
        for (i in 0 until 5) {
            val oldSessionId = UUID.randomUUID().toString()
            SessionService.insertSession(database, getMpMessage(oldSessionId), "apiKey", "{}", "{}", 1L)
            SessionService.updateSessionEndTime(database, oldSessionId, oldEndTime, 0)
        }
        for (i in 0 until 5) {
            val recentSessionId = UUID.randomUUID().toString()
            SessionService.insertSession(database, getMpMessage(recentSessionId), "apiKey", "{}", "{}", 1L)
            SessionService.updateSessionEndTime(database, recentSessionId, recentEndTime, 0)
        }
        assertEquals(10, countSessions())

        // Cut off at 7 days ago - the 5 old sessions should be removed and the 5 recent kept.
        val cutoffMillis = now - 7L * oneDayMillis
        val deleted = SessionService.deleteSessionsOlderThan(database, cutoffMillis)
        assertEquals(5, deleted)
        assertEquals(5, countSessions())

        // Rows whose END_TIME is exactly at the cutoff must not be removed (strict `<` predicate).
        val boundarySessionId = UUID.randomUUID().toString()
        SessionService.insertSession(database, getMpMessage(boundarySessionId), "apiKey", "{}", "{}", 1L)
        SessionService.updateSessionEndTime(database, boundarySessionId, cutoffMillis, 0)
        assertEquals(0, SessionService.deleteSessionsOlderThan(database, cutoffMillis))
        assertEquals(6, countSessions())
    }

    private fun countSessions(): Int {
        var count = 0
        var cursor: Cursor? = null
        try {
            cursor = SessionService.getSessions(database)
            while (cursor.moveToNext()) {
                count++
            }
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        return count
    }

    internal inner class MockMessageBatch(
        var id: Int,
    ) : MessageBatch() {
        override fun equals(other: Any?): Boolean = if (other is MockMessageBatch) {
            id == other.id
        } else {
            super.equals(other)
        }

        override fun hashCode(): Int = id
    }
}
