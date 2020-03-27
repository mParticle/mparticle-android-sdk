package com.mparticle.tooling

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class ValidationResult(val eventType: String? = null, val data: ValidationResultData? = null, val error: DataPlanError? = null) {
    var originalString: String? = null
    companion object {

        fun from(json: String?): List<ValidationResult>? {
            return try {

                val jsonArray = JSONObject(json).getJSONArray("results")
                from(jsonArray)
            } catch (jse: JSONException) {
                listOf(ValidationResult().apply { originalString = json })
            }
        }
        fun from(json: JSONArray): List<ValidationResult>  {
            val validationResults = ArrayList<ValidationResult>()
            for(i in 0..json.length() - 1) {
                val validationResultJson = json.getJSONObject(i)
                val eventType = validationResultJson.optString("event_type")
                val data = ValidationResultData.from(validationResultJson.optJSONObject("data"))
                validationResults.add(
                        ValidationResult(eventType, data)
                )
            }
            return validationResults
        }
    }
}


data class ValidationResultData(val match: ValidationResultMatch?, val validationErrors: List<ValidationResultErrors>) {
    companion object {
        fun from(json: JSONObject?): ValidationResultData? {
            return json?.let { ValidationResultData(ValidationResultMatch.from(it.optJSONObject("match")), ValidationResultErrors.from(it.optJSONArray("validation_errors"))) }
        }
    }
}

data class ValidationResultMatch(val type: String, val criteria: Map<String, String>) {
    companion object {
        fun from(json: JSONObject?): ValidationResultMatch? {
            return json?.let {
                val type = it.optString("type")
                val criteria = it.optJSONObject("criteria")?.toHashMap() ?: hashMapOf()
                ValidationResultMatch(type, criteria)
            }
        }
    }
}

data class ValidationResultErrors(val validationErrorType: ValidationErrorType, val errorPointer: String?, val key: String?, val expected: String?, val actual: String?, val schemaKeyworkd: String?) {
    companion object {
        fun from(json: JSONArray): List<ValidationResultErrors> {
            val validationResultErrors = ArrayList<ValidationResultErrors>()
            for(i in 0..json.length() - 1) {
                val jsonObject = json.getJSONObject(i)
                val validationErrorTypeString = jsonObject.getString("validation_error_type")
                val validationErrorType = ValidationErrorType.forName(validationErrorTypeString)
                val errorPointer = jsonObject.optString("error_pointer")
                val key = jsonObject.optString("key")
                val expected = jsonObject.optString("expected")
                val actual = jsonObject.optString("actual")
                val schemaKeyword = jsonObject.optString("schema_keyword")
                validationResultErrors.add(ValidationResultErrors(validationErrorType, errorPointer, key, expected, actual, schemaKeyword))
            }
            return validationResultErrors
        }
    }
}


enum class ValidationErrorType(val text: String) {
    Unplanned("unplanned"),
    MissingRequied("missing_required"),
    InvalidValue("invalid_value"),
    Unknown("unknown");

    companion object {
        fun forName(text: String): ValidationErrorType {
            return values().first { it.text == text }
        }
    }
}


fun JSONObject.toHashMap(): HashMap<String, String> {
    val keys = keys() as? Iterator<String>
    val map = HashMap<String, String>()
    while(keys?.hasNext() == true) {
        val key = keys.next()
        map.put(key, getString(key))
    }
    return map
}