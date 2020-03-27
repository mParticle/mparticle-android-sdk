package com.mparticle.lints

import com.mparticle.tooling.ValidationResult
import org.json.JSONArray
import org.junit.Test
import kotlin.test.assertEquals

class ValidationResultDeserializationTest {
    
    @Test
    fun testDeserializeValidationResult() {
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

        val validationResult = ValidationResult.from(JSONArray(serialized))
        assertEquals(1, validationResult.size)
    }
}