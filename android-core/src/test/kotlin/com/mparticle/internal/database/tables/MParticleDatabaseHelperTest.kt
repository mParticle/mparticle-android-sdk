package com.mparticle.internal.database.tables

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.database.UploadSettings
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.mockito.InOrder
import org.mockito.Mockito

class MParticleDatabaseHelperTest {
    private val originalDbName: String? = MParticleDatabaseHelper.getDbName()
    private val database: SQLiteDatabase = Mockito.mock(SQLiteDatabase::class.java)
    private val statement: SQLiteStatement = Mockito.mock(SQLiteStatement::class.java)
    private val context: Context = Mockito.mock(Context::class.java)
    private val preferences: SharedPreferences = Mockito.mock(SharedPreferences::class.java)
    private val editor: SharedPreferences.Editor = Mockito.mock(SharedPreferences.Editor::class.java)
    private val helper = MParticleDatabaseHelper(context)

    @After
    fun restoreDbName() {
        MParticleDatabaseHelper.setDbName(originalDbName)
    }

    @Test
    fun testDefaultDbNameAndVersion() {
        Assert.assertEquals("mparticle.db", originalDbName)
        Assert.assertEquals(10, MParticleDatabaseHelper.DB_VERSION)
    }

    @Test
    fun testSetDbNameReplacesTheNameAndAcceptsNull() {
        MParticleDatabaseHelper.setDbName("characterization.db")
        Assert.assertEquals("characterization.db", MParticleDatabaseHelper.getDbName())

        MParticleDatabaseHelper.setDbName(null)
        Assert.assertNull(MParticleDatabaseHelper.getDbName())
    }

    @Test
    fun testOnCreateCreatesTheSixTablesInOrder() {
        helper.onCreate(database)

        verifyCreateStatements(Mockito.inOrder(database))
        Mockito.verifyNoMoreInteractions(database)
    }

    @Test
    fun testOnDowngradeDropsTheSixTablesThenCreatesThem() {
        helper.onDowngrade(database, MParticleDatabaseHelper.DB_VERSION + 1, MParticleDatabaseHelper.DB_VERSION)

        val inOrder = Mockito.inOrder(database)
        for (table in listOf("sessions", "messages", "uploads", "breadcrumbs", "reporting", "attributes")) {
            inOrder.verify(database).execSQL("DROP TABLE IF EXISTS $table")
        }
        verifyCreateStatements(inOrder)
        Mockito.verifyNoMoreInteractions(database)
    }

    @Test
    fun testOnUpgradeFromTheCurrentVersionOnlyRunsTheCreateStatements() {
        helper.onUpgrade(database, MParticleDatabaseHelper.DB_VERSION, MParticleDatabaseHelper.DB_VERSION)

        verifyCreateStatements(Mockito.inOrder(database))
        Mockito.verifyNoMoreInteractions(database)
    }

    @Test
    fun testOnUpgradeFromVersion8AddsTheDataPlanColumnsAndStoresTheUploadSettings() {
        val configManager = mockConfigManager()
        Mockito.mockStatic(ConfigManager::class.java).use {
            Mockito.`when`(ConfigManager.getInstance(context)).thenReturn(configManager)
            Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
                helper.onUpgrade(database, 8, MParticleDatabaseHelper.DB_VERSION)

                Assert.assertEquals(1, contentValues.constructed().size)
                val uploadValues = contentValues.constructed()[0]
                Mockito.verify(uploadValues).put("upload_settings", UPLOAD_SETTINGS_JSON)
                val inOrder = Mockito.inOrder(database)
                verifyCreateStatements(inOrder)
                inOrder.verify(database).execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN)
                inOrder.verify(database).execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN)
                inOrder.verify(database).execSQL(UploadTable.UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN)
                inOrder.verify(database).update("uploads", uploadValues, null, null)
                Mockito.verifyNoMoreInteractions(database)
            }
        }
    }

    @Test
    fun testOnUpgradeFromVersion4MovesTheStoredUserAttributesAndRunsEveryLaterStep() {
        stubPreferences("{\"color\":\"blue\"}")
        Mockito.`when`(database.compileStatement(Mockito.anyString())).thenReturn(statement)
        val configManager = mockConfigManager()
        Mockito.mockStatic(ConfigManager::class.java).use { configManagerStatics ->
            Mockito.`when`(ConfigManager.getInstance(context)).thenReturn(configManager)
            Mockito.`when`(ConfigManager.getMpid(context)).thenReturn(MPID)
            Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
                helper.onUpgrade(database, 4, MParticleDatabaseHelper.DB_VERSION)

                Assert.assertEquals(2, contentValues.constructed().size)
                val attributeValues = contentValues.constructed()[0]
                Mockito.verify(attributeValues).put("attribute_key", "color")
                Mockito.verify(attributeValues).put("attribute_value", "blue")
                Mockito.verify(attributeValues).put("is_list", false)
                Mockito.verify(attributeValues).put(Mockito.eq("created_time"), Mockito.anyDouble())
                val inOrder = Mockito.inOrder(database, statement)
                verifyCreateStatements(inOrder)
                inOrder.verify(database).insert("attributes", null, attributeValues)
                inOrder.verify(database).execSQL(SessionTable.SESSION_ADD_APP_INFO_COLUMN)
                inOrder.verify(database).execSQL(SessionTable.SESSION_ADD_DEVICE_INFO_COLUMN)
                inOrder.verify(database).execSQL(ReportingTable.REPORTING_ADD_SESSION_ID_COLUMN)
                for (table in listOf("reporting", "sessions", "attributes", "breadcrumbs", "messages")) {
                    inOrder.verify(database).compileStatement("ALTER TABLE $table ADD COLUMN mp_id INTEGER DEFAULT '$MPID'")
                    inOrder.verify(statement).execute()
                }
                inOrder.verify(database).execSQL("DROP TABLE IF EXISTS gcm_messages")
                inOrder.verify(database).execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN)
                inOrder.verify(database).execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN)
                inOrder.verify(database).execSQL(UploadTable.UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN)
                inOrder.verify(database).update("uploads", contentValues.constructed()[1], null, null)
                configManagerStatics.verify { ConfigManager.setNeedsToMigrate(context, true) }
                Mockito.verify(editor).remove("mp::user_attrs::$API_KEY")
                Mockito.verify(editor).apply()
            }
        }
    }

    @Test
    fun testOnUpgradeFromVersion4WithNoStoredUserAttributesStillRunsEveryLaterStep() {
        stubPreferences(null)
        Mockito.`when`(database.compileStatement(Mockito.anyString())).thenReturn(statement)
        val configManager = mockConfigManager()
        Mockito.mockStatic(ConfigManager::class.java).use {
            Mockito.`when`(ConfigManager.getInstance(context)).thenReturn(configManager)
            Mockito.`when`(ConfigManager.getMpid(context)).thenReturn(MPID)
            Mockito.mockConstruction(ContentValues::class.java).use { contentValues ->
                helper.onUpgrade(database, 4, MParticleDatabaseHelper.DB_VERSION)

                Assert.assertEquals(1, contentValues.constructed().size)
                Mockito.verify(database, Mockito.never()).insert(Mockito.anyString(), Mockito.any(), Mockito.any())
                Mockito.verify(database).execSQL(SessionTable.SESSION_ADD_APP_INFO_COLUMN)
                Mockito.verify(database).execSQL(UploadTable.UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN)
                Mockito.verify(database).update("uploads", contentValues.constructed()[0], null, null)
                Mockito.verify(editor).remove("mp::user_attrs::$API_KEY")
                Mockito.verify(editor).apply()
            }
        }
    }

    @Test
    fun testOnUpgradeWithANullContextStopsAtTheUserAttributesStepWithoutThrowing() {
        MParticleDatabaseHelper(null).onUpgrade(database, 4, MParticleDatabaseHelper.DB_VERSION)

        verifyCreateStatements(Mockito.inOrder(database))
        Mockito.verifyNoMoreInteractions(database)
    }

    @Test
    fun testOnUpgradeStopsAtAFailingStepWithoutThrowing() {
        Mockito.doThrow(RuntimeException("duplicate column name")).`when`(database).execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN)

        helper.onUpgrade(database, 8, MParticleDatabaseHelper.DB_VERSION)

        Mockito.verify(database).execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN)
        Mockito.verify(database, Mockito.never()).execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN)
        Mockito.verify(database, Mockito.never()).execSQL(UploadTable.UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN)
    }

    private fun verifyCreateStatements(inOrder: InOrder) {
        inOrder.verify(database).execSQL(SessionTable.CREATE_SESSIONS_DDL)
        inOrder.verify(database).execSQL(MessageTable.CREATE_MESSAGES_DDL)
        inOrder.verify(database).execSQL(UploadTable.CREATE_UPLOADS_DDL)
        inOrder.verify(database).execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL)
        inOrder.verify(database).execSQL(ReportingTable.CREATE_REPORTING_DDL)
        inOrder.verify(database).execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL)
    }

    private fun stubPreferences(storedUserAttributes: String?) {
        Mockito.`when`(context.getSharedPreferences("mParticlePrefs", Context.MODE_PRIVATE)).thenReturn(preferences)
        Mockito.`when`(preferences.getString("mp::user_attrs::$API_KEY", null)).thenReturn(storedUserAttributes)
        Mockito.`when`(preferences.edit()).thenReturn(editor)
        Mockito.`when`(editor.remove(Mockito.anyString())).thenReturn(editor)
    }

    private fun mockConfigManager(): ConfigManager {
        val uploadSettings = Mockito.mock(UploadSettings::class.java)
        Mockito.`when`(uploadSettings.toJson()).thenReturn(UPLOAD_SETTINGS_JSON)
        val configManager = Mockito.mock(ConfigManager::class.java)
        Mockito.`when`(configManager.apiKey).thenReturn(API_KEY)
        Mockito.`when`(configManager.uploadSettings).thenReturn(uploadSettings)
        return configManager
    }

    companion object {
        private const val API_KEY = "key"
        private const val MPID = 42L
        private const val UPLOAD_SETTINGS_JSON = "{\"apiKey\":\"key\"}"
    }
}
