package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import com.mparticle.internal.BatchId
import com.mparticle.internal.Constants
import com.mparticle.internal.MessageBatch
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.tables.SessionTable
import com.mparticle.internal.database.tables.SessionTable.SessionTableColumns
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONException
import org.json.JSONObject

open class SessionService : SessionTable() {
    companion object {
        @JvmField
        var readyMessages: Array<String>? = arrayOf(Constants.Status.UPLOADED.toString())

        @JvmStatic
        fun deleteSessions(database: MPDatabase?, currentSessionId: String?): Int {
            val selectionArgs = arrayOf<String?>(currentSessionId)
            return database!!.delete(SessionTableColumns.TABLE_NAME, "${SessionTableColumns.SESSION_ID}!=? ", selectionArgs)
        }

        /**
         * Delete sessions whose [SessionTableColumns.END_TIME] is strictly less than
         * [cutoffMillis].
         *
         * @param database     the session database
         * @param cutoffMillis the unix-epoch millisecond cutoff; sessions that ended before this are
         *                     removed
         * @return the number of rows deleted
         */
        @JvmStatic
        fun deleteSessionsOlderThan(database: MPDatabase?, cutoffMillis: Long): Int {
            val whereArgs = arrayOf<String?>(cutoffMillis.toString())
            return database!!.delete(
                SessionTableColumns.TABLE_NAME,
                "${SessionTableColumns.END_TIME} < ?",
                whereArgs,
            )
        }

        /**
         * delete Session entries with session_id that are not a part of the Set
         *
         * @param database
         * @param exceptSessionIds the session_id value for message that SHOULD NOT be deleted
         * @return the number of Session entries deleted
         */
        @JvmStatic
        fun deleteSessions(database: MPDatabase?, exceptSessionIds: Set<String?>?): Int {
            val idsWrapped: MutableList<String> = ArrayList()
            for (id in exceptSessionIds!!) {
                idsWrapped.add("'$id'")
            }
            val idArgString = TextUtils.join(",", idsWrapped.toTypedArray())
            return database!!.delete(SessionTableColumns.TABLE_NAME, "${SessionTableColumns.SESSION_ID} NOT IN ($idArgString)", null)
        }

        @JvmStatic
        fun getSessions(db: MPDatabase?): Cursor = db!!.query(SessionTableColumns.TABLE_NAME, null, null, null, null, null, null)

        @JvmStatic
        fun updateSessionEndTime(db: MPDatabase?, sessionId: String?, endTime: Long, sessionLength: Long) {
            val sessionValues = ContentValues()
            sessionValues.put(SessionTableColumns.END_TIME, endTime)
            if (sessionLength > 0) {
                sessionValues.put(SessionTableColumns.SESSION_FOREGROUND_LENGTH, sessionLength)
            }
            val whereArgs = arrayOf<String?>(sessionId)
            db!!.update(SessionTableColumns.TABLE_NAME, sessionValues, "${SessionTableColumns.SESSION_ID}=?", whereArgs)
        }

        @JvmStatic
        fun updateSessionAttributes(db: MPDatabase?, sessionId: String?, attributes: String?) {
            val sessionValues = ContentValues()
            sessionValues.put(SessionTableColumns.ATTRIBUTES, attributes)
            val whereArgs = arrayOf<String?>(sessionId)
            db!!.update(SessionTableColumns.TABLE_NAME, sessionValues, "${SessionTableColumns.SESSION_ID}=?", whereArgs)
        }

        @JvmStatic
        fun updateSessionStatus(db: MPDatabase?, sessionId: String?, status: String?) {
            val contentValues = ContentValues()
            contentValues.put(SessionTableColumns.STATUS, status)
            val whereArgs = arrayOf<String?>(sessionId)
            db!!.update(SessionTableColumns.TABLE_NAME, contentValues, "${SessionTableColumns.SESSION_ID}=?", whereArgs)
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun getSessionForSessionEndMessage(db: MPDatabase?, sessionId: String?): Cursor {
            val selectionArgs = arrayOf<String?>(sessionId)
            val sessionColumns =
                arrayOf<String?>(
                    SessionTableColumns.START_TIME,
                    SessionTableColumns.END_TIME,
                    SessionTableColumns.SESSION_FOREGROUND_LENGTH,
                    SessionTableColumns.ATTRIBUTES,
                )
            val selectCursor =
                db!!.query(
                    SessionTableColumns.TABLE_NAME,
                    sessionColumns,
                    "${SessionTableColumns.SESSION_ID}=? and ${SessionTableColumns.STATUS} IS NULL",
                    selectionArgs,
                    null,
                    null,
                    null,
                )
            return selectCursor
        }

        @JvmStatic
        fun getOrphanSessionIds(db: MPDatabase?, apiKey: String?): MutableList<String> {
            val sessionIds: MutableList<String> = ArrayList()
            val selectionArgs = arrayOf<String?>(apiKey)
            val sessionColumns = arrayOf<String?>(SessionTableColumns.SESSION_ID)
            var selectCursor: Cursor? = null
            try {
                selectCursor =
                    db!!.query(
                        SessionTableColumns.TABLE_NAME,
                        sessionColumns,
                        "${SessionTableColumns.API_KEY}= ? and ${SessionTableColumns.STATUS} IS NULL",
                        selectionArgs,
                        null,
                        null,
                        null,
                    )
                // NOTE: There should be at most one orphan per API key - but
                // process any that are found.
                while (selectCursor.moveToNext()) {
                    sessionIds.add(selectCursor.getString(0))
                }
                return sessionIds
            } finally {
                if (selectCursor != null && !selectCursor.isClosed) {
                    selectCursor.close()
                }
            }
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun insertSession(db: MPDatabase?, message: BaseMPMessage?, apiKey: String?, appInfo: String?, deviceInfo: String?, mpId: Long) {
            val contentValues = ContentValues()
            contentValues.put(SessionTableColumns.MP_ID, mpId)
            contentValues.put(SessionTableColumns.API_KEY, apiKey)
            contentValues.put(SessionTableColumns.SESSION_ID, message!!.sessionId)
            contentValues.put(SessionTableColumns.START_TIME, message.getLong(Constants.MessageKey.TIMESTAMP))
            contentValues.put(SessionTableColumns.END_TIME, message.getLong(Constants.MessageKey.TIMESTAMP))
            contentValues.put(SessionTableColumns.SESSION_FOREGROUND_LENGTH, 0)
            contentValues.put(SessionTableColumns.APP_INFO, appInfo)
            contentValues.put(SessionTableColumns.DEVICE_INFO, deviceInfo)
            db!!.insert(SessionTableColumns.TABLE_NAME, null, contentValues)
        }

        @JvmStatic
        fun processSessions(database: MPDatabase?, uploadMessagesByBatchId: HashMap<BatchId, MessageBatch>?): MutableList<JSONObject> {
            var sessionCursor: Cursor? = null
            val deviceInfos: MutableList<JSONObject> = ArrayList()
            val batchesBySessionId = flattenBySessionId(uploadMessagesByBatchId)
            try {
                sessionCursor = getSessions(database)
                while (sessionCursor.moveToNext()) {
                    val sessionId = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow(SessionTableColumns.SESSION_ID))
                    val batchList = batchesBySessionId[sessionId]
                    if (batchList != null) {
                        try {
                            val appInfo = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow(SessionTableColumns.APP_INFO))
                            val appInfoJson = JSONObject(appInfo)
                            val deviceInfo = sessionCursor.getString(sessionCursor.getColumnIndexOrThrow(SessionTableColumns.DEVICE_INFO))
                            val deviceInfoJson = JSONObject(deviceInfo)
                            deviceInfos.add(deviceInfoJson)
                            for (batch in batchList) {
                                batch.setAppInfo(appInfoJson)
                                batch.setDeviceInfo(deviceInfoJson)
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            } finally {
                if (sessionCursor != null && !sessionCursor.isClosed) {
                    sessionCursor.close()
                }
            }
            return deviceInfos
        }

        @JvmStatic
        fun updateSessionInstallReferrer(db: MPDatabase?, appInfo: JSONObject?, sessionId: String?) {
            val contentValues = ContentValues()
            contentValues.put(SessionTableColumns.APP_INFO, appInfo!!.toString())
            val whereArgs = arrayOf<String?>(sessionId)
            db!!.update(SessionTableColumns.TABLE_NAME, contentValues, "${SessionTableColumns.SESSION_ID}=?", whereArgs)
        }

        internal fun flattenBySessionId(uploadMessagesByBatchId: Map<BatchId, MessageBatch>?): MutableMap<String?, MutableList<MessageBatch>> {
            val uploadMessagesBySessionId: MutableMap<String?, MutableList<MessageBatch>> = HashMap()
            for ((key, value) in uploadMessagesByBatchId!!) {
                val sessionId = key.sessionId
                var messageBatches = uploadMessagesBySessionId[sessionId]
                if (messageBatches == null) {
                    messageBatches = ArrayList()
                    uploadMessagesBySessionId[sessionId] = messageBatches
                }
                messageBatches.add(value)
            }
            return uploadMessagesBySessionId
        }

        @JvmStatic
        fun deleteAll(db: MPDatabase?) {
            db!!.delete(SessionTableColumns.TABLE_NAME, null, null)
        }
    }
}
