package com.mparticle.kits

import apptentive.com.android.util.InternalUseOnly
import apptentive.com.android.util.Log
import apptentive.com.android.util.LogTag
import kotlin.Any
import kotlin.Exception
import kotlin.String

@OptIn(InternalUseOnly::class)
internal object CustomDataParser {
    fun parseCustomData(map: Map<String, String?>): Map<String, Any?> {
        val res = HashMap<String, Any?>()
        for ((key, value) in map) {
            res[key] = parseValue(value)
        }
        return res
    }

    @JvmStatic
    fun parseValue(value: String?): Any? =
        try {
            if (value != null) parseValueGuarded(value) else null
        } catch (e: Exception) {
            Log.e(LogTag("pParticle"), "Unable to parse value: $value")
            value
        }

    private fun parseValueGuarded(value: String): Any {
        // check for boolean
        if ("true".equals(value, true) || "false".equals(value, true)) {
            return value.toBoolean()
        }

        // check for number
        val number = StringUtils.tryParseNumber(value)
        return number ?: value
    }
}
