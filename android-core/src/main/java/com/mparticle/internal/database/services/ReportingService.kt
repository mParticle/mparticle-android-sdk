package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.mparticle.internal.Constants
import com.mparticle.internal.JsonReportingMessage
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.tables.ReportingTable
import com.mparticle.internal.database.tables.ReportingTable.ReportingTableColumns
import com.mparticle.internal.listeners.InternalListenerManager
import org.json.JSONException
import org.json.JSONObject

open class ReportingService : ReportingTable() {
    open class ReportingMessage internal constructor(
        open val msgObject: JSONObject,
        open val sessionId: String?,
        open val reportingMessageId: Int,
        open val mpid: Long?,
    )

    companion object {
        @JvmStatic
        fun insertReportingMessage(db: MPDatabase?, message: JsonReportingMessage?, mpId: Long) {
            val values = ContentValues()
            values.put(ReportingTableColumns.MP_ID, mpId)
            values.put(ReportingTableColumns.CREATED_AT, message!!.timestamp)
            values.put(ReportingTableColumns.MODULE_ID, message.moduleId)
            values.put(ReportingTableColumns.MESSAGE, message.toJson().toString())
            values.put(ReportingTableColumns.SESSION_ID, message.sessionId)
            db!!.insert(ReportingTableColumns.TABLE_NAME, null, values)
        }

        /**
         * Will return all ReportingMessages, except for those with MP_ID == Constants.TEMPORARY_MPID,
         * useful in non-testing context.
         */
        @JvmStatic
        @Throws(JSONException::class)
        fun getReportingMessagesForUpload(database: MPDatabase?): List<ReportingMessage> =
            getReportingMessagesForUpload(database, false, Constants.TEMPORARY_MPID)

        @Throws(JSONException::class)
        internal fun getReportingMessagesForUpload(database: MPDatabase?, include: Boolean, mpid: Long): List<ReportingMessage> {
            val reportingMessages: MutableList<ReportingMessage> = ArrayList()
            var reportingMessageCursor: Cursor? = null
            try {
                reportingMessageCursor =
                    database!!.query(
                        ReportingTableColumns.TABLE_NAME,
                        null,
                        ReportingTableColumns.MP_ID + (if (include) " = ?" else " != ?"),
                        arrayOf<String?>(mpid.toString()),
                        null,
                        null,
                        "${BaseColumns._ID} asc",
                    )
                val reportingMessageIdIndex = reportingMessageCursor.getColumnIndexOrThrow(BaseColumns._ID)
                while (reportingMessageCursor.moveToNext()) {
                    val msgObject =
                        JSONObject(
                            reportingMessageCursor.getString(
                                reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns.MESSAGE),
                            ),
                        )
                    val sessionId =
                        reportingMessageCursor.getString(
                            reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns.SESSION_ID),
                        )
                    val reportingMessageId = reportingMessageCursor.getInt(reportingMessageIdIndex)
                    val reportingMessageMpid =
                        reportingMessageCursor.getLong(reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns.MP_ID))
                    val reportingMessage = ReportingMessage(msgObject, sessionId, reportingMessageId, reportingMessageMpid)
                    InternalListenerManager.listener.onCompositeObjects(reportingMessageCursor, reportingMessage)
                    reportingMessages.add(reportingMessage)
                }
            } finally {
                if (reportingMessageCursor != null && !reportingMessageCursor.isClosed) {
                    reportingMessageCursor.close()
                }
            }
            return reportingMessages
        }

        /**
         * Delete reporting messages after they've been included in an upload message.
         */
        @JvmStatic
        fun deleteReportingMessage(database: MPDatabase?, messageId: Int) {
            val whereArgs = arrayOf<String?>(messageId.toString())
            val whereClause = "_id =?"
            database!!.delete(ReportingTableColumns.TABLE_NAME, whereClause, whereArgs)
        }

        @JvmStatic
        fun deleteAll(db: MPDatabase?) {
            db!!.delete(ReportingTableColumns.TABLE_NAME, null, null)
        }
    }
}
