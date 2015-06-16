package com.mparticle.internal.embedded;

import com.mparticle.MPEvent;
import com.mparticle.MPProduct;
import com.mparticle.MParticle;
import com.mparticle.internal.Constants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ProjectionTest {
    static final String JSON = "{\"dt\":\"ac\",\"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\",\"ct\":1434392412994,\"dbg\":false,\"cue\":\"appdefined\",\"pmk\":[\"mp_message\",\"com.urbanairship.push.ALERT\",\"alert\",\"a\",\"message\"],\"cnp\":\"appdefined\",\"soc\":0,\"oo\":false,\"eks\":[{\"id\":28,\"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\",\"sendPushOpenedWhenAppInForeground\":\"False\",\"push_enabled\":\"True\",\"register_inapp\":\"True\",\"appGroupId\":\"\"},\"hs\":{\"ec\":{\"1824528345\":0,\"1824528346\":0,\"1824528347\":0,\"1824528348\":0,\"1824528341\":0,\"1824528342\":0,\"1824528343\":0,\"1824528350\":0,\"54237\":0,\"-201964253\":0,\"-1015351211\":0,\"-1698163721\":0,\"642507618\":0,\"-1750207685\":0,\"-1400806385\":0,\"-435072817\":0,\"612161276\":0,\"-2049994443\":0,\"1798380893\":0,\"-460386492\":0,\"476338248\":0,\"-1964837950\":0,\"-115592573\":0,\"-1119044065\":0,\"-1229406110\":0,\"1612904218\":0,\"-459588721\":0,\"93769843\":0,\"-1831156729\":0,\"925748342\":0,\"-1471879983\":0,\"-1471879982\":0,\"-1471879985\":0,\"-1471879984\":0,\"-1471879987\":0,\"-1471879986\":0,\"-1471879989\":0,\"-1471879988\":0,\"-1471879991\":0,\"-1471879990\":0,\"-1175432756\":0,\"-1439088726\":0,\"-630837254\":0,\"-1528980234\":0,\"866346720\":0,\"-466914683\":0,\"584870613\":0,\"-71005245\":0,\"-71005246\":0,\"-71005248\":0,\"-71005251\":0,\"-71005254\":0,\"-192614420\":0,\"-1899939497\":0,\"-138049017\":0,\"1755914106\":0,\"1713887651\":0,\"-1680991381\":0,\"1381973565\":0,\"1696869197\":0,\"530926139\":0,\"-1591103548\":0,\"606683084\":0,\"-452884081\":0,\"1156084566\":0,\"-1684704584\":0,\"-1684704582\":0,\"-1684704517\":0,\"-1684704551\":0,\"-1684704492\":0,\"-1684704484\":0,\"-1507615010\":0,\"1496713379\":0,\"1496713380\":0,\"1496713373\":0,\"1496713374\":0,\"1496713371\":0,\"1496713372\":0,\"1496713377\":0,\"1496713375\":0,\"1496713376\":0,\"448941660\":0,\"455787894\":0,\"1057880655\":0,\"-153747136\":0,\"228100699\":0,\"1956870096\":0,\"367619406\":0,\"-1728365802\":0,\"1315260226\":0,\"713953332\":0,\"54115406\":0,\"-1075988785\":0,\"-1726724035\":0,\"1195528703\":0,\"-1415615126\":0,\"-1027713269\":0,\"-181380149\":0,\"-115531678\":0,\"-100487028\":0,\"-1233979378\":0,\"843036051\":0,\"912926294\":0,\"56084205\":0,\"1594525888\":0,\"-1573616412\":0,\"-1417002190\":0,\"1794482897\":0,\"224683764\":0,\"-1471969403\":0,\"596888957\":0,\"596888956\":0,\"596888953\":0,\"596888952\":0,\"596888955\":0,\"596888954\":0,\"596888949\":0,\"596888948\":0,\"596888950\":0,\"972118770\":0,\"-1097220876\":0,\"-1097220881\":0,\"-1097220880\":0,\"-1097220879\":0,\"-1097220878\":0,\"-1097220885\":0,\"-1097220884\":0,\"-1097220883\":0,\"-1097220882\":0,\"-582505992\":0,\"-814117771\":0,\"1414371548\":0,\"682253748\":0,\"682253740\":0,\"682253745\":0,\"682253744\":0,\"682253747\":0,\"1659263444\":0,\"-136616030\":0,\"1888580672\":0,\"1888580669\":0,\"1888580668\":0,\"1888580666\":0,\"1888580663\":0,\"1888580664\":0,\"1230284208\":0,\"1684003336\":0,\"-726561745\":0,\"-1449123489\":0,\"1961938929\":0,\"1961938921\":0,\"1961938920\":0,\"1961938923\":0,\"1961938922\":0,\"1961938925\":0,\"1961938924\":0,\"1961938927\":0,\"1961938926\":0,\"1790423703\":0,\"1359366927\":0,\"1025548221\":0,\"507221049\":0,\"1515120746\":0,\"-956692642\":0,\"-1011688057\":0,\"371448668\":0,\"1101201489\":0,\"-1535298586\":0,\"56181691\":0,\"-709351854\":0,\"-1571155573\":0,\"1833524190\":0,\"1658269412\":0,\"-2138078264\":0,\"1706381873\":0,\"1795771134\":0,\"-610294159\":0},\"svea\":{\"-604737418\":0,\"-1350758925\":0,\"699878711\":0,\"-409251596\":0,\"1646521091\":0,\"1891689827\":0},\"ua\":{\"341203229\":0,\"96511\":0,\"3373707\":0,\"1193085\":0,\"635848677\":0,\"-564885382\":0,\"1168987\":0,\"102865796\":0,\"3552215\":0,\"3648196\":0,\"-892481550\":0,\"405645589\":0,\"405645588\":0,\"405645591\":0,\"405645590\":0,\"405645592\":0,\"3492908\":0}},\"pr\":[]},{\"id\":56,\"as\":{\"secretKey\":\"testappkey\",\"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\",\"sendTransactionData\":\"True\",\"eventAttributeList\":null},\"hs\":{\"et\":{\"48\":0,\"57\":0},\"ec\":{\"1824528345\":0,\"1824528346\":0,\"1824528347\":0,\"1824528348\":0,\"1824528341\":0,\"1824528342\":0,\"1824528343\":0,\"1824528350\":0,\"54237\":0,\"-201964253\":0,\"-1015351211\":0,\"-1698163721\":0,\"642507618\":0,\"-1750207685\":0,\"-1400806385\":0,\"-435072817\":0,\"612161276\":0,\"-2049994443\":0,\"1798380893\":0,\"-460386492\":0,\"476338248\":0,\"-1964837950\":0,\"-115592573\":0,\"-1119044065\":0,\"-1229406110\":0,\"1612904218\":0,\"-459588721\":0,\"93769843\":0,\"-1831156729\":0,\"925748342\":0,\"-1471879983\":0,\"-1471879982\":0,\"-1471879985\":0,\"-1471879984\":0,\"-1471879987\":0,\"-1471879986\":0,\"-1471879989\":0,\"-1471879988\":0,\"-1471879991\":0,\"-1471879990\":0,\"-1175432756\":0,\"-1439088726\":0,\"-630837254\":0,\"-1528980234\":0,\"866346720\":0,\"-466914683\":0,\"584870613\":0,\"-71005245\":0,\"-71005246\":0,\"-71005248\":0,\"-71005251\":0,\"-71005254\":0,\"-192614420\":0,\"-1899939497\":0,\"-138049017\":0,\"1755914106\":0,\"1713887651\":0,\"-1680991381\":0,\"1381973565\":0,\"1696869197\":0,\"530926139\":0,\"-1591103548\":0,\"606683084\":0,\"-452884081\":0,\"-1684704584\":0,\"-1684704582\":0,\"-1684704517\":0,\"-1684704551\":0,\"-1684704492\":0,\"-1684704484\":0,\"-1507615010\":0,\"448941660\":0,\"455787894\":0,\"1057880655\":0,\"-153747136\":0,\"228100699\":0,\"1956870096\":0,\"367619406\":0,\"-1728365802\":0,\"1315260226\":0,\"713953332\":0,\"54115406\":0,\"-1075988785\":0,\"-1726724035\":0,\"1195528703\":0,\"-1415615126\":0,\"-1027713269\":0,\"-181380149\":0,\"-115531678\":0,\"-100487028\":0,\"-1233979378\":0,\"843036051\":0,\"912926294\":0,\"1594525888\":0,\"-1573616412\":0,\"-1417002190\":0,\"1794482897\":0,\"224683764\":0,\"-1471969403\":0,\"596888957\":0,\"596888956\":0,\"596888953\":0,\"596888952\":0,\"596888955\":0,\"596888954\":0,\"596888949\":0,\"596888948\":0,\"596888950\":0,\"972118770\":0,\"-1097220876\":0,\"-1097220881\":0,\"-1097220880\":0,\"-1097220879\":0,\"-1097220878\":0,\"-1097220885\":0,\"-1097220884\":0,\"-1097220883\":0,\"-1097220882\":0,\"-582505992\":0,\"-814117771\":0,\"1414371548\":0,\"682253748\":0,\"682253740\":0,\"682253745\":0,\"682253744\":0,\"682253747\":0,\"1659263444\":0,\"-136616030\":0,\"1888580672\":0,\"1888580669\":0,\"1888580668\":0,\"1888580666\":0,\"1888580663\":0,\"1888580664\":0,\"1230284208\":0,\"1684003336\":0,\"-726561745\":0,\"-1449123489\":0,\"1961938929\":0,\"1961938921\":0,\"1961938920\":0,\"1961938923\":0,\"1961938922\":0,\"1961938925\":0,\"1961938924\":0,\"1961938927\":0,\"1961938926\":0,\"1790423703\":0,\"1359366927\":0,\"1025548221\":0,\"507221049\":0,\"1515120746\":0,\"-956692642\":0,\"-1011688057\":0,\"371448668\":0,\"1101201489\":0,\"-1535298586\":0,\"-709351854\":0,\"-1571155573\":0,\"1833524190\":0,\"1658269412\":0,\"-2138078264\":0,\"1706381873\":0,\"1795771134\":0,\"-610294159\":0},\"svec\":{\"-385188961\":0,\"303102897\":0,\"303102895\":0,\"303102890\":0,\"303102891\":0,\"303102899\":0,\"1688308747\":0,\"-149109002\":0,\"-1254039557\":0,\"847138800\":0,\"847138801\":0,\"847138799\":0,\"-204085080\":0,\"1658373353\":0,\"-1493744191\":0,\"1861873109\":0,\"-732268618\":0},\"ua\":{\"341203229\":0,\"96511\":0,\"3373707\":0,\"1193085\":0,\"635848677\":0,\"-564885382\":0,\"1168987\":0,\"102865796\":0,\"3552215\":0,\"3648196\":0,\"-892481550\":0,\"405645589\":0,\"405645588\":0,\"405645591\":0,\"405645590\":0,\"405645592\":0,\"3492908\":0}},\"pr\":[{\"id\":93,\"pmmid\":23,\"match\":{\"message_type\":4,\"event_match_type\":\"String\",\"event\":\"Product View\",\"attribute_key\":\"$MethodName\",\"attribute_value\":\"$ProductView\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - product view\",\"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\",\"match_type\":\"String\",\"value\":\"ProductCategory\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Quantity\",\"match_type\":\"String\",\"value\":\"ProductQuantity\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Total Amount\",\"match_type\":\"String\",\"value\":\"RevenueAmount\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View SKU\",\"match_type\":\"String\",\"value\":\"ProductSKU\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Currency\",\"match_type\":\"String\",\"value\":\"CurrencyCode\",\"data_type\":\"String\",\"is_required\":true},{\"projected_attribute_name\":\"Last Product View Name\",\"match_type\":\"String\",\"value\":\"ProductName\",\"data_type\":\"String\",\"is_required\":true}]}},{\"id\":89,\"match\":{\"message_type\":4,\"event_match_type\":\"\",\"event\":\"\"},\"behavior\":{\"append_unmapped_as_is\":true,\"is_default\":true},\"action\":{\"projected_event_name\":\"\",\"attribute_maps\":[]}},{\"id\":100,\"pmid\":179,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - check order status\",\"attribute_maps\":[]}},{\"id\":100,\"pmid\":180,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"1111995177\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - check order status\",\"attribute_maps\":[]}},{\"id\":92,\"pmid\":181,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - feedback\",\"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"}]}},{\"id\":92,\"pmid\":182,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"1111995177\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"account - feedback\",\"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\",\"match_type\":\"Hash\",\"value\":\"-768380952\",\"data_type\":\"String\"}]}},{\"id\":96,\"pmid\":183,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - add to tote\",\"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\",\"match_type\":\"Hash\",\"value\":\"102582760\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Print\",\"match_type\":\"Hash\",\"value\":\"102582760\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Category\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Total Amount\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote SKU\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Size\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"},{\"projected_attribute_name\":\"Last Add to Tote Quantity\",\"match_type\":\"Static\",\"value\":\"10\",\"data_type\":\"Int\"},{\"projected_attribute_name\":\"Last Add to Tote Unit Price\",\"match_type\":\"Static\",\"value\":\"1321\",\"data_type\":\"String\"}]}},{\"id\":104,\"pmid\":184,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"178531468\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - complete the look\",\"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\",\"match_type\":\"Hash\",\"value\":\"111828069\",\"data_type\":\"String\"}]}},{\"id\":104,\"pmid\":185,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"987878094\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - complete the look\",\"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\",\"match_type\":\"Hash\",\"value\":\"689388774\",\"data_type\":\"String\"}]}},{\"id\":104,\"pmid\":186,\"match\":{\"message_type\":4,\"event_match_type\":\"Hash\",\"event\":\"-754932241\"},\"behavior\":{\"max_custom_params\":0},\"action\":{\"projected_event_name\":\"pdp - complete the look\",\"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\",\"match_type\":\"Hash\",\"value\":\"992037090\",\"data_type\":\"String\"}]}}]}],\"cms\":[{\"id\":28,\"pr\":[{\"f\":\"com.appboy.installation\",\"m\":0,\"ps\":[{\"k\":\"installation_id\",\"t\":1,\"n\":\"iid\",\"d\":\"%g%\"}]},{\"f\":\"com.appboy.device\",\"m\":0,\"ps\":[{\"k\":\"device_id\",\"t\":1,\"n\":\"di\",\"d\":\"\"}]},{\"f\":\"com.appboy.offline.storagemap\",\"m\":0,\"ps\":[{\"k\":\"last_user\",\"t\":1,\"n\":\"lu\",\"d\":\"\"}]}]}],\"lsv\":\"2.3.1\",\"tri\":{\"mm\":[{\"dt\":\"x\",\"eh\":true},{\"dt\":\"x\",\"eh\":false},{\"dt\":\"ast\",\"t\":\"app_init\",\"ifr\":true,\"iu\":false}],\"evts\":[1594525888,-460386492,-1633998520,-201964253,-1698163721,-88346394,-964353845,925748342,1515120746,476338248,-2049994443]},\"pio\":30}";

    @Test
    public void testParsing() throws Exception {
        JSONObject json = new JSONObject(JSON);
        JSONArray ekConfigs = json.getJSONArray("eks");
        for (int i = 0; i < ekConfigs.length(); i++) {
            JSONObject config = ekConfigs.getJSONObject(i);
            if (config.getInt("id") == 56){
                JSONArray projJsonList = config.getJSONArray("pr");
                for (int j = 0; j < projJsonList.length(); j++) {
                    JSONObject pJson = projJsonList.getJSONObject(j);
                    Projection projection = new Projection(pJson);
                    assertEquals(pJson.getInt("id"), projection.mID);
                    // assertEquals(pJson.getInt("pmmid"), projection.mModuleMappingId);
                    JSONObject match = pJson.getJSONObject("match");
                    assertEquals(match.getInt("message_type"), projection.getMessageType());
                    assertEquals(match.getString("event_match_type"), projection.mMatchType);
                    if (projection.mMatchType.startsWith(Projection.MATCH_TYPE_HASH)) {
                        assertEquals(match.getInt("event"), projection.mEventHash);
                    } else{
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
                    int sum = (projection.mRequiredAttributeProjectionList == null ? 0 : projection.mRequiredAttributeProjectionList.size()) +
                            (projection.mAttributeProjectionList == null ? 0 : projection.mAttributeProjectionList.size())+
                            (projection.mStaticAttributeProjectionList == null ? 0 : projection.mStaticAttributeProjectionList.size());
                    assertEquals(attributes.length(), sum);
                    for (int k = 0; k < attributes.length(); k++) {
                        JSONObject attribute = attributes.getJSONObject(k);
                        Projection.AttributeProjection attProj = new Projection.AttributeProjection(attribute);
                        assertEquals(attribute.optBoolean("is_required"), attProj.mIsRequired);
                        if (attribute.optBoolean("is_required")) {
                            assertTrue(projection.mRequiredAttributeProjectionList.contains(attProj));
                            assertFalse(projection.mAttributeProjectionList.contains(attProj));
                            assertFalse(projection.mStaticAttributeProjectionList.contains(attProj));
                        }else if (attribute.getString("match_type").startsWith(Projection.MATCH_TYPE_STATIC)) {
                            assertFalse(projection.mRequiredAttributeProjectionList.contains(attProj));
                            assertFalse(projection.mAttributeProjectionList.contains(attProj));
                            assertTrue(projection.mStaticAttributeProjectionList.contains(attProj));
                        }else {
                            assertFalse(projection.mRequiredAttributeProjectionList.contains(attProj));
                            assertTrue(projection.mAttributeProjectionList.contains(attProj));
                            assertFalse(projection.mStaticAttributeProjectionList.contains(attProj));
                        }
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
        assertTrue(defaultProjection.isMatch(new EmbeddedProvider.MPEventWrapper(event)));
        MPEvent newEvent = defaultProjection.project(new EmbeddedProvider.MPEventWrapper(event));
        assertEquals(event, newEvent);
        info.put("yet another key", "yet another value");
        assertTrue(event.getInfo().containsKey("yet another key"));
        assertFalse(newEvent.getInfo().containsKey("yet another key"));
    }

    @Test
    public void testDontAppendAsIs() throws Exception {
        Projection defaultProjection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":false, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        MPEvent newEvent = defaultProjection.project(new EmbeddedProvider.MPEventWrapper(event));
        assertTrue(newEvent.getInfo().size() == 0);
    }

    @Test
    public void testMaxParams() throws Exception {
        Projection defaultProjection = new Projection(new JSONObject("{ \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"max_custom_params\":1, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        MPEvent newEvent = defaultProjection.project(new EmbeddedProvider.MPEventWrapper(event));
        assertTrue(newEvent.getInfo().size() == 1);
        assertTrue(newEvent.getInfo().containsKey("another key"));
    }

    @Test
    public void projectionTest1() throws Exception {
        EmbeddedProvider provider = new FakeProvider(Mockito.mock(EmbeddedKitManager.class));
        provider.parseConfig(new JSONObject("{ \"id\":56, \"as\":{ }, \"hs\":{ }, \"pr\":[ { \"id\":93, \"pmmid\":23, \"match\":{ \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\": \"cool_product_view\"} }, { \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }, { \"id\":100, \"pmid\":179, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - check order status\", \"attribute_maps\":[ ] } }, { \"id\":92, \"pmid\":182, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - feedback\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\" } ] } }, { \"id\":96, \"pmid\":183, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\" }, { \"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":184, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":185, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":186, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\" } ] } } ] }"));
        HashMap<String, String> info = new HashMap<String, String>();
        info.put("some key", "some value");
        info.put("another key", "another value");
        MPEvent event = new MPEvent.Builder("whatever", MParticle.EventType.Other).info(info).build();
        List<MPEvent> list = provider.projectEvents(event);
        assertEquals(1, list.size());
        assertEquals(list.get(0), event);


        MPProduct product = new MPProduct.Builder("some product name", "some product sku").build();
        product.put(Constants.MethodName.METHOD_NAME, Constants.MethodName.LOG_ECOMMERCE_VIEW);
        MPEvent productEvent = new MPEvent.Builder(MPProduct.Event.VIEW.toString(), MParticle.EventType.Transaction).info(product).build();
        list = provider.projectEvents(productEvent);
        assertEquals(1, list.size());
        MPEvent projectedProductEvent = list.get(0);
        assertEquals(0, projectedProductEvent.getInfo().size());
        assertEquals("cool_product_view", projectedProductEvent.getEventName());

    }

    @Test
    public void multiprojectionTest() throws Exception {
        EmbeddedProvider provider = new FakeProvider(Mockito.mock(EmbeddedKitManager.class));
        provider.parseConfig(new JSONObject("{ \"id\":56, \"as\":{ }, \"hs\":{ }, \"pr\":[ { \"id\":93, \"pmmid\":23, \"match\":{ \"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"$MethodName\", \"attribute_value\":\"$ProductView\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"cool_product_view\" } }, { \"id\":89, \"match\":{ \"message_type\":4, \"event_match_type\":\"\", \"event\":\"\" }, \"behavior\":{ \"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{ \"projected_event_name\":\"\", \"attribute_maps\":[ ] } }, { \"id\":100, \"pmid\":179, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":10, \"append_unmapped_as_is\":true }, \"action\":{ \"projected_event_name\":\"account - check order status\", \"attribute_maps\":[ ] } }, { \"id\":92, \"pmid\":182, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"account - feedback\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\" } ] } }, { \"id\":96, \"pmid\":183, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\", \"is_required\":true }, { \"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\" }, { \"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":184, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\" }, { \"projected_attribute_name\":\"Complete the Look Product Name 2\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\", \"is_required\":true } ] } }, { \"id\":104, \"pmid\":185, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\" } ] } }, { \"id\":104, \"pmid\":186, \"match\":{ \"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\" }, \"behavior\":{ \"max_custom_params\":0 }, \"action\":{ \"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[ { \"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\" } ] } } ] }"));
        MPEvent.Builder builder = new MPEvent.Builder("sproj 1", MParticle.EventType.UserContent);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("attribute we don't care about", "some value");
        builder.info(attributes);
        //OK, first test that we pick up on just the hash and rename the event.
        //the above json has 3 mappings for this hash, but two of them require additional attributes which have not been added
        List<MPEvent> eventList = provider.projectEvents(builder.build());
        assertEquals(1, eventList.size());
        MPEvent projEvent1 = eventList.get(0);
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getInfo().get("attribute we don't care about"));

        //Now more complicated, add an attribute that's required by 1 of them
        attributes.put("Value", "product name");
        eventList = provider.projectEvents(builder.build());
        assertEquals(2, eventList.size());
        projEvent1 = eventList.get(0);
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getInfo().get("attribute we don't care about"));

        projEvent1 = eventList.get(1);
        assertEquals("pdp - add to tote", projEvent1.getEventName());
        //required attribute
        assertEquals("product name",  projEvent1.getInfo().get("Last Add to Tote Category"));
        //non-required attributes
        assertEquals("product name",  projEvent1.getInfo().get("Last Add to Tote Total Amount"));
        assertEquals("product name", projEvent1.getInfo().get("Last Add to Tote SKU"));
        //static attributes
        assertEquals("10",  projEvent1.getInfo().get("Last Add to Tote Quantity"));
        assertEquals("1321", projEvent1.getInfo().get("Last Add to Tote Unit Price"));

        //now test with all 3 projections. same for 1&2 exception 2 now should have some new attributes (product label)
        attributes.put("Label", "product label");
        eventList = provider.projectEvents(builder.build());
        assertEquals(3, eventList.size());

        projEvent1 = eventList.get(0);
        assertEquals("account - check order status", projEvent1.getEventName());
        assertEquals("some value", projEvent1.getInfo().get("attribute we don't care about"));

        projEvent1 = eventList.get(1);
        assertEquals("pdp - add to tote", projEvent1.getEventName());

        assertEquals("product name",  projEvent1.getInfo().get("Last Add to Tote Category"));

        assertEquals("product name",  projEvent1.getInfo().get("Last Add to Tote Total Amount"));
        assertEquals("product name",  projEvent1.getInfo().get("Last Add to Tote SKU"));

        //now we should have these
        assertEquals("product label",  projEvent1.getInfo().get("Last Add to Tote Name"));
        assertEquals("product label",  projEvent1.getInfo().get("Last Add to Tote Print"));

        assertEquals("10",  projEvent1.getInfo().get("Last Add to Tote Quantity"));
        assertEquals("1321",  projEvent1.getInfo().get("Last Add to Tote Unit Price"));
        projEvent1 = eventList.get(2);
        assertEquals("pdp - complete the look", projEvent1.getEventName());
        assertEquals("product name",  projEvent1.getInfo().get("Complete the Look Product Name"));
        assertEquals("product label",  projEvent1.getInfo().get("Complete the Look Product Name 2"));

    }

}