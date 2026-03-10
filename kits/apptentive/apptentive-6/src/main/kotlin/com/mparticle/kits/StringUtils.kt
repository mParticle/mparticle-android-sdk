package com.mparticle.kits

internal object StringUtils {
    @JvmStatic
    fun tryParseSettingFlag(
        settings: Map<String, String>,
        key: String?,
        defaultValue: Boolean,
    ): Boolean {
        val value = settings[key]
        return value?.toBoolean() ?: defaultValue
    }

    fun tryParseLongSettingFlag(
        settings: Map<String, String>,
        key: String?,
        defaultValue: Long,
    ): Long {
        val value =
            key?.let {
                settings[key]
            }
        return try {
            value?.toLong() ?: defaultValue
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    @JvmStatic
    fun tryParseNumber(value: String): Number? {
        val longValue = tryParseLong(value)
        return if (longValue != null) {
            if (isInIntegerRange(longValue)) {
                longValue.toInt()
            } else {
                longValue
            }
        } else {
            tryParseDouble(value)
        }
    }

    private fun isInIntegerRange(value: Long): Boolean = value >= Int.MIN_VALUE && value <= Int.MAX_VALUE

    private fun tryParseLong(value: String): Long? =
        try {
            value.toLong()
        } catch (e: NumberFormatException) {
            null
        }

    private fun tryParseDouble(value: String): Double? =
        try {
            value.toDouble()
        } catch (e: NumberFormatException) {
            null
        }
}
