package com.mparticle.internal.embedded;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.Promotion;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.CommerceEventUtil;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProjectionTest {
    static final String JSON = "{\"dt\":\"ac\",\"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\",\"ct\":1434392412994,\"dbg\":false,\"cue\":\"appdefined\",\"pmk\":[\"mp_message\",\"com.urbanairship.push.ALERT\",\"alert\",\"a\",\"message\"],\"cnp\":\"appdefined\",\"soc\":0,\"oo\":false,\"eks\":[{\"id\":28,\"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\",\"sendPushOpenedWhenAppInForeground\":\"False\",\"push_enabled\":\"True\",\"register_inapp\":\"True\",\"appGroupId\":\"\"},\"hs\":{\"ec\":{\"1824528345\":0,\"1824528346\":0,\"1824528347\":0,\"1824528348\":0,\"1824528341\":0,\"1824528342\":0,\"1824528343\":0,\"1824528350\":0,\"54237\":0,\"-201964253\":0,\"-1015351211\":0,\"-1698163721\":0,\"642507618\":0,\"-1750207685\":0,\"-1400806385\":0,\"-435072817\":0,\"612161276\":0,\"-2049994443\":0,\"1798380893\":0,\"-460386492\":0,\"476338248\":0,\"-1964837950\":0,\"-115592573\":0,\"-1119044065\":0,\"-1229406110\":0,\"1612904218\":0,\"-459588721\":0,\"93769843\":0,\"-1831156729\":0,\"925748342\":0,\"-1471879983\":0,\"-1471879982\":0,\"-1471879985\":0,\"-1471879984\":0,\"-1471879987\":0,\"-1471879986\":0,\"-1471879989\":0,\"-1471879988\":0,\"-1471879991\":0,\"-1471879990\":0,\"-1175432756\":0,\"-1439088726\":0,\"-630837254\":0,\"-1528980234\":0,\"866346720\":0,\"-466914683\":0,\"584870613\":0,\"-71005245\":0,\"-71005246\":0,\"-71005248\":0,\"-71005251\":0,\"-71005254\":0,\"-192614420\":0,\"-1899939497\":0,\"-138049017\":0,\"1755914106\":0,\"1713887651\":0,\"-1680991381\":0,\"1381973565\":0,\"1696869197\":0,\"530926139\":0,\"-1591103548\":0,\"606683084\":0,\"-452884081\":0,\"1156084566\":0,\"-1684704584\":0,\"-1684704582\":0,\"-1684704517\":0,\"-1684704551\":0,\"-1684704492\":0,\"-1684704484\":0,\"-1507615010\":0,\"1496713379\":0,\"1496713380\":0,\"1496713373\":0,\"1496713374\":0,\"1496713371\":0,\"1496713372\":0,\"1496713377\":0,\"1496713375\":0,\"1496713376\":0,\"448941660\":0,\"455787894\":0,\"1057880655\":0,\"-153747136\":0,\"228100699\":0,\"1956870096\":0,\"367619406\":0,\"-1728365802\":0,\"1315260226\":0,\"713953332\":0,\"54115406\":0,\"-1075988785\":0,\"-1726724035\":0,\"1195528703\":0,\"-1415615126\":0,\"-1027713269\":0,\"-181380149\":0,\"-115531678\":0,\"-100487028\":0,\"-1233979378\":0,\"843036051\":0,\"912926294\":0,\"56084205\":0,\"1594525888\":0,\"-1573616412\":0,\"-1417002190\":0,\"1794482897\":0,\"224683764\":0,\"-1471969403\":0,\"596888957\":0,\"596888956\":0,\"596888953\":0,\"596888952\":0,\"596888955\":0,\"596888954\":0,\"596888949\":0,\"596888948\":0,\"596888950\":0,\"972118770\":0,\"-1097220876\":0,\"-1097220881\":0,\"-1097220880\":0,\"-1097220879\":0,\"-1097220878\":0,\"-1097220885\":0,\"-1097220884\":0,\"-1097220883\":0,\"-1097220882\":0,\"-582505992\":0,\"-814117771\":0,\"1414371548\":0,\"682253748\":0,\"682253740\":0,\"682253745\":0,\"682253744\":0,\"682253747\":0,\"1659263444\":0,\"-136616030\":0,\"1888580672\":0,\"1888580669\":0,\"1888580668\":0,\"1888580666\":0,\"1888580663\":0,\"1888580664\":0,\"1230284208\":0,\"1684003336\":0,\"-726561745\":0,\"-1449123489\":0,\"1961938929\":0,\"1961938921\":0,\"1961938920\":0,\"1961938923\":0,\"1961938922\":0,\"1961938925\":0,\"1961938924\":0,\"1961938927\":0,\"1961938926\":0,\"1790423703\":0,\"1359366927\":0,\"1025548221\":0,\"507221049\":0,\"1515120746\":0,\"-956692642\":0,\"-1011688057\":0,\"371448668\":0,\"1101201489\":0,\"-1535298586\":0,\"56181691\":0,\"-709351854\":0,\"-1571155573\":0,\"1833524190\":0,\"1658269412\":0,\"-2138078264\":0,\"1706381873\":0,\"1795771134\":0,\"-610294159\":0},\"svea\":{\"-604737418\":0,\"-1350758925\":0,\"699878711\":0,\"-409251596\":0,\"1646521091\":0,\"1891689827\":0},\"ua\":{\"341203229\":0,\"96511\":0,\"3373707\":0,\"1193085\":0,\"635848677\":0,\"-564885382\":0,\"1168987\":0,\"102865796\":0,\"3552215\":0,\"3648196\":0,\"-892481550\":0,\"405645589\":0,\"405645588\":0,\"405645591\":0,\"405645590\":0,\"405645592\":0,\"3492908\":0}},\"pr\":[]},{\"id\":56,\"as\":{\"secretKey\":\"testappkey\",\"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\",\"sendTransactionData\":\"True\",\"eventAttributeList\":null},\"hs\":{\"et\":{\"48\":0,\"57\":0},\"ec\":{\"1824528345\":0,\"1824528346\":0,\"1824528347\":0,\"1824528348\":0,\"1824528341\":0,\"1824528342\":0,\"1824528343\":0,\"1824528350\":0,\"54237\":0,\"-201964253\":0,\"-1015351211\":0,\"-1698163721\":0,\"642507618\":0,\"-1750207685\":0,\"-1400806385\":0,\"-435072817\":0,\"612161276\":0,\"-2049994443\":0,\"1798380893\":0,\"-460386492\":0,\"476338248\":0,\"-1964837950\":0,\"-115592573\":0,\"-1119044065\":0,\"-1229406110\":0,\"1612904218\":0,\"-459588721\":0,\"93769843\":0,\"-1831156729\":0,\"925748342\":0,\"-1471879983\":0,\"-1471879982\":0,\"-1471879985\":0,\"-1471879984\":0,\"-1471879987\":0,\"-1471879986\":0,\"-1471879989\":0,\"-1471879988\":0,\"-1471879991\":0,\"-1471879990\":0,\"-1175432756\":0,\"-1439088726\":0,\"-630837254\":0,\"-1528980234\":0,\"866346720\":0,\"-466914683\":0,\"584870613\":0,\"-71005245\":0,\"-71005246\":0,\"-71005248\":0,\"-71005251\":0,\"-71005254\":0,\"-192614420\":0,\"-1899939497\":0,\"-138049017\":0,\"1755914106\":0,\"1713887651\":0,\"-1680991381\":0,\"1381973565\":0,\"1696869197\":0,\"530926139\":0,\"-1591103548\":0,\"606683084\":0,\"-452884081\":0,\"-1684704584\":0,\"-1684704582\":0,\"-1684704517\":0,\"-1684704551\":0,\"-1684704492\":0,\"-1684704484\":0,\"-1507615010\":0,\"448941660\":0,\"455787894\":0,\"1057880655\":0,\"-153747136\":0,\"228100699\":0,\"1956870096\":0,\"367619406\":0,\"-1728365802\":0,\"1315260226\":0,\"713953332\":0,\"54115406\":0,\"-1075988785\":0,\"-1726724035\":0,\"1195528703\":0,\"-1415615126\":0,\"-1027713269\":0,\"-181380149\":0,\"-115531678\":0,\"-100487028\":0,\"-1233979378\":0,\"843036051\":0,\"912926294\":0,\"1594525888\":0,\"-1573616412\":0,\"-1417002190\":0,\"1794482897\":0,\"224683764\":0,\"-1471969403\":0,\"596888957\":0,\"596888956\":0,\"596888953\":0,\"596888952\":0,\"596888955\":0,\"596888954\":0,\"596888949\":0,\"596888948\":0,\"596888950\":0,\"972118770\":0,\"-1097220876\":0,\"-1097220881\":0,\"-1097220880\":0,\"-1097220879\":0,\"-1097220878\":0,\"-1097220885\":0,\"-1097220884\":0,\"-1097220883\":0,\"-1097220882\":0,\"-582505992\":0,\"-814117771\":0,\"1414371548\":0,\"682253748\":0,\"682253740\":0,\"682253745\":0,\"682253744\":0,\"682253747\":0,\"1659263444\":0,\"-136616030\":0,\"1888580672\":0,\"1888580669\":0,\"1888580668\":0,\"1888580666\":0,\"1888580663\":0,\"1888580664\":0,\"1230284208\":0,\"1684003336\":0,\"-726561745\":0,\"-1449123489\":0,\"1961938929\":0,\"1961938921\":0,\"1961938920\":0,\"1961938923\":0,\"1961938922\":0,\"1961938925\":0,\"1961938924\":0,\"1961938927\":0,\"1961938926\":0,\"1790423703\":0,\"1359366927\":0,\"1025548221\":0,\"507221049\":0,\"1515120746\":0,\"-956692642\":0,\"-1011688057\":0,\"371448668\":0,\"1101201489\":0,\"-1535298586\":0,\"-709351854\":0,\"-1571155573\":0,\"1833524190\":0,\"1658269412\":0,\"-2138078264\":0,\"1706381873\":0,\"1795771134\":0,\"-610294159\":0},\"svec\":{\"-385188961\":0,\"303102897\":0,\"303102895\":0,\"303102890\":0,\"303102891\":0,\"303102899\":0,\"1688308747\":0,\"-149109002\":0,\"-1254039557\":0,\"847138800\":0,\"847138801\":0,\"847138799\":0,\"-204085080\":0,\"1658373353\":0,\"-1493744191\":0,\"1861873109\":0,\"-732268618\":0},\"ua\":{\"341203229\":0,\"96511\":0,\"3373707\":0,\"1193085\":0,\"635848677\":0,\"-564885382\":0,\"1168987\":0,\"102865796\":0,\"3552215\":0,\"3648196\":0,\"-892481550\":0,\"405645589\":0,\"405645588\":0,\"405645591\":0,\"405645590\":0,\"405645592\":0,\"3492908\":0}},\"pr\":[{\"id\":93,\"pmmid\":23,\"match\":{\"message_type\":4,\"event_match_type\":\"String\",\"event\":\"Product View\",\"attribute_key\":\"$MethodName\",\"attribute_value\":\"$ProductView\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - product view\",\"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\",\"match_type\":\"String\",\"value\":\"ProductCategory\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Quantity\",\"match_type\":\"String\",\"value\":\"ProductQuantity\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Total Amount\",\"match_type\":\"String\",\"value\":\"RevenueAmount\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View SKU\",\"match_type\":\"String\",\"value\":\"ProductSKU\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Currency\",\"match_type\":\"String\",\"value\":\"CurrencyCode\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Name\",\"match_type\":\"String\",\"value\":\"ProductName\",\"data_type\":\"String\",\"is_required\":true}]}},{\"id\":89,\"match\":{\"message_type\":4,\"event_match_type\":\"\",\"event\":\"\"},\"behavior\":{\"append_unmapped_as_is\":true,\"is_default\":true},\"action\":{\"projected_event_name\":\"\",\"attribute_maps\":[]}},{\"id\":100,\"pmid\":179,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - check order status\",\"attribute_maps\":[]}},{\"id\":100,\"pmid\":180,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"1111995177\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - check order status\",\"attribute_maps\":[]}},{\"id\":92,\"pmid\":181,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - feedback\",\"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"}]}},{\"id\":92,\"pmid\":182,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"1111995177\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - feedback\",\"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\",\"match_type\":\"Hash\",\"value\":\"-768380952\",\"data_type\":\"String\"}]}},{\"id\":96,\"pmid\":183,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - add to tote\",\"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\",\"match_type\":\"Hash\",\"value\":\"102582760\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Print\",\"match_type\":\"Hash\",\"value\":\"102582760\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Category\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Total Amount\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote SKU\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Size\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Quantity\",\"match_type\":\"Static\",\"value\":\"10\",\"data_type\":\"Int\"},{\"projected_attribute_name\":\"Last Add to Tote Unit Price\",\"match_type\":\"Static\",\"value\":\"1321\",\"data_type\":\"String\"}]}},{\"id\":104,\"pmid\":184,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - complete the look\",\"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"}]}},{\"id\":104,\"pmid\":185,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"987878094\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - complete the look\",\"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\",\"match_type\":\"Hash\",\"value\":\"689388774\",\"data_type\":\"String\"}]}},{\"id\":104,\"pmid\":186,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"-754932241\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - complete the look\",\"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\",\"match_type\":\"Hash\",\"value\":\"992037090\",\"data_type\":\"String\"}]}}]}],\"cms\":[{\"id\":28,\"pr\":[{\"f\":\"com.appboy.installation\",\"m\":0,\"ps\":[{\"k\":\"installation_id\",\"t\":1,\"n\":\"iid\",\"d\":\"%g%\"}]},{\"f\":\"com.appboy.device\",\"m\":0,\"ps\":[{\"k\":\"device_id\",\"t\":1,\"n\":\"di\",\"d\":\"\"}]},{\"f\":\"com.appboy.offline.storagemap\",\"m\":0,\"ps\":[{\"k\":\"last_user\",\"t\":1,\"n\":\"lu\",\"d\":\"\"}]}]}],\"lsv\":\"2.3.1\",\"tri\":{\"mm\":[{\"dt\":\"x\",\"eh\":true},{\"dt\":\"x\",\"eh\":false},{\"dt\":\"ast\",\"t\":\"app_init\",\"ifr\":true,\"iu\":false}],\"evts\":[1594525888,-460386492,-1633998520,-201964253,-1698163721,-88346394,-964353845,925748342,1515120746,476338248,-2049994443]},\"pio\":30}";

    /**
     * This is pretty implementation specific. Just making sure that the objects we create accurately reflect the JSON.
     */
    @Test
    public void testParsing() throws Exception {
        JSONObject json = new JSONObject(JSON);
        JSONArray ekConfigs = json.getJSONArray("eks");
        for (int i = 0; i < ekConfigs.length(); i++) {
            JSONObject config = ekConfigs.getJSONObject(i);
            if (config.getInt("id") == 56) {
                JSONArray projJsonList = config.getJSONArray("pr");
                for (int j = 0; j < projJsonList.length(); j++) {
                    JSONObject pJson = projJsonList.getJSONObject(j);
                    Projection projection = new Projection(pJson);
                    assertEquals(pJson.getInt("id"), projection.mID);
                    //sometimes pmmid doesn't exist...:/
                    // assertEquals(pJson.getInt("pmmid"), projection.mModuleMappingId);
                    JSONObject match = pJson.getJSONObject("match");
                    assertEquals(match.getInt("message_type"), projection.getMessageType());
                    assertEquals(match.getString("event_match_type"), projection.mMatchType);
                    if (projection.mMatchType.startsWith(Projection.MATCH_TYPE_HASH)) {
                        assertEquals(match.getInt("event"), projection.mEventHash);
                    } else {
                        assertEquals(match.getString("event"), projection.mEventName);
                    }
                    if (match.has("attribute_key")) {
                        assertEquals(match.getString("attribute_key"), projection.mAttributeKey);
                        assertEquals(match.getString("attribute_value"), projection.mAttributeValue);
                    }

                    JSONObject behaviors = pJson.getJSONObject("behavior");
                    assertEquals(behaviors.optBoolean("is_default"), projection.isDefault());
                    assertEquals(behaviors.optBoolean("append_unmapped_as_is"), projection.mAppendUnmappedAsIs);
                    assertEquals(behaviors.optInt("max_custom_params", Integer.MAX_VALUE), projection.mMaxCustomParams);

                    JSONObject action = pJson.getJSONObject("action");

                    assertEquals(projection.mProjectedEventName, action.getString("projected_event_name"));

                    JSONArray attributes = action.getJSONArray("attribute_maps");
                    int sum = (projection.mRequiredAttributeMapList == null ? 0 : projection.mRequiredAttributeMapList.size()) +
                            (projection.mStaticAttributeMapList == null ? 0 : projection.mStaticAttributeMapList.size());
                    assertEquals(attributes.length(), sum);
                    for (int k = 0; k < attributes.length(); k++) {
                        JSONObject attribute = attributes.getJSONObject(k);
                        Projection.AttributeMap attProj = new Projection.AttributeMap(attribute);
                        assertEquals(attribute.optBoolean("is_required"), attProj.mIsRequired);
                        if (attribute.getString("match_type").startsWith(Projection.MATCH_TYPE_STATIC)) {
                            assertFalse(projection.mRequiredAttributeMapList.contains(attProj));
                            assertTrue(projection.mStaticAttributeMapList.contains(attProj));
                        } else {
                            assertTrue(projection.mRequiredAttributeMapList.contains(attProj));
                            assertFalse(projection.mStaticAttributeMapList.contains(attProj));
                        }
                    }
                    boolean notRequiredFound = false;
                    for (Projection.AttributeMap attributeMap : projection.mRequiredAttributeMapList) {
                        if (notRequiredFound && attributeMap.mIsRequired) {
                            fail("Projection attributes are out of order!");
                        }
                        notRequiredFound = !attributeMap.mIsRequired;
                    }
                }
            }
        }
    }

    @Test
    public void testDefault() throws Exception {
        Projection defaultProjection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).category("whatever!!").info(info).build();
        assertTrue(defaultProjection.isMatch(new EventWrapper.MPEventWrapper(event)));
        MPEvent newEvent = defaultProjection.project(new EventWrapper.MPEventWrapper(event)).get(0).getMPEvent();
        assertEquals(event, newEvent);
        info.put("yet another key", "yet another value");
        //sanity check to make sure we're duplicating the map, not passing around a reference
        assertTrue(event.getInfo().containsKey("yet another key"));
        assertFalse(newEvent.getInfo().containsKey("yet another key"));
    }

    /**
     * Same as above but evaluating from one level up, from the Provider's perspective
     */
    @Test
    public void testDefaultProjection2() throws Exception {
        EmbeddedProvider provider = new FakeProvider(Mockito.mock(EmbeddedKitManager.class));
        provider.parseConfig(new JSONObject("{ \"id\":56, \"as\":{ }, \"hs\":{ }, \"pr\":[ { \"id\":93, \"pmmid\":23, \"match\":{ \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\": \"cool_product_view\"} }, { \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }, { \"id\":100, \"pmid\":179, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - check order status\", \"attribute_maps\":[ ] } }, { \"id\":92, \"pmid\":182, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - feedback\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\" } ] } }, { \"id\":96, \"pmid\":183, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\" }, { \"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":184, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":185, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":186, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\" } ] } } ] }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        List<Projection.ProjectionResult> list = provider.projectEvents(event);
        assertEquals(1, list.size());
        assertEquals(list.get(0).getMPEvent(), event);
    }

    /**
     * Tests both that when there's no default specified, return null, and also matching on message_type
     * @throws Exception
     */
    @Test
    public void testNoDefaultEventAndDefaultScreen() throws Exception {
        EmbeddedProvider provider = new FakeProvider(Mockito.mock(EmbeddedKitManager.class));
        provider.parseConfig(new JSONObject("{ \"id\":56, \"as\":{ }, \"hs\":{ }, \"pr\":[ { \"id\":93, \"pmmid\":23, \"match\":{ \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\": \"cool_product_view\"} }, { \"id\":89, \"match\":{ \"message_type\":3, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }, { \"id\":100, \"pmid\":179, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - check order status\", \"attribute_maps\":[ ] } }, { \"id\":92, \"pmid\":182, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - feedback\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\" } ] } }, { \"id\":96, \"pmid\":183, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\" }, { \"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":184, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":185, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":186, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\" } ] } } ] }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        List<Projection.ProjectionResult> list = provider.projectEvents(event);
        assertNull(list);
        //but if we say this is a screen event, the default projection should be detected
        list = provider.projectEvents(event, true);
        assertEquals(1, list.size());
    }

    /**
     * Make sure that we respect the append_unmapped_as_is property
     *
     * @throws Exception
     */
    @Test
    public void testDontAppendAsIs() throws Exception {
        Projection projection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":false, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        Projection.ProjectionResult newEvent = projection.project(new EventWrapper.MPEventWrapper(event)).get(0);
        assertTrue(newEvent.getMPEvent().getInfo().size() == 0);
        projection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        newEvent = projection.project(new EventWrapper.MPEventWrapper(event)).get(0);
        assertTrue(newEvent.getMPEvent().getInfo().size() == 2);
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
        Projection projection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":5, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        //add them out of order, why not?
        info.put("key 3", "123af4");
        info.put("key 4", "123adf45");
        info.put("key 1", "12ad3456");
        info.put("key 6", "asdasd");
        info.put("key 2", "asdasdaf");
        info.put("key 5", "asdfasea");

        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        Projection.ProjectionResult result = projection.project(new EventWrapper.MPEventWrapper(event)).get(0);
        MPEvent newEvent = result.getMPEvent();
        assertTrue(newEvent.getInfo().size() == 5);
        //key 5 should be there but key 6 should be kicked out due to alphabetic sorting
        assertTrue(newEvent.getInfo().containsKey("key 5"));
        assertFalse(newEvent.getInfo().containsKey("key 6"));
        //now create the SAME projection, except specify an optional attribute key 6 - it should boot key 5 from the resulting event
        projection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":5, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" }] } }"));
        result = projection.project(new EventWrapper.MPEventWrapper(event)).get(0);
        newEvent = result.getMPEvent();
        assertFalse(newEvent.getInfo().containsKey("key 5"));
        assertTrue(newEvent.getInfo().containsKey("key 6")); //note that the json also doesn't specify a key name - so we just use the original
        assertTrue(newEvent.getInfo().size() == 5);

        //test what happens if max isn't even set (everything should be there)
        projection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" }] } }"));
        result = projection.project(new EventWrapper.MPEventWrapper(event)).get(0);
        newEvent = result.getMPEvent();
        assertTrue(newEvent.getInfo().containsKey("key 5"));
        assertTrue(newEvent.getInfo().containsKey("key 6")); //note that the json also doesn't specify a key name - so we just use the original
        assertTrue(newEvent.getInfo().size() == 6);
    }

    /**
     * Test an attribute projection with match_type Field
     *
     * @throws Exception
     */
    @Test
    public void testFieldName() throws Exception {
        Projection projection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 6\", \"data_type\":\"String\" },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"));
        MPEvent event = new MPEvent.Builder("screenname", MParticle.EventType.Other).build();
        Projection.ProjectionResult projectedEvent = projection.project(new EventWrapper.MPEventWrapper(event)).get(0);
        assertEquals("screenname", projectedEvent.getMPEvent().getInfo().get("screen_name_key"));
    }

    @Test
    public void testRequiredDataType() throws Exception {
        Projection projection = new Projection(new JSONObject("{ \"id\":89, \"match\": { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Added to Cart\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$AddToCart\" }, \"action\":{ \"projected_event_name\":\"cool add to cart, man!\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 1\", \"data_type\":\"4\",\"is_required\":true }, { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 2\", \"data_type\":\"2\",\"is_required\":true },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"));
        MPProduct product = new MPProduct.Builder("some product name", "some product sku").build();
        product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_ADD_TO_CART);
        product.put("key 1", "not an int");
        product.put("key 2", "not a float");
        MPEvent productEvent = new MPEvent.Builder(MPProduct.Event.ADD_TO_CART.toString(), MParticle.EventType.Transaction).info(product).build();
        List<Projection.ProjectionResult> projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNull(projectedEvent);

        //still shouldn't work
        product.put("key 1", "2323232");
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNull(projectedEvent);

        //ok now we should be good
        product.put("key 2", "232211");
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNotNull(projectedEvent.get(0).getMPEvent());
        assertEquals("cool add to cart, man!", projectedEvent.get(0).getMPEvent().getEventName());

        //ok now break it again - same projection as above exception key 1 needs to be a boolean
        product.put("key 1", "notfalse OR IS IT?");
        projection = new Projection(new JSONObject("{ \"id\":89, \"match\": { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Added to Cart\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$AddToCart\" }, \"action\":{ \"projected_event_name\":\"cool add to cart, man!\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 1\", \"data_type\":\"3\",\"is_required\":true }, { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 2\", \"data_type\":\"2\",\"is_required\":true },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"));
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNull(projectedEvent);

        //both "false" and "true" should work
        product.put("key 1", "fAlsE");
        projection = new Projection(new JSONObject("{ \"id\":89, \"match\": { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Added to Cart\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$AddToCart\" }, \"action\":{ \"projected_event_name\":\"cool add to cart, man!\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 1\", \"data_type\":\"3\",\"is_required\":true }, { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 2\", \"data_type\":\"2\",\"is_required\":true },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"));
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNotNull(projectedEvent.get(0).getMPEvent());

        product.put("key 1", "tRue");
        projection = new Projection(new JSONObject("{ \"id\":89, \"match\": { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Added to Cart\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$AddToCart\" }, \"action\":{ \"projected_event_name\":\"cool add to cart, man!\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 1\", \"data_type\":\"3\",\"is_required\":true }, { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 2\", \"data_type\":\"2\",\"is_required\":true },{ \"projected_attribute_name\": \"screen_name_key\", \"match_type\": \"Field\", \"value\": \"ScreenName\", \"data_type\": 1, \"is_required\": true }] } }"));
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNotNull(projectedEvent.get(0).getMPEvent());
    }

    @Test
    public void testOptionalDataType() throws Exception {
        Projection projection = new Projection(new JSONObject("{ \"id\":89, \"match\": { \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Added to Cart\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$AddToCart\" }, \"action\":{ \"projected_event_name\":\"cool add to cart, man!\", \"attribute_maps\":[ { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 1\", \"data_type\":\"4\" }, { \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 2\", \"data_type\":\"2\" },{ \"projected_attribute_name\":\"\", \"match_type\":\"String\", \"value\":\"key 3\", \"data_type\":\"3\" }] } }"));
        MPProduct product = new MPProduct.Builder("some product name", "some product sku").build();
        product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_ADD_TO_CART);
        product.put("key 1", "not an int");
        product.put("key 2", "not a float");
        product.put("key 3", "not a boolean");
        MPEvent productEvent = new MPEvent.Builder(MPProduct.Event.ADD_TO_CART.toString(), MParticle.EventType.Transaction).info(product).build();
        List<Projection.ProjectionResult> projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertEquals("cool add to cart, man!", projectedEvent.get(0).getMPEvent().getEventName());
        assertNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 1"));
        assertNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 2"));
        assertNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 3"));

        //now we should see key 1
        product.put("key 1", "2323232");
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertEquals("cool add to cart, man!", projectedEvent.get(0).getMPEvent().getEventName());
        assertEquals("2323232", projectedEvent.get(0).getMPEvent().getInfo().get("key 1"));
        assertNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 2"));
        assertNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 3"));

        //now we should see keys 1 and 2
        product.put("key 2", "232211");
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNotNull(projectedEvent.get(0).getMPEvent());
        assertEquals("cool add to cart, man!", projectedEvent.get(0).getMPEvent().getEventName());
        assertEquals("2323232", projectedEvent.get(0).getMPEvent().getInfo().get("key 1"));
        assertEquals("232211", projectedEvent.get(0).getMPEvent().getInfo().get("key 2"));
        assertNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 3"));

        //now we should see all the keys
        product.put("key 3", "tRuE");
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNotNull(projectedEvent.get(0).getMPEvent());
        assertEquals("cool add to cart, man!", projectedEvent.get(0).getMPEvent().getEventName());
        assertEquals("2323232", projectedEvent.get(0).getMPEvent().getInfo().get("key 1"));
        assertEquals("232211", projectedEvent.get(0).getMPEvent().getInfo().get("key 2"));
        assertNotNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 3"));
        assertTrue(Boolean.parseBoolean(projectedEvent.get(0).getMPEvent().getInfo().get("key 3")));

        //works for true and false
        product.put("key 3", "falsE");
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        projectedEvent = projection.project(new EventWrapper.MPEventWrapper(productEvent));
        assertNotNull(projectedEvent.get(0).getMPEvent());
        assertEquals("cool add to cart, man!", projectedEvent.get(0).getMPEvent().getEventName());
        assertEquals("232211", projectedEvent.get(0).getMPEvent().getInfo().get("key 2"));
        assertNotNull(projectedEvent.get(0).getMPEvent().getInfo().get("key 3"));
        assertFalse(Boolean.parseBoolean(projectedEvent.get(0).getMPEvent().getInfo().get("key 3")));
    }


    /**
     * Check String event match type w/ attribute_key and attribute_value
     */
    @Test
    public void testStringMatch() throws Exception {
        EmbeddedProvider provider = new FakeProvider(Mockito.mock(EmbeddedKitManager.class));
        provider.parseConfig(new JSONObject("{ \"id\":56, \"as\":{ }, \"hs\":{ }, \"pr\":[ { \"id\":93, \"pmmid\":23, \"match\":{ \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\": \"cool_product_view\"} }, { \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }, { \"id\":100, \"pmid\":179, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - check order status\", \"attribute_maps\":[ ] } }, { \"id\":92, \"pmid\":182, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - feedback\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\" } ] } }, { \"id\":96, \"pmid\":183, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\" }, { \"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":184, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":185, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":186, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\" } ] } } ] }"));
        MPProduct product = new MPProduct.Builder("some product name", "some product sku").build();
        product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_VIEW);
        MPEvent productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        List<Projection.ProjectionResult> list = provider.projectEvents(productEvent);
        assertEquals(1, list.size());
        MPEvent projectedProductEvent = list.get(0).getMPEvent();
        assertNotEquals(productEvent, projectedProductEvent);
        assertEquals(0, projectedProductEvent.getInfo().size());
        assertEquals("cool_product_view", projectedProductEvent.getEventName());

        //this shouldn't match, should just spit back the same event.
        product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_VIEW + "WRONG"); //this shouldn't match
        productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        list = provider.projectEvents(productEvent);
        assertEquals(1, list.size());
        projectedProductEvent = list.get(0).getMPEvent();
        assertEquals(productEvent, projectedProductEvent);
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
        MPEvent event = new MPEvent.Builder("some event name", MParticle.EventType.Other).info(info).build();
        EventWrapper.MPEventWrapper wrapper = new EventWrapper.MPEventWrapper(event);

        //make sure the attribute hashes work as intended
        Map<Integer, String> hashes = wrapper.getAttributeHashes();
        String key = hashes.get(MPUtility.mpHash(event.getEventType().ordinal() + event.getEventName() + "key 1"));
        assertEquals(info.get(key), "value 1");

        //make sure event hash is generated correctly
        assertEquals(MPUtility.mpHash(event.getEventType().ordinal() + event.getEventName()),wrapper.getEventHash());

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
        MPEvent event = new MPEvent.Builder("some event name", MParticle.EventType.Other).info(info).build();
        EventWrapper.MPEventWrapper wrapper = new EventWrapper.MPEventWrapper(event, true);

        //make sure the attribute hashes work as intended
        Map<Integer, String> hashes = wrapper.getAttributeHashes();
        //event type be 0 for screen views
        String key = hashes.get(MPUtility.mpHash(0 + event.getEventName() + "key 1"));
        assertEquals(info.get(key), "value 1");

        //make sure event hash is generated correctly
        assertEquals(MPUtility.mpHash(0 + event.getEventName()),wrapper.getEventHash());

        assertEquals(3, wrapper.getMessageType());
        assertEquals(0, wrapper.getEventTypeOrdinal());
    }

    @Test
    public void testECommerceProjections() {

    }

    /**
     * This is a sort-of mega-test. See comments below.
     */
    @Test
    public void testMultiHashProjections() throws Exception {
        EmbeddedProvider provider = new FakeProvider(Mockito.mock(EmbeddedKitManager.class));
        provider.parseConfig(new JSONObject("{ \"id\":56, \"as\":{ }, \"hs\":{ }, \"pr\":[ { \"id\":93, \"pmmid\":23, \"match\":{ \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"cool_product_view\" } }, { \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }, { \"id\":100, \"pmid\":179, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"account - check order status\", \"attribute_maps\":[ ] } }, { \"id\":92, \"pmid\":182, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - feedback\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\" } ] } }, { \"id\":96, \"pmid\":183, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, { \"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\" }, { \"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":184, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, { \"id\":104, \"pmid\":185, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":186, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\" } ] } } ] }"));

        /**
         * Test 1
         * The first test shows that we correctly match on the Hash, but that the other projections are not triggered b/c attributes are missing
         */
        //all of these projections match on a Hash of the following name/type
        MPEvent.Builder builder = new MPEvent.Builder("sproj 1", MParticle.EventType.UserContent);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("attribute we don't care about", "some value");
        builder.info(attributes);
        List<Projection.ProjectionResult> eventList = provider.projectEvents(builder.build());
        assertEquals(1, eventList.size());
        MPEvent projEvent1 = eventList.get(0).getMPEvent();
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getInfo().get("attribute we don't care about"));


        /**
         * Test 2
         * Now add a new attribute "Value" to the same event, which now triggers the original projection and a new projection
         * that requires the new attribute
         */
        //add an attribute that's required by 1 of them, we should end up with 2 triggered projections
        attributes.put("Value", "product name");
        eventList = provider.projectEvents(builder.build());
        assertEquals(2, eventList.size());
        projEvent1 = eventList.get(0).getMPEvent();
        //same as test 1, but verify for the fun of it.
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getInfo().get("attribute we don't care about"));

        //this is the new projection which requires the Value attribute
        projEvent1 = eventList.get(1).getMPEvent();
        assertEquals("pdp - add to tote", projEvent1.getEventName());
        //required attribute
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote Category")); //the required attribute has been renamed
        //non-required attributes which define the same hash as the required one.
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote Total Amount"));
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote SKU"));

        //static attributes are in this projection as well.
        assertEquals("10", projEvent1.getInfo().get("Last Add to Tote Quantity"));
        assertEquals("1321", projEvent1.getInfo().get("Last Add to Tote Unit Price"));

        /**
         * Test 3
         * The final test shows adding another attribute which not only triggers a 3rd projection, but also adds onto the 2nd projection which
         * defines that attribute as non-required.
         */
        attributes.put("Label", "product label");
        eventList = provider.projectEvents(builder.build());
        assertEquals(3, eventList.size());

        projEvent1 = eventList.get(0).getMPEvent();
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getInfo().get("attribute we don't care about"));

        projEvent1 = eventList.get(1).getMPEvent();
        assertEquals("pdp - add to tote", projEvent1.getEventName());
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote Category"));
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote Total Amount"));
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote SKU"));
        assertEquals("10", projEvent1.getInfo().get("Last Add to Tote Quantity"));
        assertEquals("1321", projEvent1.getInfo().get("Last Add to Tote Unit Price"));

        //these are new for the 2nd projection, as they match the Hash for the new  "Label" attribute
        assertEquals("product label", projEvent1.getInfo().get("Last Add to Tote Name"));
        assertEquals("product label", projEvent1.getInfo().get("Last Add to Tote Print"));

        //and here's our 3rd projection, which defines both the original "Value" attribute hash as well as "Label"
        projEvent1 = eventList.get(2).getMPEvent();
        assertEquals("pdp - complete the look", projEvent1.getEventName());
        assertEquals("product name", projEvent1.getInfo().get("Complete the Look Product Name"));
        assertEquals("product label", projEvent1.getInfo().get("Complete the Look Product Name 2"));

    }

    /**
     * Testing:
     *
     * 1. Converting CommerceEvent to MPEvent
     * 2. Foreach logic - n products should yield n events
     * 3. ProductField, EventField mapping
     * 4. Hash matching
     *
     * @throws Exception
     */
    @Test
    public void testCommerceEventToMPEvent() throws Exception {
        String config = "{\"id\":93, \"pmid\":220, \"match\":{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1572\"}, \"behavior\":{\"max_custom_params\":0, \"selector\":\"foreach\"}, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"Hash\", \"value\":\"2000445218\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"Hash\", \"value\":\"881337592\", \"data_type\":\"String\", \"property\":\"EventField\"}, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"Hash\", \"value\":\"1514047\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"Hash\", \"value\":\"1455148719\", \"data_type\":\"String\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"Hash\", \"value\":\"664929967\", \"data_type\":\"Int\", \"property\":\"ProductField\"}, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"Hash\", \"value\":\"1647761705\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":4 } }";
        Projection projection = new Projection(new JSONObject(config));
        Product.Builder productBuilder = new Product.Builder("product name 0", "product id 0", 1).category("product category 0").quantity(1);

        CommerceEvent.Builder commerceEventBuilder = new CommerceEvent.Builder(CommerceEvent.DETAIL, productBuilder.build()).currency("dollar bills");
        for (int i = 1; i < 5; i++){
            commerceEventBuilder.addProduct(productBuilder.name("product name " + i).sku("product id " + i).category("product category " + i).quantity(i+1).unitPrice(i+1).build());
        }
        CommerceEvent commerceEvent = commerceEventBuilder.build();
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        List<Projection.ProjectionResult> result = projection.project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(5, result.size());

        for (int i = 0; i < 5; i++) {
            MPEvent event = result.get(i).getMPEvent();
            assertNotNull(event);
            assertEquals("pdp - product view", event.getEventName());
            Map<String, String> attributes = event.getInfo();
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
        JSONObject config = new JSONObject("{\"id\":99, \"pmid\":229, \"match\":{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", }, \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }");
        CommerceEvent event = new CommerceEvent.Builder(CommerceEvent.DETAIL, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        Projection projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

         event = new CommerceEvent.Builder(CommerceEvent.ADD_TO_CART, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
         projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.PURCHASE, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.ADD_TO_WISHLIST, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.CHECKOUT_OPTION, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.CLICK, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.REFUND, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.REMOVE_FROM_CART, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(CommerceEvent.REMOVE_FROM_WISHLIST, product).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(new CommerceEvent.Impression("list name", product)).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Promotion.VIEW, new Promotion().setId("id")).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));

        event = new CommerceEvent.Builder(Promotion.CLICK, new Promotion().setId("id")).transactionAttributes(new TransactionAttributes().setId("id")).build();
        config.getJSONObject("match").put("event",""+MPUtility.mpHash(""+CommerceEventUtil.getEventType(event)));
        projection = new Projection(config);
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(event)));
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
        JSONObject config = new JSONObject("{\"id\":99, \"pmid\":229, \"match\":{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", \"property\":\"EventField\", \"property_name\":\"-601244443\", \"property_value\":\"5\"}, \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }");
        Projection projection = new Projection(config);
        Product product = new Product.Builder("name", "sku", 0).build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product).checkoutStep(4).build();
        assertFalse(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product).checkoutStep(5).build();
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));

        config.getJSONObject("match").put("property", "EventAttribute");
        projection = new Projection(config);
        assertFalse(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("Checkout Step", "5");
        commerceEvent = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product).customAttributes(attributes).build();
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));

        config.getJSONObject("match").put("property", "ProductAttribute");
        projection = new Projection(config);
        assertFalse(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product).addProduct(new Product.Builder("name 2", "sku", 0).customAttributes(attributes).build()).build();
        EventWrapper.CommerceEventWrapper wrapper = new EventWrapper.CommerceEventWrapper(commerceEvent);
        assertTrue(projection.isMatch(wrapper));
        //only 1 product has the required attribute so there should only be 1 product after the matching step
        assertEquals(1, wrapper.getEvent().getProducts().size());
        assertEquals("name 2", wrapper.getEvent().getProducts().get(0).getName());

        config.getJSONObject("match").put("property", "ProductField");
        config.getJSONObject("match").put("property_name", "-1167125985"); //checkout + category hash
        config.getJSONObject("match").put("property_value", "some product cat");
        projection = new Projection(config);
        assertFalse(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product).addProduct(new Product.Builder("name 2", "sku", 0).category("some product cat").build()).build();
        wrapper = new EventWrapper.CommerceEventWrapper(commerceEvent);
        assertTrue(projection.isMatch(wrapper));
        //only 1 product has the required attribute so there should only be 1 product after the matching step
        assertEquals(1, wrapper.getEvent().getProducts().size());
        assertEquals("some product cat", wrapper.getEvent().getProducts().get(0).getCategory());

        config.getJSONObject("match").put("property", "PromotionField");
        config.getJSONObject("match").put("property_name", "835505623"); //click + creative hash
        config.getJSONObject("match").put("property_value", "some promotion creative");
        projection = new Projection(config);
        assertFalse(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        commerceEvent = new CommerceEvent.Builder(Promotion.CLICK, new Promotion().setCreative("some promotion creative")).addPromotion(
                new Promotion().setCreative("some other creative")
        ).build();
        wrapper = new EventWrapper.CommerceEventWrapper(commerceEvent);
        assertTrue(projection.isMatch(wrapper));
        //only 1 promotion has the required attribute so there should only be 1 promotion after the matching step
        assertEquals(1, wrapper.getEvent().getPromotions().size());
        assertEquals("some promotion creative", wrapper.getEvent().getPromotions().get(0).getCreative());
    }

    /**
     * Test projecting from CommerceEvent to CommerceEvent
     *
     * Also testing foreach vs last when going CE->CE
     *
     * @throws Exception
     */
    @Test
    public void testCommerceEventToCommerceEvent() throws Exception {
        JSONObject config = new JSONObject("{\"id\":99, \"pmid\":229, \"match\":{\"message_type\":16, \"event_match_type\":\"Hash\", \"event\":\"1569\", \"property\":\"EventField\", \"property_name\":\"-601244443\", \"property_value\":\"5\"}, \"behavior\":{\"max_custom_params\":0, \"selector\":\"last\"}, \"action\":{\"projected_event_name\":\"checkout - place order\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Place Order Category\", \"match_type\":\"Hash\", \"value\":\"-1167125985\", \"data_type\":\"String\", \"property\":\"ProductField\"} ], \"outbound_message_type\":16 } }");
        Projection projection = new Projection(config);
        Product product = new Product.Builder("name", "sku", 0).category("category 0").build();
        CommerceEvent commerceEvent = new CommerceEvent.Builder(CommerceEvent.CHECKOUT, product)
                .addProduct(new Product.Builder("name 1", "sku", 0).category("category 1").build())
                .addProduct(new Product.Builder("name 2", "sku", 0).category("category 2").build())
                .addProduct(new Product.Builder("name 3", "sku", 0).category("category 3").build())
                .addProduct(new Product.Builder("name 4", "sku", 0).category("category 4").build())
                .checkoutStep(5).build();
        assertTrue(projection.isMatch(new EventWrapper.CommerceEventWrapper(commerceEvent)));
        List<Projection.ProjectionResult> result = projection.project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(1, result.size());
        Projection.ProjectionResult projectionResult = result.get(0);
        CommerceEvent event = projectionResult.getCommerceEvent();
        assertNotNull(event);
        assertEquals("checkout - place order", event.getEventName());
        assertEquals(5, event.getProducts().size());
        assertEquals("category 4", event.getCustomAttributes().get("Last Place Order Category"));

        config.getJSONObject("behavior").put("selector", "foreach");
        result = new Projection(config).project(new EventWrapper.CommerceEventWrapper(commerceEvent));
        assertEquals(5, result.size());

        int i = 0;
        for (Projection.ProjectionResult projectionResult1 : result) {
            CommerceEvent event1 = projectionResult1.getCommerceEvent();
            assertEquals("checkout - place order", event1.getEventName());
            assertEquals(1, event1.getProducts().size());
            assertNotNull(event1);
            assertEquals("category " + i, event1.getCustomAttributes().get("Last Place Order Category"));
            i++;
        }
    }
}