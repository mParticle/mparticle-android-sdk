package com.mparticle.kits.mocks

import android.util.SparseBooleanArray
import com.mparticle.internal.Logger
import com.mparticle.kits.KitConfiguration
import org.json.JSONException
import org.json.JSONObject

open class MockKitConfiguration : KitConfiguration() {
    @Throws(JSONException::class)
    override fun parseConfiguration(json: JSONObject): KitConfiguration {
        mTypeFilters = MockSparseBooleanArray()
        mNameFilters = MockSparseBooleanArray()
        mAttributeFilters = MockSparseBooleanArray()
        mScreenNameFilters = MockSparseBooleanArray()
        mScreenAttributeFilters = MockSparseBooleanArray()
        mUserIdentityFilters = MockSparseBooleanArray()
        mUserAttributeFilters = MockSparseBooleanArray()
        mCommerceAttributeFilters = MockSparseBooleanArray()
        mCommerceEntityFilters = MockSparseBooleanArray()
        return super.parseConfiguration(json)
    }

    override fun convertToSparseArray(json: JSONObject): SparseBooleanArray {
        val map: SparseBooleanArray = MockSparseBooleanArray()
        val iterator = json.keys()
        while (iterator.hasNext()) {
            try {
                val key = iterator.next().toString()
                map.put(key.toInt(), json.getInt(key) == 1)
            } catch (jse: JSONException) {
                Logger.error("Issue while parsing kit configuration: " + jse.message)
            }
        }
        return map
    }

    internal inner class MockSparseBooleanArray : SparseBooleanArray() {
        override fun get(key: Int): Boolean = get(key, false)

        override fun get(key: Int, valueIfKeyNotFound: Boolean): Boolean {
            print("SparseArray getting: $key")
            return if (map.containsKey(key)) {
                map[key] ?: valueIfKeyNotFound
            } else {
                valueIfKeyNotFound
            }
        }

        var map: MutableMap<Int, Boolean> = HashMap()
        override fun put(key: Int, value: Boolean) {
            map[key] = value
        }

        override fun clear() {
            map.clear()
        }

        override fun size(): Int = map.size

        override fun toString(): String = map.toString()
    }

    companion object {
        @Throws(JSONException::class)
        fun createKitConfiguration(json: JSONObject): KitConfiguration = MockKitConfiguration().parseConfiguration(json)

        @Throws(JSONException::class)
        fun createKitConfiguration(): KitConfiguration {
            val jsonObject = JSONObject()
            jsonObject.put("id", 42)
            return MockKitConfiguration().parseConfiguration(jsonObject)
        }
    }
}
