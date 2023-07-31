package com.mparticle.tooling

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class ValidationResult(
    val eventType: String? = null,
    val data: ValidationResultData? = null,
    val error: DataPlanError? = null,
    val arguments: List<String>
) {
    var originalString: String? = null

    companion object {

        fun from(json: String?, arguments: List<String>): List<ValidationResult>? {
            return try {
                val jsonArray = JSONObject(json).getJSONArray("results")
                from(jsonArray, arguments)
            } catch (jse: JSONException) {
                listOf(ValidationResult(arguments = arguments).apply { originalString = json })
            }
        }

        fun from(json: JSONArray, arguments: List<String>): List<ValidationResult> {
            val validationResults = ArrayList<ValidationResult>()
            for (i in 0..json.length() - 1) {
                val validationResultJson = json.getJSONObject(i)
                val eventType = validationResultJson.optString("event_type")
                val data = ValidationResultData.from(validationResultJson.optJSONObject("data"))
                validationResults.add(
                    ValidationResult(eventType, data, arguments = arguments)
                )
            }
            return validationResults
        }
    }

    override fun toString(): String {
        try {
            val jsonResponse = JSONObject()
                .put("Error Message", error?.message)
                .put(
                    "Data",
                    JSONObject()
                        .put("Match", data?.match)
                        .put(
                            "ValidationErrors",
                            data?.validationErrors?.foldRight(JSONArray()) { item, arr ->
                                arr.put(item)
                            }
                        )
                )
                .put("Event Type", eventType)
            return """
        Arguments:
        ${
            arguments.indexOfFirst { it.startsWith("--dataPlan") }.let {
                arguments.toMutableList().apply {
                    if (it >= 0) {
                        val dataplan = removeAt(it + 1)
                        add(
                            it + 1,
                            "${dataplan.substring(0, Math.min(dataplan.length, 20))}..."
                        )
                    }
                }
            }.joinToString(" ")
            }
        
        Response:
        ${jsonResponse.toString(4)}

            """.trimIndent()
        } catch (e: Exception) {
            return e.message + e.stackTrace.joinToString(("\n"))
        }
    }
}

data class ValidationResultData(
    val match: ValidationResultMatch?,
    val validationErrors: List<ValidationResultErrors>
) {
    companion object {
        fun from(json: JSONObject?): ValidationResultData? {
            return json?.let {
                ValidationResultData(
                    ValidationResultMatch.from(it.optJSONObject("match")),
                    ValidationResultErrors.from(it.optJSONArray("validation_errors"))
                )
            }
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

data class ValidationResultErrors(
    val validationErrorType: ValidationErrorType,
    val errorPointer: String?,
    val key: String?,
    val expected: String?,
    val actual: String?,
    val schemaKeyworkd: String?
) {
    companion object {
        fun from(json: JSONArray): List<ValidationResultErrors> {
            val validationResultErrors = ArrayList<ValidationResultErrors>()
            for (i in 0..json.length() - 1) {
                val jsonObject = json.getJSONObject(i)
                val validationErrorTypeString = jsonObject.getString("validation_error_type")
                val validationErrorType = ValidationErrorType.forName(validationErrorTypeString)
                val errorPointer = jsonObject.optString("error_pointer")
                val key = jsonObject.optString("key")
                val expected = jsonObject.optString("expected")
                val actual = jsonObject.optString("actual")
                val schemaKeyword = jsonObject.optString("schema_keyword")
                validationResultErrors.add(
                    ValidationResultErrors(
                        validationErrorType,
                        errorPointer,
                        key,
                        expected,
                        actual,
                        schemaKeyword
                    )
                )
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
    while (keys?.hasNext() == true) {
        val key = keys.next()
        map.put(key, getString(key))
    }
    return map
}
