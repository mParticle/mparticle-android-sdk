package com.mparticle.kits

import com.mparticle.MParticleOptions
import junit.framework.Assert.*
import org.json.JSONObject
import org.junit.Test
import java.io.File
import java.util.function.Predicate

class DataplanFilterTest {

    @Test
    fun testParseDataplan() {
        val dataplan = File("src/test/java/com/mparticle/kits/sample_dataplan.json")
                .readText()
                .toJSON()
        val dataplanOptions = MParticleOptions.DataplanOptions.builder()
                .dataplanVersion(dataplan)
                .build()
        val dataplanFilter = DataplanFilter(dataplanOptions!!)
        assertEquals(11, dataplanFilter.dataPoints.size)
        dataplanFilter.apply {
            dataPoints["custom_event.Search Event.search"].also {
                assertEquals(it, null)
            }
            dataPoints["custom_event.locationEvent.location"].also {
                assertEquals(it, hashSetOf("foo number", "foo", "foo foo"))
            }
            dataPoints["product_action.add_to_cart"].also {
                assertEquals(it, hashSetOf("attributeNumEnum", "attributeEmail", "attributeStringAlpha", "attributeBoolean", "attributeNumMinMax"))
            }
            dataPoints["promotion_action.view"].also {
                assertEquals(it, hashSetOf("not required", "required"))
            }
            dataPoints["custom_event.TestEvent.navigation"].also {
                assertNull(it)
            }
            dataPoints["product_impression"].also {
                assertEquals(it, hashSetOf("thing1"))
            }
            dataPoints["screen_view.A New ScreenViewEvent"].also {
                assertEquals(it, hashSetOf<String>())
            }
        }

    }

    private fun String.toJSON() = JSONObject(this)
}