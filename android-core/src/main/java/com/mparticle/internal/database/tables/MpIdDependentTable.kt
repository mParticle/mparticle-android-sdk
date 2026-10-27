package com.mparticle.internal.database.tables

import android.content.ContentValues
import com.mparticle.internal.database.MPDatabase

abstract class MpIdDependentTable {
    abstract fun getTableName(): String?

    open fun updateMpId(database: MPDatabase?, oldMpId: Long, newMpId: Long) {
        val contentValues = ContentValues()
        contentValues.put(MP_ID, newMpId)
        database!!.update(getTableName(), contentValues, "$MP_ID = ?", arrayOf(oldMpId.toString()))
    }

    companion object {
        const val MP_ID: String = "mp_id"
    }
}
