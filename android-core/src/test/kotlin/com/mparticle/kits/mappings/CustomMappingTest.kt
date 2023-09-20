package com.mparticle.kits.mappings

import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.kits.CommerceEventUtils
import com.mparticle.kits.KitUtils
import com.mparticle.kits.mappings.EventWrapper.CommerceEventWrapper
import com.mparticle.kits.mappings.EventWrapper.MPEventWrapper
import com.mparticle.mock.MockKitConfiguration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito

class CustomMappingTest {
    /**
     * This is pretty implementation specific. Just making sure that the objects we create accurately reflect the JSON.
     */
    @Test
    @Throws(Exception::class)
    fun testParsing() {
        val json = JSONObject(JSON)
        val ekConfigs = json.getJSONArray("eks")
        for (i in 0 until ekConfigs.length()) {
            val config = ekConfigs.getJSONObject(i)
            // if (config.getInt("id") == 56) {
            val projJsonList = config.getJSONArray("pr")
            for (j in 0 until projJsonList.length()) {
                val pJson = projJsonList.getJSONObject(j)
                val customMapping = CustomMapping(pJson)
                Assert.assertEquals(pJson.getInt("id").toLong(), customMapping.mID.toLong())
                // sometimes pmmid doesn't exist...:/
                // assertEquals(pJson.getInt("pmmid"), projection.mModuleMappingId);
                val matches = pJson.getJSONArray("matches")
                Assert.assertEquals(
                    matches.getJSONObject(0).getInt("message_type").toLong(),
                    customMapping.messageType.toLong()
                )
                Assert.assertEquals(
                    matches.getJSONObject(0).getString("event_match_type"),
                    customMapping.matchList[0].mMatchType
                )
                if (customMapping.matchList[0].mMatchType.startsWith(CustomMapping.MATCH_TYPE_HASH)) {
                    Assert.assertEquals(
                        matches.getJSONObject(0).getInt("event").toLong(),
                        customMapping.matchList[0].mEventHash.toLong()
                    )
                } else {
                    Assert.assertEquals(
                        matches.getJSONObject(0).getString("event"),
                        customMapping.matchList[0].mEventName
                    )
                }
                if (matches.getJSONObject(0).has("attribute_key")) {
                    Assert.assertEquals(
                        matches.getJSONObject(0).getString("attribute_key"),
                        customMapping.matchList[0].mAttributeKey
                    )
                    if (matches.getJSONObject(0)["attribute_values"] is JSONArray) {
                        val attributeValues =
                            matches.getJSONObject(0).getJSONArray("attribute_values")
                        Assert.assertEquals(
                            attributeValues.length().toLong(),
                            customMapping.matchList[0].attributeValues.size.toLong()
                        )
                    } else {
                        Assert.assertTrue(
                            customMapping.matchList[0].attributeValues.contains(
                                matches.getJSONObject(0).getString("attribute_values").lowercase()
                            )
                        )
                    }
                }
                if (pJson.has("behavior")) {
                    val behaviors = pJson.getJSONObject("behavior")
                    Assert.assertEquals(behaviors.optBoolean("is_default"), customMapping.isDefault)
                    Assert.assertEquals(
                        behaviors.optBoolean("append_unmapped_as_is"),
                        customMapping.mAppendUnmappedAsIs
                    )
                    Assert.assertEquals(
                        behaviors.optInt("max_custom_params", Int.MAX_VALUE).toLong(),
                        customMapping.mMaxCustomParams.toLong()
                    )
                }
                val action = pJson.getJSONObject("action")
                Assert.assertEquals(
                    customMapping.mProjectedEventName,
                    action.getString("projected_event_name")
                )
                val attributes = action.getJSONArray("attribute_maps")
                val sum =
                    (if (customMapping.mRequiredAttributeMapList == null) 0 else customMapping.mRequiredAttributeMapList.size) +
                        if (customMapping.mStaticAttributeMapList == null) 0 else customMapping.mStaticAttributeMapList.size
                Assert.assertEquals(attributes.length().toLong(), sum.toLong())
                for (k in 0 until attributes.length()) {
                    val attribute = attributes.getJSONObject(k)
                    val attProj = CustomMapping.AttributeMap(attribute)
                    Assert.assertEquals(attribute.optBoolean("is_required"), attProj.mIsRequired)
                    if (attribute.getString("match_type")
                        .startsWith(CustomMapping.MATCH_TYPE_STATIC)
                    ) {
                        Assert.assertFalse(customMapping.mRequiredAttributeMapList.contains(attProj))
                        Assert.assertTrue(customMapping.mStaticAttributeMapList.contains(attProj))
                    } else {
                        Assert.assertTrue(customMapping.mRequiredAttributeMapList.contains(attProj))
                        Assert.assertFalse(customMapping.mStaticAttributeMapList.contains(attProj))
                    }
                }
                var notRequiredFound = false
                for (attributeMap in customMapping.mRequiredAttributeMapList) {
                    if (notRequiredFound && attributeMap.mIsRequired) {
                        Assert.fail("Projection attributes are out of order!")
                    }
                    notRequiredFound = !attributeMap.mIsRequired
                }
            }
            // }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testDefault() {
        val defaultCustomMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"))
        val info = HashMap<String, String?>()
        info["some key"] = "some value"
        info["another key"] = "another value"
        val event = MPEvent.Builder("whatever", MParticle.EventType.Other).category("whatever!!")
            .customAttributes(info).build()
        Assert.assertTrue(defaultCustomMapping.isMatch(MPEventWrapper(event)))
        val newEvent = defaultCustomMapping.project(MPEventWrapper(event))[0].mpEvent
        Assert.assertEquals(event, newEvent)
        info["yet another key"] = "yet another value"
        // sanity check to make sure we're duplicating the map, not passing around a reference
        Assert.assertTrue(event.customAttributeStrings!!.containsKey("yet another key"))
        Assert.assertFalse(newEvent.customAttributeStrings!!.containsKey("yet another key"))
    }

    /**
     * Same as above but evaluating from one level up, from the Provider's perspective
     */
    @Test
    @Throws(Exception::class)
    fun testDefaultProjection2() {
        val kitConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                "{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_values\":\"\$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"
            )
        )
        val info = HashMap<String, String?>()
        info["some key"] = "some value"
        info["another key"] = "another value"
        val event =
            MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build()
        val list = CustomMapping.projectEvents(
            event,
            kitConfiguration.customMappingList,
            kitConfiguration.defaultEventProjection
        )
        Assert.assertEquals(1, list.size.toLong())
        Assert.assertEquals(list[0].mpEvent, event)
    }

    /**
     * Tests both that when there's no default specified, return null, and also matching on message_type
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testNoDefaultEventAndDefaultScreen() {
        val kitConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                "{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_values\":\"\$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":3, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"
            )
        )
        val info = HashMap<String, String?>()
        info["some key"] = "some value"
        info["another key"] = "another value"
        val event =
            MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build()
        var list = CustomMapping.projectEvents(
            event,
            kitConfiguration.customMappingList,
            kitConfiguration.defaultEventProjection
        )
        Assert.assertNull(list)
        // but if we say this is a screen event, the default projection should be detected
        list = CustomMapping.projectEvents(
            event,
            true,
            kitConfiguration.customMappingList,
            kitConfiguration.defaultEventProjection,
            kitConfiguration.defaultScreenCustomMapping
        )
        Assert.assertEquals(1, list.size.toLong())
    }

    /**
     * Make sure that we respect the append_unmapped_as_is property
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testDontAppendAsIs() {
        var customMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":false, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"))
        val info = HashMap<String, String?>()
        info["some key"] = "some value"
        info["another key"] = "another value"
        val event =
            MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build()
        var newEvent = customMapping.project(MPEventWrapper(event))[0]
        Assert.assertTrue(newEvent.mpEvent.customAttributeStrings!!.isEmpty())
        customMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"))
        newEvent = customMapping.project(MPEventWrapper(event))[0]
        Assert.assertTrue(newEvent.mpEvent.customAttributeStrings!!.size == 2)
    }

    /**
     * Testing -
     * 1. max_custom_params
     * 2. alphabetic ordering
     * 3. precedence of attribute projections over non-mapped attributes
     * 4. if projected_event_name isn't specified, use what we were given
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testMaxParams() {
        var customMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":5, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"))
        val info = HashMap<String, String?>()
        // add them out of order, why not?
        info["key 3"] = "123af4"
        info["key 4"] = "123adf45"
        info["key 1"] = "12ad3456"
        info["key 6"] = "asdasd"
        info["key 2"] = "asdasdaf"
        info["key 5"] = "asdfasea"
        val event =
            MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build()
        var result = customMapping.project(MPEventWrapper(event))[0]
        var newEvent = result.mpEvent
        Assert.assertTrue(newEvent.customAttributeStrings!!.size == 5)
        // key 5 should be there but key 6 should be kicked out due to alphabetic sorting
        Assert.assertTrue(newEvent.customAttributeStrings!!.containsKey("key 5"))
        Assert.assertFalse(newEvent.customAttributeStrings!!.containsKey("key 6"))
        // now create the SAME projection, except specify an optional attribute key 6 - it should boot key 5 from the resulting event
        customMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":5, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" }] } }"))
        result = customMapping.project(MPEventWrapper(event))[0]
        newEvent = result.mpEvent
        Assert.assertFalse(newEvent.customAttributeStrings!!.containsKey("key 5"))
        Assert.assertTrue(newEvent.customAttributeStrings!!.containsKey("key 6")) // note that the json also doesn't specify a key name - so we just use the original
        Assert.assertTrue(newEvent.customAttributeStrings!!.size == 5)

        // test what happens if max isn't even set (everything should be there)
        customMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" }] } }"))
        result = customMapping.project(MPEventWrapper(event))[0]
        newEvent = result.mpEvent
        Assert.assertTrue(newEvent.customAttributeStrings!!.containsKey("key 5"))
        Assert.assertTrue(newEvent.customAttributeStrings!!.containsKey("key 6")) // note that the json also doesn't specify a key name - so we just use the original
        Assert.assertTrue(newEvent.customAttributeStrings!!.size == 6)
    }

    /**
     * Test an attribute projection with match_type Field
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testFieldName() {
        val customMapping =
            CustomMapping(JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"))
        val event = MPEvent.Builder("screenname", MParticle.EventType.Other).build()
        val projectedEvent = customMapping.project(MPEventWrapper(event))[0]
        Assert.assertEquals(
            "screenname",
            projectedEvent.mpEvent.customAttributeStrings!!["screen_name_key"]
        )
    }

    /**
     * EventWrapper is used to add some properties to an MPEvent without having to expose them in the public API
     *
     * It abstracts the differences between MPEvents that are screen views vs. app events, and also allows for caching
     * of attribute hashes
     */
    @Test
    fun testEventWrapper() {
        val info: MutableMap<String?, String?> = HashMap()
        info["key 1"] = "value 1"
        val event =
            MPEvent.Builder("some event name", MParticle.EventType.Other).customAttributes(info)
                .build()
        val wrapper = MPEventWrapper(event)

        // make sure the attribute hashes work as intended
        val hashes = wrapper.getAttributeHashes()
        val key =
            hashes[KitUtils.hashForFiltering(event.eventType.ordinal.toString() + event.eventName + "key 1")]
        Assert.assertEquals(info[key], "value 1")

        // make sure event hash is generated correctly
        Assert.assertEquals(
            KitUtils.hashForFiltering(event.eventType.ordinal.toString() + event.eventName)
                .toLong(),
            wrapper.eventHash.toLong()
        )
        Assert.assertEquals(4, wrapper.messageType.toLong())
        Assert.assertEquals(event.eventType.ordinal.toLong(), wrapper.eventTypeOrdinal.toLong())
    }

    /**
     * Same as testEventWrapper except with isScreenEvent=true
     */
    @Test
    fun testScreenEventWrapper() {
        val info: MutableMap<String?, String?> = HashMap()
        info["key 1"] = "value 1"
        val event =
            MPEvent.Builder("some event name", MParticle.EventType.Other).customAttributes(info)
                .build()
        val wrapper = MPEventWrapper(event, true)

        // make sure the attribute hashes work as intended
        val hashes = wrapper.getAttributeHashes()
        // event type be 0 for screen views
        val key = hashes[KitUtils.hashForFiltering(0.toString() + event.eventName + "key 1")]
        Assert.assertEquals(info[key], "value 1")

        // make sure event hash is generated correctly
        Assert.assertEquals(
            KitUtils.hashForFiltering(0.toString() + event.eventName).toLong(),
            wrapper.eventHash.toLong()
        )
        Assert.assertEquals(3, wrapper.messageType.toLong())
        Assert.assertEquals(0, wrapper.eventTypeOrdinal.toLong())
    }

    /**
     * Test 1
     * The first test shows that we correctly match on the Hash, but that the other projections are not triggered b/c attributes are missing
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testMultiHashProjections1() {
        val kitConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                "{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_values\":\"\$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"
            )
        )

        /**
         * Test 1
         * The first test shows that we correctly match on the Hash, but that the other projections are not triggered b/c attributes are missing
         */
        // all of these projections match on a Hash of the following name/type
        val builder = MPEvent.Builder("sproj 1", MParticle.EventType.UserContent)
        val attributes: MutableMap<String, String?> = HashMap()
        attributes["attribute we don't care about"] = "some value"
        builder.customAttributes(attributes)
        val eventList = CustomMapping.projectEvents(
            builder.build(),
            kitConfiguration.customMappingList,
            kitConfiguration.defaultEventProjection
        )
        Assert.assertEquals(1, eventList.size.toLong())
        val projEvent1 = eventList[0].mpEvent
        Assert.assertEquals("account - check order status", projEvent1.eventName)
        Assert.assertEquals(
            "some value",
            projEvent1.customAttributeStrings!!["attribute we don't care about"]
        )
    }

    /**
     * Test 2
     * Now add a new attribute "Value" to the same event, which now triggers the original projection and a new projection
     * that requires the new attribute
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testMultiHashProjections2() {
        val kitConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                "{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_values\":\"\$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"
            )
        )
        val builder = MPEvent.Builder("sproj 1", MParticle.EventType.UserContent)
        val attributes: MutableMap<String, String?> = HashMap()
        attributes["attribute we don't care about"] = "some value"
        builder.customAttributes(attributes)

        // add an attribute that's required by 1 of them, we should end up with 2 triggered projections
        attributes["Value"] = "product name"
        val eventList = CustomMapping.projectEvents(
            builder.build(),
            kitConfiguration.customMappingList,
            kitConfiguration.defaultEventProjection
        )
        Assert.assertEquals(2, eventList.size.toLong())
        var projEvent1 = eventList[0].mpEvent
        // same as test 1, but verify for the fun of it.
        Assert.assertEquals("account - check order status", projEvent1.eventName)
        Assert.assertEquals(
            "some value",
            projEvent1.customAttributeStrings!!["attribute we don't care about"]
        )

        // this is the new projection which requires the Value attribute
        projEvent1 = eventList[1].mpEvent
        Assert.assertEquals("pdp - add to tote", projEvent1.eventName)
        // required attribute
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Last Add to Tote Category"]
        ) // the required attribute has been renamed
        // non-required attributes which define the same hash as the required one.
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Last Add to Tote Total Amount"]
        )
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Last Add to Tote SKU"]
        )

        // static attributes are in this projection as well.
        Assert.assertEquals("10", projEvent1.customAttributeStrings!!["Last Add to Tote Quantity"])
        Assert.assertEquals(
            "1321",
            projEvent1.customAttributeStrings!!["Last Add to Tote Unit Price"]
        )
    }

    /**
     * Test 3
     * The final test shows adding another attribute which not only triggers a 3rd projection, but also adds onto the 2nd projection which
     * defines that attribute as non-required.
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testMultiHashProjections3() {
        val kitConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                "{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_values\":\"\$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"
            )
        )
        val builder = MPEvent.Builder("sproj 1", MParticle.EventType.UserContent)
        val attributes: MutableMap<String, String?> = HashMap()
        attributes["attribute we don't care about"] = "some value"
        builder.customAttributes(attributes)

        // add an attribute that's required by 1 of them, we should end up with 2 triggered projections
        attributes["Value"] = "product name"
        /**
         * Test 3
         * The final test shows adding another attribute which not only triggers a 3rd projection, but also adds onto the 2nd projection which
         * defines that attribute as non-required.
         */
        attributes["Label"] = "product label"
        val eventList = CustomMapping.projectEvents(
            builder.build(),
            kitConfiguration.customMappingList,
            kitConfiguration.defaultEventProjection
        )
        Assert.assertEquals(3, eventList.size.toLong())
        var projEvent1 = eventList[0].mpEvent
        Assert.assertEquals("account - check order status", projEvent1.eventName)
        Assert.assertEquals(
            "some value",
            projEvent1.customAttributeStrings!!["attribute we don't care about"]
        )
        projEvent1 = eventList[1].mpEvent
        Assert.assertEquals("pdp - add to tote", projEvent1.eventName)
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Last Add to Tote Category"]
        )
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Last Add to Tote Total Amount"]
        )
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Last Add to Tote SKU"]
        )
        Assert.assertEquals("10", projEvent1.customAttributeStrings!!["Last Add to Tote Quantity"])
        Assert.assertEquals(
            "1321",
            projEvent1.customAttributeStrings!!["Last Add to Tote Unit Price"]
        )

        // these are new for the 2nd projection, as they match the Hash for the new  "Label" attribute
        Assert.assertEquals(
            "product label",
            projEvent1.customAttributeStrings!!["Last Add to Tote Name"]
        )
        Assert.assertEquals(
            "product label",
            projEvent1.customAttributeStrings!!["Last Add to Tote Print"]
        )

        // and here's our 3rd projection, which defines both the original "Value" attribute hash as well as "Label"
        projEvent1 = eventList[2].mpEvent
        Assert.assertEquals("pdp - complete the look", projEvent1.eventName)
        Assert.assertEquals(
            "product name",
            projEvent1.customAttributeStrings!!["Complete the Look Product Name"]
        )
        Assert.assertEquals(
            "product label",
            projEvent1.customAttributeStrings!!["Complete the Look Product Name 2"]
        )
    }

    /**
     * Testing:
     *
     * 1. Mapping CommerceEvent to MPEvent
     * 2. Foreach logic - n products should yield n events
     * 3. ProductField, EventField mapping
     * 4. Hash matching
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testCommerceEventToMPEvent() {
        val config =
            "{\"id\":93, \"pmid\":220, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1572\"}], \"behavior\":{\"max_custom_params\":0, \"selector\":\"foreach\"}, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"Hash\", \"value\":\"2000445218\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"Hash\", \"value\":\"881337592\", \"data_type\":\"String\", \"property\":\"EventField\"}, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"Hash\", \"value\":\"1514047\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"Hash\", \"value\":\"1455148719\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"Hash\", \"value\":\"664929967\", \"data_type\":\"Int\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"Hash\", \"value\":\"1647761705\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":4 } }"
        val customMapping = CustomMapping(JSONObject(config))
        val productBuilder =
            Product.Builder("product name 0", "product id 0", 1.0).category("product category 0")
                .quantity(1.0)
        val commerceEventBuilder =
            CommerceEvent.Builder(Product.DETAIL, productBuilder.build()).currency("dollar bills")
        for (i in 1..4) {
            commerceEventBuilder.addProduct(
                productBuilder.name("product name $i").sku("product id $i").category(
                    "product category $i"
                ).quantity((i + 1).toDouble()).unitPrice((i + 1).toDouble()).build()
            )
        }
        val commerceEvent = commerceEventBuilder.build()
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        val result = customMapping.project(CommerceEventWrapper(commerceEvent))
        Assert.assertEquals(5, result.size.toLong())
        for (i in 0..4) {
            val event = result[i].mpEvent
            Assert.assertNotNull(event)
            Assert.assertEquals("pdp - product view", event.eventName)
            val attributes = event.customAttributeStrings
            Assert.assertEquals("product category $i", attributes!!["Last Product View Category"])
            Assert.assertEquals("dollar bills", attributes["Last Product View Currency"])
            Assert.assertEquals("product id $i", attributes["Last Product View SKU"])
            Assert.assertEquals("product name $i", attributes["Last Product View Name"])
            Assert.assertEquals(
                i.plus(1).toString() + ".0",
                attributes["Last Product View Quantity"]
            )
            Assert.assertEquals(
                ((i + 1) * (i + 1)).toString() + ".0",
                attributes["Last Product View Total Amount"]
            )
            Assert.assertEquals("pdp - product view", event.eventName)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testMatchCommerceEventType() {
        val product = Product.Builder("name", "sku", 0.0).build()
        val config =
            JSONObject("{\"id\":99, \"pmid\":229, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", }], \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }")
        var event = CommerceEvent.Builder(Product.DETAIL, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        var customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.ADD_TO_CART, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.PURCHASE, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.ADD_TO_WISHLIST, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.CHECKOUT, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.CHECKOUT_OPTION, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.CLICK, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.REFUND, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.REMOVE_FROM_CART, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Product.REMOVE_FROM_WISHLIST, product)
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Impression("list name", product))
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Promotion.VIEW, Promotion().setId("id"))
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
        event = CommerceEvent.Builder(Promotion.CLICK, Promotion().setId("id"))
            .transactionAttributes(TransactionAttributes().setId("id")).build()
        config.getJSONArray("matches").getJSONObject(0).put(
            "event",
            "" + KitUtils.hashForFiltering("" + CommerceEventUtils.getEventType(event))
        )
        customMapping = CustomMapping(config)
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(event)))
    }

    /**
     * Test match of CommerceEvent "property"
     *
     * 1. EventField
     * 2. EventAttribute
     * 3. ProductField
     * 4. ProductAttribute
     * 5. PromotionField
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testMatchCommerceEventProperty() {
        val config =
            JSONObject("{\"id\":99, \"pmid\":229, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", \"property\":\"EventField\", \"property_name\":\"-601244443\", \"property_values\":[\"5\"]}], \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }")
        var customMapping = CustomMapping(config)
        val product = Product.Builder("name", "sku", 0.0).build()
        var commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product).checkoutStep(4).build()
        Assert.assertFalse(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product).checkoutStep(5).build()
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        config.getJSONArray("matches").getJSONObject(0).put("property", "EventAttribute")
        customMapping = CustomMapping(config)
        Assert.assertFalse(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        val attributes: MutableMap<String, String> = HashMap()
        attributes["Checkout Step"] = "5"
        commerceEvent =
            CommerceEvent.Builder(Product.CHECKOUT, product).customAttributes(attributes).build()
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        config.getJSONArray("matches").getJSONObject(0).put("property", "ProductAttribute")
        customMapping = CustomMapping(config)
        Assert.assertFalse(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .addProduct(Product.Builder("name 2", "sku", 0.0).customAttributes(attributes).build())
            .build()
        var wrapper = CommerceEventWrapper(commerceEvent)
        Assert.assertTrue(customMapping.isMatch(wrapper))
        // only 1 product has the required attribute so there should only be 1 product after the matching step
        Assert.assertEquals(1, wrapper.event.products!!.size.toLong())
        Assert.assertEquals("name 2", wrapper.event.products!![0].name)
        config.getJSONArray("matches").getJSONObject(0).put("property", "ProductField")
        config.getJSONArray("matches").getJSONObject(0)
            .put("property_name", "-1167125985") // checkout + category hash
        var values = JSONArray()
        values.put("some product cat")
        config.getJSONArray("matches").getJSONObject(0).put("property_values", values)
        customMapping = CustomMapping(config)
        Assert.assertFalse(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .addProduct(Product.Builder("name 2", "sku", 0.0).category("some product cat").build())
            .build()
        wrapper = CommerceEventWrapper(commerceEvent)
        Assert.assertTrue(customMapping.isMatch(wrapper))
        // only 1 product has the required attribute so there should only be 1 product after the matching step
        Assert.assertEquals(1, wrapper.event.products!!.size.toLong())
        Assert.assertEquals("some product cat", wrapper.event.products!![0].category)
        config.getJSONArray("matches").getJSONObject(0).put("property", "PromotionField")
        config.getJSONArray("matches").getJSONObject(0)
            .put("property_name", "835505623") // click + creative hash
        values = JSONArray()
        values.put("some promotion creative")
        config.getJSONArray("matches").getJSONObject(0).put("property_values", values)
        customMapping = CustomMapping(config)
        Assert.assertFalse(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        commerceEvent = CommerceEvent.Builder(
            Promotion.CLICK,
            Promotion().setCreative("some promotion creative")
        ).addPromotion(
            Promotion().setCreative("some other creative")
        ).build()
        wrapper = CommerceEventWrapper(commerceEvent)
        Assert.assertTrue(customMapping.isMatch(wrapper))
        // only 1 promotion has the required attribute so there should only be 1 promotion after the matching step
        Assert.assertEquals(1, wrapper.event.promotions!!.size.toLong())
        Assert.assertEquals("some promotion creative", wrapper.event.promotions!![0].creative)
    }

    /**
     * Test projecting from CommerceEvent to CommerceEvent:
     *
     * - matching on Hash of EventField (already tested above)
     * - "last" selector yields a single CommerceEvent with all of the original products, but only attributes of the last Product
     * - "foreach" selector yields 5 CommerceEvents, each with 1 product and the attributes of that Product
     * - test mapping/setting of an event name for CommerceEvent
     * - for all of the above, mapping EventField, EventAttribute, ProductField, and ProductAttributes
     * - is_required limits resulting CommerceEvents to only those with the matching Products
     *
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testCommerceEventToCommerceEvent() {
        val config = JSONObject(
            "{\"id\":99, \"pmid\":229, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", \"property\":\"EventField\", \"property_name\":\"-601244443\", \"property_values\":[\"5\", \"7\"]}], \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Projected sample custom attribute\", \"match_type\":\"Hash\", \"value\":\"153565474\", \"data_type\":\"String\", \"property\":\"ProductAttribute\"}, {\"projected_attribute_name\":\"Projected list source\", \"match_type\":\"Hash\", \"value\":\"-882952085\", \"data_type\":\"String\", \"property\":\"EventField\"}, {\"projected_attribute_name\":\"Projected sample event attribute\", \"match_type\":\"Hash\", \"value\":\"1957440897\", \"data_type\":\"String\", \"property\":\"EventAttribute\"} ], \"outbound_message_type\":16 } }"
        )
        val customMapping = CustomMapping(config)
        val product = Product.Builder("name", "sku", 0.0).category("category 0").build()
        val productAttributes: MutableMap<String, String> = HashMap()
        productAttributes["sample custom attribute"] = "sample custom product attribute value"
        val eventAttributes: MutableMap<String, String> = HashMap()
        eventAttributes["sample event attribute"] = "sample custom event value"
        var commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .addProduct(
                Product.Builder("name 1", "sku", 0.0).category("category 1")
                    .customAttributes(productAttributes).build()
            )
            .addProduct(
                Product.Builder("name 2", "sku", 0.0).category("category 2")
                    .customAttributes(productAttributes).build()
            )
            .addProduct(Product.Builder("name 3", "sku", 0.0).category("category 3").build())
            .addProduct(Product.Builder("name 4", "sku", 0.0).category("category 4").build())
            .productListSource("some product list source")
            .customAttributes(eventAttributes)
            .checkoutStep(4).build()
        Assert.assertFalse(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .addProduct(
                Product.Builder("name 1", "sku", 0.0).category("category 1")
                    .customAttributes(productAttributes).build()
            )
            .addProduct(
                Product.Builder("name 2", "sku", 0.0).category("category 2")
                    .customAttributes(productAttributes).build()
            )
            .addProduct(Product.Builder("name 3", "sku", 0.0).category("category 3").build())
            .addProduct(Product.Builder("name 4", "sku", 0.0).category("category 4").build())
            .productListSource("some product list source")
            .customAttributes(eventAttributes)
            .checkoutStep(5).build()
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        commerceEvent = CommerceEvent.Builder(Product.CHECKOUT, product)
            .addProduct(
                Product.Builder("name 1", "sku", 0.0).category("category 1")
                    .customAttributes(productAttributes).build()
            )
            .addProduct(
                Product.Builder("name 2", "sku", 0.0).category("category 2")
                    .customAttributes(productAttributes).build()
            )
            .addProduct(Product.Builder("name 3", "sku", 0.0).category("category 3").build())
            .addProduct(Product.Builder("name 4", "sku", 0.0).category("category 4").build())
            .productListSource("some product list source")
            .customAttributes(eventAttributes)
            .checkoutStep(7).build()
        Assert.assertTrue(customMapping.isMatch(CommerceEventWrapper(commerceEvent)))
        var result = customMapping.project(CommerceEventWrapper(commerceEvent))
        Assert.assertEquals(1, result.size.toLong())
        val projectionResult = result[0]
        val event = projectionResult.commerceEvent
        Assert.assertNotNull(event)
        Assert.assertEquals("checkout - place order", event.eventName)
        Assert.assertEquals(5, event.products!!.size.toLong())
        Assert.assertEquals(
            "category 4",
            event.customAttributeStrings!!["Last Place Order Category"]
        )
        Assert.assertEquals(
            "some product list source",
            event.customAttributeStrings!!["Projected list source"]
        )
        Assert.assertEquals(
            "sample custom event value",
            event.customAttributeStrings!!["Projected sample event attribute"]
        )
        config.getJSONObject("behavior").put("selector", "foreach")
        result = CustomMapping(config).project(CommerceEventWrapper(commerceEvent))
        Assert.assertEquals(5, result.size.toLong())
        for (i in result.indices) {
            val projectionResult1 = result[i]
            val event1 = projectionResult1.commerceEvent
            Assert.assertEquals("checkout - place order", event1.eventName)
            Assert.assertEquals(1, event1.products!!.size.toLong())
            Assert.assertEquals(
                "some product list source",
                event1.customAttributeStrings!!["Projected list source"]
            )
            Assert.assertEquals(
                "sample custom event value",
                event1.customAttributeStrings!!["Projected sample event attribute"]
            )
            if (i == 1 || i == 2) {
                Assert.assertEquals(
                    "sample custom product attribute value",
                    event1.customAttributeStrings!!["Projected sample custom attribute"]
                )
            }
            Assert.assertNotNull(event1)
            Assert.assertEquals(
                "category $i",
                event1.customAttributeStrings!!["Last Place Order Category"]
            )
        }

        // make the ProductAttribute mapping required, should limit down the results to 2 products
        config.getJSONObject("action").getJSONArray("attribute_maps").getJSONObject(1)
            .put("is_required", "true")
        result = CustomMapping(config).project(CommerceEventWrapper(commerceEvent))
        Assert.assertEquals(2, result.size.toLong())
    }

    /**
     * Tests if a custom mapping is properly applied to a ComerceEvent
     * @throws Exception
     */
    @Test
    @Throws(Exception::class)
    fun testCommerceCustomMapping() {
        val cmEventName = "test_content_view"
        val cmQuantityName = "test_quantity"
        val cmProductName = "test_content_quantity"
        val cmCurrencyName = "test_currency"
        val cmStaticCurrency = "USD"
        val customAttributes = HashMap<String, String?>()
        customAttributes["customAttribute1"] = "value1"
        customAttributes["customAttribute2"] = "value2"
        val name = "product_1"
        val sku = "sku_12345"
        val quantity = 3f
        val kitConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                "{ \"id\":92, \"as\":{ \"devKey\":\"HXpL4jHPTkUzwmcrJJFV9k\", \"appleAppId\":null }, \"hs\":{ }, \"pr\":[ { \"id\":166, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" } ] }, { \"id\":157, \"pmid\":540, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"$cmEventName\", \"attribute_maps\":[ { \"projected_attribute_name\":\"$cmProductName\", \"match_type\":\"Hash\", \"value\":\"1455148719\", \"data_type\":\"String\", \"property\":\"ProductField\" }, { \"projected_attribute_name\":\"$cmQuantityName\", \"match_type\":\"Hash\", \"value\":\"1817448224\", \"data_type\":\"Float\", \"property\":\"ProductField\" }, { \"projected_attribute_name\":\"$cmCurrencyName\", \"match_type\":\"Static\", \"value\":\"$cmStaticCurrency\", \"data_type\":\"String\" } ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1572\" } ] } ] }"
            )
        )
        val product = Product.Builder(name, sku, quantity.toDouble())
            .build()
        val commerceEvent = CommerceEvent.Builder(Product.DETAIL, product)
            .customAttributes(customAttributes)
            .build()
        val results =
            CustomMapping.projectEvents(commerceEvent, kitConfiguration.customMappingList, null)
        val expectedInfo: MutableMap<String, String?> = HashMap()
        expectedInfo[cmQuantityName] = quantity.toString()
        expectedInfo[cmProductName] = name
        expectedInfo[cmCurrencyName] = cmStaticCurrency
        expectedInfo.putAll(customAttributes)
        val expectedMappedEvent = MPEvent.Builder(cmEventName)
            .customAttributes(expectedInfo)
            .build()
        Assert.assertEquals(1, results.size.toLong())
        val result = results[0].mpEvent
        Assert.assertEquals(expectedMappedEvent.eventName, result.eventName)
        Assert.assertEquals(
            expectedMappedEvent.customAttributeStrings!!.keys.size.toLong(),
            result.customAttributeStrings!!.size.toLong()
        )
        for (key in expectedMappedEvent.customAttributeStrings!!.keys) {
            val `val` = result.customAttributeStrings!![key]
            Assert.assertTrue(`val` != null)
            Assert.assertEquals(expectedMappedEvent.customAttributeStrings!![key], `val`)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSingleBasicMatch() {
        val config =
            JSONObject("{ \"id\":144, \"pmmid\":24, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"new_premium_subscriber\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"subscription_success\", \"attribute_key\":\"plan\", \"attribute_values\":[ \"premium\" ] } ] }")
        val mapping = CustomMapping(config)
        val info: MutableMap<String, String?> = HashMap()
        info["plan"] = "premium"
        info["plan 2"] = "premium 2"
        var event = MPEvent.Builder("subscription_success").customAttributes(info).build()
        val events = mapping.project(MPEventWrapper(event))
        Assert.assertTrue(mapping.isMatch(MPEventWrapper(event)))
        Assert.assertTrue(events.size == 1)
        Assert.assertTrue(events[0].mpEvent.eventName == "new_premium_subscriber")
        Assert.assertTrue(events[0].mpEvent.customAttributeStrings?.get("plan") == "premium")
        Assert.assertTrue(events[0].mpEvent.customAttributeStrings?.get("plan 2") == "premium 2")
        info["plan"] = "notPremium"
        event = MPEvent.Builder("subscription_success").customAttributes(info).build()
        Assert.assertFalse(mapping.isMatch(MPEventWrapper(event)))
    }

    @Test
    @Throws(Exception::class)
    fun testORingAttributeValues() {
        val config =
            JSONObject("{ \"id\":167, \"pmmid\":26, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"X_FIRST_APP_OPEN\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"APPLICATION_START\", \"attribute_key\":\"has_launched_before\", \"attribute_values\":[ \"false\", \"N\" ] } ] }")
        val mapping = CustomMapping(config)
        val info: MutableMap<String, String?> = HashMap()
        info["has_launched_before"] = "Y"
        var event = MPEvent.Builder("APPLICATION_START").customAttributes(info).build()
        Assert.assertFalse(mapping.isMatch(MPEventWrapper(event)))
        info["has_launched_before"] = "false"
        event = MPEvent.Builder("APPLICATION_START").customAttributes(info).build()
        Assert.assertTrue(mapping.isMatch(MPEventWrapper(event)))
        info["has_launched_before"] = "N"
        event = MPEvent.Builder("APPLICATION_START").customAttributes(info).build()
        Assert.assertTrue(
            mapping.matchList[0].attributeValues.toString(),
            mapping.isMatch(MPEventWrapper(event))
        )
    }

    @Test
    @Throws(Exception::class)
    fun testANDingMatchedeMatches() {
        val config =
            JSONObject("{ \"id\":171, \"pmmid\":30, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"X_NEW_NOAH_SUBSCRIPTION\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":[ \"new_subscription\" ] }, { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"plan_id\", \"attribute_values\":[ \"3\", \"8\" ] } ] }")
        val mapping = CustomMapping(config)
        val info: MutableMap<String, String?> = HashMap()
        info["outcome"] = "new_subscription"
        var event = MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build()
        Assert.assertFalse(mapping.isMatch(MPEventWrapper(event)))
        info["plan_id"] = "3"
        event = MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build()
        Assert.assertTrue(mapping.isMatch(MPEventWrapper(event)))
        info["plan_id"] = "8"
        event = MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build()
        Assert.assertTrue(mapping.isMatch(MPEventWrapper(event)))
        val events = mapping.project(MPEventWrapper(event))
        Assert.assertTrue(events.size == 1)
        Assert.assertTrue(events[0].mpEvent.eventName == "X_NEW_NOAH_SUBSCRIPTION")
        Assert.assertTrue(events[0].mpEvent.customAttributeStrings?.get("plan_id") == "8")
        Assert.assertTrue(events[0].mpEvent.customAttributeStrings?.get("outcome") == "new_subscription")
        info.remove("outcome")
        event = MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build()
        Assert.assertFalse(mapping.isMatch(MPEventWrapper(event)))
    }

    companion object {
        const val JSON =
            "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[{\"id\":92, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"pr\":[{\"id\":5, \"pmid\":167, \"pmmid\":26, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"APPLICATION_START\", \"attribute_key\":\"has_launched_before\", \"attribute_values\":[\"true\", \"Y\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_FIRST_APP_OPEN\", \"attribute_maps\":[] } }, {\"id\":6, \"pmid\":168, \"pmmid\":27, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"APPLICATION_START\", \"attribute_key\":\"has_launched_before\", \"attribute_values\":[\"false\", \"N\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_SUBSEQUENT_APP_OPEN\", \"attribute_maps\":[] } }, {\"id\":7, \"pmid\":169, \"pmmid\":28, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_START\", \"attribute_key\":\"has_device_authenticated_before\", \"attribute_values\":[\"false\", \"N\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NONSUB_SUBSCRIPTION_START\", \"attribute_maps\":[] } }, {\"id\":8, \"pmid\":170, \"pmmid\":29, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":\"new_subscription\"} ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NEW_SUBSCRIPTION\", \"attribute_maps\":[] } }, {\"id\":9, \"pmid\":171, \"pmmid\":30, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":\"new_subscription\"}, {\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"plan_id\", \"attribute_values\":[\"3\", \"8\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NEW_NOAH_SUBSCRIPTION\", \"attribute_maps\":[] } }, {\"id\":10, \"pmid\":172, \"pmmid\":31, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":\"new_subscription\"}, {\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"plan_id\", \"attribute_values\":[\"1\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NEW_SASH_SUBSCRIPTION\", \"attribute_maps\":[] } } ] }, {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_values\":\"\$ProductView\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"String\", \"value\":\"ProductCategory\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"String\", \"value\":\"ProductQuantity\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"String\", \"value\":\"RevenueAmount\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"String\", \"value\":\"ProductSKU\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"String\", \"value\":\"CurrencyCode\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"String\", \"value\":\"ProductName\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"} ], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":180, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":181, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] } ], \"cms\":[{\"id\":28, \"pr\":[{\"f\":\"com.appboy.installation\", \"m\":0, \"ps\":[{\"k\":\"installation_id\", \"t\":1, \"n\":\"iid\", \"d\":\"%g%\"} ] }, {\"f\":\"com.appboy.device\", \"m\":0, \"ps\":[{\"k\":\"device_id\", \"t\":1, \"n\":\"di\", \"d\":\"\"} ] }, {\"f\":\"com.appboy.offline.storagemap\", \"m\":0, \"ps\":[{\"k\":\"last_user\", \"t\":1, \"n\":\"lu\", \"d\":\"\"} ] } ] } ], \"lsv\":\"2.3.1\", \"tri\":{\"mm\":[{\"dt\":\"x\", \"eh\":true }, {\"dt\":\"x\", \"eh\":false }, {\"dt\":\"ast\", \"t\":\"app_init\", \"ifr\":true, \"iu\":false } ], \"evts\":[1594525888, -460386492, -1633998520, -201964253, -1698163721, -88346394, -964353845, 925748342, 1515120746, 476338248, -2049994443 ] }, \"pio\":30 }"

        @BeforeClass
        fun setupAll() {
            val mockMp = Mockito.mock(MParticle::class.java)
            Mockito.`when`(mockMp.environment).thenReturn(MParticle.Environment.Development)
            MParticle.setInstance(mockMp)
        }
    }
}
