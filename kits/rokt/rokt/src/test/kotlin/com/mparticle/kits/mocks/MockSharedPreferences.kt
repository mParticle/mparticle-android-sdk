package com.mparticle.kits.mocks

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import java.util.TreeSet

class MockSharedPreferences :
    SharedPreferences,
    Editor {

    override fun getAll(): Map<String, Any>? = null

    override fun getString(key: String, defValue: String?): String = ""

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String> = TreeSet()

    override fun getInt(key: String, defValue: Int): Int = 0

    override fun getLong(key: String, defValue: Long): Long = 0

    override fun getFloat(key: String, defValue: Float): Float = 0f

    override fun getBoolean(key: String, defValue: Boolean): Boolean = false

    override fun contains(key: String): Boolean = false

    override fun edit(): Editor = this

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {}

    override fun putString(key: String, value: String?): Editor = this

    override fun putStringSet(key: String, values: Set<String>?): Editor = this

    override fun putInt(key: String, value: Int): Editor = this

    override fun putLong(key: String, value: Long): Editor = this

    override fun putFloat(key: String, value: Float): Editor = this

    override fun putBoolean(key: String, value: Boolean): Editor = this

    override fun remove(key: String): Editor = this

    override fun clear(): Editor = this

    override fun commit(): Boolean = false

    override fun apply() {}
}
