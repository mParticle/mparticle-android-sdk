package com.mparticle.kits.mappings;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.kits.CommerceEventUtils;
import com.mparticle.kits.KitConfiguration;
import com.mparticle.kits.KitUtils;
import com.mparticle.mock.MockKitConfiguration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CustomMappingTest {
    static final String JSON = "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[{\"id\":92, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"pr\":[{\"id\":5, \"pmid\":167, \"pmmid\":26, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"APPLICATION_START\", \"attribute_key\":\"has_launched_before\", \"attribute_values\":[\"true\", \"Y\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_FIRST_APP_OPEN\", \"attribute_maps\":[] } }, {\"id\":6, \"pmid\":168, \"pmmid\":27, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"APPLICATION_START\", \"attribute_key\":\"has_launched_before\", \"attribute_values\":[\"false\", \"N\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_SUBSEQUENT_APP_OPEN\", \"attribute_maps\":[] } }, {\"id\":7, \"pmid\":169, \"pmmid\":28, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_START\", \"attribute_key\":\"has_device_authenticated_before\", \"attribute_values\":[\"false\", \"N\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NONSUB_SUBSCRIPTION_START\", \"attribute_maps\":[] } }, {\"id\":8, \"pmid\":170, \"pmmid\":29, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":\"new_subscription\"} ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NEW_SUBSCRIPTION\", \"attribute_maps\":[] } }, {\"id\":9, \"pmid\":171, \"pmmid\":30, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":\"new_subscription\"}, {\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"plan_id\", \"attribute_values\":[\"3\", \"8\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NEW_NOAH_SUBSCRIPTION\", \"attribute_maps\":[] } }, {\"id\":10, \"pmid\":172, \"pmmid\":31, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":\"new_subscription\"}, {\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"plan_id\", \"attribute_values\":[\"1\"] } ], \"behaviors\":{\"max_custom_params\":0, \"append_unmapped_as_is\":false, \"is_default\":false }, \"action\":{\"projected_event_name\":\"X_NEW_SASH_SUBSCRIPTION\", \"attribute_maps\":[] } } ] }, {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_values\":\"$ProductView\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"String\", \"value\":\"ProductCategory\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"String\", \"value\":\"ProductQuantity\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"String\", \"value\":\"RevenueAmount\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"String\", \"value\":\"ProductSKU\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"String\", \"value\":\"CurrencyCode\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"String\", \"value\":\"ProductName\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"} ], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":180, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":181, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"} ], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] } ], \"cms\":[{\"id\":28, \"pr\":[{\"f\":\"com.appboy.installation\", \"m\":0, \"ps\":[{\"k\":\"installation_id\", \"t\":1, \"n\":\"iid\", \"d\":\"%g%\"} ] }, {\"f\":\"com.appboy.device\", \"m\":0, \"ps\":[{\"k\":\"device_id\", \"t\":1, \"n\":\"di\", \"d\":\"\"} ] }, {\"f\":\"com.appboy.offline.storagemap\", \"m\":0, \"ps\":[{\"k\":\"last_user\", \"t\":1, \"n\":\"lu\", \"d\":\"\"} ] } ] } ], \"lsv\":\"2.3.1\", \"tri\":{\"mm\":[{\"dt\":\"x\", \"eh\":true }, {\"dt\":\"x\", \"eh\":false }, {\"dt\":\"ast\", \"t\":\"app_init\", \"ifr\":true, \"iu\":false } ], \"evts\":[1594525888, -460386492, -1633998520, -201964253, -1698163721, -88346394, -964353845, 925748342, 1515120746, 476338248, -2049994443 ] }, \"pio\":30 }";

    @BeforeClass
    public static void setupAll() {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
    }
    /**
     * This is pretty implementation specific. Just making sure that the objects we create accurately reflect the JSON.
     */
    @Test
    public void testParsing() throws Exception {
        JSONObject json = new JSONObject(JSON);
        JSONArray ekConfigs = json.getJSONArray("eks");
        for (int i = 0; i < ekConfigs.length(); i++) {
            JSONObject config = ekConfigs.getJSONObject(i);
         //   if (config.getInt("id") == 56) {
                JSONArray projJsonList = config.getJSONArray("pr");
                for (int j = 0; j < projJsonList.length(); j++) {
                    JSONObject pJson = projJsonList.getJSONObject(j);
                    CustomMapping customMapping = new CustomMapping(pJson);
                    assertEquals(pJson.getInt("id"), customMapping.mID);
                    //sometimes pmmid doesn't exist...:/
                    // assertEquals(pJson.getInt("pmmid"), projection.mModuleMappingId);
                    JSONArray matches = pJson.getJSONArray("matches");
                    assertEquals(matches.getJSONObject(0).getInt("message_type"), customMapping.getMessageType());
                    assertEquals(matches.getJSONObject(0).getString("event_match_type"), customMapping.getMatchList().get(0).mMatchType);
                    if (customMapping.getMatchList().get(0).mMatchType.startsWith(CustomMapping.MATCH_TYPE_HASH)) {
                        assertEquals(matches.getJSONObject(0).getInt("event"), customMapping.getMatchList().get(0).mEventHash);
                    } else {
                        assertEquals(matches.getJSONObject(0).getString("event"), customMapping.getMatchList().get(0).mEventName);
                    }
                    if (matches.getJSONObject(0).has("attribute_key")) {
                        assertEquals(matches.getJSONObject(0).getString("attribute_key"), customMapping.getMatchList().get(0).mAttributeKey);
                        if (matches.getJSONObject(0).get("attribute_values") instanceof JSONArray) {
                            JSONArray attributeValues = matches.getJSONObject(0).getJSONArray("attribute_values");
                            assertEquals(attributeValues.length(), customMapping.getMatchList().get(0).getAttributeValues().size());
                        } else {
                            assertTrue(customMapping.getMatchList().get(0).getAttributeValues().contains(matches.getJSONObject(0).getString("attribute_values").toLowerCase(Locale.US)));
                        }
                    }

                    if (pJson.has("behavior")) {
                        JSONObject behaviors = pJson.getJSONObject("behavior");
                        assertEquals(behaviors.optBoolean("is_default"), customMapping.isDefault());
                        assertEquals(behaviors.optBoolean("append_unmapped_as_is"), customMapping.mAppendUnmappedAsIs);
                        assertEquals(behaviors.optInt("max_custom_params", Integer.MAX_VALUE), customMapping.mMaxCustomParams);
                    }

                    JSONObject action = pJson.getJSONObject("action");

                    assertEquals(customMapping.mProjectedEventName, action.getString("projected_event_name"));

                    JSONArray attributes = action.getJSONArray("attribute_maps");
                    int sum = (customMapping.mRequiredAttributeMapList == null ? 0 : customMapping.mRequiredAttributeMapList.size()) +
                            (customMapping.mStaticAttributeMapList == null ? 0 : customMapping.mStaticAttributeMapList.size());
                    assertEquals(attributes.length(), sum);
                    for (int k = 0; k < attributes.length(); k++) {
                        JSONObject attribute = attributes.getJSONObject(k);
                        CustomMapping.AttributeMap attProj = new CustomMapping.AttributeMap(attribute);
                        assertEquals(attribute.optBoolean("is_required"), attProj.mIsRequired);
                        if (attribute.getString("match_type").startsWith(CustomMapping.MATCH_TYPE_STATIC)) {
                            assertFalse(customMapping.mRequiredAttributeMapList.contains(attProj));
                            assertTrue(customMapping.mStaticAttributeMapList.contains(attProj));
                        } else {
                            assertTrue(customMapping.mRequiredAttributeMapList.contains(attProj));
                            assertFalse(customMapping.mStaticAttributeMapList.contains(attProj));
                        }
                    }
                    boolean notRequiredFound = false;
                    for (CustomMapping.AttributeMap attributeMap : customMapping.mRequiredAttributeMapList) {
                        if (notRequiredFound && attributeMap.mIsRequired) {
                            fail("Projection attributes are out of order!");
                        }
                        notRequiredFound = !attributeMap.mIsRequired;
                    }
                }
           // }
        }
    }

    @Test
    public void testDefault() throws Exception {
        CustomMapping defaultCustomMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).category("whatever!!").customAttributes(info).build();
        assertTrue(defaultCustomMapping.isMatch(new EventWrapper.MPEventWrapper(event)));
        MPEvent newEvent = defaultCustomMapping.project(new EventWrapper.MPEventWrapper(event)).get(0).getMPEvent();
        assertEquals(event, newEvent);
        info.put("yet another key", "yet another value");
        //sanity check to make sure we're duplicating the map, not passing around a reference
        assertTrue(event.getCustomAttributeStrings().containsKey("yet another key"));
        assertFalse(newEvent.getCustomAttributeStrings().containsKey("yet another key"));
    }

    /**
     * Same as above but evaluating from one level up, from the Provider's perspective
     */
    @Test
    public void testDefaultProjection2() throws Exception {
        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject("{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_values\":\"$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build();
        List<CustomMapping.ProjectionResult> list = CustomMapping.projectEvents(event, kitConfiguration.getCustomMappingList(), kitConfiguration.getDefaultEventProjection());
        assertEquals(1, list.size());
        assertEquals(list.get(0).getMPEvent(), event);
    }

    /**
     * Tests both that when there's no default specified, return null, and also matching on message_type
     * @throws Exception
     */
    @Test
    public void testNoDefaultEventAndDefaultScreen() throws Exception {
        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject("{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_values\":\"$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":3, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build();
        List<CustomMapping.ProjectionResult> list = CustomMapping.projectEvents(event, kitConfiguration.getCustomMappingList(), kitConfiguration.getDefaultEventProjection());
        assertNull(list);
        //but if we say this is a screen event, the default projection should be detected
        list = CustomMapping.projectEvents(event, true, kitConfiguration.getCustomMappingList(), kitConfiguration.getDefaultEventProjection(), kitConfiguration.getDefaultScreenCustomMapping());
        assertEquals(1, list.size());
    }

    /**
     * Make sure that we respect the append_unmapped_as_is property
     *
     * @throws Exception
     */
    @Test
    public void testDontAppendAsIs() throws Exception {
        CustomMapping customMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":false, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build();
        CustomMapping.ProjectionResult newEvent = customMapping.project(new EventWrapper.MPEventWrapper(event)).get(0);
        assertTrue(newEvent.getMPEvent().getCustomAttributeStrings().size() == 0);
        customMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        newEvent = customMapping.project(new EventWrapper.MPEventWrapper(event)).get(0);
        assertTrue(newEvent.getMPEvent().getCustomAttributeStrings().size() == 2);
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
    public void testMaxParams() throws Exception {
        CustomMapping customMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":5, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        //add them out of order, why not?
        info.put("key 3", "123af4");
        info.put("key 4", "123adf45");
        info.put("key 1", "12ad3456");
        info.put("key 6", "asdasd");
        info.put("key 2", "asdasdaf");
        info.put("key 5", "asdfasea");

        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).customAttributes(info).build();
        CustomMapping.ProjectionResult result = customMapping.project(new EventWrapper.MPEventWrapper(event)).get(0);
        MPEvent newEvent = result.getMPEvent();
        assertTrue(newEvent.getCustomAttributeStrings().size() == 5);
        //key 5 should be there but key 6 should be kicked out due to alphabetic sorting
        assertTrue(newEvent.getCustomAttributeStrings().containsKey("key 5"));
        assertFalse(newEvent.getCustomAttributeStrings().containsKey("key 6"));
        //now create the SAME projection, except specify an optional attribute key 6 - it should boot key 5 from the resulting event
        customMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":5, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" }] } }"));
        result = customMapping.project(new EventWrapper.MPEventWrapper(event)).get(0);
        newEvent = result.getMPEvent();
        assertFalse(newEvent.getCustomAttributeStrings().containsKey("key 5"));
        assertTrue(newEvent.getCustomAttributeStrings().containsKey("key 6")); //note that the json also doesn't specify a key name - so we just use the original
        assertTrue(newEvent.getCustomAttributeStrings().size() == 5);

        //test what happens if max isn't even set (everything should be there)
        customMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" }] } }"));
        result = customMapping.project(new EventWrapper.MPEventWrapper(event)).get(0);
        newEvent = result.getMPEvent();
        assertTrue(newEvent.getCustomAttributeStrings().containsKey("key 5"));
        assertTrue(newEvent.getCustomAttributeStrings().containsKey("key 6")); //note that the json also doesn't specify a key name - so we just use the original
        assertTrue(newEvent.getCustomAttributeStrings().size() == 6);
    }

    /**
     * Test an attribute projection with match_type Field
     *
     * @throws Exception
     */
    @Test
    public void testFieldName() throws Exception {
        CustomMapping customMapping = new CustomMapping(new JSONObject("{ \"id\":89, \"matches\":[{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }], \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"));
        MPEvent event = new MPEvent.Builder("screenname", MParticle.EventType.Other).build();
        CustomMapping.ProjectionResult projectedEvent = customMapping.project(new EventWrapper.MPEventWrapper(event)).get(0);
        assertEquals("screenname", projectedEvent.getMPEvent().getCustomAttributeStrings().get("screen_name_key"));
    }

    /**
     * EventWrapper is used to add some properties to an MPEvent without having to expose them in the public API
     *
     * It abstracts the differences between MPEvents that are screen views vs. app events, and also allows for caching
     * of attribute hashes
     */
    @Test
    public void testEventWrapper() {
        Map<String, String> info = new HashMap<String, String>();
        info.put("key 1", "value 1");
        MPEvent event = new MPEvent.Builder("some event name", MParticle.EventType.Other).customAttributes(info).build();
        EventWrapper.MPEventWrapper wrapper = new EventWrapper.MPEventWrapper(event);

        //make sure the attribute hashes work as intended
        Map<Integer, String> hashes = wrapper.getAttributeHashes();
        String key = hashes.get(KitUtils.hashForFiltering(event.getEventType().ordinal() + event.getEventName() + "key 1"));
        assertEquals(info.get(key), "value 1");

        //make sure event hash is generated correctly
        assertEquals(KitUtils.hashForFiltering(event.getEventType().ordinal() + event.getEventName()),wrapper.getEventHash());

        assertEquals(4, wrapper.getMessageType());
        assertEquals(event.getEventType().ordinal(), wrapper.getEventTypeOrdinal());
    }

    /**
     * Same as testEventWrapper except with isScreenEvent=true
     */
    @Test
    public void testScreenEventWrapper() {
        Map<String, String> info = new HashMap<String, String>();
        info.put("key 1", "value 1");
        MPEvent event = new MPEvent.Builder("some event name", MParticle.EventType.Other).customAttributes(info).build();
        EventWrapper.MPEventWrapper wrapper = new EventWrapper.MPEventWrapper(event, true);

        //make sure the attribute hashes work as intended
        Map<Integer, String> hashes = wrapper.getAttributeHashes();
        //event type be 0 for screen views
        String key = hashes.get(KitUtils.hashForFiltering(0 + event.getEventName() + "key 1"));
        assertEquals(info.get(key), "value 1");

        //make sure event hash is generated correctly
        assertEquals(KitUtils.hashForFiltering(0 + event.getEventName()),wrapper.getEventHash());

        assertEquals(3, wrapper.getMessageType());
        assertEquals(0, wrapper.getEventTypeOrdinal());
    }

    /**
     * Test 1
     * The first test shows that we correctly match on the Hash, but that the other projections are not triggered b/c attributes are missing
     * @throws Exception
     */
    @Test
    public void testMultiHashProjections1() throws Exception {

        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject("{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_values\":\"$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"));

        /**
         * Test 1
         * The first test shows that we correctly match on the Hash, but that the other projections are not triggered b/c attributes are missing
         */
        //all of these projections match on a Hash of the following name/type
        MPEvent.Builder builder = new MPEvent.Builder("sproj 1", MParticle.EventType.UserContent);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("attribute we don't care about", "some value");
        builder.customAttributes(attributes);
        List<CustomMapping.ProjectionResult> eventList = CustomMapping.projectEvents(builder.build(), kitConfiguration.getCustomMappingList(), kitConfiguration.getDefaultEventProjection());
        assertEquals(1, eventList.size());
        MPEvent projEvent1 = eventList.get(0).getMPEvent();
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getCustomAttributeStrings().get("attribute we don't care about"));


    }

    /**
     * Test 2
     * Now add a new attribute "Value" to the same event, which now triggers the original projection and a new projection
     * that requires the new attribute
     * @throws Exception
     */
    @Test
    public void testMultiHashProjections2() throws Exception {

        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject("{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_values\":\"$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"));

        MPEvent.Builder builder = new MPEvent.Builder("sproj 1", MParticle.EventType.UserContent);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("attribute we don't care about", "some value");
        builder.customAttributes(attributes);

        //add an attribute that's required by 1 of them, we should end up with 2 triggered projections
        attributes.put("Value", "product name");
        List<CustomMapping.ProjectionResult> eventList = CustomMapping.projectEvents(builder.build(), kitConfiguration.getCustomMappingList(), kitConfiguration.getDefaultEventProjection());
        assertEquals(2, eventList.size());
        MPEvent projEvent1 = eventList.get(0).getMPEvent();
        //same as test 1, but verify for the fun of it.
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getCustomAttributeStrings().get("attribute we don't care about"));

        //this is the new projection which requires the Value attribute
        projEvent1 = eventList.get(1).getMPEvent();
        assertEquals("pdp - add to tote", projEvent1.getEventName());
        //required attribute
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Category")); //the required attribute has been renamed
        //non-required attributes which define the same hash as the required one.
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Total Amount"));
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Last Add to Tote SKU"));

        //static attributes are in this projection as well.
        assertEquals("10", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Quantity"));
        assertEquals("1321", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Unit Price"));

    }

    /**
     * Test 3
     * The final test shows adding another attribute which not only triggers a 3rd projection, but also adds onto the 2nd projection which
     * defines that attribute as non-required.
     *
     * @throws Exception
     */
    @Test
    public void testMultiHashProjections3() throws Exception {

        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject("{\"id\":56, \"as\":{}, \"hs\":{}, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_values\":\"$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"cool_product_view\"} }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] }"));

        MPEvent.Builder builder = new MPEvent.Builder("sproj 1", MParticle.EventType.UserContent);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("attribute we don't care about", "some value");
        builder.customAttributes(attributes);

        //add an attribute that's required by 1 of them, we should end up with 2 triggered projections
        attributes.put("Value", "product name");

        /**
         * Test 3
         * The final test shows adding another attribute which not only triggers a 3rd projection, but also adds onto the 2nd projection which
         * defines that attribute as non-required.
         */
        attributes.put("Label", "product label");
        List<CustomMapping.ProjectionResult> eventList = CustomMapping.projectEvents(builder.build(), kitConfiguration.getCustomMappingList(), kitConfiguration.getDefaultEventProjection());
        assertEquals(3, eventList.size());

        MPEvent projEvent1 = eventList.get(0).getMPEvent();
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getCustomAttributeStrings().get("attribute we don't care about"));

        projEvent1 = eventList.get(1).getMPEvent();
        assertEquals("pdp - add to tote", projEvent1.getEventName());
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Category"));
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Total Amount"));
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Last Add to Tote SKU"));
        assertEquals("10", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Quantity"));
        assertEquals("1321", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Unit Price"));

        //these are new for the 2nd projection, as they match the Hash for the new  "Label" attribute
        assertEquals("product label", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Name"));
        assertEquals("product label", projEvent1.getCustomAttributeStrings().get("Last Add to Tote Print"));

        //and here's our 3rd projection, which defines both the original "Value" attribute hash as well as "Label"
        projEvent1 = eventList.get(2).getMPEvent();
        assertEquals("pdp - complete the look", projEvent1.getEventName());
        assertEquals("product name", projEvent1.getCustomAttributeStrings().get("Complete the Look Product Name"));
        assertEquals("product label", projEvent1.getCustomAttributeStrings().get("Complete the Look Product Name 2"));

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
    public void testCommerceEventToMPEvent() throws Exception {
        String config = "{\"id\":93, \"pmid\":220, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1572\"}], \"behavior\":{\"max_custom_params\":0, \"selector\":\"foreach\"}, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"Hash\", \"value\":\"2000445218\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"Hash\", \"value\":\"881337592\", \"data_type\":\"String\", \"property\":\"EventField\"}, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"Hash\", \"value\":\"1514047\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"Hash\", \"value\":\"1455148719\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"Hash\", \"value\":\"664929967\", \"data_type\":\"Int\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"Hash\", \"value\":\"1647761705\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":4 } }";
        CustomMapping customMapping = new CustomMapping(new JSONObject(config));
        Product.Builder productBuilder = new Product.Builder("product name 0", "product id 0", 1).category("product category 0").quantity(1);

        CommerceEvent.Builder commerceEventBuilder = new CommerceEvent.Builder(Product.DETAIL, productBuilder.build()).currency("dollar bills");
        for (int i = 1; i < 5; i++){
            commerceEventBuilder.addProduct(productBuilder.name("product name " + i).sku("product id " + i).category("product category " + i).quantity(i+1).unitPrice(i+1).build());
        }
        CommerceEvent commerceEvent = commerceEventBuilder.build();
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        List<CustomMapping.ProjectionResult> result = customMapping.project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(5, result.size());

        for (int i = 0; i < 5; i++) {
            MPEvent event = result.get(i).getMPEvent();
            assertNotNull(event);
            assertEquals("pdp - product view", event.getEventName());
            Map<String, String> attributes = event.getCustomAttributeStrings();
            assertEquals("product category " + i, attributes.get("Last Product View Category"));
            assertEquals("dollar bills", attributes.get("Last Product View Currency"));
            assertEquals("product id " + i, attributes.get("Last Product View SKU"));
            assertEquals("product name " + i, attributes.get("Last Product View Name"));
            assertEquals(i+1+".0", attributes.get("Last Product View Quantity"));
            assertEquals((i+1)*(i+1)+".0", attributes.get("Last Product View Total Amount"));
            assertEquals("pdp - product view", event.getEventName());
        }
    }

    @Test
    public void testMatchCommerceEventType() throws Exception {
        Product product = new Product.Builder("name", "sku", 0).build();
        JSONObject config = new JSONObject("{\"id\":99, \"pmid\":229, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", }], \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }");
        CommerceEvent event = new CommerceEvent.Builder(Product.DETAIL, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        CustomMapping customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

         event = new CommerceEvent.Builder(Product.ADD_TO_CART, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
         customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.PURCHASE, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.ADD_TO_WISHLIST, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.CHECKOUT, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.CHECKOUT_OPTION, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.CLICK, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.REFUND, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.REMOVE_FROM_CART, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Product.REMOVE_FROM_WISHLIST, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(new Impression("list name", product)).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Promotion.VIEW, new Promotion().setId("id")).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Promotion.CLICK, new Promotion().setId("id")).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONArray("matches").getJSONObject(0).put("event",""+KitUtils.hashForFiltering(""+ CommerceEventUtils.getEventType(event)));
        customMapping = new CustomMapping(config);
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(event)));
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
    public void testMatchCommerceEventProperty() throws Exception {
        JSONObject config = new JSONObject("{\"id\":99, \"pmid\":229, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", \"property\":\"EventField\", \"property_name\":\"-601244443\", \"property_values\":[\"5\"]}], \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }");
        CustomMapping customMapping = new CustomMapping(config);
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product).checkoutStep(4).build();
        assertFalse(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product).checkoutStep(5).build();
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));

        config.getJSONArray("matches").getJSONObject(0).put("property", "EventAttribute");
        customMapping = new CustomMapping(config);
        assertFalse(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("Checkout Step", "5");
        commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product).customAttributes(attributes).build();
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));

        config.getJSONArray("matches").getJSONObject(0).put("property", "ProductAttribute");
        customMapping = new CustomMapping(config);
        assertFalse(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product).addProduct(new Product.Builder("name 2", "sku", 0).customAttributes(attributes).build()).build();
        EventWrapper.CommerceEventWrapper wrapper = new EventWrapper.CommerceEventWrapper(commerceEvent);
        assertTrue(customMapping.isMatch(wrapper));
        //only 1 product has the required attribute so there should only be 1 product after the matching step
        assertEquals(1, wrapper.getEvent().getProducts().size());
        assertEquals("name 2", wrapper.getEvent().getProducts().get(0).getName());

        config.getJSONArray("matches").getJSONObject(0).put("property", "ProductField");
        config.getJSONArray("matches").getJSONObject(0).put("property_name", "-1167125985"); //checkout + category hash
        JSONArray values = new JSONArray();
        values.put("some product cat");
        config.getJSONArray("matches").getJSONObject(0).put("property_values",values);
        customMapping = new CustomMapping(config);
        assertFalse(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product).addProduct(new Product.Builder("name 2", "sku", 0).category("some product cat").build()).build();
        wrapper = new EventWrapper.CommerceEventWrapper(commerceEvent);
        assertTrue(customMapping.isMatch(wrapper));
        //only 1 product has the required attribute so there should only be 1 product after the matching step
        assertEquals(1, wrapper.getEvent().getProducts().size());
        assertEquals("some product cat", wrapper.getEvent().getProducts().get(0).getCategory());

        config.getJSONArray("matches").getJSONObject(0).put("property", "PromotionField");
        config.getJSONArray("matches").getJSONObject(0).put("property_name", "835505623"); //click + creative hash
        values = new JSONArray();
        values.put("some promotion creative");
        config.getJSONArray("matches").getJSONObject(0).put("property_values", values);
        customMapping = new CustomMapping(config);
        assertFalse(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(Promotion.CLICK, new Promotion().setCreative("some promotion creative")).addPromotion(
                new Promotion().setCreative("some other creative")
        ).build();
        wrapper = new EventWrapper.CommerceEventWrapper(commerceEvent);
        assertTrue(customMapping.isMatch(wrapper));
        //only 1 promotion has the required attribute so there should only be 1 promotion after the matching step
        assertEquals(1, wrapper.getEvent().getPromotions().size());
        assertEquals("some promotion creative", wrapper.getEvent().getPromotions().get(0).getCreative());
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
    public void testCommerceEventToCommerceEvent() throws Exception {
        JSONObject config = new JSONObject("{\"id\":99, \"pmid\":229, \"matches\":[{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", \"property\":\"EventField\", \"property_name\":\"-601244443\", \"property_values\":[\"5\", \"7\"]}], \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Projected sample custom attribute\", \"match_type\":\"Hash\", \"value\":\"153565474\", \"data_type\":\"String\", \"property\":\"ProductAttribute\"}, {\"projected_attribute_name\":\"Projected list source\", \"match_type\":\"Hash\", \"value\":\"-882952085\", \"data_type\":\"String\", \"property\":\"EventField\"}, {\"projected_attribute_name\":\"Projected sample event attribute\", \"match_type\":\"Hash\", \"value\":\"1957440897\", \"data_type\":\"String\", \"property\":\"EventAttribute\"} ], \"outbound_message_type\":16 } }");
        CustomMapping customMapping = new CustomMapping(config);
        Product product = new Product.Builder("name", "sku", 0).category("category 0").build();
        Map<String, String> productAttributes = new HashMap<String, String>();
        productAttributes.put("sample custom attribute", "sample custom product attribute value");
        Map<String, String> eventAttributes = new HashMap<String, String>();
        eventAttributes.put("sample event attribute", "sample custom event value");
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product)
                .addProduct(new Product.Builder("name 1", "sku", 0).category("category 1").customAttributes(productAttributes).build())
                .addProduct(new Product.Builder("name 2", "sku", 0).category("category 2").customAttributes(productAttributes).build())
                .addProduct(new Product.Builder("name 3", "sku", 0).category("category 3").build())
                .addProduct(new Product.Builder("name 4", "sku", 0).category("category 4").build())
                .productListSource("some product list source")
                .customAttributes(eventAttributes)
                .checkoutStep(4).build();
        assertFalse(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));

        commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product)
                .addProduct(new Product.Builder("name 1", "sku", 0).category("category 1").customAttributes(productAttributes).build())
                .addProduct(new Product.Builder("name 2", "sku", 0).category("category 2").customAttributes(productAttributes).build())
                .addProduct(new Product.Builder("name 3", "sku", 0).category("category 3").build())
                .addProduct(new Product.Builder("name 4", "sku", 0).category("category 4").build())
                .productListSource("some product list source")
                .customAttributes(eventAttributes)
                .checkoutStep(5).build();
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(Product.CHECKOUT, product)
                .addProduct(new Product.Builder("name 1", "sku", 0).category("category 1").customAttributes(productAttributes).build())
                .addProduct(new Product.Builder("name 2", "sku", 0).category("category 2").customAttributes(productAttributes).build())
                .addProduct(new Product.Builder("name 3", "sku", 0).category("category 3").build())
                .addProduct(new Product.Builder("name 4", "sku", 0).category("category 4").build())
                .productListSource("some product list source")
                .customAttributes(eventAttributes)
                .checkoutStep(7).build();
        assertTrue(customMapping.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        List<CustomMapping.ProjectionResult> result = customMapping.project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(1, result.size());
        CustomMapping.ProjectionResult projectionResult = result.get(0);
        CommerceEvent event = projectionResult.getCommerceEvent();
        assertNotNull(event);
        assertEquals("checkout - place order", event.getEventName());
        assertEquals(5, event.getProducts().size());
        assertEquals("category 4", event.getCustomAttributeStrings().get("Last Place Order Category"));
        assertEquals("some product list source", event.getCustomAttributeStrings().get("Projected list source"));
        assertEquals("sample custom event value", event.getCustomAttributeStrings().get("Projected sample event attribute"));

        config.getJSONObject("behavior").put("selector", "foreach");
        result = new CustomMapping(config).project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(5, result.size());


        for (int i = 0; i < result.size(); i++) {
            CustomMapping.ProjectionResult projectionResult1 = result.get(i);
            CommerceEvent event1 = projectionResult1.getCommerceEvent();
            assertEquals("checkout - place order", event1.getEventName());
            assertEquals(1, event1.getProducts().size());
            assertEquals("some product list source", event1.getCustomAttributeStrings().get("Projected list source"));
            assertEquals("sample custom event value", event1.getCustomAttributeStrings().get("Projected sample event attribute"));
            if (i == 1 || i == 2) {
                assertEquals("sample custom product attribute value", event1.getCustomAttributeStrings().get("Projected sample custom attribute"));
            }
            assertNotNull(event1);
            assertEquals("category " + i, event1.getCustomAttributeStrings().get("Last Place Order Category"));
        }

        //make the ProductAttribute mapping required, should limit down the results to 2 products
        config.getJSONObject("action").getJSONArray("attribute_maps").getJSONObject(1).put("is_required", "true");
        result = new CustomMapping(config).project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(2, result.size());
    }

    /**
     * Tests if a custom mapping is properly applied to a ComerceEvent
     * @throws Exception
     */
    @Test
    public void testCommerceCustomMapping() throws Exception {
        String cmEventName = "test_content_view";
        String cmQuantityName = "test_quantity";
        String cmProductName = "test_content_quantity";
        String cmCurrencyName = "test_currency";
        String cmStaticCurrency = "USD";
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("customAttribute1", "value1");
        customAttributes.put("customAttribute2", "value2");
        String name = "product_1";
        String sku = "sku_12345";
        float quantity = 3;
        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject("{ \"id\":92, \"as\":{ \"devKey\":\"HXpL4jHPTkUzwmcrJJFV9k\", \"appleAppId\":null }, \"hs\":{ }, \"pr\":[ { \"id\":166, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" } ] }, { \"id\":157, \"pmid\":540, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"" + cmEventName + "\", \"attribute_maps\":[ { \"projected_attribute_name\":\"" + cmProductName + "\", \"match_type\":\"Hash\", \"value\":\"1455148719\", \"data_type\":\"String\", \"property\":\"ProductField\" }, { \"projected_attribute_name\":\"" + cmQuantityName +  "\", \"match_type\":\"Hash\", \"value\":\"1817448224\", \"data_type\":\"Float\", \"property\":\"ProductField\" }, { \"projected_attribute_name\":\"" + cmCurrencyName + "\", \"match_type\":\"Static\", \"value\":\"" + cmStaticCurrency + "\", \"data_type\":\"String\" } ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1572\" } ] } ] }"));
        Product product = new Product.Builder(name, sku, quantity)
                .build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(Product.DETAIL, product)
                .customAttributes(customAttributes)
                .build();
        List<CustomMapping.ProjectionResult> results = CustomMapping.projectEvents(commerceEvent, kitConfiguration.getCustomMappingList(), null);
        Map<String, String> expectedInfo = new HashMap<>();
        expectedInfo.put(cmQuantityName, String.valueOf(quantity));
        expectedInfo.put(cmProductName, name);
        expectedInfo.put(cmCurrencyName, cmStaticCurrency);
        expectedInfo.putAll(customAttributes);
        MPEvent expectedMappedEvent = new MPEvent.Builder(cmEventName)
                .customAttributes(expectedInfo)
                .build();
        assertEquals(1, results.size());
        MPEvent result = results.get(0).getMPEvent();
        assertEquals(expectedMappedEvent.getEventName(), result.getEventName());
        assertEquals(expectedMappedEvent.getCustomAttributeStrings().keySet().size(), result.getCustomAttributeStrings().size());
        for (String key: expectedMappedEvent.getCustomAttributeStrings().keySet()) {
            String val = result.getCustomAttributeStrings().get(key);
            assertTrue(val != null);
            assertEquals(expectedMappedEvent.getCustomAttributeStrings().get(key), val);
        }
    }

    @Test
    public void testSingleBasicMatch() throws Exception {
        JSONObject config = new JSONObject("{ \"id\":144, \"pmmid\":24, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"new_premium_subscriber\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"subscription_success\", \"attribute_key\":\"plan\", \"attribute_values\":[ \"premium\" ] } ] }");
        CustomMapping mapping = new CustomMapping(config);
        Map<String, String> info = new HashMap<>();
        info.put("plan", "premium");
        info.put("plan 2", "premium 2");
        MPEvent event = new MPEvent.Builder("subscription_success").customAttributes(info).build();
        List<CustomMapping.ProjectionResult> events = mapping.project(new EventWrapper.MPEventWrapper(event));
        assertTrue(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));
        assertTrue(events.size() == 1);
        assertTrue(events.get(0).getMPEvent().getEventName().equals("new_premium_subscriber"));
        assertTrue(events.get(0).getMPEvent().getCustomAttributeStrings().get("plan").equals("premium"));
        assertTrue(events.get(0).getMPEvent().getCustomAttributeStrings().get("plan 2").equals("premium 2"));
        info.put("plan", "notPremium");
        event = new MPEvent.Builder("subscription_success").customAttributes(info).build();
        assertFalse(mapping.isMatch(new EventWrapper.MPEventWrapper(event) ));
    }

    @Test
    public void testORingAttributeValues() throws Exception {
        JSONObject config = new JSONObject("{ \"id\":167, \"pmmid\":26, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"X_FIRST_APP_OPEN\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"APPLICATION_START\", \"attribute_key\":\"has_launched_before\", \"attribute_values\":[ \"false\", \"N\" ] } ] }");
        CustomMapping mapping = new CustomMapping(config);
        Map<String, String> info = new HashMap<>();
        info.put("has_launched_before", "Y");
        MPEvent event = new MPEvent.Builder("APPLICATION_START").customAttributes(info).build();
        assertFalse(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));

        info.put("has_launched_before", "false");
        event = new MPEvent.Builder("APPLICATION_START").customAttributes(info).build();
        assertTrue(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));

        info.put("has_launched_before", "N");
        event = new MPEvent.Builder("APPLICATION_START").customAttributes(info).build();
        assertTrue(mapping.getMatchList().get(0).getAttributeValues().toString(), mapping.isMatch(new EventWrapper.MPEventWrapper(event)));
    }

    @Test
    public void testANDingMatchedeMatches() throws Exception {
        JSONObject config = new JSONObject("{ \"id\":171, \"pmmid\":30, \"behavior\":{ \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"X_NEW_NOAH_SUBSCRIPTION\", \"attribute_maps\":[ ], \"outbound_message_type\":4 }, \"matches\":[ { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"outcome\", \"attribute_values\":[ \"new_subscription\" ] }, { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"SUBSCRIPTION_END\", \"attribute_key\":\"plan_id\", \"attribute_values\":[ \"3\", \"8\" ] } ] }");
        CustomMapping mapping = new CustomMapping(config);
        Map<String, String> info = new HashMap<>();
        info.put("outcome", "new_subscription");
        MPEvent event = new MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build();
        assertFalse(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));

        info.put("plan_id", "3");
        event = new MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build();
        assertTrue(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));

        info.put("plan_id", "8");
        event = new MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build();
        assertTrue(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));
        List<CustomMapping.ProjectionResult> events = mapping.project(new EventWrapper.MPEventWrapper(event));
        assertTrue(events.size() == 1);
        assertTrue(events.get(0).getMPEvent().getEventName().equals("X_NEW_NOAH_SUBSCRIPTION"));
        assertTrue(events.get(0).getMPEvent().getCustomAttributeStrings().get("plan_id").equals("8"));
        assertTrue(events.get(0).getMPEvent().getCustomAttributeStrings().get("outcome").equals("new_subscription"));
        info.remove("outcome");
        event = new MPEvent.Builder("SUBSCRIPTION_END").customAttributes(info).build();
        assertFalse(mapping.isMatch(new EventWrapper.MPEventWrapper(event)));
    }
}