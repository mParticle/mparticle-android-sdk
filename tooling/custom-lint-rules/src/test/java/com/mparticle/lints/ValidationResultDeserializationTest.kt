package com.mparticle.lints

import com.mparticle.tooling.ValidationResult
import org.intellij.lang.annotations.Language
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationResultDeserializationTest {

    @Test
    fun testDeserializeValidationResult() {
        @Language("JSON")
        val serialized = """
        [
              {
                    "event_type": "validation_result",
                    "data": {
                        "match": {
                            "type": "custom_event",
                            "criteria": {
                                "event_name": "testEvent1",
                                "custom_event_type": "Other"
                            }
                        },
                        "validation_errors": [
                            {
                                "validation_error_type": "unplanned",
                                "key": "testEvent1",
                                "error_pointer": "#"
                            }
                        ]
                    }
               }
        ]
        """.trimIndent()

        val validationResult = ValidationResult.from(JSONArray(serialized), listOf(""))
        assertEquals(1, validationResult.size)
    }
}
