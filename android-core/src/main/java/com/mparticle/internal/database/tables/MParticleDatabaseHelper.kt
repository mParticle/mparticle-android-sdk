package com.mparticle.internal.database.tables

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import org.json.JSONException
import org.json.JSONObject

open class MParticleDatabaseHelper(context: Context?) : SQLiteOpenHelperWrapper {
    private val mContext: Context? = context

    override fun onCreate(database: SQLiteDatabase) {
        database.execSQL(SessionTable.CREATE_SESSIONS_DDL)
        database.execSQL(MessageTable.CREATE_MESSAGES_DDL)
        database.execSQL(UploadTable.CREATE_UPLOADS_DDL)
        database.execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL)
        database.execSQL(ReportingTable.CREATE_REPORTING_DDL)
        database.execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL(SessionTable.CREATE_SESSIONS_DDL)
        database.execSQL(MessageTable.CREATE_MESSAGES_DDL)
        database.execSQL(UploadTable.CREATE_UPLOADS_DDL)
        database.execSQL(BreadcrumbTable.CREATE_BREADCRUMBS_DDL)
        database.execSQL(ReportingTable.CREATE_REPORTING_DDL)
        database.execSQL(UserAttributesTable.CREATE_USER_ATTRIBUTES_DDL)
        try {
            if (oldVersion < 5) {
                upgradeUserAttributes(database)
            }
            if (oldVersion < 6) {
                upgradeSessionTable(database)
                upgradeReportingTable(database)
            }
            if (oldVersion < 7) {
                upgradeMpId(database)
                ConfigManager.setNeedsToMigrate(mContext, true)
            }
            if (oldVersion < 8) {
                removeGcmTable(database)
            }
            if (oldVersion < 9) {
                upgradeMessageTable(database)
            }
            if (oldVersion < 10) {
                upgradeUploadsTable(database)
            }
        } catch (e: Exception) {
            Logger.warning(
                "Exception while upgrading SQLite Database:\n${e.message}\nThis may have been caused by the database having been already upgraded",
            )
        }
    }

    override fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        database.execSQL("DROP TABLE IF EXISTS ${SessionTable.SessionTableColumns.TABLE_NAME}")
        database.execSQL("DROP TABLE IF EXISTS ${MessageTable.MessageTableColumns.TABLE_NAME}")
        database.execSQL("DROP TABLE IF EXISTS ${UploadTable.UploadTableColumns.TABLE_NAME}")
        database.execSQL("DROP TABLE IF EXISTS ${BreadcrumbTable.BreadcrumbTableColumns.TABLE_NAME}")
        database.execSQL("DROP TABLE IF EXISTS ${ReportingTable.ReportingTableColumns.TABLE_NAME}")
        database.execSQL("DROP TABLE IF EXISTS ${UserAttributesTable.UserAttributesTableColumns.TABLE_NAME}")
        onCreate(database)
    }

    private fun upgradeSessionTable(db: SQLiteDatabase) {
        db.execSQL(SessionTable.SESSION_ADD_APP_INFO_COLUMN)
        db.execSQL(SessionTable.SESSION_ADD_DEVICE_INFO_COLUMN)
    }

    private fun upgradeReportingTable(db: SQLiteDatabase) {
        db.execSQL(ReportingTable.REPORTING_ADD_SESSION_ID_COLUMN)
    }

    private fun upgradeMessageTable(db: SQLiteDatabase) {
        db.execSQL(MessageTable.ADD_DATAPLAN_ID_COLUMN)
        db.execSQL(MessageTable.ADD_DATAPLAN_VERSION_COLUMN)
    }

    private fun upgradeMpId(db: SQLiteDatabase) {
        val currentMpId = ConfigManager.getMpid(mContext).toString()
        val updateStatement = "ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT '%s'"
        val tableNames =
            arrayOf(
                ReportingTable.ReportingTableColumns.TABLE_NAME,
                SessionTable.SessionTableColumns.TABLE_NAME,
                UserAttributesTable.UserAttributesTableColumns.TABLE_NAME,
                BreadcrumbTable.BreadcrumbTableColumns.TABLE_NAME,
                MessageTable.MessageTableColumns.TABLE_NAME,
            )
        for (tableName in tableNames) {
            val statement = db.compileStatement(String.format(updateStatement, tableName, MpIdDependentTable.MP_ID, currentMpId))
            statement.execute()
        }
    }

    private fun upgradeUserAttributes(db: SQLiteDatabase) {
        val sharedPreferences = mContext!!.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)

        val userAttrs = sharedPreferences.getString(Constants.PrefKeys.DEPRECATED_USER_ATTRS + ConfigManager.getInstance(mContext).apiKey, null)
        try {
            val userAttributes = JSONObject(userAttrs!!)
            val iter = userAttributes.keys()
            val time = System.currentTimeMillis().toDouble()
            while (iter.hasNext()) {
                val key = iter.next()
                try {
                    val value: Any? = userAttributes.get(key)
                    var stringValue: String? = null
                    if (value != null) {
                        stringValue = value.toString()
                    }
                    val values = ContentValues()
                    values.put(UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_KEY, key)
                    values.put(UserAttributesTable.UserAttributesTableColumns.ATTRIBUTE_VALUE, stringValue)
                    values.put(UserAttributesTable.UserAttributesTableColumns.IS_LIST, false)
                    values.put(UserAttributesTable.UserAttributesTableColumns.CREATED_AT, time)
                    db.insert(UserAttributesTable.UserAttributesTableColumns.TABLE_NAME, null, values)
                } catch (e: JSONException) {
                }
            }
        } catch (e: Exception) {
        } finally {
            sharedPreferences.edit().remove(Constants.PrefKeys.DEPRECATED_USER_ATTRS + ConfigManager.getInstance(mContext).apiKey).apply()
        }
    }

    private fun removeGcmTable(db: SQLiteDatabase) {
        try {
            db.execSQL("DROP TABLE IF EXISTS gcm_messages")
        } catch (e: Exception) {
            Logger.error(e)
        }
    }

    private fun upgradeUploadsTable(db: SQLiteDatabase) {
        db.execSQL(UploadTable.UPLOAD_ADD_UPLOAD_SETTINGS_COLUMN)

        // Insert current upload settings
        val uploadSettings = ConfigManager.getInstance(mContext).uploadSettings
        val values = ContentValues()
        values.put(UploadTable.UploadTableColumns.UPLOAD_SETTINGS, uploadSettings.toJson())
        db.update(UploadTable.UploadTableColumns.TABLE_NAME, values, null, null)
    }

    companion object {
        const val DB_VERSION: Int = 10
        private var databaseName: String? = "mparticle.db"

        @JvmStatic
        fun getDbName(): String? = databaseName

        @JvmStatic
        @VisibleForTesting
        fun setDbName(name: String?) {
            databaseName = name
        }
    }
}
