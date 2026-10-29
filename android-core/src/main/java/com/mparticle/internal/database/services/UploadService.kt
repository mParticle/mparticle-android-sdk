package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.UploadSettings
import com.mparticle.internal.database.tables.UploadTable
import com.mparticle.internal.database.tables.UploadTable.UploadTableColumns
import com.mparticle.internal.listeners.InternalListenerManager
import org.json.JSONObject

open class UploadService : UploadTable() {
    companion object {
        @JvmStatic
        fun cleanupUploadMessages(database: MPDatabase?): Int = database!!.delete(
            UploadTableColumns.TABLE_NAME,
            "length(${UploadTableColumns.MESSAGE}) > ${Constants.LIMIT_MAX_UPLOAD_SIZE}",
            null,
        )

        /**
         * Delete uploads whose [UploadTableColumns.CREATED_AT] is strictly less than
         * [cutoffMillis].
         *
         * @param database     the upload database
         * @param cutoffMillis the unix-epoch millisecond cutoff; rows older than this are removed
         * @return the number of rows deleted
         */
        @JvmStatic
        fun deleteUploadsOlderThan(database: MPDatabase?, cutoffMillis: Long): Int {
            val whereArgs = arrayOf<String?>(cutoffMillis.toString())
            return database!!.delete(UploadTableColumns.TABLE_NAME, "${UploadTableColumns.CREATED_AT} < ?", whereArgs)
        }

        /**
         * Generic method to insert a new upload,
         * either a regular message batch, or a session history.
         *
         * @param message
         */
        @JvmStatic
        fun insertUpload(database: MPDatabase?, message: JSONObject?, uploadSettings: UploadSettings?) {
            val contentValues = ContentValues()
            contentValues.put(UploadTableColumns.API_KEY, uploadSettings!!.apiKey)
            contentValues.put(UploadTableColumns.CREATED_AT, message!!.optLong(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis()))
            contentValues.put(UploadTableColumns.MESSAGE, message.toString())
            contentValues.put(UploadTableColumns.REQUEST_TYPE, UploadTable.UPLOAD_REQUEST)
            contentValues.put(UploadTableColumns.UPLOAD_SETTINGS, uploadSettings.toJson())
            InternalListenerManager.listener.onCompositeObjects(message, contentValues)
            database!!.insert(UploadTableColumns.TABLE_NAME, null, contentValues)
        }

        @JvmStatic
        fun getReadyUploads(database: MPDatabase?): List<MParticleDBManager.ReadyUpload> {
            val readyUploads: MutableList<MParticleDBManager.ReadyUpload> = ArrayList()
            var readyUploadsCursor: Cursor? = null
            try {
                readyUploadsCursor =
                    database!!.query(
                        UploadTableColumns.TABLE_NAME,
                        arrayOf<String?>("_id", UploadTableColumns.MESSAGE, UploadTableColumns.REQUEST_TYPE, UploadTableColumns.UPLOAD_SETTINGS),
                        null,
                        null,
                        null,
                        null,
                        UploadTableColumns.CREATED_AT,
                    )
                val messageIdIndex = readyUploadsCursor.getColumnIndexOrThrow(BaseColumns._ID)
                val messageIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns.MESSAGE)
                val requestTypeIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns.REQUEST_TYPE)
                val uploadSettingsIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns.UPLOAD_SETTINGS)
                while (readyUploadsCursor.moveToNext()) {
                    val readyUpload =
                        MParticleDBManager.ReadyUpload(
                            readyUploadsCursor.getInt(messageIdIndex),
                            UploadTable.ALIAS_REQUEST == readyUploadsCursor.getString(requestTypeIndex),
                            readyUploadsCursor.getString(messageIndex),
                            UploadSettings.withJson(readyUploadsCursor.getString(uploadSettingsIndex)),
                        )
                    readyUploads.add(readyUpload)
                    InternalListenerManager.listener.onCompositeObjects(readyUploadsCursor, readyUpload)
                }
            } catch (e: Exception) {
                Logger.error(e, "Failed to get ready uploads")
            } finally {
                if (readyUploadsCursor != null && !readyUploadsCursor.isClosed) {
                    readyUploadsCursor.close()
                }
            }
            return readyUploads
        }

        /**
         * After an actually successful upload over the wire.
         *
         * @param id
         * @return number of rows deleted (should be 1)
         */
        @JvmStatic
        fun deleteUpload(database: MPDatabase?, id: Int): Int {
            val whereArgs = arrayOf<String?>(id.toString())
            return database!!.delete(UploadTableColumns.TABLE_NAME, "_id=?", whereArgs)
        }

        @JvmStatic
        fun insertAliasRequest(database: MPDatabase?, request: JSONObject?, uploadSettings: UploadSettings?): Long {
            val contentValues = ContentValues()
            contentValues.put(UploadTableColumns.API_KEY, uploadSettings!!.apiKey)
            contentValues.put(UploadTableColumns.CREATED_AT, System.currentTimeMillis())
            contentValues.put(UploadTableColumns.MESSAGE, request!!.toString())
            contentValues.put(UploadTableColumns.REQUEST_TYPE, UploadTable.ALIAS_REQUEST)
            contentValues.put(UploadTableColumns.UPLOAD_SETTINGS, uploadSettings.toJson())
            InternalListenerManager.listener.onCompositeObjects(request, contentValues)
            return database!!.insert(UploadTableColumns.TABLE_NAME, null, contentValues)
        }
    }
}
