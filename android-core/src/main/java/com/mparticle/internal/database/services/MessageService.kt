package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import android.provider.BaseColumns
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.tables.MessageTable
import com.mparticle.internal.database.tables.MessageTable.MessageTableColumns
import com.mparticle.internal.listeners.InternalListenerManager
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONException

open class MessageService : MessageTable() {
    open class ReadyMessage internal constructor(
        open val mpid: Long,
        open val sessionId: String?,
        open val messageId: Int,
        open val message: String?,
        open val dataplanId: String?,
        open val dataplanVersion: Int?,
    )

    companion object {
        private val prepareSelection =
            arrayOf<String?>(
                "_id",
                MessageTableColumns.MESSAGE,
                MessageTableColumns.CREATED_AT,
                MessageTableColumns.STATUS,
                MessageTableColumns.SESSION_ID,
                MessageTableColumns.MP_ID,
                MessageTableColumns.DATAPLAN_ID,
                MessageTableColumns.DATAPLAN_VERSION,
            )
        private val prepareOrderBy = "${BaseColumns._ID} asc"

        private fun getSessionHistorySelection(includesMpid: Boolean): String = String.format(
            "(%s = %d) and (%s != ?) and (%s " + (if (includesMpid) " = ?" else "!= ?") + ")",
            MessageTableColumns.STATUS,
            Constants.Status.UPLOADED,
            MessageTableColumns.SESSION_ID,
            MessageTableColumns.MP_ID,
        )

        @JvmStatic
        fun getSessionIds(database: MPDatabase?): MutableSet<String?> {
            val sessionIds: MutableSet<String?> = HashSet()
            var cursor: Cursor? = null
            try {
                val query = "SELECT DISTINCT ${MessageTableColumns.SESSION_ID} FROM ${MessageTableColumns.TABLE_NAME}"
                cursor = database!!.rawQuery(query) // mobsf-ignore: sqlite_injection
                while (cursor.moveToNext()) {
                    sessionIds.add(cursor.getString(0))
                }
                return sessionIds
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
            }
        }

        @JvmStatic
        fun getSessionHistory(database: MPDatabase?, currentSessionId: String?): MutableList<ReadyMessage> =
            getSessionHistory(database, currentSessionId, false, Constants.TEMPORARY_MPID)

        internal fun getSessionHistory(database: MPDatabase?, currentSessionId: String?, includes: Boolean, mpid: Long): MutableList<ReadyMessage> {
            val selectionArgs = arrayOf<String?>(currentSessionId, mpid.toString())
            var readyMessagesCursor: Cursor? = null
            val readyMessages: MutableList<ReadyMessage> = ArrayList()
            try {
                readyMessagesCursor =
                    database!!.query(
                        MessageTableColumns.TABLE_NAME,
                        prepareSelection,
                        getSessionHistorySelection(includes),
                        selectionArgs,
                        null,
                        null,
                        prepareOrderBy,
                        Constants.maxMessagePerBatch.toString(),
                    )
                val messageIdIndex = readyMessagesCursor.getColumnIndexOrThrow(BaseColumns._ID)
                val messageIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.MESSAGE)
                val sessionIdIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.SESSION_ID)
                val messageMpidIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.MP_ID)
                val dataplanIdIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.DATAPLAN_ID)
                val dataplanVersinIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.DATAPLAN_VERSION)
                while (readyMessagesCursor.moveToNext()) {
                    val sessionId = readyMessagesCursor.getString(sessionIdIndex)
                    val messageId = readyMessagesCursor.getInt(messageIdIndex)
                    val message = readyMessagesCursor.getString(messageIndex)
                    val messageMpid = readyMessagesCursor.getLong(messageMpidIndex)
                    val dataplanId = readyMessagesCursor.getString(dataplanIdIndex)
                    var dataplanVersion: Int? = null
                    if (!readyMessagesCursor.isNull(dataplanVersinIndex)) {
                        dataplanVersion = readyMessagesCursor.getInt(dataplanVersinIndex)
                    }
                    val readyMessage = ReadyMessage(messageMpid, sessionId, messageId, message, dataplanId, dataplanVersion)
                    InternalListenerManager.listener.onCompositeObjects(readyMessagesCursor, readyMessage)
                    readyMessages.add(readyMessage)
                }
            } finally {
                if (readyMessagesCursor != null && !readyMessagesCursor.isClosed) {
                    readyMessagesCursor.close()
                }
            }
            return readyMessages
        }

        @JvmStatic
        fun deleteOldMessages(database: MPDatabase?, currentSessionId: String?): Int {
            val selectionArgs = arrayOf<String?>(currentSessionId, Constants.TEMPORARY_MPID.toString())
            return database!!.delete(
                MessageTableColumns.TABLE_NAME,
                getSessionHistorySelection(false),
                selectionArgs,
            )
        }

        /**
         * Delete messages whose [MessageTableColumns.CREATED_AT] is strictly less than
         * [cutoffMillis].
         *
         * @param database     the message database
         * @param cutoffMillis the unix-epoch millisecond cutoff; rows older than this are removed
         * @return the number of rows deleted
         */
        @JvmStatic
        fun deleteMessagesOlderThan(database: MPDatabase?, cutoffMillis: Long): Int {
            val whereArgs = arrayOf<String?>(cutoffMillis.toString())
            return database!!.delete(
                MessageTableColumns.TABLE_NAME,
                "${MessageTableColumns.CREATED_AT} < ?",
                whereArgs,
            )
        }

        @JvmStatic
        fun hasMessagesForUpload(database: MPDatabase?): Boolean {
            var messageIds: Cursor? = null
            try {
                val selection =
                    "${MessageTableColumns.STATUS} != ? and ${MessageTableColumns.CREATED_AT} < ${System.currentTimeMillis()}" +
                        " and ${MessageTableColumns.MP_ID} != ?"
                messageIds =
                    database!!.query(
                        MessageTableColumns.TABLE_NAME,
                        arrayOf<String?>(BaseColumns._ID),
                        selection,
                        arrayOf<String?>(Constants.Status.UPLOADED.toString(), Constants.TEMPORARY_MPID.toString()),
                        null,
                        null,
                        prepareOrderBy,
                    )
                return messageIds.count > 0
            } finally {
                if (messageIds != null && !messageIds.isClosed) {
                    messageIds.close()
                }
            }
        }

        /**
         * Will return all Messages for upload, except for those with MP_ID == Constants.TEMPORARY_MPID,
         * useful in non-testing context.
         */
        @JvmStatic
        fun getMessagesForUpload(database: MPDatabase?): MutableList<ReadyMessage> = getMessagesForUpload(database, false, Constants.TEMPORARY_MPID)

        internal fun getMessagesForUpload(database: MPDatabase?, includes: Boolean, mpid: Long): MutableList<ReadyMessage> {
            var readyMessagesCursor: Cursor? = null
            val readyMessages: MutableList<ReadyMessage> = ArrayList()
            try {
                val selection =
                    "${MessageTableColumns.STATUS} != ? and ${MessageTableColumns.CREATED_AT} < ${System.currentTimeMillis()}" +
                        " and ${MessageTableColumns.MP_ID}" + (if (includes) " = ?" else " != ?")
                readyMessagesCursor =
                    database!!.query(
                        MessageTableColumns.TABLE_NAME,
                        null,
                        selection,
                        arrayOf<String?>(Constants.Status.UPLOADED.toString(), mpid.toString()),
                        null,
                        null,
                        prepareOrderBy,
                        Constants.maxMessagePerBatch.toString(),
                    )
                val messageIdIndex = readyMessagesCursor.getColumnIndexOrThrow(BaseColumns._ID)
                val messageIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.MESSAGE)
                val sessionIdIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.SESSION_ID)
                val messageMpidIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.MP_ID)
                val dataplanIdIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.DATAPLAN_ID)
                val dataplanVersinIndex = readyMessagesCursor.getColumnIndexOrThrow(MessageTableColumns.DATAPLAN_VERSION)
                while (readyMessagesCursor.moveToNext()) {
                    val sessionId = readyMessagesCursor.getString(sessionIdIndex)
                    val messageId = readyMessagesCursor.getInt(messageIdIndex)
                    val message = readyMessagesCursor.getString(messageIndex)
                    val messageMpid = readyMessagesCursor.getLong(messageMpidIndex)
                    val dataplanId = readyMessagesCursor.getString(dataplanIdIndex)
                    var dataplanVersion: Int? = null
                    if (!readyMessagesCursor.isNull(dataplanVersinIndex)) {
                        dataplanVersion = readyMessagesCursor.getInt(dataplanVersinIndex)
                    }
                    val readyMessage = ReadyMessage(messageMpid, sessionId, messageId, message, dataplanId, dataplanVersion)
                    InternalListenerManager.listener.onCompositeObjects(readyMessagesCursor, readyMessage)
                    readyMessages.add(readyMessage)
                }
            } finally {
                if (readyMessagesCursor != null && !readyMessagesCursor.isClosed) {
                    readyMessagesCursor.close()
                }
            }
            return readyMessages
        }

        @JvmStatic
        fun cleanupMessages(database: MPDatabase?): Int =
            database!!.delete(MessageTableColumns.TABLE_NAME, "length(${MessageTableColumns.MESSAGE}) > ${Constants.LIMIT_MAX_MESSAGE_SIZE}", null)

        @JvmStatic
        fun markMessagesAsUploaded(database: MPDatabase?, messageId: Int): Int {
            val whereArgs = arrayOf<String?>(messageId.toString(), Constants.TEMPORARY_MPID.toString())
            val contentValues = ContentValues()
            contentValues.put(MessageTableColumns.STATUS, Constants.Status.UPLOADED)
            return database!!.update(
                MessageTableColumns.TABLE_NAME,
                contentValues,
                "${BaseColumns._ID} <= ? and ${MessageTableColumns.MP_ID} != ? ",
                whereArgs,
            )
        }

        /**
         * Delete a message that has been uploaded in session history.
         */
        @JvmStatic
        fun deleteMessages(database: MPDatabase?, messageId: Int): Int {
            val whereArgs = arrayOf<String?>(messageId.toString(), Constants.TEMPORARY_MPID.toString())
            return database!!.delete(MessageTableColumns.TABLE_NAME, "${BaseColumns._ID} <= ? and ${MessageTableColumns.MP_ID} != ?", whereArgs)
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun insertMessage(db: MPDatabase?, apiKey: String?, message: BaseMPMessage?, mpId: Long, dataplanId: String?, dataplanVersion: Int?) {
            val contentValues = ContentValues()
            contentValues.put(MessageTableColumns.API_KEY, apiKey)
            contentValues.put(MessageTableColumns.CREATED_AT, message!!.getLong(Constants.MessageKey.TIMESTAMP))
            val sessionID = message.sessionId
            contentValues.put(MessageTableColumns.SESSION_ID, sessionID)
            contentValues.put(MessageTableColumns.MP_ID, mpId)
            contentValues.put(MessageTableColumns.DATAPLAN_ID, dataplanId)
            contentValues.put(MessageTableColumns.DATAPLAN_VERSION, dataplanVersion)
            if (Constants.NO_SESSION_ID == sessionID) {
                message.remove(Constants.MessageKey.SESSION_ID)
            }
            val messageString = message.toString()
            if (messageString.length > Constants.LIMIT_MAX_MESSAGE_SIZE) {
                Logger.error(
                    "Message logged of size ${messageString.length} that exceeds maximum safe size of ${Constants.LIMIT_MAX_MESSAGE_SIZE} bytes.",
                )
                return
            }
            contentValues.put(MessageTableColumns.MESSAGE, messageString)

            if (Constants.MessageType.FIRST_RUN == message.getString(Constants.MessageKey.TYPE)) {
                // Force the first run message to be parsed immediately.
                contentValues.put(MessageTableColumns.STATUS, Constants.Status.BATCH_READY)
            } else {
                contentValues.put(MessageTableColumns.STATUS, Constants.Status.READY)
            }
            InternalListenerManager.listener.onCompositeObjects(message, contentValues)
            db!!.insert(MessageTableColumns.TABLE_NAME, null, contentValues)
        }

        @JvmStatic
        fun deleteAll(db: MPDatabase?) {
            db!!.delete(MessageTableColumns.TABLE_NAME, null, null)
        }
    }
}
