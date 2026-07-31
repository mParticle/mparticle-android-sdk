package com.braze.models.outgoing

import java.util.HashMap

class BrazeProperties {
    val properties = HashMap<String, Any>()

    fun addProperty(
        key: String,
        value: Long,
    ): BrazeProperties {
        properties[key] = value
        return this
    }

    fun addProperty(
        key: String,
        value: Int,
    ): BrazeProperties {
        properties[key] = value
        return this
    }

    fun addProperty(
        key: String,
        value: String,
    ): BrazeProperties {
        properties[key] = value
        return this
    }

    fun addProperty(
        key: String,
        value: Double,
    ): BrazeProperties {
        properties[key] = value
        return this
    }

    fun addProperty(
        key: String,
        value: Boolean,
    ): BrazeProperties {
        properties[key] = value
        return this
    }

    fun addProperty(
        key: String,
        value: Any,
    ): BrazeProperties {
        properties[key] = value
        return this
    }
}
