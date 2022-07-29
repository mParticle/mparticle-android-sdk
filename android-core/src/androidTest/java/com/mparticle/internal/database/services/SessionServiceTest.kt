package com.mparticle.internal.database.services

import android.database.Cursor
import com.mparticle.internal.BatchId
import com.mparticle.internal.MessageBatch
import com.mparticle.internal.database.tables.SessionTable
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID
import kotlin.random.Random

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
            1
        )
        fooObject = JSONObject()
        val randomId = UUID.randomUUID().toString()
        fooObject.put("foo", randomId)
        SessionService.updateSessionInstallReferrer(
            database,
            fooObject,
            sessionId
        )
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
                    Assert.assertEquals(randomId, appInfoObject.getString("foo"))
                    return
                }
            }
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
        fail("Failed to find updated app customAttributes object.")
    }

    @Test
    fun flattenMessagesByBatchIdTest() {
        val batchMap: MutableMap<BatchId, MessageBatch> = HashMap()
        batchMap[BatchId(Random.Default.nextLong(), "1", "a", 1)] = MockMessageBatch(1)
        batchMap[BatchId(Random.Default.nextLong(), "2", null, null)] = MockMessageBatch(2)
        batchMap[BatchId(Random.Default.nextLong(), "1", "a", 2)] = MockMessageBatch(3)
        batchMap[BatchId(Random.Default.nextLong(), "1", "ab", null)] = MockMessageBatch(4)
        batchMap[BatchId(Random.Default.nextLong(), "2", null, 3)] = MockMessageBatch(5)
        batchMap[BatchId(Random.Default.nextLong(), "3", null, 3)] = MockMessageBatch(6)
        batchMap[BatchId(Random.Default.nextLong(), "1", null, 3)] = MockMessageBatch(7)
        val batchBySessionId = SessionService.flattenBySessionId(batchMap)
        Assert.assertEquals(4, batchBySessionId["1"]!!.size.toLong())
        Assert.assertEquals(2, batchBySessionId["2"]!!.size.toLong())
        Assert.assertEquals(1, batchBySessionId["3"]!!.size.toLong())

        // make sure the elements in the list are unique..no inadvertent copies
        val session1Batches = mutableListOf(
            batchBySessionId["1"]
        )
        var size = session1Batches.size
        for (messageBatch in session1Batches) {
            batchBySessionId["1"]!!.removeAll(messageBatch ?: listOf())
            Assert.assertEquals(--size, batchBySessionId["1"]!!.size)
        }
    }

    internal inner class MockMessageBatch(var id: Int) : MessageBatch() {
        override fun equals(other: Any?): Boolean {
            return if (other is MockMessageBatch) {
                id == other.id
            } else {
                super.equals(other)
            }
        }
    }
}
