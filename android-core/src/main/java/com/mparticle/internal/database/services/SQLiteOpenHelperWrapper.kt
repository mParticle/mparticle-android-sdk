package com.mparticle.internal.database.services

import android.database.sqlite.SQLiteDatabase

interface SQLiteOpenHelperWrapper {
    fun onCreate(database: SQLiteDatabase)

    fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int)

    fun onDowngrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int)
}
