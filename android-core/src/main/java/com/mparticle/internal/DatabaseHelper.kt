package com.mparticle.internal

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper
import com.mparticle.internal.database.tables.MParticleDatabaseHelper

open class DatabaseHelper : SQLiteOpenHelper {
    internal val mContext: Context?
    internal val sqLiteOpenHelperWrapper: SQLiteOpenHelperWrapper

    constructor(context: Context?) : super(context, MParticleDatabaseHelper.getDbName(), null, MParticleDatabaseHelper.DB_VERSION) {
        // mContext is set first because the default getSQLiteOpenHelperWrapper() reads it.
        mContext = context
        sqLiteOpenHelperWrapper = getSQLiteOpenHelperWrapper()
    }

    protected open fun getSQLiteOpenHelperWrapper(): SQLiteOpenHelperWrapper = MParticleDatabaseHelper(mContext)

    override fun onCreate(db: SQLiteDatabase) {
        sqLiteOpenHelperWrapper.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteOpenHelperWrapper.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        sqLiteOpenHelperWrapper.onDowngrade(db, oldVersion, newVersion)
    }
}
