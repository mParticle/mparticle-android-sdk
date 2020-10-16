package com.mparticle.kits

import com.mparticle.BaseEvent
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.kits.DataplanFilterImpl.Companion.CUSTOM_EVENT_KEY
import com.mparticle.kits.DataplanFilterImpl.Companion.PRODUCT_ACTION_KEY
import com.mparticle.kits.DataplanFilterImpl.Companion.PRODUCT_IMPRESSION_KEY
import com.mparticle.kits.DataplanFilterImpl.Companion.PROMOTION_ACTION_KEY
import com.mparticle.kits.DataplanFilterImpl.Companion.SCREEN_EVENT_KEY
import com.mparticle.kits.DataplanFilterImpl.Companion.USER_ATTRIBUTES_KEY
import com.mparticle.kits.DataplanFilterImpl.Companion.USER_IDENTITIES_KEY
import junit.framework.Assert.*
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import kotlin.random.Random

class DataplanFilterImplTest {
    lateinit var dataplanPoints: MutableMap<String, HashSet<String>?>

    @Test
    fun testParseDataplan() {
        val dataplan = File("src/test/java/com/mparticle/kits/sample_dataplan.json")
                .readText()
                .toJSON()
        val dataplanOptions = MParticleOptions.DataplanOptions.builder()
                .dataplanVersion(dataplan)
                .build()
        val dataplanFilter = DataplanFilterImpl(dataplanOptions!!)
        assertEquals(11, dataplanFilter.dataPoints.size)
        dataplanFilter.apply {
            dataPoints["$CUSTOM_EVENT_KEY.Search Event.search"].also {
                assertEquals(it, null)
            }
            dataPoints["$CUSTOM_EVENT_KEY.locationEvent.location"].also {
                assertEquals(it, hashSetOf("foo number", "foo", "foo foo"))
            }
            dataPoints["$PRODUCT_ACTION_KEY.add_to_cart"].also {
                assertEquals(it, hashSetOf("attributeNumEnum", "attributeEmail", "attributeStringAlpha", "attributeBoolean", "attributeNumMinMax"))
            }
            dataPoints["$PROMOTION_ACTION_KEY.view"].also {
                assertEquals(it, hashSetOf("not required", "required"))
            }
            dataPoints["$CUSTOM_EVENT_KEY.TestEvent.navigation"].also {
                assertNull(it)
            }
            dataPoints[PRODUCT_IMPRESSION_KEY].also {
                assertEquals(it, hashSetOf("thing1"))
            }
            dataPoints["$SCREEN_EVENT_KEY.A New ScreenViewEvent"].also {
                assertEquals(it, hashSetOf<String>())
            }
            dataPoints[USER_ATTRIBUTES_KEY].also {
                assertEquals(it, hashSetOf("a third attribute", "my attribute", "my other attribute"))
            }
            dataPoints[USER_IDENTITIES_KEY].also {
                assertNull(it)
            }
        }

    }

    @Before
    fun before() {
        dataplanPoints = getRandomDataplanPoints()
    }

    @Test
    fun `test commerce blockEvent is blocking properly`() {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            dataplanPoints.remove(datapoint.toString())
            val event = getRandomEvent(datapoint).apply {
                customAttributes = randomAttributes()
            }

            val dataplanFilter = DataplanFilterImpl(dataplanPoints, true, Random.nextBoolean(), false, false)
            assertNull(dataplanFilter.transformEventForEvent(event)?.toString())
            diversity.remove(datapoint.type)
        }
    }

    @Test
    fun `test commerce event is passing properly when planned`() {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            val event = getRandomEvent(datapoint).apply {
                customAttributes = randomAttributes()
            }

            dataplanPoints.put(datapoint.toString(), null)

            val dataplanFilter = DataplanFilterImpl(dataplanPoints, true, Random.nextBoolean(), false, false)
            assertEquals(event.toString(), dataplanFilter.transformEventForEvent(event)?.toString())
            diversity.remove(datapoint.type)
        }
    }

    @Test
    fun `test commerce event is passing properly when unplanned but blockEvent off`() {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            dataplanPoints.remove(datapoint.toString())
            val event = getRandomEvent(datapoint).apply {
                customAttributes = randomAttributes()
            }

            val dataplanFilter = DataplanFilterImpl(dataplanPoints, false, Random.nextBoolean(), false, false)
            assertEquals(event.toString(), dataplanFilter.transformEventForEvent(event)?.toString())
            diversity.remove(datapoint.type)
        }
    }


    @Test
    fun `test blockEventAttribute is passing properly for planned events` () {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            val allowedAttributes = randomAttributes()
            val event = getRandomEvent(datapoint).apply {
                customAttributes = allowedAttributes
            }

            dataplanPoints.put(datapoint.toString(), allowedAttributes.keys.toHashSet())
            val dataplanFilter = DataplanFilterImpl(dataplanPoints, true, Random.nextBoolean(), false, false)
            assertEquals(allowedAttributes, dataplanFilter.transformEventForEvent(event)?.customAttributes)
            diversity.remove(datapoint.type)
        }
    }

    @Test
    fun `test blockEvent is passing properly for unplanned events but event blocking off` () {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            val allowedAttributes = randomAttributes()
            val event = getRandomEvent(datapoint).apply {
                customAttributes = allowedAttributes
            }

            val dataplanFilter = DataplanFilterImpl(dataplanPoints, false, Random.nextBoolean(), false, false)
            assertEquals(event, dataplanFilter.transformEventForEvent(event))
            diversity.remove(datapoint.type)
        }
    }

    @Test
    fun `test blockEventAttribute is blocking properly`() {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            dataplanPoints.remove(datapoint.toString())
            val allowedAttributes = randomAttributes()
            val blockedAttributes = randomAttributes()
            val event = getRandomEvent(datapoint).apply {
                customAttributes = allowedAttributes + blockedAttributes
            }

            dataplanPoints.put(datapoint.toString(), allowedAttributes.keys.toHashSet())
            val dataplanFilter = DataplanFilterImpl(dataplanPoints, Random.Default.nextBoolean(), true, false, false)
            assertEquals(allowedAttributes, dataplanFilter.transformEventForEvent(event)?.customAttributes)
            diversity.remove(datapoint.type)
        }
    }

    @Test
    fun `test blockEvent higher priority than blockEventAttribute`() {
        val diversity = types.toHashSet()
        while (diversity.size != 0) {
            dataplanPoints = getRandomDataplanPoints()
            val datapoint = getRandomDataplanEventKey()
            dataplanPoints.remove(datapoint.toString())
            val event = getRandomEvent(datapoint)
            event.customAttributes = randomAttributes()
            val dataplanFilter = DataplanFilterImpl(dataplanPoints, true, true, false, false)
            assertNull(dataplanFilter.transformEventForEvent(event))
            diversity.remove(datapoint.type)
        }
    }

    private val types = setOf(CUSTOM_EVENT_KEY, PRODUCT_ACTION_KEY, PROMOTION_ACTION_KEY, PRODUCT_IMPRESSION_KEY, SCREEN_EVENT_KEY)

    fun getRandomEvent(datapoint: DataplanPoint): BaseEvent {
        return when (datapoint.type) {
            CUSTOM_EVENT_KEY -> MPEvent.Builder(datapoint.name!!, MParticle.EventType.values().first { it.ordinal == datapoint.eventType?.toInt() }).build()
            SCREEN_EVENT_KEY -> ScreenEventBuilder(datapoint.name!!).build().also { assertTrue(it.isScreenEvent) }
            PRODUCT_ACTION_KEY -> CommerceEvent.Builder(datapoint.name!!, Product.Builder("a", "b", 1.0).build()).build()
            PROMOTION_ACTION_KEY -> CommerceEvent.Builder(datapoint.name!!, Promotion()).build()
            PRODUCT_IMPRESSION_KEY -> CommerceEvent.Builder(Impression("impressionname", Product.Builder("a", "b", 1.0).build())).build()
            else -> throw IllegalArgumentException(datapoint.type + ": messed this implementation up :/")
        }
    }


    fun getRandomDataplanEventKey(): DataplanPoint {
        return when (Random.Default.nextInt(0, 5)) {
            0 -> DataplanPoint(CUSTOM_EVENT_KEY, randomString(5), randomEventType().ordinal.toString())
            1 -> DataplanPoint(SCREEN_EVENT_KEY,randomString(8))
            2 -> DataplanPoint(PRODUCT_ACTION_KEY, randomProductAction())
            3 -> DataplanPoint(PROMOTION_ACTION_KEY, randomPromotionAction())
            4 -> DataplanPoint(PRODUCT_IMPRESSION_KEY)
            else -> throw IllegalArgumentException("messed this implementation up :/")
        }
    }

    fun getRandomDataplanPoints(): MutableMap<String, HashSet<String>?> {
        return (0..Random.Default.nextInt(0, 10))
                .associate {
                    getRandomDataplanEventKey().toString() to randomAttributes().keys.toHashSet()
                }
                .toMutableMap()
    }

    val chars: List<Char> = ('a'..'z') + ('A'..'Z')


    fun randomAttributes(): Map<String, String> {
        return (0..Random.Default.nextInt(0,5)).map {
            randomString(4) to randomString(8)
        }.toMap()
    }

    fun randomString(length: Int): String {
        return (0..length - 1).map {
            chars[Random.Default.nextInt(0, chars.size - 1)]
        }.joinToString("")
    }

    fun randomEventType(): MParticle.EventType {
        return MParticle.EventType.values()[Random.Default.nextInt(0, MParticle.EventType.values().size - 1)]
    }

    fun randomProductAction(): String {
        return randomConstString(Product::class.java)
    }

    fun randomPromotionAction(): String {
        return randomConstString(Promotion::class.java)
    }

    fun randomConstString(clazz: Class<*>): String {
        return clazz.fields
                .filter { Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers) }
                .filter { it.name.all { it.isUpperCase() } }
                .filter { it.type == String::class.java }
                .let {
                    it[Random.Default.nextInt(0, it.size - 1)].get(null) as String
                }
    }

    private fun String.toJSON() = JSONObject(this)

    class ScreenEvent(event: MPEvent): MPEvent(event) {
        init {
            isScreenEvent = true
        }
    }
    class ScreenEventBuilder(name: String): MPEvent.Builder(name, MParticle.EventType.Other) {
        override fun build(): MPEvent {
            return ScreenEvent(super.build())
        }
    }

    class DataplanPoint(val type: String, val name: String? = null, val eventType: String? = null) {
        override fun toString() = "$type${if (name != null) ".$name" else ""}${if(eventType != null) ".$eventType" else ""}"
    }
}