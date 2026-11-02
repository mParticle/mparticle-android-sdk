package com.mparticle.internal.database.services

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.tables.BreadcrumbTable
import com.mparticle.internal.database.tables.BreadcrumbTable.BreadcrumbTableColumns
import com.mparticle.internal.messages.BaseMPMessage
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

open class BreadcrumbService : BreadcrumbTable() {
    companion object {
        private val idColumns = arrayOf<String?>("_id")

        private val breadcrumbColumns = arrayOf<String?>(BreadcrumbTableColumns.CREATED_AT, BreadcrumbTableColumns.MESSAGE)

        @JvmStatic
        @Throws(JSONException::class)
        fun insertBreadcrumb(db: MPDatabase?, context: Context?, message: BaseMPMessage?, apiKey: String?, mpid: Long?): Int {
            if (message == null) {
                return -1
            }
            val contentValues = ContentValues()
            contentValues.put(BreadcrumbTableColumns.MP_ID, mpid)
            contentValues.put(BreadcrumbTableColumns.API_KEY, apiKey)
            contentValues.put(BreadcrumbTableColumns.CREATED_AT, message.getLong(Constants.MessageKey.TIMESTAMP))
            contentValues.put(BreadcrumbTableColumns.SESSION_ID, message.sessionId)
            contentValues.put(BreadcrumbTableColumns.MESSAGE, message.toString())

            db!!.insert(BreadcrumbTableColumns.TABLE_NAME, null, contentValues)
            val cursor =
                db.query(
                    BreadcrumbTableColumns.TABLE_NAME,
                    idColumns,
                    "${BreadcrumbTableColumns.MP_ID} = ?",
                    arrayOf<String?>(mpid.toString()),
                    null,
                    null,
                    " _id asc",
                )
            if (cursor.moveToFirst()) {
                val minId = cursor.getInt(0)
                if (cursor.count > ConfigManager.getBreadcrumbLimit(context, mpid!!)) {
                    val limit = arrayOf<String?>(minId.toString())
                    return db.delete(BreadcrumbTableColumns.TABLE_NAME, " _id = ?", limit)
                }
            }
            return -1
        }

        /**
         * For testing only.
         */
        internal fun getBreadcrumbCount(db: MPDatabase?, mpid: Long?): Int {
            var rawIds: Cursor? = null

            try {
                rawIds =
                    db!!.query(
                        BreadcrumbTableColumns.TABLE_NAME,
                        null,
                        "${BreadcrumbTableColumns.MP_ID} = ? ",
                        arrayOf<String?>(mpid.toString()),
                        null,
                        null,
                        null,
                    )
                return rawIds.count
            } finally {
                if (rawIds != null && !rawIds.isClosed) {
                    rawIds.close()
                }
            }
        }

        @JvmStatic
        @Throws(JSONException::class)
        fun getBreadcrumbs(db: MPDatabase?, context: Context?, mpid: Long?): JSONArray {
            var breadcrumbCursor: Cursor? = null
            try {
                breadcrumbCursor =
                    db!!.query(
                        BreadcrumbTableColumns.TABLE_NAME,
                        breadcrumbColumns,
                        "${BreadcrumbTableColumns.MP_ID} = ? ",
                        arrayOf<String?>(mpid.toString()),
                        null,
                        null,
                        "${BreadcrumbTableColumns.CREATED_AT} desc limit ${ConfigManager.getBreadcrumbLimit(context, mpid!!)}",
                    )

                if (breadcrumbCursor.count > 0) {
                    val breadcrumbs = JSONArray()
                    val breadcrumbIndex = breadcrumbCursor.getColumnIndexOrThrow(BreadcrumbTableColumns.MESSAGE)
                    while (breadcrumbCursor.moveToNext()) {
                        val breadcrumbObject = JSONObject(breadcrumbCursor.getString(breadcrumbIndex))
                        breadcrumbs.put(breadcrumbObject)
                    }
                    return breadcrumbs
                }
            } catch (e: Exception) {
                Logger.debug("Error while appending breadcrumbs: $e")
            } finally {
                if (breadcrumbCursor != null && !breadcrumbCursor.isClosed) {
                    breadcrumbCursor.close()
                }
            }
            return JSONArray()
        }

        @JvmStatic
        fun deleteAll(db: MPDatabase?) {
            db!!.delete(BreadcrumbTableColumns.TABLE_NAME, null, null)
        }
    }
}
