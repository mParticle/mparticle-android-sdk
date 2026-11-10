package com.mparticle.internal.database.services

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.TypedUserAttributeListener
import com.mparticle.identity.UserAttributeListenerWrapper
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.DatabaseHelper
import com.mparticle.internal.DeviceAttributes
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import com.mparticle.internal.MessageManager
import com.mparticle.internal.MessageManagerCallbacks
import com.mparticle.internal.UserStorage
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.mock.MockContext
import com.mparticle.networking.NetworkOptions
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.Mockito

class MParticleDBManagerTest {
    private val context = MockContext()
    private val database: MPDatabase = Mockito.mock(MPDatabase::class.java)
    private val emptyCursor: Cursor = Mockito.mock(Cursor::class.java)
    private lateinit var logger: MockedStatic<Logger>
    private lateinit var manager: MParticleDBManager

    @Before
    fun setUp() {
        logger = Mockito.mockStatic(Logger::class.java)
        stubQuery(null, emptyCursor)
        Mockito.`when`(database.rawQuery(Mockito.anyString())).thenReturn(emptyCursor)
        manager = managerOn(database)
    }

    @After
    fun tearDown() {
        logger.close()
        MParticleDBManager.setMessageListener(null)
        MParticle.setInstance(null)
    }

    @Test
    fun testConstructorOpensThePreferencesFileAndTheDatabaseHelperWithTheContext() {
        val mockContext = Mockito.mock(Context::class.java)
        val helperArguments = ArrayList<Any?>()
        Mockito.mockConstruction(DatabaseHelper::class.java) { _, construction -> helperArguments.addAll(construction.arguments()) }.use {
            MParticleDBManager(mockContext)

            Mockito.verify(mockContext).getSharedPreferences("mParticlePrefs", Context.MODE_PRIVATE)
            Assert.assertEquals(listOf<Any?>(mockContext), helperArguments)
        }
    }

    @Test
    fun testConstructorWithANullContextThrows() {
        val nullContext: Context? = null

        Assert.assertThrows(NullPointerException::class.java) { MParticleDBManager(nullContext) }
    }

    @Test
    fun testGetDatabaseWrapsTheHelpersWritableDatabaseOnEachCall() {
        val sqLiteDatabase = Mockito.mock(SQLiteDatabase::class.java)
        Mockito.mockConstruction(DatabaseHelper::class.java) { helper, _ -> Mockito.`when`(helper.writableDatabase).thenReturn(sqLiteDatabase) }.use { helpers ->
            val realManager = MParticleDBManager(context)
            val first = realManager.getDatabase()
            val second = realManager.getDatabase()

            Assert.assertNotSame(first, second)
            first!!.beginTransaction()
            second!!.endTransaction()
            Mockito.verify(helpers.constructed()[0], Mockito.times(2)).writableDatabase
            Mockito.verify(sqLiteDatabase).beginTransaction()
            Mockito.verify(sqLiteDatabase).endTransaction()
        }
    }

    @Test
    fun testGetDatabaseOfTheTestConstructorThrows() {
        Assert.assertThrows(NullPointerException::class.java) { MParticleDBManager().getDatabase() }
    }

    @Test
    fun testUpdateMpIdMovesEveryMpidTableInOneTransaction() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.updateMpId(1L, 2L)

            Assert.assertEquals(5, contentValues.constructed().size)
            for (values in contentValues.constructed()) {
                Mockito.verify(values).put("mp_id", 2L)
            }
        }
        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        for (table in listOf("breadcrumbs", "messages", "reporting", "sessions", "attributes")) {
            order.verify(database).update(Mockito.eq(table), Mockito.any(), Mockito.eq("mp_id = ?"), Mockito.eq(arrayOf<String?>("1")))
        }
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testUpdateMpIdWithoutADatabaseThrows() {
        Assert.assertThrows(NullPointerException::class.java) { managerOn(null).updateMpId(1L, 2L) }
    }

    @Test
    fun testInsertBreadcrumbStoresTheMessageUnderItsMpid() {
        val message = message(7L)
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.insertBreadcrumb(message, "apiKey")

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("api_key", "apiKey")
            Mockito.verify(database).insert("breadcrumbs", null, values)
        }
    }

    @Test
    fun testInsertBreadcrumbWithoutAMessageThrowsBeforeAnyWrite() {
        Assert.assertThrows(NullPointerException::class.java) { manager.insertBreadcrumb(null, "apiKey") }

        Mockito.verify(database, Mockito.never()).insert(Mockito.any(), Mockito.any(), Mockito.any())
    }

    @Test
    fun testAppendBreadcrumbsAddsTheStoredBreadcrumbsReadWithTheManagersContext() {
        val message = message(7L)
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.count).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getString(1)).thenReturn("{\"dt\":\"e\"}")
        stubQuery("breadcrumbs", cursor)
        Mockito.mockStatic(ConfigManager::class.java).use { configManager ->
            configManager.`when`<Int> { ConfigManager.getBreadcrumbLimit(context, 7L) }.thenReturn(3)

            manager.appendBreadcrumbs(message)

            configManager.verify { ConfigManager.getBreadcrumbLimit(context, 7L) }
        }
        val breadcrumbs = ArgumentCaptor.forClass(JSONArray::class.java)
        Mockito.verify(message).put(Mockito.eq("bc"), breadcrumbs.capture())
        Assert.assertEquals("e", breadcrumbs.value.getJSONObject(0).getString("dt"))
    }

    @Test
    fun testAppendBreadcrumbsWithNothingStoredLeavesTheMessageAlone() {
        val message = message(7L)
        Mockito.mockStatic(ConfigManager::class.java).use {
            manager.appendBreadcrumbs(message)
        }

        Mockito.verify(message, Mockito.never()).put(Mockito.eq("bc"), Mockito.any<Any>())
    }

    @Test
    fun testCleanupMessagesDeletesOversizedMessages() {
        manager.cleanupMessages()

        Mockito.verify(database).delete("messages", "length(message) > 102400", null)
    }

    @Test
    fun testInsertMessageStoresTheMessageAndNotifiesTheListener() {
        val message = message(7L)
        Mockito.`when`(message.sessionId).thenReturn("session-1")
        Mockito.`when`(message.getString("dt")).thenReturn("e")
        Mockito.`when`(message.toString()).thenReturn("{\"dt\":\"e\"}")
        val stored = ArrayList<BaseMPMessage>()
        MParticleDBManager.setMessageListener(MParticleDBManager.MessageListener { stored.add(it) })
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.insertMessage("apiKey", message, "plan", 2)

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("session_id", "session-1")
            Mockito.verify(values).put("dataplan_id", "plan")
            Mockito.verify(values).put("dataplan_version", 2)
            Mockito.verify(values).put("message", "{\"dt\":\"e\"}")
            Mockito.verify(database).insert("messages", null, values)
        }
        Assert.assertEquals(listOf(message), stored)
    }

    @Test
    fun testInsertMessageWithoutAMessageThrowsBeforeAnyWrite() {
        val stored = ArrayList<BaseMPMessage>()
        MParticleDBManager.setMessageListener(MParticleDBManager.MessageListener { stored.add(it) })

        Assert.assertThrows(NullPointerException::class.java) { manager.insertMessage("apiKey", null, null, null) }

        Mockito.verify(database, Mockito.never()).insert(Mockito.any(), Mockito.any(), Mockito.any())
        Assert.assertTrue(stored.isEmpty())
    }

    @Test
    fun testUpdateSessionInstallReferrerStoresTheAppInfo() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.updateSessionInstallReferrer("session-1", JSONObject().put("ir", "x"))

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("app_info", "{\"ir\":\"x\"}")
            Mockito.verify(database).update("sessions", values, "session_id=?", arrayOf<String?>("session-1"))
        }
    }

    @Test
    fun testUpdateSessionEndTimeAttributesAndStatusUpdateTheSessionRow() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.updateSessionEndTime("session-1", 500L, 20L)
            manager.updateSessionAttributes("session-1", "{\"a\":1}")
            manager.updateSessionStatus("session-1", "1")

            val values = contentValues.constructed()
            Mockito.verify(values[0]).put("end_time", 500L)
            Mockito.verify(values[0]).put("session_length", 20L)
            Mockito.verify(values[1]).put("attributes", "{\"a\":1}")
            Mockito.verify(values[2]).put("cfuuid", "1")
            for (value in values) {
                Mockito.verify(database).update("sessions", value, "session_id=?", arrayOf<String?>("session-1"))
            }
        }
    }

    @Test
    fun testGetSessionForSessionEndMessageBuildsTheMessageAndResetsTheEventCounter() {
        val preferences = context.getSharedPreferences("mParticlePrefs", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt("mp::events::counter", 4).apply()
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.moveToFirst()).thenReturn(true)
        Mockito.`when`(cursor.getLong(0)).thenReturn(1000L)
        Mockito.`when`(cursor.getLong(1)).thenReturn(5000L)
        Mockito.`when`(cursor.getLong(2)).thenReturn(3000L)
        Mockito.`when`(cursor.getString(3)).thenReturn("{\"color\":\"blue\"}")
        stubQuery("sessions", cursor)
        // MessageManager.getStateInfo() is a companion function, which Mockito.mockStatic does not reach from Kotlin.
        // The real one runs on a mocked MPUtility, with the context and preferences the constructor stores.
        MessageManager(context, null, null, null, null, null, null)
        Mockito.mockStatic(MPUtility::class.java, Mockito.RETURNS_MOCKS).use {
            Mockito.`when`(MPUtility.getTotalMemory(Mockito.any())).thenReturn(424242L)
            val message = manager.getSessionForSessionEndMessage("session-1", null, linkedSetOf(0L, 5L, 9L))!!

            Assert.assertEquals("se", message.getString("dt"))
            Assert.assertEquals("session-1", message.getString("sid"))
            Assert.assertEquals(1000L, message.getLong("sct"))
            Assert.assertEquals(5000L, message.getLong("ct"))
            Assert.assertEquals(4, message.getInt("en"))
            Assert.assertEquals(3000L, message.getLong("sl"))
            Assert.assertEquals(4000L, message.getLong("slx"))
            Assert.assertEquals(424242L, message.getJSONObject("cs").getLong(Constants.MessageKey.STATE_INFO_TOTAL_MEMORY))
            Assert.assertEquals("[5,9]", message.getJSONArray("smpids").toString())
            Assert.assertEquals("blue", message.getJSONObject("attrs").getString("color"))
            Assert.assertEquals(36, message.getString("id").length)
            Assert.assertEquals(9L, message.mpId)
        }
        Assert.assertEquals(0, preferences.getInt("mp::events::counter", -1))
        Mockito.verify(database).query(
            Mockito.eq("sessions"),
            Mockito.any(),
            Mockito.eq("session_id=? and cfuuid IS NULL"),
            Mockito.eq(arrayOf<String?>("session-1")),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
        )
        Mockito.verify(cursor).close()
    }

    @Test
    fun testGetSessionForSessionEndMessageWithNoOpenSessionReturnsNull() {
        Assert.assertNull(manager.getSessionForSessionEndMessage("session-1", null, setOf(5L)))

        Mockito.verify(emptyCursor).close()
    }

    @Test
    fun testGetOrphanSessionIdsReadsTheOpenSessionsOfTheApiKey() {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(cursor.getString(0)).thenReturn("a", "b")
        stubQuery("sessions", cursor)

        Assert.assertEquals(listOf("a", "b"), manager.getOrphanSessionIds("apiKey"))
        Mockito.verify(database).query(
            Mockito.eq("sessions"),
            Mockito.any(),
            Mockito.any(),
            Mockito.eq(arrayOf<String?>("apiKey")),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
        )
    }

    @Test
    fun testInsertSessionStoresTheAppAndDeviceInfo() {
        val message = message(7L)
        Mockito.`when`(message.sessionId).thenReturn("session-1")
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.insertSession(message, "apiKey", JSONObject().put("a", 1), JSONObject().put("d", 2))

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("mp_id", 7L)
            Mockito.verify(values).put("session_id", "session-1")
            Mockito.verify(values).put("app_info", "{\"a\":1}")
            Mockito.verify(values).put("device_info", "{\"d\":2}")
            Mockito.verify(database).insert("sessions", null, values)
        }
    }

    @Test
    fun testInsertSessionWithoutAppInfoThrowsBeforeAnyWrite() {
        Assert.assertThrows(NullPointerException::class.java) { manager.insertSession(message(7L), "apiKey", null, JSONObject()) }

        Mockito.verify(database, Mockito.never()).insert(Mockito.any(), Mockito.any(), Mockito.any())
    }

    @Test
    fun testDeleteMessagesAndSessionsKeepsTheCurrentSessionInOneTransaction() {
        manager.deleteMessagesAndSessions("session-1")

        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        order.verify(database).delete(Mockito.eq("messages"), Mockito.anyString(), Mockito.eq(arrayOf<String?>("session-1", "0")))
        order.verify(database).delete("sessions", "session_id!=? ", arrayOf<String?>("session-1"))
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testDeleteRecordsOlderThanSweepsTheThreeTablesAndReportsSuccess() {
        Assert.assertTrue(manager.deleteRecordsOlderThan(100L))

        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        order.verify(database).delete("messages", "message_time < ?", arrayOf<String?>("100"))
        order.verify(database).delete("uploads", "message_time < ?", arrayOf<String?>("100"))
        order.verify(database).delete("sessions", "end_time < ?", arrayOf<String?>("100"))
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testDeleteRecordsOlderThanLogsAFailureAndReportsIt() {
        val failure = IllegalStateException("disk full")
        Mockito.`when`(database.delete(Mockito.eq("uploads"), Mockito.any(), Mockito.any())).thenThrow(failure)

        Assert.assertFalse(manager.deleteRecordsOlderThan(100L))

        Mockito.verify(database, Mockito.never()).setTransactionSuccessful()
        Mockito.verify(database).endTransaction()
        logger.verify { Logger.warning(failure, "Error pruning persisted records older than 100 ms.") }
    }

    @Test
    fun testDeleteRecordsOlderThanWithoutADatabaseThrows() {
        Assert.assertThrows(NullPointerException::class.java) { managerOn(null).deleteRecordsOlderThan(100L) }
    }

    @Test
    fun testHasMessagesForUploadCountsTheReadyMessages() {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.count).thenReturn(2, 0)
        stubQuery("messages", cursor)

        Assert.assertTrue(manager.hasMessagesForUpload())
        Assert.assertFalse(manager.hasMessagesForUpload())
        Mockito.verify(cursor, Mockito.times(2)).close()
    }

    @Test
    fun testCreateMessagesForUploadMessageWithNothingReadyOnlyCommits() {
        manager.createMessagesForUploadMessage(Mockito.mock(ConfigManager::class.java), Mockito.mock(DeviceAttributes::class.java), "session-1", uploadSettings())

        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        order.verify(database).query(
            Mockito.eq("messages"),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
            Mockito.any(),
        )
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
        Mockito.verify(database, Mockito.never()).insert(Mockito.any(), Mockito.any(), Mockito.any())
        Mockito.verify(database, Mockito.never()).delete(Mockito.any(), Mockito.any(), Mockito.any())
    }

    @Test
    fun testCreateMessagesForUploadMessageStoresOneBatchWithTheReportingMessage() {
        stubReadyMessage(12, "{\"dt\":\"e\",\"n\":\"event\"}")
        stubReportingMessage(3)
        val upload = createUploads(managerOn(database))

        Assert.assertEquals("event", upload!!.getJSONArray("msgs").getJSONObject(0).getString("n"))
        Assert.assertEquals("x", upload.getJSONArray("fsr").getJSONObject(0).getString("dt"))
        Assert.assertEquals("{}", upload.getJSONObject("ua").toString())
        Assert.assertEquals("[{\"i\":1}]", upload.getJSONArray("ui").toString())
        Assert.assertEquals("{\"app\":1}", upload.getJSONObject("ai").toString())
        Assert.assertEquals("{\"device\":1}", upload.getJSONObject("di").toString())
        Assert.assertFalse(upload.has("mb"))
        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        order.verify(database).delete("messages", "_id <= ? and mp_id != ?", arrayOf<String?>("12", "0"))
        order.verify(database).delete("reporting", "_id =?", arrayOf<String?>("3"))
        order.verify(database).insert(Mockito.eq("uploads"), Mockito.isNull(), Mockito.any())
        order.verify(database).delete("sessions", "session_id NOT IN ('session-current')", null)
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testCreateMessagesForUploadMessageStoresTheBatchTheListenerReturns() {
        stubReadyMessage(12, "{\"dt\":\"e\"}")
        val options = Mockito.mock(MParticleOptions::class.java)
        Mockito.`when`(options.batchCreationListener).thenReturn(MParticleOptions.BatchCreationListener { JSONObject().put("replaced", true) })

        val upload = createUploads(managerOn(database, options))

        Assert.assertEquals(2, upload!!.length())
        Assert.assertTrue(upload.getBoolean("replaced"))
        Assert.assertTrue(upload.getBoolean("mb"))
    }

    @Test
    fun testCreateMessagesForUploadMessageDropsTheBatchWhenTheListenerReturnsNothing() {
        stubReadyMessage(12, "{\"dt\":\"e\"}")
        val options = Mockito.mock(MParticleOptions::class.java)
        Mockito.`when`(options.batchCreationListener).thenReturn(MParticleOptions.BatchCreationListener { null })

        Assert.assertNull(createUploads(managerOn(database, options)))

        logger.verify { Logger.error("Not uploading batch due to 'onCreateBatch' handler being empty") }
        Mockito.verify(database).delete("messages", "_id <= ? and mp_id != ?", arrayOf<String?>("12", "0"))
        Mockito.verify(database).setTransactionSuccessful()
    }

    @Test
    fun testCreateMessagesForUploadMessageStoresTheOriginalBatchWhenTheListenerFails() {
        stubReadyMessage(12, "{\"dt\":\"e\",\"n\":\"event\"}")
        val failure = IllegalStateException("listener failed")
        val options = Mockito.mock(MParticleOptions::class.java)
        Mockito.`when`(options.batchCreationListener).thenReturn(MParticleOptions.BatchCreationListener { throw failure })

        val upload = createUploads(managerOn(database, options))

        Assert.assertEquals("event", upload!!.getJSONArray("msgs").getJSONObject(0).getString("n"))
        logger.verify { Logger.error(failure, "batch creation listener error, original batch will be uploaded") }
    }

    @Test
    fun testCreateMessagesForUploadMessageDropsAnOversizedBatch() {
        stubReadyMessage(12, "{\"dt\":\"e\",\"x\":\"" + "a".repeat(Constants.LIMIT_MAX_UPLOAD_SIZE) + "\"}")

        Assert.assertNull(createUploads(managerOn(database)))

        logger.verify { Logger.error(Mockito.startsWith("Not uploading batch of size ")) }
        Mockito.verify(database).delete("messages", "_id <= ? and mp_id != ?", arrayOf<String?>("12", "0"))
    }

    @Test
    fun testInsertReportingMessagesStoresEachMessageInOneTransaction() {
        val first = reportingMessage(1)
        val second = reportingMessage(2)
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.insertReportingMessages(listOf(first, second), 7L)

            Assert.assertEquals(2, contentValues.constructed().size)
            Mockito.verify(contentValues.constructed()[0]).put("module_id", 1)
            Mockito.verify(contentValues.constructed()[1]).put("module_id", 2)
        }
        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        order.verify(database, Mockito.times(2)).insert(Mockito.eq("reporting"), Mockito.isNull(), Mockito.any())
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testInsertReportingMessagesLogsAFailureAndEndsTheTransaction() {
        val broken = Mockito.mock(JsonReportingMessage::class.java)
        Mockito.`when`(broken.toJson()).thenThrow(IllegalStateException("broken"))

        Mockito.mockConstruction(ContentValues::class.java).use {
            manager.insertReportingMessages(listOf(broken), 7L)
        }

        Mockito.verify(database, Mockito.never()).setTransactionSuccessful()
        Mockito.verify(database).endTransaction()
        logger.verify { Logger.verbose("Error inserting reporting message: java.lang.IllegalStateException: broken") }
    }

    @Test
    fun testInsertReportingMessagesWithoutAListLogsAndEndsTheTransaction() {
        manager.insertReportingMessages(null, 7L)

        Mockito.verify(database, Mockito.never()).setTransactionSuccessful()
        Mockito.verify(database).endTransaction()
    }

    @Test
    fun testCleanupUploadMessagesDeletesOversizedUploads() {
        manager.cleanupUploadMessages()

        Mockito.verify(database).delete("uploads", "length(message) > 204800", null)
    }

    @Test
    fun testGetReadyUploadsReadsEachStoredUpload() {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("cfuuid")).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("upload_settings")).thenReturn(3)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getInt(0)).thenReturn(4)
        Mockito.`when`(cursor.getString(1)).thenReturn("{\"dt\":\"al\"}")
        Mockito.`when`(cursor.getString(2)).thenReturn("1")
        Mockito.`when`(cursor.getString(3)).thenReturn(uploadSettings().toJson())
        stubQuery("uploads", cursor)

        val uploads = manager.getReadyUploads()

        Assert.assertEquals(1, uploads.size)
        Assert.assertEquals(4, uploads[0].id)
        Assert.assertTrue(uploads[0].isAliasRequest)
        Assert.assertEquals("{\"dt\":\"al\"}", uploads[0].message)
        Assert.assertEquals("apiKey", uploads[0].uploadSettings!!.apiKey)
    }

    @Test
    fun testDeleteUploadReturnsTheDeletedRowCount() {
        Mockito.`when`(database.delete("uploads", "_id=?", arrayOf<String?>("4"))).thenReturn(1)

        Assert.assertEquals(1, manager.deleteUpload(4))
    }

    @Test
    fun testInsertAliasRequestStoresAnAliasUpload() {
        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            manager.insertAliasRequest(JSONObject().put("dt", "al"), uploadSettings())

            val values = contentValues.constructed()[0]
            Mockito.verify(values).put("api_key", "apiKey")
            Mockito.verify(values).put("message", "{\"dt\":\"al\"}")
            Mockito.verify(values).put("cfuuid", "1")
            Mockito.verify(database).insert("uploads", null, values)
        }
    }

    @Test
    fun testGetUserAttributeSinglesTypesTheStoredValues() {
        stubSingles(arrayOf("age", "name", "score"), arrayOf("30", "Ann", "1.5"))

        Assert.assertEquals(mapOf("age" to 30, "name" to "Ann", "score" to 1.5), manager.getUserAttributeSingles(7L))
    }

    @Test
    fun testGetUserAttributeListsReturnsTheStoredLists() {
        stubLists(arrayOf("colors", "colors"), arrayOf("red", "blue"))

        Assert.assertEquals(mapOf("colors" to listOf("red", "blue")), manager.getUserAttributeLists(7L))
    }

    @Test
    fun testUserAttributeReadsWithoutADatabaseReturnNullOrEmpty() {
        val noDatabase = managerOn(null)

        Assert.assertNull(noDatabase.getUserAttributeSingles(7L))
        Assert.assertNull(noDatabase.getUserAttributeLists(7L))
        Assert.assertTrue(noDatabase.getUserAttributes(7L).isEmpty())
        Assert.assertEquals("{}", noDatabase.getAllUserAttributesJson(7L).toString())
    }

    @Test
    fun testGetUserAttributesMergesSinglesAndLists() {
        stubSingles(arrayOf("age"), arrayOf("30"))
        stubLists(arrayOf("colors"), arrayOf("red"))

        Assert.assertEquals(mapOf("age" to 30, "colors" to listOf("red")), manager.getUserAttributes(7L))
    }

    @Test
    fun testGetUserAttributesOffTheMainThreadCallsTheListenerAtOnce() {
        stubSingles(arrayOf("age"), arrayOf("30"))
        val received = ArrayList<Any?>()
        Mockito.mockStatic(Looper::class.java).use { looper ->
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(Mockito.mock(Looper::class.java))

            val attributes = manager.getUserAttributes(listenerInto(received), 7L)

            Assert.assertEquals(mapOf("age" to 30), attributes)
        }
        Assert.assertEquals(listOf<Any?>(mapOf("age" to 30), emptyMap<String, Any>(), 7L), received)
    }

    @Test
    fun testGetUserAttributesOnTheMainThreadReadsOnTheMessageHandlerAndAnswersOnTheMainThread() {
        stubSingles(arrayOf("age"), arrayOf("30"))
        val messageHandler = mockMParticleMessageHandler()
        val received = ArrayList<Any?>()
        Mockito.mockStatic(Looper::class.java).use { looper ->
            val mainLooper = Mockito.mock(Looper::class.java)
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            looper.`when`<Looper> { Looper.myLooper() }.thenReturn(mainLooper)

            Assert.assertNull(manager.getUserAttributes(listenerInto(received), 7L))
            Assert.assertTrue(received.isEmpty())
            val work = ArgumentCaptor.forClass(Runnable::class.java)
            Mockito.verify(messageHandler).post(work.capture())
            val mainHandlers =
                Mockito.mockConstruction(Handler::class.java) { mainHandler, _ ->
                    Mockito.`when`(mainHandler.post(Mockito.any())).thenAnswer { invocation ->
                        (invocation.arguments[0] as Runnable).run()
                        true
                    }
                }
            mainHandlers.use { work.value.run() }
        }
        Assert.assertEquals(listOf<Any?>(mapOf("age" to 30), emptyMap<String, Any>(), 7L), received)
    }

    @Test
    fun testGetUserAttributesOnTheMainThreadWithoutAnInstanceReturnsNull() {
        val received = ArrayList<Any?>()
        Mockito.mockStatic(Looper::class.java).use { looper ->
            val mainLooper = Mockito.mock(Looper::class.java)
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mainLooper)
            looper.`when`<Looper> { Looper.myLooper() }.thenReturn(mainLooper)

            Assert.assertNull(manager.getUserAttributes(listenerInto(received), 7L))
        }
        Assert.assertTrue(received.isEmpty())
    }

    @Test
    fun testGetAllUserAttributesJsonWritesSinglesAsTextAndListsAsArrays() {
        stubSingles(arrayOf("age", "nick"), arrayOf("30", null))
        stubLists(arrayOf("colors", "colors"), arrayOf("red", "blue"))

        val json = manager.getAllUserAttributesJson(7L)

        Assert.assertEquals("30", json.get("age"))
        Assert.assertEquals("null", json.get("nick"))
        Assert.assertEquals("[\"red\",\"blue\"]", json.getJSONArray("colors").toString())
    }

    @Test
    fun testSetUserAttributeWithoutADatabaseChangesNothing() {
        val response = MParticleDBManager.UserAttributeResponse()
        response.attributeSingles = mutableMapOf<String, Any?>("age" to 30)

        Assert.assertTrue(managerOn(null).setUserAttribute(response).isEmpty())
    }

    @Test
    fun testSetUserAttributeStoresNewValuesAndReportsEachChange() {
        val response = MParticleDBManager.UserAttributeResponse()
        response.attributeLists = mutableMapOf("colors" to mutableListOf("red", "blue"))
        response.attributeSingles = mutableMapOf<String, Any?>("age" to 30)
        response.time = 50L
        response.mpId = 7L

        Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
            val changes = manager.setUserAttribute(response)

            Assert.assertEquals(2, changes.size)
            assertChange(changes[0], "colors", listOf("red", "blue"), null, true)
            assertChange(changes[1], "age", "30", null, true)
            val values = contentValues.constructed()
            Assert.assertEquals(3, values.size)
            Mockito.verify(values[0]).put("attribute_value", "red")
            Mockito.verify(values[0]).put("is_list", true)
            Mockito.verify(values[1]).put("attribute_value", "blue")
            Mockito.verify(values[2]).put("attribute_value", "30")
            Mockito.verify(values[2]).put("is_list", false)
        }
        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        order.verify(database).delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("colors", "7"))
        order.verify(database).delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("age", "7"))
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testSetUserAttributeSkipsUnchangedValuesAndReportsTheOldValue() {
        stubSingles(arrayOf("name", "city"), arrayOf("ann", "Paris"))
        stubLists(arrayOf("colors", "colors"), arrayOf("red", "blue"))
        Mockito.`when`(database.delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("city", "7"))).thenReturn(1)
        val response = MParticleDBManager.UserAttributeResponse()
        response.attributeLists = mutableMapOf("colors" to mutableListOf("red", "blue"))
        response.attributeSingles = mutableMapOf<String, Any?>("name" to "ANN", "city" to "Rome")
        response.time = 50L
        response.mpId = 7L

        Mockito.mockConstruction(ContentValues::class.java).use {
            val changes = manager.setUserAttribute(response)

            Assert.assertEquals(1, changes.size)
            assertChange(changes[0], "city", "Rome", "Paris", false)
        }
        Mockito.verify(database, Mockito.never()).delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("colors", "7"))
        Mockito.verify(database, Mockito.never()).delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("name", "7"))
    }

    @Test
    fun testSetUserAttributeLogsAFailureAndReturnsTheChangesSoFar() {
        val failure = IllegalStateException("disk full")
        Mockito.`when`(database.delete(Mockito.eq("attributes"), Mockito.any(), Mockito.any())).thenThrow(failure)
        val response = MParticleDBManager.UserAttributeResponse()
        response.attributeSingles = mutableMapOf<String, Any?>("age" to 30)

        Assert.assertTrue(manager.setUserAttribute(response).isEmpty())

        Mockito.verify(database, Mockito.never()).setTransactionSuccessful()
        Mockito.verify(database).endTransaction()
        logger.verify { Logger.error(failure, "Error while adding user attributes: ", failure.toString()) }
    }

    @Test
    fun testRemoveUserAttributeReportsARemovedAttribute() {
        stubSingles(arrayOf("color"), arrayOf("blue"))
        Mockito.`when`(database.delete("attributes", "attribute_key = ? and mp_id = ?", arrayOf<String?>("color", "7"))).thenReturn(1)
        val callbacks = Mockito.mock(MessageManagerCallbacks::class.java)

        manager.removeUserAttribute(removal("color"), callbacks)

        Mockito.verify(callbacks).attributeRemoved("color", 7L)
        Mockito.verify(callbacks).logUserAttributeChangeMessage("color", null, "blue", true, false, 50L, 7L)
        Mockito.verify(database).setTransactionSuccessful()
        Mockito.verify(database).endTransaction()
    }

    @Test
    fun testRemoveUserAttributeOfAMissingAttributeReportsNothing() {
        val callbacks = Mockito.mock(MessageManagerCallbacks::class.java)

        manager.removeUserAttribute(removal("color"), callbacks)
        manager.removeUserAttribute(removal("color"), null)

        Mockito.verifyNoInteractions(callbacks)
        Mockito.verify(database, Mockito.times(2)).setTransactionSuccessful()
    }

    @Test
    fun testResetDatabaseForWorkspaceSwitchingEmptiesEveryTableInOneTransaction() {
        manager.resetDatabaseForWorkspaceSwitching()

        val order = Mockito.inOrder(database)
        order.verify(database).beginTransaction()
        for (table in listOf("breadcrumbs", "messages", "reporting", "sessions", "attributes")) {
            order.verify(database).delete(table, null, null)
        }
        order.verify(database).setTransactionSuccessful()
        order.verify(database).endTransaction()
    }

    @Test
    fun testResetDatabaseForWorkspaceSwitchingSwallowsAFailure() {
        Mockito.`when`(database.delete("messages", null, null)).thenThrow(IllegalStateException("disk full"))

        manager.resetDatabaseForWorkspaceSwitching()

        Mockito.verify(database, Mockito.never()).setTransactionSuccessful()
        Mockito.verify(database).endTransaction()
    }

    @Test
    fun testNestedValueClassesKeepTheirValues() {
        val change = MParticleDBManager.AttributionChange("key", "new", "old", true, false, 5L, 7L)
        Assert.assertEquals("key", change.key)
        Assert.assertEquals("new", change.newValue)
        Assert.assertEquals("old", change.oldValue)
        Assert.assertTrue(change.isDeleted)
        Assert.assertFalse(change.isNewAttribute)
        Assert.assertEquals(5L, change.time)
        Assert.assertEquals(7L, change.mpId)
        val settings = uploadSettings()
        val upload = MParticleDBManager.ReadyUpload(4, false, "{}", settings)
        Assert.assertEquals(4, upload.id)
        Assert.assertFalse(upload.isAliasRequest)
        Assert.assertEquals("{}", upload.message)
        Assert.assertSame(settings, upload.uploadSettings)
        val removal = MParticleDBManager.UserAttributeRemoval()
        Assert.assertNull(removal.key)
        Assert.assertEquals(0L, removal.time)
        Assert.assertEquals(0L, removal.mpId)
        val response = MParticleDBManager.UserAttributeResponse()
        Assert.assertNull(response.attributeSingles)
        Assert.assertNull(response.attributeLists)
        Assert.assertEquals(0L, response.time)
        Assert.assertEquals(0L, response.mpId)
    }

    private fun managerOn(db: MPDatabase?, options: MParticleOptions? = null): MParticleDBManager = object : MParticleDBManager(context, options) {
        override fun getDatabase(): MPDatabase? = db
    }

    private fun stubQuery(table: String?, cursor: Cursor) {
        val tableMatcher: String? = if (table == null) Mockito.any() else Mockito.eq(table)
        Mockito
            .`when`(database.query(tableMatcher, Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(cursor)
        val limitedTableMatcher: String? = if (table == null) Mockito.any() else Mockito.eq(table)
        Mockito
            .`when`(database.query(limitedTableMatcher, Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(cursor)
    }

    private fun stubSingles(keys: Array<String>, values: Array<String?>) {
        stubAttributeQuery("is_list != ? and mp_id = ?", keys, values)
    }

    private fun stubLists(keys: Array<String>, values: Array<String?>) {
        stubAttributeQuery("is_list = ? and mp_id = ?", keys, values)
    }

    private fun stubAttributeQuery(selection: String, keys: Array<String>, values: Array<String?>) {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.getColumnIndexOrThrow("attribute_key")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("attribute_value")).thenReturn(1)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, *Array(keys.size - 1) { true }, false)
        Mockito.`when`(cursor.getString(0)).thenReturn(keys[0], *keys.copyOfRange(1, keys.size))
        Mockito.`when`(cursor.getString(1)).thenReturn(values[0], *values.copyOfRange(1, values.size))
        Mockito
            .`when`(database.query(Mockito.eq("attributes"), Mockito.any(), Mockito.eq(selection), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(cursor)
    }

    private fun stubReadyMessage(id: Int, json: String) {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("session_id")).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("mp_id")).thenReturn(3)
        Mockito.`when`(cursor.getColumnIndexOrThrow("dataplan_id")).thenReturn(4)
        Mockito.`when`(cursor.getColumnIndexOrThrow("dataplan_version")).thenReturn(5)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getInt(0)).thenReturn(id)
        Mockito.`when`(cursor.getString(1)).thenReturn(json)
        Mockito.`when`(cursor.getString(2)).thenReturn("session-1")
        Mockito.`when`(cursor.getLong(3)).thenReturn(7L)
        Mockito.`when`(cursor.isNull(5)).thenReturn(true)
        stubQuery("messages", cursor)
    }

    private fun stubReportingMessage(id: Int) {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.getColumnIndexOrThrow("_id")).thenReturn(0)
        Mockito.`when`(cursor.getColumnIndexOrThrow("message")).thenReturn(1)
        Mockito.`when`(cursor.getColumnIndexOrThrow("session_id")).thenReturn(2)
        Mockito.`when`(cursor.getColumnIndexOrThrow("mp_id")).thenReturn(3)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getInt(0)).thenReturn(id)
        Mockito.`when`(cursor.getString(1)).thenReturn("{\"dt\":\"x\"}")
        Mockito.`when`(cursor.getString(2)).thenReturn("session-1")
        Mockito.`when`(cursor.getLong(3)).thenReturn(7L)
        stubQuery("reporting", cursor)
    }

    /**
     * Runs createMessagesForUploadMessage for session-current and returns the batch it stored, or null.
     */
    private fun createUploads(target: MParticleDBManager): JSONObject? {
        val configManager = Mockito.mock(ConfigManager::class.java)
        Mockito.`when`(configManager.getUserIdentityJson(7L)).thenReturn(JSONArray("[{\"i\":1}]"))
        val deviceAttributes = Mockito.mock(DeviceAttributes::class.java)
        Mockito.`when`(deviceAttributes.getAppInfo(context)).thenReturn(JSONObject("{\"app\":1}"))
        Mockito.`when`(deviceAttributes.getDeviceInfo(context)).thenReturn(JSONObject("{\"device\":1}"))
        // MessageBatch.create runs for real: Kotlin calls it through its companion object, which a static mock does not reach.
        Mockito.`when`(configManager.getUserStorage(Mockito.anyLong())).thenReturn(Mockito.mock(UserStorage::class.java))
        val storedMessages = ArrayList<String>()
        Mockito.mockStatic(TextUtils::class.java).use { textUtils ->
            textUtils
                .`when`<String> { TextUtils.join(Mockito.any(), Mockito.any<Array<Any>>()) }
                .thenAnswer { invocation -> (invocation.arguments[1] as Array<*>).joinToString(invocation.arguments[0] as CharSequence) }
            val contentValues =
                Mockito.mockConstruction(ContentValues::class.java) { values, _ ->
                    Mockito
                        .doAnswer { invocation -> storedMessages.add(invocation.arguments[1] as String) }
                        .`when`(values)
                        .put(Mockito.eq("message"), Mockito.anyString())
                }
            contentValues.use {
                target.createMessagesForUploadMessage(configManager, deviceAttributes, "session-current", uploadSettings())
            }
        }
        return storedMessages.singleOrNull()?.let { JSONObject(it) }
    }

    private fun message(mpId: Long): BaseMPMessage {
        val message = Mockito.mock(BaseMPMessage::class.java)
        Mockito.`when`(message.mpId).thenReturn(mpId)
        return message
    }

    private fun reportingMessage(moduleId: Int): JsonReportingMessage {
        val message = Mockito.mock(JsonReportingMessage::class.java)
        Mockito.`when`(message.moduleId).thenReturn(moduleId)
        Mockito.`when`(message.toJson()).thenReturn(JSONObject())
        return message
    }

    private fun removal(key: String): MParticleDBManager.UserAttributeRemoval {
        val removal = MParticleDBManager.UserAttributeRemoval()
        removal.key = key
        removal.time = 50L
        removal.mpId = 7L
        return removal
    }

    private fun uploadSettings(): UploadSettings = UploadSettings("apiKey", "secret", NetworkOptions.builder().build(), "", "")

    private fun listenerInto(received: MutableList<Any?>): UserAttributeListenerWrapper = UserAttributeListenerWrapper(
        object : TypedUserAttributeListener {
            override fun onUserAttributesReceived(
                userAttributes: Map<String, Any?>,
                userAttributeLists: Map<String, List<String?>?>,
                mpid: Long,
            ) {
                received.add(userAttributes)
                received.add(userAttributeLists)
                received.add(mpid)
            }
        },
    )

    private fun mockMParticleMessageHandler(): Handler {
        val instance = Mockito.mock(MParticle::class.java)
        val internal = Mockito.mock(MParticle.Internal::class.java)
        val messageManager = Mockito.mock(MessageManager::class.java)
        val messageHandler = Mockito.mock(Handler::class.java)
        Mockito.`when`(instance.Internal()).thenReturn(internal)
        Mockito.`when`(internal.messageManager).thenReturn(messageManager)
        Mockito.`when`(messageManager.messageHandler).thenReturn(messageHandler)
        MParticle.setInstance(instance)
        return messageHandler
    }

    private fun assertChange(change: MParticleDBManager.AttributionChange, key: String, newValue: Any?, oldValue: Any?, isNew: Boolean) {
        Assert.assertEquals(key, change.key)
        Assert.assertEquals(newValue, change.newValue)
        Assert.assertEquals(oldValue, change.oldValue)
        Assert.assertEquals(isNew, change.isNewAttribute)
        Assert.assertFalse(change.isDeleted)
        Assert.assertEquals(50L, change.time)
        Assert.assertEquals(7L, change.mpId)
    }
}
