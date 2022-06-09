package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Impression;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.consent.CCPAConsent;
import com.mparticle.consent.ConsentState;
import com.mparticle.consent.GDPRConsent;
import com.mparticle.identity.MParticleUser;
import com.mparticle.internal.Constants;
import com.mparticle.mock.MockKitConfiguration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class KitConfigurationTest {

    @BeforeClass
    public static void setupAll() {
        MParticle mockMp = Mockito.mock(MParticle.class);
        Mockito.when(mockMp.getEnvironment()).thenReturn(MParticle.Environment.Development);
        MParticle.setInstance(mockMp);
    }
    final String json = "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[{\"id\":28, \"avf\":{\"i\":true, \"a\":3288498, \"v\":3611952 }, \"eau\": true, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"ec\":{\"1824528345\":0, \"1824528346\":0, \"1824528347\":0, \"1824528348\":0, \"1824528341\":0, \"1824528342\":0, \"1824528343\":0, \"1824528350\":0, \"54237\":0, \"-201964253\":0, \"-1015351211\":0, \"-1698163721\":0, \"642507618\":0, \"-1750207685\":0, \"-1400806385\":0, \"-435072817\":0, \"612161276\":0, \"-2049994443\":0, \"1798380893\":0, \"-460386492\":0, \"476338248\":0, \"-1964837950\":0, \"-115592573\":0, \"-1119044065\":0, \"-1229406110\":0, \"1612904218\":0, \"-459588721\":0, \"93769843\":0, \"-1831156729\":0, \"925748342\":0, \"-1471879983\":0, \"-1471879982\":0, \"-1471879985\":0, \"-1471879984\":0, \"-1471879987\":0, \"-1471879986\":0, \"-1471879989\":0, \"-1471879988\":0, \"-1471879991\":0, \"-1471879990\":0, \"-1175432756\":0, \"-1439088726\":0, \"-630837254\":0, \"-1528980234\":0, \"866346720\":0, \"-466914683\":0, \"584870613\":0, \"-71005245\":0, \"-71005246\":0, \"-71005248\":0, \"-71005251\":0, \"-71005254\":0, \"-192614420\":0, \"-1899939497\":0, \"-138049017\":0, \"1755914106\":0, \"1713887651\":0, \"-1680991381\":0, \"1381973565\":0, \"1696869197\":0, \"530926139\":0, \"-1591103548\":0, \"606683084\":0, \"-452884081\":0, \"1156084566\":0, \"-1684704584\":0, \"-1684704582\":0, \"-1684704517\":0, \"-1684704551\":0, \"-1684704492\":0, \"-1684704484\":0, \"-1507615010\":0, \"1496713379\":0, \"1496713380\":0, \"1496713373\":0, \"1496713374\":0, \"1496713371\":0, \"1496713372\":0, \"1496713377\":0, \"1496713375\":0, \"1496713376\":0, \"448941660\":0, \"455787894\":0, \"1057880655\":0, \"-153747136\":0, \"228100699\":0, \"1956870096\":0, \"367619406\":0, \"-1728365802\":0, \"1315260226\":0, \"713953332\":0, \"54115406\":0, \"-1075988785\":0, \"-1726724035\":0, \"1195528703\":0, \"-1415615126\":0, \"-1027713269\":0, \"-181380149\":0, \"-115531678\":0, \"-100487028\":0, \"-1233979378\":0, \"843036051\":0, \"912926294\":0, \"56084205\":0, \"1594525888\":0, \"-1573616412\":0, \"-1417002190\":0, \"1794482897\":0, \"224683764\":0, \"-1471969403\":0, \"596888957\":0, \"596888956\":0, \"596888953\":0, \"596888952\":0, \"596888955\":0, \"596888954\":0, \"596888949\":0, \"596888948\":0, \"596888950\":0, \"972118770\":0, \"-1097220876\":0, \"-1097220881\":0, \"-1097220880\":0, \"-1097220879\":0, \"-1097220878\":0, \"-1097220885\":0, \"-1097220884\":0, \"-1097220883\":0, \"-1097220882\":0, \"-582505992\":0, \"-814117771\":0, \"1414371548\":0, \"682253748\":0, \"682253740\":0, \"682253745\":0, \"682253744\":0, \"682253747\":0, \"1659263444\":0, \"-136616030\":0, \"1888580672\":0, \"1888580669\":0, \"1888580668\":0, \"1888580666\":0, \"1888580663\":0, \"1888580664\":0, \"1230284208\":0, \"1684003336\":0, \"-726561745\":0, \"-1449123489\":0, \"1961938929\":0, \"1961938921\":0, \"1961938920\":0, \"1961938923\":0, \"1961938922\":0, \"1961938925\":0, \"1961938924\":0, \"1961938927\":0, \"1961938926\":0, \"1790423703\":0, \"1359366927\":0, \"1025548221\":0, \"507221049\":0, \"1515120746\":0, \"-956692642\":0, \"-1011688057\":0, \"371448668\":0, \"1101201489\":0, \"-1535298586\":0, \"56181691\":0, \"-709351854\":0, \"-1571155573\":0, \"1833524190\":0, \"1658269412\":0, \"-2138078264\":0, \"1706381873\":0, \"1795771134\":0, \"-610294159\":0 }, \"svea\":{\"-604737418\":0, \"-1350758925\":0, \"699878711\":0, \"-409251596\":0, \"1646521091\":0, \"1891689827\":0 }, \"ua\":{\"341203229\":0, \"96511\":0, \"3373707\":0, \"1193085\":0, \"635848677\":0, \"-564885382\":0, \"1168987\":0, \"102865796\":0, \"3552215\":0, \"3648196\":0, \"-892481550\":0, \"405645589\":0, \"405645588\":0, \"405645591\":0, \"405645590\":0, \"405645592\":0, \"3492908\":0 }, \"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }, {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"hs\":{\"et\":{\"48\":0, \"57\":0 }, \"ec\":{\"1824528345\":0, \"1824528346\":0, \"1824528347\":0, \"1824528348\":0, \"1824528341\":0, \"1824528342\":0, \"1824528343\":0, \"1824528350\":0, \"54237\":0, \"-201964253\":0, \"-1015351211\":0, \"-1698163721\":0, \"642507618\":0, \"-1750207685\":0, \"-1400806385\":0, \"-435072817\":0, \"612161276\":0, \"-2049994443\":0, \"1798380893\":0, \"-460386492\":0, \"476338248\":0, \"-1964837950\":0, \"-115592573\":0, \"-1119044065\":0, \"-1229406110\":0, \"1612904218\":0, \"-459588721\":0, \"93769843\":0, \"-1831156729\":0, \"925748342\":0, \"-1471879983\":0, \"-1471879982\":0, \"-1471879985\":0, \"-1471879984\":0, \"-1471879987\":0, \"-1471879986\":0, \"-1471879989\":0, \"-1471879988\":0, \"-1471879991\":0, \"-1471879990\":0, \"-1175432756\":0, \"-1439088726\":0, \"-630837254\":0, \"-1528980234\":0, \"866346720\":0, \"-466914683\":0, \"584870613\":0, \"-71005245\":0, \"-71005246\":0, \"-71005248\":0, \"-71005251\":0, \"-71005254\":0, \"-192614420\":0, \"-1899939497\":0, \"-138049017\":0, \"1755914106\":0, \"1713887651\":0, \"-1680991381\":0, \"1381973565\":0, \"1696869197\":0, \"530926139\":0, \"-1591103548\":0, \"606683084\":0, \"-452884081\":0, \"-1684704584\":0, \"-1684704582\":0, \"-1684704517\":0, \"-1684704551\":0, \"-1684704492\":0, \"-1684704484\":0, \"-1507615010\":0, \"448941660\":0, \"455787894\":0, \"1057880655\":0, \"-153747136\":0, \"228100699\":0, \"1956870096\":0, \"367619406\":0, \"-1728365802\":0, \"1315260226\":0, \"713953332\":0, \"54115406\":0, \"-1075988785\":0, \"-1726724035\":0, \"1195528703\":0, \"-1415615126\":0, \"-1027713269\":0, \"-181380149\":0, \"-115531678\":0, \"-100487028\":0, \"-1233979378\":0, \"843036051\":0, \"912926294\":0, \"1594525888\":0, \"-1573616412\":0, \"-1417002190\":0, \"1794482897\":0, \"224683764\":0, \"-1471969403\":0, \"596888957\":0, \"596888956\":0, \"596888953\":0, \"596888952\":0, \"596888955\":0, \"596888954\":0, \"596888949\":0, \"596888948\":0, \"596888950\":0, \"972118770\":0, \"-1097220876\":0, \"-1097220881\":0, \"-1097220880\":0, \"-1097220879\":0, \"-1097220878\":0, \"-1097220885\":0, \"-1097220884\":0, \"-1097220883\":0, \"-1097220882\":0, \"-582505992\":0, \"-814117771\":0, \"1414371548\":0, \"682253748\":0, \"682253740\":0, \"682253745\":0, \"682253744\":0, \"682253747\":0, \"1659263444\":0, \"-136616030\":0, \"1888580672\":0, \"1888580669\":0, \"1888580668\":0, \"1888580666\":0, \"1888580663\":0, \"1888580664\":0, \"1230284208\":0, \"1684003336\":0, \"-726561745\":0, \"-1449123489\":0, \"1961938929\":0, \"1961938921\":0, \"1961938920\":0, \"1961938923\":0, \"1961938922\":0, \"1961938925\":0, \"1961938924\":0, \"1961938927\":0, \"1961938926\":0, \"1790423703\":0, \"1359366927\":0, \"1025548221\":0, \"507221049\":0, \"1515120746\":0, \"-956692642\":0, \"-1011688057\":0, \"371448668\":0, \"1101201489\":0, \"-1535298586\":0, \"-709351854\":0, \"-1571155573\":0, \"1833524190\":0, \"1658269412\":0, \"-2138078264\":0, \"1706381873\":0, \"1795771134\":0, \"-610294159\":0 }, \"svec\":{\"-385188961\":0, \"303102897\":0, \"303102895\":0, \"303102890\":0, \"303102891\":0, \"303102899\":0, \"1688308747\":0, \"-149109002\":0, \"-1254039557\":0, \"847138800\":0, \"847138801\":0, \"847138799\":0, \"-204085080\":0, \"1658373353\":0, \"-1493744191\":0, \"1861873109\":0, \"-732268618\":0 }, \"ua\":{\"341203229\":0, \"96511\":0, \"3373707\":0, \"1193085\":0, \"635848677\":0, \"-564885382\":0, \"1168987\":0, \"102865796\":0, \"3552215\":0, \"3648196\":0, \"-892481550\":0, \"405645589\":0, \"405645588\":0, \"405645591\":0, \"405645590\":0, \"405645592\":0, \"3492908\":0 } }, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"String\", \"value\":\"ProductCategory\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"String\", \"value\":\"ProductQuantity\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"String\", \"value\":\"RevenueAmount\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"String\", \"value\":\"ProductSKU\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"String\", \"value\":\"CurrencyCode\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"String\", \"value\":\"ProductName\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":180, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":181, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] } ], \"cms\":[{\"id\":28, \"pr\":[{\"f\":\"com.appboy.installation\", \"m\":0, \"ps\":[{\"k\":\"installation_id\", \"t\":1, \"n\":\"iid\", \"d\":\"%g%\"} ] }, {\"f\":\"com.appboy.device\", \"m\":0, \"ps\":[{\"k\":\"device_id\", \"t\":1, \"n\":\"di\", \"d\":\"\"} ] }, {\"f\":\"com.appboy.offline.storagemap\", \"m\":0, \"ps\":[{\"k\":\"last_user\", \"t\":1, \"n\":\"lu\", \"d\":\"\"} ] } ] } ], \"lsv\":\"2.3.1\", \"tri\":{\"mm\":[{\"dt\":\"x\", \"eh\":true }, {\"dt\":\"x\", \"eh\":false }, {\"dt\":\"ast\", \"t\":\"app_init\", \"ifr\":true, \"iu\":false } ], \"evts\":[1594525888, -460386492, -1633998520, -201964253, -1698163721, -88346394, -964353845, 925748342, 1515120746, 476338248, -2049994443 ] }, \"pio\":30 }";
    @Test
    public void testParseConfig() throws Exception {

        JSONObject config = new JSONObject(json);
        JSONArray modules = config.getJSONArray("eks");
        for (int i = 0; i < modules.length(); i++) {
            JSONObject ekConfig = modules.getJSONObject(i);
            KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(ekConfig);
            if (ekConfig.has("avf")) {
                JSONObject attributeValueFilter = ekConfig.getJSONObject("avf");
                if (attributeValueFilter.has("i") && attributeValueFilter.has("a") && attributeValueFilter.has("v")) {
                    Object shouldIncludeMatches = attributeValueFilter.get("i");
                    Object hashedAttribute = attributeValueFilter.get("a");
                    Object hashedValue = attributeValueFilter.get("v");
                    if (shouldIncludeMatches instanceof Boolean && hashedAttribute instanceof Integer && hashedValue instanceof Integer) {
                        assertTrue(configuration.isAttributeValueFilteringActive());
                        assertEquals(attributeValueFilter.getBoolean("i"), configuration.isAvfShouldIncludeMatches());
                        assertEquals(attributeValueFilter.getInt("a"), configuration.getAvfHashedAttribute());
                        assertEquals(attributeValueFilter.getInt("v"), configuration.getAvfHashedValue());
                    }
                    else {
                        assertFalse(configuration.isAttributeValueFilteringActive());
                    }
                }
            }
            if (ekConfig.has("hs")) {
                JSONObject filters = ekConfig.getJSONObject("hs");
                if (filters.has("et")) {
                    assertEquals(filters.getJSONObject("et").length(), configuration.getEventTypeFilters().size());
                }
                if (filters.has("ec")) {
                    assertEquals(filters.getJSONObject("ec").length(), configuration.getEventNameFilters().size());
                }
                if (filters.has("ea")) {
                    assertEquals(filters.getJSONObject("ea").length(), configuration.getEventAttributeFilters().size());
                }
                if (filters.has("svec")) {
                    assertEquals(filters.getJSONObject("svec").length(), configuration.getScreenNameFilters().size());
                }
                if (filters.has("svea")) {
                    assertEquals(filters.getJSONObject("svea").length(), configuration.getScreenAttributeFilters().size());
                }
                if (filters.has("uid")) {
                    assertEquals(filters.getJSONObject("uid").length(), configuration.getUserIdentityFilters().size());
                }
                if (filters.has("ua")) {
                    assertEquals(filters.getJSONObject("ua").length(), configuration.getUserAttributeFilters().size());
                }
                if (filters.has("cea")) {
                    assertEquals(filters.getJSONObject("cea").length(), configuration.getCommerceAttributeFilters().size());
                }
                if (filters.has("ent")) {
                    assertEquals(filters.getJSONObject("ent").length(), configuration.getCommerceEntityFilters().size());
                }
                if (filters.has("afa")) {
                    JSONObject entityAttFilters = filters.getJSONObject("afa");
                    assertEquals(entityAttFilters.length(), configuration.getCommerceEntityAttributeFilters().size());
                    Iterator<String> keys = entityAttFilters.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        assertEquals(entityAttFilters.getJSONObject(key).length(), configuration.getCommerceEntityAttributeFilters().get(Integer.parseInt(key)).size());
                    }
                }
            }
            if (ekConfig.has("bk")) {
                JSONObject bracketing = ekConfig.getJSONObject("bk");
                assertEquals(bracketing.optInt("lo"), configuration.getLowBracket());
                assertEquals(bracketing.optInt("hi"), configuration.getHighBracket());
            } else {
                assertEquals(0, configuration.getLowBracket());
                assertEquals(101, configuration.getHighBracket());
            }
            if (ekConfig.has("pr")) {
                JSONArray projections = ekConfig.getJSONArray("pr");
                if (projections.length() > 0) {
                    assertNotNull(configuration.getDefaultEventProjection());
                    assertEquals(projections.length(), configuration.getCustomMappingList().size() + 1);
                }
            }
            if (ekConfig.getLong("id") == 28) {
                assertTrue(configuration.excludeAnonymousUsers());
            } else {
                assertFalse(configuration.excludeAnonymousUsers());
            }
        }
    }


    static String COMMERCE_FILTERS = "{\"id\":28, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }";

    static String COMMERCE_FILTERS_2 = "{\"id\":28, \"as\":{\"apiKey\":\"1fd18e0e-22cd-4b86-a106-551ccc59175f\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"a8f63b1f-1bc2-4373-8947-8dacdd113ad4\", \"addEventAttributeList\":\"[{\\\"map\\\":\\\"Value\\\",\\\"value\\\":null,\\\"maptype\\\":\\\"AttributeSelector\\\"}]\", \"removeEventAttributeList\":\"[]\", \"singleItemEventAttributeList\":\"[]\"}, \"hs\":{\"et\":{\"1568\":0, \"1576\":0 }, \"cea\":{\"-1015386651\":0, \"-1091394645\":0, \"-2090340318\":0 }, \"afa\":{\"1\":{\"93997959\":0, \"-870793808\":0 }, \"2\":{\"1820422063\":0 } } }, \"pr\":[] }";

    @Test
    public void testFilterCommerceAttributes() throws Exception {

        //CUSTOM ATTRIBUTES
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(new JSONObject(COMMERCE_FILTERS));

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("my custom attribute", "whatever");
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("name", "sku", 2).build()).customAttributes(attributes).build();
        assertEquals("whatever", event.getCustomAttributeStrings().get("my custom attribute"));
        CommerceEvent filteredEvent = configuration.filterCommerceEvent(event);
        assertNull(filteredEvent.getCustomAttributeStrings().get("my custom attribute"));

        //make sure we're only doing it for ADD_TO_CART
        CommerceEvent event2 = new CommerceEvent.Builder(Product.PURCHASE, new Product.Builder("name", "sku", 2).build()).customAttributes(attributes).transactionAttributes(new TransactionAttributes().setId("some id")).build();

        assertEquals("whatever", event2.getCustomAttributeStrings().get("my custom attribute"));
        CommerceEvent filteredEvent2 = configuration.filterCommerceEvent(event2);
        assertEquals("whatever", filteredEvent2.getCustomAttributeStrings().get("my custom attribute"));

        event = new CommerceEvent.Builder(Product.CHECKOUT, new Product.Builder("name", "sku", 2).build()).checkoutOptions("cool options").build();

        assertEquals("cool options", event.getCheckoutOptions());
        filteredEvent2 = configuration.filterCommerceEvent(event);
        assertNull(filteredEvent2.getCheckoutOptions());

        event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("name", "sku", 2).build()).checkoutOptions("cool options").build();
        assertEquals("cool options", event.getCheckoutOptions());
        filteredEvent2 = configuration.filterCommerceEvent(event);
        assertEquals("cool options", filteredEvent2.getCheckoutOptions());

        event2 = new CommerceEvent.Builder(Product.PURCHASE, new Product.Builder("name", "sku", 2).build()).customAttributes(attributes).transactionAttributes(new TransactionAttributes().setId("some id").setAffiliation("cool affiliation")).build();
        assertEquals("cool affiliation", event2.getTransactionAttributes().getAffiliation());
        filteredEvent2 = configuration.filterCommerceEvent(event2);
        assertNull(filteredEvent2.getTransactionAttributes().getAffiliation());
    }

    @Test
    public void testFilterCommerceEntity() throws Exception {
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(new JSONObject(COMMERCE_FILTERS));

        Impression impression = new Impression("Cool list name", new Product.Builder("name2", "sku", 2).build());
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("name", "sku", 2).build()).addImpression(impression).build();
        List<Impression> impressionList = event.getImpressions();
        for (Impression imp : impressionList) {
            assertEquals(1, imp.getProducts().size());
        }
        event = configuration.filterCommerceEvent(event);
        assertNull(event.getProducts());
        List<Impression> impressionList2 = event.getImpressions();
        assertTrue(impressionList2.size() > 0);
        for (Impression imp : impressionList2) {
            assertEquals(0, imp.getProducts().size());
        }
        JSONObject config = new JSONObject(COMMERCE_FILTERS);
        //enable product, disable impressions
        config.getJSONObject("hs").getJSONObject("ent").put("2", 0);
        config.getJSONObject("hs").getJSONObject("ent").put("1", 1);
        configuration = MockKitConfiguration.createKitConfiguration(config);
        event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("name", "sku", 2).build()).addImpression(impression).build();
        impressionList = event.getImpressions();
        for (Impression imp : impressionList) {
            assertEquals(1, imp.getProducts().size());
        }
        assertEquals(1, event.getProducts().size());
        event = configuration.filterCommerceEvent(event);
        assertEquals(1, event.getProducts().size());
        impressionList2 = event.getImpressions();
        assertTrue(impressionList2.size() > 0);
        for (Impression imp : impressionList2) {
            assertEquals(1, imp.getProducts().size());
        }
    }

    @Test
    public void testFilterCommerceEntityAttribute() throws Exception {
        CommerceEvent event = new CommerceEvent.Builder(Promotion.VIEW, new Promotion().setId("promo id").setCreative("the creative").setName("promotion name")).build();
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(new JSONObject(COMMERCE_FILTERS));
        assertEquals("the creative", event.getPromotions().get(0).getCreative());
        CommerceEvent filteredEvent = configuration.filterCommerceEvent(event);
        assertNull(filteredEvent.getPromotions().get(0).getCreative());
        assertEquals("promotion name", filteredEvent.getPromotions().get(0).getName());

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("my custom product attribute", "whatever");
        event = new CommerceEvent.Builder(Product.CHECKOUT, new Product.Builder("name", "sku", 5)
                .customAttributes(attributes)
                .brand("cool brand").build())
                .build();
        assertEquals("whatever", event.getProducts().get(0).getCustomAttributes().get("my custom product attribute"));
        assertEquals("cool brand", event.getProducts().get(0).getBrand());
        configuration = MockKitConfiguration.createKitConfiguration(new JSONObject(COMMERCE_FILTERS_2));
        filteredEvent = configuration.filterCommerceEvent(event);
        assertNull(filteredEvent.getProducts().get(0).getCustomAttributes().get("my custom product attribute"));
        assertNull(filteredEvent.getProducts().get(0).getBrand());
    }

    @Test
    public void testFilterCommerceEventType() throws Exception {
        //CUSTOM ATTRIBUTES
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(new JSONObject(COMMERCE_FILTERS));
        CommerceEvent event = new CommerceEvent.Builder(Product.ADD_TO_CART, new Product.Builder("name", "sku", 2).build()).build();
        assertNotNull(configuration.filterCommerceEvent(event));
        CommerceEvent event2 = new CommerceEvent.Builder(Product.REMOVE_FROM_CART, new Product.Builder("name", "sku", 2).build()).build();
        assertNull(configuration.filterCommerceEvent(event2));

    }

    // attribute hash 3288498 matches "key1", value hash 3611952 matches "val1"
    static String ATTRIBUTE_VALUE_FILTERING_INCLUDE_TRUE = "{\"id\":28, \"avf\":{\"i\":true, \"a\":3288498, \"v\":3611952}, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }";
    static String ATTRIBUTE_VALUE_FILTERING_INCLUDE_FALSE = "{\"id\":28, \"avf\":{\"i\":false, \"a\":3288498, \"v\":3611952}, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }";

    @Test
    public void testAttributeValueFiltering() throws Exception {
        Map<String, String> matchingAttributes, halfMatchingAttributes, nonMatchingAttributes;
        {
            matchingAttributes = new HashMap<>();
            halfMatchingAttributes = new HashMap<>();
            nonMatchingAttributes = new HashMap<>();
            matchingAttributes.put("key1", "val1");
            halfMatchingAttributes.put("key1", "valWrong");
            nonMatchingAttributes.put("keyWrong", "valWrong");
        }
        KitConfiguration includeTrueConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject(ATTRIBUTE_VALUE_FILTERING_INCLUDE_TRUE));
        KitConfiguration includeFalseConfiguration = MockKitConfiguration.createKitConfiguration(new JSONObject(ATTRIBUTE_VALUE_FILTERING_INCLUDE_FALSE));


        boolean result;

        result = includeTrueConfiguration.shouldIncludeFromAttributeValueFiltering(matchingAttributes);
        assertTrue(result);
        result = includeTrueConfiguration.shouldIncludeFromAttributeValueFiltering(halfMatchingAttributes);
        assertFalse(result);
        result = includeTrueConfiguration.shouldIncludeFromAttributeValueFiltering(nonMatchingAttributes);
        assertFalse(result);

        result = includeFalseConfiguration.shouldIncludeFromAttributeValueFiltering(matchingAttributes);
        assertFalse(result);
        result = includeFalseConfiguration.shouldIncludeFromAttributeValueFiltering(halfMatchingAttributes);
        assertTrue(result);
        result = includeFalseConfiguration.shouldIncludeFromAttributeValueFiltering(nonMatchingAttributes);
        assertTrue(result);
    }


    @Test
    public void testUserAttributeFiltering() throws Exception {
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(new JSONObject(" {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"hs\":{\"ua\":{\"-430909305\":0, \"1510554026\":1, \"341203229\":0, \"96511\":0, \"3373707\":0, \"1193085\":0, \"635848677\":0, \"-564885382\":0, \"1168987\":0, \"102865796\":0, \"3552215\":0, \"3648196\":0, \"-892481550\":0, \"405645589\":0, \"405645588\":0, \"405645591\":0, \"405645590\":0, \"405645592\":0, \"3492908\":0 } }, }"));
        assertFalse(configuration.shouldForwardAttribute(configuration.getUserAttributeFilters(), "test key in config as false"));
        assertTrue(configuration.shouldForwardAttribute(configuration.getUserAttributeFilters(), "test key in config as true"));
        assertTrue(configuration.shouldForwardAttribute(configuration.getUserAttributeFilters(), "test key not in config"));
    }

    @Test
    public void testConsentForwardingRules() throws Exception {
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(
                new JSONObject("{\"id\":56,\"crvf\":{\"i\": true, \"v\":[{\"c\":true, \"h\":123}, {\"c\":false, \"h\":456}]}}"));
        assertEquals(2, configuration.mConsentForwardingRules.size());
        assertEquals(true, configuration.consentForwardingIncludeMatches);
        assertEquals(true, configuration.mConsentForwardingRules.get(123));
        assertEquals(false, configuration.mConsentForwardingRules.get(456));

        configuration = configuration.parseConfiguration(
                new JSONObject("{\"id\":56,\"crvf\":{\"i\": false, \"v\":[{\"c\":false, \"h\":123}, {\"c\":true, \"h\":456}]}}"));
        assertEquals(2, configuration.mConsentForwardingRules.size());
        assertEquals(false, configuration.consentForwardingIncludeMatches);
        assertEquals(false, configuration.mConsentForwardingRules.get(123));
        assertEquals(true, configuration.mConsentForwardingRules.get(456));

        configuration.parseConfiguration(new JSONObject("{\"id\":\"1\"}"));
        assertEquals(0, configuration.mConsentForwardingRules.size());
    }

    @Test
    public void testConsentStateShouldFilterFromForwardingRulesEmptyConsent() throws Exception {
        JSONObject jsonConfiguration = new JSONObject("{\"id\":56}");
        JSONObject consentForwardingRule = new JSONObject();
        jsonConfiguration.put("crvf", consentForwardingRule);
        consentForwardingRule.put("i", false);
        JSONArray ruleArray = new JSONArray();
        consentForwardingRule.put("v", ruleArray);
        JSONObject rule1 = new JSONObject();
        ruleArray.put(rule1);
        rule1.put("h", KitUtils.hashForFiltering("1" + "foo purpose"));
        rule1.put("c", false);
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(
                new JSONObject("{\"id\":56,\"crvf\":{\"i\": true, \"v\":[{\"c\":true, \"h\":123}, {\"c\":false, \"h\":456}]}}"));

        assertFalse(configuration.isConsentStateFilterMatch(ConsentState.builder().build()));
    }

    @Test
    public void testConsentStateShouldFilterFromMultipleForwardingRulesAndStates() throws Exception {
        JSONObject jsonConfiguration = new JSONObject("{\"id\":56}");
        JSONObject consentForwardingRule = new JSONObject();
        jsonConfiguration.put("crvf", consentForwardingRule);
        consentForwardingRule.put("i", false);
        JSONArray ruleArray = new JSONArray();
        consentForwardingRule.put("v", ruleArray);
        JSONObject rule1 = new JSONObject();
        ruleArray.put(rule1);
        rule1.put("h", KitUtils.hashForFiltering("1" + "foo purpose 1"));
        rule1.put("c", false);

        JSONObject rule2 = new JSONObject();
        ruleArray.put(rule2);
        rule2.put("h", KitUtils.hashForFiltering("1" + "foo purpose 2"));
        rule2.put("c", false);

        JSONObject rule3 = new JSONObject();
        ruleArray.put(rule3);
        rule3.put("h", KitUtils.hashForFiltering("1" + "foo purpose 3"));
        rule3.put("c", false);

        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(jsonConfiguration);

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose 1",
                                        GDPRConsent.builder(false).build())
                                .addGDPRConsentState(
                                        "foo purpose 2",
                                        GDPRConsent.builder(true).build())
                                .addGDPRConsentState(
                                        "foo purpose 3",
                                        GDPRConsent.builder(true).build())
                                .build()
                )
        );

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose 1",
                                        GDPRConsent.builder(true).build())
                                .addGDPRConsentState(
                                        "foo purpose 2",
                                        GDPRConsent.builder(false).build())
                                .addGDPRConsentState(
                                        "foo purpose 3",
                                        GDPRConsent.builder(true).build())
                                .build()
                )
        );

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose 1",
                                        GDPRConsent.builder(true).build())
                                .addGDPRConsentState(
                                        "foo purpose 2",
                                        GDPRConsent.builder(true).build())
                                .addGDPRConsentState(
                                        "foo purpose 3",
                                        GDPRConsent.builder(false).build())
                                .setCCPAConsentState(
                                        CCPAConsent.builder(false).build())
                                .build()
                )
        );

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose 1",
                                        GDPRConsent.builder(false).build())
                                .addGDPRConsentState(
                                        "foo purpose 2",
                                        GDPRConsent.builder(false).build())
                                .addGDPRConsentState(
                                        "foo purpose 3",
                                        GDPRConsent.builder(false).build())
                                .setCCPAConsentState(
                                        CCPAConsent.builder(true).build())
                                .build()
                )
        );
    }

    @Test
    public void testConsentStateShouldFilterFromForwardingRules() throws Exception {
        JSONObject jsonConfiguration = new JSONObject("{\"id\":56}");
        JSONObject consentForwardingRule = new JSONObject();
        jsonConfiguration.put("crvf", consentForwardingRule);
        consentForwardingRule.put("i", false);
        JSONArray ruleArray = new JSONArray();
        consentForwardingRule.put("v", ruleArray);
        JSONObject rule1 = new JSONObject();
        ruleArray.put(rule1);
        rule1.put("h", KitUtils.hashForFiltering("1" + "foo purpose"));
        rule1.put("c", false);
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(jsonConfiguration);

        assertTrue(
                configuration.isConsentStateFilterMatch(
                    ConsentState.builder()
                            .addGDPRConsentState(
                                    "foo purpose",
                                    GDPRConsent.builder(false).build())
                            .build()
                    )
        );

        assertFalse(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose",
                                        GDPRConsent.builder(true).build())
                                .build()
                )
        );

        ruleArray.getJSONObject(0).put("c", true);
        configuration.parseConfiguration(jsonConfiguration);

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose",
                                        GDPRConsent.builder(true).build())
                                .build()
                )
        );

        assertFalse(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .addGDPRConsentState(
                                        "foo purpose",
                                        GDPRConsent.builder(false).build())
                                .build()
                )
        );
    }


    @Test
    public void testCCPAConsentStateShouldFilterFromForwardingRules() throws Exception {
        JSONObject jsonConfiguration = new JSONObject("{\"id\":56}");
        JSONObject consentForwardingRule = new JSONObject();
        jsonConfiguration.put("crvf", consentForwardingRule);
        consentForwardingRule.put("i", false);
        JSONArray ruleArray = new JSONArray();
        consentForwardingRule.put("v", ruleArray);
        JSONObject rule1 = new JSONObject();
        ruleArray.put(rule1);
        rule1.put("h", KitUtils.hashForFiltering("2" + Constants.MessageKey.CCPA_CONSENT_KEY));
        rule1.put("c", false);
        KitConfiguration configuration = MockKitConfiguration.createKitConfiguration(jsonConfiguration);

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .setCCPAConsentState(
                                        CCPAConsent.builder(false)
                                                .build())
                                .build()
                )
        );

        assertFalse(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .setCCPAConsentState(
                                        CCPAConsent.builder(true)
                                                .build())
                                .build()
                )
        );

        ruleArray.getJSONObject(0).put("c", true);
        configuration.parseConfiguration(jsonConfiguration);

        assertTrue(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .setCCPAConsentState(
                                        CCPAConsent.builder(true)
                                                .build())
                                .build()
                )
        );

        assertFalse(
                configuration.isConsentStateFilterMatch(
                        ConsentState.builder()
                                .setCCPAConsentState(
                                        CCPAConsent.builder(false)
                                                .build())
                                .build()
                )
        );
    }

    @Test
    public void testExcludeUser() {
        KitConfiguration kitConfiguration = new KitConfiguration();
        MParticleUser mockUser = Mockito.mock(MParticleUser.class);

        kitConfiguration.mExcludeAnonymousUsers = true;
        Mockito.when(mockUser.isLoggedIn()).thenReturn(false);
        assertFalse(mockUser.isLoggedIn());

        assertTrue(kitConfiguration.shouldExcludeUser(mockUser));

        kitConfiguration.mExcludeAnonymousUsers = false;

        assertFalse(kitConfiguration.shouldExcludeUser(mockUser));

        Mockito.when(mockUser.isLoggedIn()).thenReturn(true);

        assertFalse(kitConfiguration.shouldExcludeUser(mockUser));

        kitConfiguration.mExcludeAnonymousUsers = true;

        assertFalse(kitConfiguration.shouldExcludeUser(mockUser));
    }


    /**
     * This tests parsing of the attributeAddToUser ("eea"), attributeRemoveFromUser("ear") and attributeSingleItemUser("eas")
     */
    @Test
    public void testAttributeToUser() throws JSONException {
        JSONObject kitConfig = new JSONObject()
                .put("id", 10)
                .put("hs", new JSONObject()
                        .put("eaa", new JSONObject()
                                .put("-1107245616", "add-item"))
                        .put("ear", new JSONObject()
                                .put("238178128", "remove-item"))
                        .put("eas", new JSONObject()
                                .put("1784066270", "single-item"))
                );

        KitConfiguration kitConfiguration = MockKitConfiguration.createKitConfiguration(kitConfig);

        Map<Integer, String> addMap = kitConfiguration.getEventAttributesAddToUser();
        assertEquals(1, addMap.size());
        assertTrue(addMap.containsKey(-1107245616));
        assertEquals("add-item", addMap.get(-1107245616));

        Map<Integer, String> removeMap = kitConfiguration.getEventAttributesRemoveFromUser();
        assertEquals(1, removeMap.size());
        assertTrue(removeMap.containsKey(238178128));
        assertEquals("remove-item", removeMap.get(238178128));

        Map<Integer, String> singleMap = kitConfiguration.getEventAttributesSingleItemUser();
        assertEquals(1, singleMap.size());
        assertTrue(singleMap.containsKey(1784066270));
        assertEquals("single-item", singleMap.get(1784066270));

        kitConfig = new JSONObject()
                .put("id", 10)
                .put("hs", new JSONObject());
        kitConfiguration = MockKitConfiguration.createKitConfiguration(kitConfig);

        assertEquals(0, kitConfiguration.getEventAttributesAddToUser().size());
        assertEquals(0, kitConfiguration.getEventAttributesRemoveFromUser().size());
        assertEquals(0, kitConfiguration.getEventAttributesSingleItemUser().size());
    }
}