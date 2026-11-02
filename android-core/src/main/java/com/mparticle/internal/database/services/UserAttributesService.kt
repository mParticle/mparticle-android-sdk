package com.mparticle.internal.database.services

import android.content.ContentValues
import android.database.Cursor
import com.mparticle.internal.Logger
import com.mparticle.internal.database.MPDatabase
import com.mparticle.internal.database.tables.UserAttributesTable
import com.mparticle.internal.database.tables.UserAttributesTable.UserAttributesTableColumns
import java.util.TreeMap

open class UserAttributesService : UserAttributesTable() {
    companion object {
        @JvmStatic
        fun insertAttribute(db: MPDatabase?, key: String?, attributeValue: String?, time: Long, isList: Boolean, mpId: Long) {
            val values = ContentValues()
            values.put(UserAttributesTableColumns.MP_ID, mpId)
            values.put(UserAttributesTableColumns.ATTRIBUTE_KEY, key)
            values.put(UserAttributesTableColumns.ATTRIBUTE_VALUE, attributeValue)
            values.put(UserAttributesTableColumns.IS_LIST, isList)
            values.put(UserAttributesTableColumns.CREATED_AT, time)
            db!!.insert(UserAttributesTableColumns.TABLE_NAME, null, values)
        }

        @JvmStatic
        fun deleteAttributes(db: MPDatabase?, key: String?, mpId: Long): Int {
            val deleteWhereArgs = arrayOf(key, mpId.toString())
            return db!!.delete(
                UserAttributesTableColumns.TABLE_NAME,
                "${UserAttributesTableColumns.ATTRIBUTE_KEY} = ? and ${UserAttributesTableColumns.MP_ID} = ?",
                deleteWhereArgs,
            )
        }

        @JvmStatic
        fun getUserAttributesSingles(db: MPDatabase?, mpId: Long): TreeMap<String, String?> {
            val attributes = TreeMap<String, String?>(String.CASE_INSENSITIVE_ORDER)
            var cursor: Cursor? = null
            try {
                val args = arrayOf<String?>("1", mpId.toString())
                cursor =
                    db!!.query(
                        UserAttributesTableColumns.TABLE_NAME,
                        null,
                        "${UserAttributesTableColumns.IS_LIST} != ? and ${UserAttributesTableColumns.MP_ID} = ?",
                        args,
                        null,
                        null,
                        "${UserAttributesTableColumns.ATTRIBUTE_KEY}, ${UserAttributesTableColumns.CREATED_AT} desc",
                    )
                val keyIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_KEY)
                val valueIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_VALUE)
                while (cursor.moveToNext()) {
                    attributes[cursor.getString(keyIndex)] = cursor.getString(valueIndex)
                }
            } catch (e: Exception) {
                Logger.error(e, "Error while querying user attributes: ", e.toString())
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
            }
            return attributes
        }

        @JvmStatic
        fun getUserAttributesLists(db: MPDatabase?, mpId: Long): TreeMap<String, MutableList<String?>> {
            val attributes = TreeMap<String, MutableList<String?>>(String.CASE_INSENSITIVE_ORDER)
            var cursor: Cursor? = null
            try {
                val args = arrayOf<String?>("1", mpId.toString())
                cursor =
                    db!!.query(
                        UserAttributesTableColumns.TABLE_NAME,
                        null,
                        "${UserAttributesTableColumns.IS_LIST} = ? and ${UserAttributesTableColumns.MP_ID} = ?",
                        args,
                        null,
                        null,
                        "${UserAttributesTableColumns.ATTRIBUTE_KEY}, ${UserAttributesTableColumns.CREATED_AT} desc",
                    )
                val keyIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_KEY)
                val valueIndex = cursor.getColumnIndexOrThrow(UserAttributesTableColumns.ATTRIBUTE_VALUE)
                var previousKey: String? = null
                while (cursor.moveToNext()) {
                    val currentKey: String = cursor.getString(keyIndex)
                    if (currentKey != previousKey) {
                        previousKey = currentKey
                        attributes[currentKey] = ArrayList()
                    }
                    attributes[currentKey]!!.add(cursor.getString(valueIndex))
                }
            } catch (e: Exception) {
                Logger.error(e, "Error while querying user attribute lists: ", e.toString())
            } finally {
                if (cursor != null && !cursor.isClosed) {
                    cursor.close()
                }
            }
            return attributes
        }

        @JvmStatic
        fun deleteAll(db: MPDatabase?) {
            db!!.delete(UserAttributesTableColumns.TABLE_NAME, null, null)
        }
    }
}
