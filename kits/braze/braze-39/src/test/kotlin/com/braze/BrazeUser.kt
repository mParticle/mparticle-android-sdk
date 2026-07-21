package com.braze

import com.braze.enums.Month

class BrazeUser {
    var dobYear = -1
    var dobMonth: Month? = null
    var dobDay = -1

    fun setDateOfBirth(
        year: Int,
        month: Month?,
        day: Int,
    ): Boolean {
        dobYear = year
        dobMonth = month
        dobDay = day
        return true
    }

    val customAttributeArray = HashMap<String, MutableList<String>>()
    val customUserAttributes = HashMap<String, Any>()

    fun addToCustomAttributeArray(
        key: String,
        value: String,
    ): Boolean {
        var customArray = customAttributeArray[key]
        if (customArray == null) {
            customArray = ArrayList()
        }
        customArray.add(value)
        customAttributeArray[key] = customArray
        return true
    }

    fun removeFromCustomAttributeArray(
        key: String,
        value: String,
    ): Boolean =
        try {
            if (customAttributeArray.containsKey(key)) {
                customAttributeArray.remove(key)
            }
            true
        } catch (npe: NullPointerException) {
            false
        }

    fun setCustomUserAttribute(
        key: String,
        value: String,
    ): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun setCustomUserAttribute(
        key: String,
        value: Boolean,
    ): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun setCustomUserAttribute(
        key: String,
        value: Int,
    ): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun setCustomUserAttribute(
        key: String,
        value: Double,
    ): Boolean {
        customUserAttributes[key] = value
        return true
    }

    fun addToSubscriptionGroup(key: String): Boolean {
        customUserAttributes[key] = true
        return true
    }

    fun removeFromSubscriptionGroup(key: String): Boolean {
        customUserAttributes[key] = false
        return true
    }

    fun getCustomAttribute(): HashMap<String, MutableList<String>> = customAttributeArray

    fun getCustomUserAttribute(): HashMap<String, Any> = customUserAttributes
}
