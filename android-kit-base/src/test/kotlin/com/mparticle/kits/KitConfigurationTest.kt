package com.mparticle.kits

import android.util.SparseBooleanArray
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.consent.CCPAConsent
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.Constants
import com.mparticle.mock.MockKitConfiguration
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.lang.reflect.Method

class KitConfigurationTest {
    private val json =
        "{\"dt\":\"ac\", \"id\":\"fddf1f96-560e-41f6-8f9b-ddd070be0765\", \"ct\":1434392412994, \"dbg\":false, \"cue\":\"appdefined\", \"pmk\":[\"mp_message\", \"com.urbanairship.push.ALERT\", \"alert\", \"a\", \"message\"], \"cnp\":\"appdefined\", \"soc\":0, \"oo\":false, \"eks\":[{\"id\":28, \"avf\":{\"i\":true, \"a\":3288498, \"v\":3611952 }, \"eau\": true, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"ec\":{\"1824528345\":0, \"1824528346\":0, \"1824528347\":0, \"1824528348\":0, \"1824528341\":0, \"1824528342\":0, \"1824528343\":0, \"1824528350\":0, \"54237\":0, \"-201964253\":0, \"-1015351211\":0, \"-1698163721\":0, \"642507618\":0, \"-1750207685\":0, \"-1400806385\":0, \"-435072817\":0, \"612161276\":0, \"-2049994443\":0, \"1798380893\":0, \"-460386492\":0, \"476338248\":0, \"-1964837950\":0, \"-115592573\":0, \"-1119044065\":0, \"-1229406110\":0, \"1612904218\":0, \"-459588721\":0, \"93769843\":0, \"-1831156729\":0, \"925748342\":0, \"-1471879983\":0, \"-1471879982\":0, \"-1471879985\":0, \"-1471879984\":0, \"-1471879987\":0, \"-1471879986\":0, \"-1471879989\":0, \"-1471879988\":0, \"-1471879991\":0, \"-1471879990\":0, \"-1175432756\":0, \"-1439088726\":0, \"-630837254\":0, \"-1528980234\":0, \"866346720\":0, \"-466914683\":0, \"584870613\":0, \"-71005245\":0, \"-71005246\":0, \"-71005248\":0, \"-71005251\":0, \"-71005254\":0, \"-192614420\":0, \"-1899939497\":0, \"-138049017\":0, \"1755914106\":0, \"1713887651\":0, \"-1680991381\":0, \"1381973565\":0, \"1696869197\":0, \"530926139\":0, \"-1591103548\":0, \"606683084\":0, \"-452884081\":0, \"1156084566\":0, \"-1684704584\":0, \"-1684704582\":0, \"-1684704517\":0, \"-1684704551\":0, \"-1684704492\":0, \"-1684704484\":0, \"-1507615010\":0, \"1496713379\":0, \"1496713380\":0, \"1496713373\":0, \"1496713374\":0, \"1496713371\":0, \"1496713372\":0, \"1496713377\":0, \"1496713375\":0, \"1496713376\":0, \"448941660\":0, \"455787894\":0, \"1057880655\":0, \"-153747136\":0, \"228100699\":0, \"1956870096\":0, \"367619406\":0, \"-1728365802\":0, \"1315260226\":0, \"713953332\":0, \"54115406\":0, \"-1075988785\":0, \"-1726724035\":0, \"1195528703\":0, \"-1415615126\":0, \"-1027713269\":0, \"-181380149\":0, \"-115531678\":0, \"-100487028\":0, \"-1233979378\":0, \"843036051\":0, \"912926294\":0, \"56084205\":0, \"1594525888\":0, \"-1573616412\":0, \"-1417002190\":0, \"1794482897\":0, \"224683764\":0, \"-1471969403\":0, \"596888957\":0, \"596888956\":0, \"596888953\":0, \"596888952\":0, \"596888955\":0, \"596888954\":0, \"596888949\":0, \"596888948\":0, \"596888950\":0, \"972118770\":0, \"-1097220876\":0, \"-1097220881\":0, \"-1097220880\":0, \"-1097220879\":0, \"-1097220878\":0, \"-1097220885\":0, \"-1097220884\":0, \"-1097220883\":0, \"-1097220882\":0, \"-582505992\":0, \"-814117771\":0, \"1414371548\":0, \"682253748\":0, \"682253740\":0, \"682253745\":0, \"682253744\":0, \"682253747\":0, \"1659263444\":0, \"-136616030\":0, \"1888580672\":0, \"1888580669\":0, \"1888580668\":0, \"1888580666\":0, \"1888580663\":0, \"1888580664\":0, \"1230284208\":0, \"1684003336\":0, \"-726561745\":0, \"-1449123489\":0, \"1961938929\":0, \"1961938921\":0, \"1961938920\":0, \"1961938923\":0, \"1961938922\":0, \"1961938925\":0, \"1961938924\":0, \"1961938927\":0, \"1961938926\":0, \"1790423703\":0, \"1359366927\":0, \"1025548221\":0, \"507221049\":0, \"1515120746\":0, \"-956692642\":0, \"-1011688057\":0, \"371448668\":0, \"1101201489\":0, \"-1535298586\":0, \"56181691\":0, \"-709351854\":0, \"-1571155573\":0, \"1833524190\":0, \"1658269412\":0, \"-2138078264\":0, \"1706381873\":0, \"1795771134\":0, \"-610294159\":0 }, \"svea\":{\"-604737418\":0, \"-1350758925\":0, \"699878711\":0, \"-409251596\":0, \"1646521091\":0, \"1891689827\":0 }, \"ua\":{\"341203229\":0, \"96511\":0, \"3373707\":0, \"1193085\":0, \"635848677\":0, \"-564885382\":0, \"1168987\":0, \"102865796\":0, \"3552215\":0, \"3648196\":0, \"-892481550\":0, \"405645589\":0, \"405645588\":0, \"405645591\":0, \"405645590\":0, \"405645592\":0, \"3492908\":0 }, \"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }, {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"hs\":{\"et\":{\"48\":0, \"57\":0 }, \"ec\":{\"1824528345\":0, \"1824528346\":0, \"1824528347\":0, \"1824528348\":0, \"1824528341\":0, \"1824528342\":0, \"1824528343\":0, \"1824528350\":0, \"54237\":0, \"-201964253\":0, \"-1015351211\":0, \"-1698163721\":0, \"642507618\":0, \"-1750207685\":0, \"-1400806385\":0, \"-435072817\":0, \"612161276\":0, \"-2049994443\":0, \"1798380893\":0, \"-460386492\":0, \"476338248\":0, \"-1964837950\":0, \"-115592573\":0, \"-1119044065\":0, \"-1229406110\":0, \"1612904218\":0, \"-459588721\":0, \"93769843\":0, \"-1831156729\":0, \"925748342\":0, \"-1471879983\":0, \"-1471879982\":0, \"-1471879985\":0, \"-1471879984\":0, \"-1471879987\":0, \"-1471879986\":0, \"-1471879989\":0, \"-1471879988\":0, \"-1471879991\":0, \"-1471879990\":0, \"-1175432756\":0, \"-1439088726\":0, \"-630837254\":0, \"-1528980234\":0, \"866346720\":0, \"-466914683\":0, \"584870613\":0, \"-71005245\":0, \"-71005246\":0, \"-71005248\":0, \"-71005251\":0, \"-71005254\":0, \"-192614420\":0, \"-1899939497\":0, \"-138049017\":0, \"1755914106\":0, \"1713887651\":0, \"-1680991381\":0, \"1381973565\":0, \"1696869197\":0, \"530926139\":0, \"-1591103548\":0, \"606683084\":0, \"-452884081\":0, \"-1684704584\":0, \"-1684704582\":0, \"-1684704517\":0, \"-1684704551\":0, \"-1684704492\":0, \"-1684704484\":0, \"-1507615010\":0, \"448941660\":0, \"455787894\":0, \"1057880655\":0, \"-153747136\":0, \"228100699\":0, \"1956870096\":0, \"367619406\":0, \"-1728365802\":0, \"1315260226\":0, \"713953332\":0, \"54115406\":0, \"-1075988785\":0, \"-1726724035\":0, \"1195528703\":0, \"-1415615126\":0, \"-1027713269\":0, \"-181380149\":0, \"-115531678\":0, \"-100487028\":0, \"-1233979378\":0, \"843036051\":0, \"912926294\":0, \"1594525888\":0, \"-1573616412\":0, \"-1417002190\":0, \"1794482897\":0, \"224683764\":0, \"-1471969403\":0, \"596888957\":0, \"596888956\":0, \"596888953\":0, \"596888952\":0, \"596888955\":0, \"596888954\":0, \"596888949\":0, \"596888948\":0, \"596888950\":0, \"972118770\":0, \"-1097220876\":0, \"-1097220881\":0, \"-1097220880\":0, \"-1097220879\":0, \"-1097220878\":0, \"-1097220885\":0, \"-1097220884\":0, \"-1097220883\":0, \"-1097220882\":0, \"-582505992\":0, \"-814117771\":0, \"1414371548\":0, \"682253748\":0, \"682253740\":0, \"682253745\":0, \"682253744\":0, \"682253747\":0, \"1659263444\":0, \"-136616030\":0, \"1888580672\":0, \"1888580669\":0, \"1888580668\":0, \"1888580666\":0, \"1888580663\":0, \"1888580664\":0, \"1230284208\":0, \"1684003336\":0, \"-726561745\":0, \"-1449123489\":0, \"1961938929\":0, \"1961938921\":0, \"1961938920\":0, \"1961938923\":0, \"1961938922\":0, \"1961938925\":0, \"1961938924\":0, \"1961938927\":0, \"1961938926\":0, \"1790423703\":0, \"1359366927\":0, \"1025548221\":0, \"507221049\":0, \"1515120746\":0, \"-956692642\":0, \"-1011688057\":0, \"371448668\":0, \"1101201489\":0, \"-1535298586\":0, \"-709351854\":0, \"-1571155573\":0, \"1833524190\":0, \"1658269412\":0, \"-2138078264\":0, \"1706381873\":0, \"1795771134\":0, \"-610294159\":0 }, \"svec\":{\"-385188961\":0, \"303102897\":0, \"303102895\":0, \"303102890\":0, \"303102891\":0, \"303102899\":0, \"1688308747\":0, \"-149109002\":0, \"-1254039557\":0, \"847138800\":0, \"847138801\":0, \"847138799\":0, \"-204085080\":0, \"1658373353\":0, \"-1493744191\":0, \"1861873109\":0, \"-732268618\":0 }, \"ua\":{\"341203229\":0, \"96511\":0, \"3373707\":0, \"1193085\":0, \"635848677\":0, \"-564885382\":0, \"1168987\":0, \"102865796\":0, \"3552215\":0, \"3648196\":0, \"-892481550\":0, \"405645589\":0, \"405645588\":0, \"405645591\":0, \"405645590\":0, \"405645592\":0, \"3492908\":0 } }, \"pr\":[{\"id\":93, \"pmmid\":23, \"matches\":[{\"message_type\":4, \"event_match_type\":\"String\", \"event\":\"Product View\", \"attribute_key\":\"\$MethodName\", \"attribute_value\":\"\$ProductView\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - product view\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Product View Category\", \"match_type\":\"String\", \"value\":\"ProductCategory\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Quantity\", \"match_type\":\"String\", \"value\":\"ProductQuantity\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Total Amount\", \"match_type\":\"String\", \"value\":\"RevenueAmount\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View SKU\", \"match_type\":\"String\", \"value\":\"ProductSKU\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Currency\", \"match_type\":\"String\", \"value\":\"CurrencyCode\", \"data_type\":\"String\", \"is_required\":true }, {\"projected_attribute_name\":\"Last Product View Name\", \"match_type\":\"String\", \"value\":\"ProductName\", \"data_type\":\"String\", \"is_required\":true } ] } }, {\"id\":89, \"matches\":[{\"message_type\":4, \"event_match_type\":\"\", \"event\":\"\"}], \"behavior\":{\"append_unmapped_as_is\":true, \"is_default\":true }, \"action\":{\"projected_event_name\":\"\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":179, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":100, \"pmid\":180, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - check order status\", \"attribute_maps\":[] } }, {\"id\":92, \"pmid\":181, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":92, \"pmid\":182, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"1111995177\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"account - feedback\", \"attribute_maps\":[{\"projected_attribute_name\":\"Feedback Type\", \"match_type\":\"Hash\", \"value\":\"-768380952\", \"data_type\":\"String\"} ] } }, {\"id\":96, \"pmid\":183, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - add to tote\", \"attribute_maps\":[{\"projected_attribute_name\":\"Last Add to Tote Name\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Print\", \"match_type\":\"Hash\", \"value\":\"102582760\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Category\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Total Amount\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote SKU\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Size\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"}, {\"projected_attribute_name\":\"Last Add to Tote Quantity\", \"match_type\":\"Static\", \"value\":\"10\", \"data_type\":\"Int\"}, {\"projected_attribute_name\":\"Last Add to Tote Unit Price\", \"match_type\":\"Static\", \"value\":\"1321\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":184, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"178531468\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"111828069\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":185, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"987878094\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"689388774\", \"data_type\":\"String\"} ] } }, {\"id\":104, \"pmid\":186, \"matches\":[{\"message_type\":4, \"event_match_type\":\"Hash\", \"event\":\"-754932241\"}], \"behavior\":{\"max_custom_params\":0 }, \"action\":{\"projected_event_name\":\"pdp - complete the look\", \"attribute_maps\":[{\"projected_attribute_name\":\"Complete the Look Product Name\", \"match_type\":\"Hash\", \"value\":\"992037090\", \"data_type\":\"String\"} ] } } ] } ], \"cms\":[{\"id\":28, \"pr\":[{\"f\":\"com.appboy.installation\", \"m\":0, \"ps\":[{\"k\":\"installation_id\", \"t\":1, \"n\":\"iid\", \"d\":\"%g%\"} ] }, {\"f\":\"com.appboy.device\", \"m\":0, \"ps\":[{\"k\":\"device_id\", \"t\":1, \"n\":\"di\", \"d\":\"\"} ] }, {\"f\":\"com.appboy.offline.storagemap\", \"m\":0, \"ps\":[{\"k\":\"last_user\", \"t\":1, \"n\":\"lu\", \"d\":\"\"} ] } ] } ], \"lsv\":\"2.3.1\", \"tri\":{\"mm\":[{\"dt\":\"x\", \"eh\":true }, {\"dt\":\"x\", \"eh\":false }, {\"dt\":\"ast\", \"t\":\"app_init\", \"ifr\":true, \"iu\":false } ], \"evts\":[1594525888, -460386492, -1633998520, -201964253, -1698163721, -88346394, -964353845, 925748342, 1515120746, 476338248, -2049994443 ] }, \"pio\":30 }"

    @Test
    @Throws(Exception::class)
    fun testParseConfig() {
        val config = JSONObject(json)
        val modules = config.getJSONArray("eks")
        for (i in 0 until modules.length()) {
            val ekConfig = modules.getJSONObject(i)
            val configuration = MockKitConfiguration.createKitConfiguration(ekConfig)
            if (ekConfig.has("avf")) {
                val attributeValueFilter = ekConfig.getJSONObject("avf")
                if (attributeValueFilter.has("i") && attributeValueFilter.has("a") && attributeValueFilter.has(
                        "v"
                    )
                ) {
                    val shouldIncludeMatches = attributeValueFilter["i"]
                    val hashedAttribute = attributeValueFilter["a"]
                    val hashedValue = attributeValueFilter["v"]
                    if (shouldIncludeMatches is Boolean && hashedAttribute is Int && hashedValue is Int) {
                        Assert.assertTrue(configuration.isAttributeValueFilteringActive)
                        Assert.assertEquals(
                            attributeValueFilter.getBoolean("i"),
                            configuration.isAvfShouldIncludeMatches
                        )
                        Assert.assertEquals(
                            attributeValueFilter.getInt("a").toLong(),
                            configuration.avfHashedAttribute.toLong()
                        )
                        Assert.assertEquals(
                            attributeValueFilter.getInt("v").toLong(),
                            configuration.avfHashedValue.toLong()
                        )
                    } else {
                        Assert.assertFalse(configuration.isAttributeValueFilteringActive)
                    }
                }
            }
            if (ekConfig.has("hs")) {
                val filters = ekConfig.getJSONObject("hs")
                if (filters.has("et")) {
                    Assert.assertEquals(
                        filters.getJSONObject("et").length().toLong(),
                        configuration.eventTypeFilters.size().toLong()
                    )
                }
                if (filters.has("ec")) {
                    Assert.assertEquals(
                        filters.getJSONObject("ec").length().toLong(),
                        configuration.eventNameFilters.size().toLong()
                    )
                }
                if (filters.has("ea")) {
                    Assert.assertEquals(
                        filters.getJSONObject("ea").length().toLong(),
                        configuration.eventAttributeFilters.size().toLong()
                    )
                }
                if (filters.has("svec")) {
                    Assert.assertEquals(
                        filters.getJSONObject("svec").length().toLong(),
                        configuration.screenNameFilters.size().toLong()
                    )
                }
                if (filters.has("svea")) {
                    Assert.assertEquals(
                        filters.getJSONObject("svea").length().toLong(),
                        configuration.screenAttributeFilters.size().toLong()
                    )
                }
                if (filters.has("uid")) {
                    Assert.assertEquals(
                        filters.getJSONObject("uid").length().toLong(),
                        configuration.userIdentityFilters.size().toLong()
                    )
                }
                if (filters.has("ua")) {
                    Assert.assertEquals(
                        filters.getJSONObject("ua").length().toLong(),
                        configuration.userAttributeFilters.size().toLong()
                    )
                }
                if (filters.has("cea")) {
                    Assert.assertEquals(
                        filters.getJSONObject("cea").length().toLong(),
                        configuration.commerceAttributeFilters.size().toLong()
                    )
                }
                if (filters.has("ent")) {
                    Assert.assertEquals(
                        filters.getJSONObject("ent").length().toLong(),
                        configuration.commerceEntityFilters.size().toLong()
                    )
                }
                if (filters.has("afa")) {
                    val entityAttFilters = filters.getJSONObject("afa")
                    Assert.assertEquals(
                        entityAttFilters.length().toLong(),
                        configuration.commerceEntityAttributeFilters.size.toLong()
                    )
                    val keys: MutableIterator<Any?> = entityAttFilters.keys()
                    while (keys.hasNext()) {
                        val key: String = keys.next() as String
                        Assert.assertEquals(
                            entityAttFilters.getJSONObject(key).length().toLong(),
                            configuration.commerceEntityAttributeFilters[key.toInt()]
                                ?.size()?.toLong()
                        )
                    }
                }
            }
            if (ekConfig.has("bk")) {
                val bracketing = ekConfig.getJSONObject("bk")
                Assert.assertEquals(
                    bracketing.optInt("lo").toLong(),
                    configuration.lowBracket.toLong()
                )
                Assert.assertEquals(
                    bracketing.optInt("hi").toLong(),
                    configuration.highBracket.toLong()
                )
            } else {
                Assert.assertEquals(0, configuration.lowBracket.toLong())
                Assert.assertEquals(101, configuration.highBracket.toLong())
            }
            if (ekConfig.has("pr")) {
                val projections = ekConfig.getJSONArray("pr")
                if (projections.length() > 0) {
                    Assert.assertNotNull(configuration.defaultEventProjection)
                    Assert.assertEquals(
                        projections.length().toLong(),
                        (configuration.customMappingList.size + 1).toLong()
                    )
                }
            }
            if (ekConfig.getLong("id") == 28L) {
                Assert.assertTrue(configuration.excludeAnonymousUsers())
            } else {
                Assert.assertFalse(configuration.excludeAnonymousUsers())
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFilterCommerceAttributes() {
        // CUSTOM ATTRIBUTES
        val configuration =
            MockKitConfiguration.createKitConfiguration(JSONObject(COMMERCE_FILTERS))
        val attributes: MutableMap<String, String> = HashMap()
        attributes["my custom attribute"] = "whatever"
        var event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("name", "sku", 2.0).build())
                .customAttributes(attributes).build()
        Assert.assertEquals("whatever", event.customAttributeStrings?.get("my custom attribute"))
        val filteredEvent = configuration.filterCommerceEvent(event)
        Assert.assertNull(filteredEvent.customAttributeStrings?.get("my custom attribute"))

        // make sure we're only doing it for ADD_TO_CART
        var event2 =
            CommerceEvent.Builder(Product.PURCHASE, Product.Builder("name", "sku", 2.0).build())
                .customAttributes(attributes)
                .transactionAttributes(TransactionAttributes().setId("some id")).build()
        Assert.assertEquals(
            "whatever",
            event2.customAttributeStrings?.get("my custom attribute")
        )
        var filteredEvent2 = configuration.filterCommerceEvent(event2)
        Assert.assertEquals(
            "whatever",
            filteredEvent2.customAttributeStrings?.get("my custom attribute")
        )
        event = CommerceEvent.Builder(Product.CHECKOUT, Product.Builder("name", "sku", 2.0).build())
            .checkoutOptions("cool options").build()
        Assert.assertEquals("cool options", event.checkoutOptions)
        filteredEvent2 = configuration.filterCommerceEvent(event)
        Assert.assertNull(filteredEvent2.checkoutOptions)
        event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("name", "sku", 2.0).build())
                .checkoutOptions("cool options").build()
        Assert.assertEquals("cool options", event.checkoutOptions)
        filteredEvent2 = configuration.filterCommerceEvent(event)
        Assert.assertEquals("cool options", filteredEvent2.checkoutOptions)
        event2 =
            CommerceEvent.Builder(Product.PURCHASE, Product.Builder("name", "sku", 2.0).build())
                .customAttributes(attributes).transactionAttributes(
                    TransactionAttributes().setId("some id").setAffiliation("cool affiliation")
                ).build()
        Assert.assertEquals("cool affiliation", event2.transactionAttributes?.affiliation)
        filteredEvent2 = configuration.filterCommerceEvent(event2)
        Assert.assertNull(filteredEvent2.transactionAttributes?.affiliation)
    }

    @Test
    @Throws(Exception::class)
    fun testFilterCommerceEntity() {
        var configuration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                COMMERCE_FILTERS
            )
        )
        val impression = Impression("Cool list name", Product.Builder("name2", "sku", 2.0).build())
        var event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("name", "sku", 2.0).build())
                .addImpression(impression).build()
        var impressionList = event.impressions
        if (impressionList != null) {
            for (imp in impressionList) {
                Assert.assertEquals(1, imp.products.size.toLong())
            }
        }
        event = configuration.filterCommerceEvent(event)
        Assert.assertNull(event.products)
        var impressionList2 = event.impressions
        if (impressionList2 != null) {
            Assert.assertTrue(impressionList2.size > 0)
            for (imp in impressionList2) {
                Assert.assertEquals(0, imp.products.size.toLong())
            }
        }
        val config = JSONObject(COMMERCE_FILTERS)
        // enable product, disable impressions
        config.getJSONObject("hs").getJSONObject("ent").put("2", 0)
        config.getJSONObject("hs").getJSONObject("ent").put("1", 1)
        configuration = MockKitConfiguration.createKitConfiguration(config)
        event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("name", "sku", 2.0).build())
                .addImpression(impression).build()
        impressionList = event.impressions
        if (impressionList != null) {
            for (imp in impressionList) {
                Assert.assertEquals(1, imp.products.size.toLong())
            }
        }
        event.products?.size?.let { Assert.assertEquals(1, it.toLong()) }
        event = configuration.filterCommerceEvent(event)
        event.products?.size?.let { Assert.assertEquals(1, it.toLong()) }
        impressionList2 = event.impressions
        if (impressionList2 != null) {
            Assert.assertTrue(impressionList2.size > 0)
            for (imp in impressionList2) {
                Assert.assertEquals(1, imp.products.size.toLong())
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFilterCommerceEntityAttribute() {
        var event = CommerceEvent.Builder(
            Promotion.VIEW,
            Promotion().setId("promo id").setCreative("the creative").setName("promotion name")
        ).build()
        var configuration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                COMMERCE_FILTERS
            )
        )
        Assert.assertEquals("the creative", event.promotions?.get(0)?.creative)
        var filteredEvent = configuration.filterCommerceEvent(event)
        Assert.assertNull(filteredEvent.promotions?.get(0)?.creative)
        Assert.assertEquals("promotion name", filteredEvent.promotions?.get(0)?.name)
        val attributes: MutableMap<String, String> = HashMap()
        attributes["my custom product attribute"] = "whatever"
        event = CommerceEvent.Builder(
            Product.CHECKOUT,
            Product.Builder("name", "sku", 5.0)
                .customAttributes(attributes)
                .brand("cool brand").build()
        )
            .build()
        Assert.assertEquals(
            "whatever",
            event.products?.get(0)?.customAttributes?.get("my custom product attribute")
        )
        Assert.assertEquals("cool brand", event.products?.get(0)?.brand)
        configuration = MockKitConfiguration.createKitConfiguration(JSONObject(COMMERCE_FILTERS_2))
        filteredEvent = configuration.filterCommerceEvent(event)
        Assert.assertNull(filteredEvent.products?.get(0)?.customAttributes?.get("my custom product attribute"))
        Assert.assertNull(filteredEvent.products?.get(0)?.brand)
    }

    @Test
    @Throws(Exception::class)
    fun testFilterCommerceEventType() {
        // CUSTOM ATTRIBUTES
        val configuration =
            MockKitConfiguration.createKitConfiguration(JSONObject(COMMERCE_FILTERS))
        val event =
            CommerceEvent.Builder(Product.ADD_TO_CART, Product.Builder("name", "sku", 2.0).build())
                .build()
        Assert.assertNotNull(configuration.filterCommerceEvent(event))
        val event2 = CommerceEvent.Builder(
            Product.REMOVE_FROM_CART,
            Product.Builder("name", "sku", 2.0).build()
        ).build()
        Assert.assertNull(configuration.filterCommerceEvent(event2))
    }

    @Test
    @Throws(Exception::class)
    fun testAttributeValueFiltering() {
        var matchingAttributes: MutableMap<String?, String?>
        var halfMatchingAttributes: MutableMap<String?, String?>
        var nonMatchingAttributes: MutableMap<String?, String?>
        run {
            matchingAttributes = HashMap()
            halfMatchingAttributes = HashMap()
            nonMatchingAttributes = HashMap()
            matchingAttributes["key1"] = "val1"
            halfMatchingAttributes["key1"] = "valWrong"
            nonMatchingAttributes.put("keyWrong", "valWrong")
        }
        val includeTrueConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                ATTRIBUTE_VALUE_FILTERING_INCLUDE_TRUE
            )
        )
        val includeFalseConfiguration = MockKitConfiguration.createKitConfiguration(
            JSONObject(
                ATTRIBUTE_VALUE_FILTERING_INCLUDE_FALSE
            )
        )
        var result: Boolean =
            includeTrueConfiguration.shouldIncludeFromAttributeValueFiltering(matchingAttributes)
        Assert.assertTrue(result)
        result =
            includeTrueConfiguration.shouldIncludeFromAttributeValueFiltering(halfMatchingAttributes)
        Assert.assertFalse(result)
        result =
            includeTrueConfiguration.shouldIncludeFromAttributeValueFiltering(nonMatchingAttributes)
        Assert.assertFalse(result)
        result =
            includeFalseConfiguration.shouldIncludeFromAttributeValueFiltering(matchingAttributes)
        Assert.assertFalse(result)
        result = includeFalseConfiguration.shouldIncludeFromAttributeValueFiltering(
            halfMatchingAttributes
        )
        Assert.assertTrue(result)
        result =
            includeFalseConfiguration.shouldIncludeFromAttributeValueFiltering(nonMatchingAttributes)
        Assert.assertTrue(result)
    }

    @Test
    @Throws(Exception::class)
    fun testUserAttributeFiltering() {
        val configuration =
            MockKitConfiguration.createKitConfiguration(JSONObject(" {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"hs\":{\"ua\":{\"-430909305\":0, \"1510554026\":1, \"341203229\":0, \"96511\":0, \"3373707\":0, \"1193085\":0, \"635848677\":0, \"-564885382\":0, \"1168987\":0, \"102865796\":0, \"3552215\":0, \"3648196\":0, \"-892481550\":0, \"405645589\":0, \"405645588\":0, \"405645591\":0, \"405645590\":0, \"405645592\":0, \"3492908\":0 } }, }"))
        Assert.assertFalse(
            KitConfiguration.shouldForwardAttribute(
                configuration.userAttributeFilters,
                "test key in config as false"
            )
        )
        Assert.assertTrue(
            KitConfiguration.shouldForwardAttribute(
                configuration.userAttributeFilters,
                "test key in config as true"
            )
        )
        Assert.assertTrue(
            KitConfiguration.shouldForwardAttribute(
                configuration.userAttributeFilters,
                "test key not in config"
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testScreenAttributeFiltering() {
        val configuration =
            MockKitConfiguration.createKitConfiguration(JSONObject(" {\"id\":56, \"as\":{\"secretKey\":\"testappkey\", \"eventList\":\"[\\\"test1\\\",\\\"test2\\\",\\\"test3\\\"]\", \"sendTransactionData\":\"True\", \"eventAttributeList\":null }, \"hs\":{\"svea\":{\"1689004688\":0, \"1198002448\":1 } }, }"))
        val testAttributes = mapOf(
            "screen attribute should not forward" to "testVal",
            "screen attribute should forward" to "testVal",
            "screen attribute should also forward" to "testVal"
        )
        val filterdScreenAttributes = KitConfiguration.filterEventAttributes(
            null,
            "testScreenView",
            configuration.screenAttributeFilters,
            testAttributes
        )
        Assert.assertFalse(
            filterdScreenAttributes.containsKey("screen attribute should not forward")
        )
        Assert.assertTrue(
            filterdScreenAttributes.containsKey("screen attribute should forward")
        )
        Assert.assertTrue(
            filterdScreenAttributes.containsKey("screen attribute should also forward")
        )
    }

    @Test
    @Throws(Exception::class)
    fun testConsentForwardingRules() {
        var configuration = MockKitConfiguration.createKitConfiguration(
            JSONObject("{\"id\":56,\"crvf\":{\"i\": true, \"v\":[{\"c\":true, \"h\":123}, {\"c\":false, \"h\":456}]}}")
        )
        Assert.assertEquals(2, configuration.mConsentForwardingRules.size.toLong())
        Assert.assertEquals(true, configuration.consentForwardingIncludeMatches)
        Assert.assertEquals(true, configuration.mConsentForwardingRules[123])
        Assert.assertEquals(false, configuration.mConsentForwardingRules[456])
        configuration = configuration.parseConfiguration(
            JSONObject("{\"id\":56,\"crvf\":{\"i\": false, \"v\":[{\"c\":false, \"h\":123}, {\"c\":true, \"h\":456}]}}")
        )
        Assert.assertEquals(2, configuration.mConsentForwardingRules.size.toLong())
        Assert.assertEquals(false, configuration.consentForwardingIncludeMatches)
        Assert.assertEquals(false, configuration.mConsentForwardingRules[123])
        Assert.assertEquals(true, configuration.mConsentForwardingRules[456])
        configuration.parseConfiguration(JSONObject("{\"id\":\"1\"}"))
        Assert.assertEquals(0, configuration.mConsentForwardingRules.size.toLong())
    }

    @Test
    @Throws(Exception::class)
    fun testConsentStateShouldFilterFromForwardingRulesEmptyConsent() {
        val jsonConfiguration = JSONObject("{\"id\":56}")
        val consentForwardingRule = JSONObject()
        jsonConfiguration.put("crvf", consentForwardingRule)
        consentForwardingRule.put("i", false)
        val ruleArray = JSONArray()
        consentForwardingRule.put("v", ruleArray)
        val rule1 = JSONObject()
        ruleArray.put(rule1)
        rule1.put("h", KitUtils.hashForFiltering("1" + "foo purpose"))
        rule1.put("c", false)
        val configuration = MockKitConfiguration.createKitConfiguration(
            JSONObject("{\"id\":56,\"crvf\":{\"i\": true, \"v\":[{\"c\":true, \"h\":123}, {\"c\":false, \"h\":456}]}}")
        )
        Assert.assertFalse(configuration.isConsentStateFilterMatch(ConsentState.builder().build()))
    }

    @Test
    @Throws(Exception::class)
    fun testConsentStateShouldFilterFromMultipleForwardingRulesAndStates() {
        val jsonConfiguration = JSONObject("{\"id\":56}")
        val consentForwardingRule = JSONObject()
        jsonConfiguration.put("crvf", consentForwardingRule)
        consentForwardingRule.put("i", false)
        val ruleArray = JSONArray()
        consentForwardingRule.put("v", ruleArray)
        val rule1 = JSONObject()
        ruleArray.put(rule1)
        rule1.put("h", KitUtils.hashForFiltering("1" + "foo purpose 1"))
        rule1.put("c", false)
        val rule2 = JSONObject()
        ruleArray.put(rule2)
        rule2.put("h", KitUtils.hashForFiltering("1" + "foo purpose 2"))
        rule2.put("c", false)
        val rule3 = JSONObject()
        ruleArray.put(rule3)
        rule3.put("h", KitUtils.hashForFiltering("1" + "foo purpose 3"))
        rule3.put("c", false)
        val configuration = MockKitConfiguration.createKitConfiguration(jsonConfiguration)
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose 1",
                        GDPRConsent.builder(false).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 2",
                        GDPRConsent.builder(true).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 3",
                        GDPRConsent.builder(true).build()
                    )
                    .build()
            )
        )
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose 1",
                        GDPRConsent.builder(true).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 2",
                        GDPRConsent.builder(false).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 3",
                        GDPRConsent.builder(true).build()
                    )
                    .build()
            )
        )
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose 1",
                        GDPRConsent.builder(true).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 2",
                        GDPRConsent.builder(true).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 3",
                        GDPRConsent.builder(false).build()
                    )
                    .setCCPAConsentState(
                        CCPAConsent.builder(false).build()
                    )
                    .build()
            )
        )
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose 1",
                        GDPRConsent.builder(false).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 2",
                        GDPRConsent.builder(false).build()
                    )
                    .addGDPRConsentState(
                        "foo purpose 3",
                        GDPRConsent.builder(false).build()
                    )
                    .setCCPAConsentState(
                        CCPAConsent.builder(true).build()
                    )
                    .build()
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testConsentStateShouldFilterFromForwardingRules() {
        val jsonConfiguration = JSONObject("{\"id\":56}")
        val consentForwardingRule = JSONObject()
        jsonConfiguration.put("crvf", consentForwardingRule)
        consentForwardingRule.put("i", false)
        val ruleArray = JSONArray()
        consentForwardingRule.put("v", ruleArray)
        val rule1 = JSONObject()
        ruleArray.put(rule1)
        rule1.put("h", KitUtils.hashForFiltering("1" + "foo purpose"))
        rule1.put("c", false)
        val configuration = MockKitConfiguration.createKitConfiguration(jsonConfiguration)
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose",
                        GDPRConsent.builder(false).build()
                    )
                    .build()
            )
        )
        Assert.assertFalse(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose",
                        GDPRConsent.builder(true).build()
                    )
                    .build()
            )
        )
        ruleArray.getJSONObject(0).put("c", true)
        configuration.parseConfiguration(jsonConfiguration)
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose",
                        GDPRConsent.builder(true).build()
                    )
                    .build()
            )
        )
        Assert.assertFalse(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .addGDPRConsentState(
                        "foo purpose",
                        GDPRConsent.builder(false).build()
                    )
                    .build()
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCCPAConsentStateShouldFilterFromForwardingRules() {
        val jsonConfiguration = JSONObject("{\"id\":56}")
        val consentForwardingRule = JSONObject()
        jsonConfiguration.put("crvf", consentForwardingRule)
        consentForwardingRule.put("i", false)
        val ruleArray = JSONArray()
        consentForwardingRule.put("v", ruleArray)
        val rule1 = JSONObject()
        ruleArray.put(rule1)
        rule1.put("h", KitUtils.hashForFiltering("2" + Constants.MessageKey.CCPA_CONSENT_KEY))
        rule1.put("c", false)
        val configuration = MockKitConfiguration.createKitConfiguration(jsonConfiguration)
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .setCCPAConsentState(
                        CCPAConsent.builder(false)
                            .build()
                    )
                    .build()
            )
        )
        Assert.assertFalse(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .setCCPAConsentState(
                        CCPAConsent.builder(true)
                            .build()
                    )
                    .build()
            )
        )
        ruleArray.getJSONObject(0).put("c", true)
        configuration.parseConfiguration(jsonConfiguration)
        Assert.assertTrue(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .setCCPAConsentState(
                        CCPAConsent.builder(true)
                            .build()
                    )
                    .build()
            )
        )
        Assert.assertFalse(
            configuration.isConsentStateFilterMatch(
                ConsentState.builder()
                    .setCCPAConsentState(
                        CCPAConsent.builder(false)
                            .build()
                    )
                    .build()
            )
        )
    }

    @Test
    fun testConvertToSparseArray() {
        val kitConfiguration = MockKitConfiguration()
        val jsonData = """   
        {
           "7456529": 0,
          "10887805": 0,
          "13992010": 0,
          "15360852": 0,
          "17455322": 0,
          "18683141": 0,
          "23029959": 0,
          "41851400": 0,
          "47355425": 0,
          "54925556": 0,
          "56409892": 0,
          "66701264": 0
         }
        """.trimIndent()
        val jsonConfiguration = JSONObject(jsonData)
        val method: Method = MockKitConfiguration::class.java.getDeclaredMethod("convertToSparseArray", JSONObject::class.java)
        method.isAccessible = true
        val result = method.invoke(kitConfiguration, jsonConfiguration) as SparseBooleanArray
        Assert.assertEquals(12, result.size())
    }

    @Test
    fun testConvertToSparseArray_When_JSON_OBJECT_IS_NULL() {
        val kitConfiguration = MockKitConfiguration()
        val jsonData = """   
        {
          "ec": {
         }
         }
        """
        val method: Method = MockKitConfiguration::class.java.getDeclaredMethod("convertToSparseArray", JSONObject::class.java)
        method.isAccessible = true
        val jsonObject = JSONObject(jsonData)
        val ecData = jsonObject.get("ec") as JSONObject
        val result = method.invoke(kitConfiguration, ecData) as SparseBooleanArray
        Assert.assertEquals(0, result.size())
    }

    @Test
    fun testConvertToSparseArray_When_JSON_IS_NULL() {
        val kitConfiguration = MockKitConfiguration()
        val method: Method = MockKitConfiguration::class.java.getDeclaredMethod("convertToSparseArray", JSONObject::class.java)
        method.isAccessible = true
        val result = method.invoke(kitConfiguration, null) as SparseBooleanArray
        Assert.assertEquals(0, result.size())
    }

    @Test
    fun testConvertToSparseArray_When_JSON_Data_IS_INVALID() {
        val kitConfiguration = MockKitConfiguration()
        val jsn = """   
        {
           "7456529": 0,
          "10887805": 0,
          "-36!037962": 0,
          "15360852": 0,
          "17455322": 0,
          "18683141": 0,
          "23029959": 0,
          "41851400": 0,
          "47355425": 0,
          "54925556": 0,
          "56409892": 0,
          "66701264": 0
         }
        """.trimIndent()
        val jsonConfiguration = JSONObject(jsn)
        val method: Method = MockKitConfiguration::class.java.getDeclaredMethod("convertToSparseArray", JSONObject::class.java)
        method.isAccessible = true

        val result = method.invoke(kitConfiguration, jsonConfiguration) as SparseBooleanArray
        Assert.assertEquals(11, result.size())
    }

    @Test
    fun testConvertToSparseArray_When_JSON_OBJECT_IS_INVALID() {
        val kitConfiguration = MockKitConfiguration()
        val jsn = """   
        {
            "name": "John",
             "age": "30",
             "18683141": 0
        }
        """.trimIndent()
        val jsonConfiguration = JSONObject(jsn)
        val method: Method = MockKitConfiguration::class.java.getDeclaredMethod(
            "convertToSparseArray",
            JSONObject::class.java
        )
        method.isAccessible = true

        val result = method.invoke(kitConfiguration, jsonConfiguration) as SparseBooleanArray
        Assert.assertEquals(1, result.size())
    }

    @Test
    fun testExcludeUser() {
        val kitConfiguration = KitConfiguration()
        val mockUser = Mockito.mock(MParticleUser::class.java)
        kitConfiguration.mExcludeAnonymousUsers = true
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(false)
        Assert.assertFalse(mockUser.isLoggedIn)
        Assert.assertTrue(kitConfiguration.shouldExcludeUser(mockUser))
        kitConfiguration.mExcludeAnonymousUsers = false
        Assert.assertFalse(kitConfiguration.shouldExcludeUser(mockUser))
        Mockito.`when`(mockUser.isLoggedIn).thenReturn(true)
        Assert.assertFalse(kitConfiguration.shouldExcludeUser(mockUser))
        kitConfiguration.mExcludeAnonymousUsers = true
        Assert.assertFalse(kitConfiguration.shouldExcludeUser(mockUser))
    }

    /**
     * This tests parsing of the attributeAddToUser ("eea"), attributeRemoveFromUser("ear") and attributeSingleItemUser("eas")
     */
    @Test
    @Throws(JSONException::class)
    fun testAttributeToUser() {
        var kitConfig = JSONObject()
            .put("id", 10)
            .put(
                "hs",
                JSONObject()
                    .put(
                        "eaa",
                        JSONObject()
                            .put("-1107245616", "add-item")
                    )
                    .put(
                        "ear",
                        JSONObject()
                            .put("238178128", "remove-item")
                    )
                    .put(
                        "eas",
                        JSONObject()
                            .put("1784066270", "single-item")
                    )
            )
        var kitConfiguration = MockKitConfiguration.createKitConfiguration(kitConfig)
        val addMap = kitConfiguration.eventAttributesAddToUser
        Assert.assertEquals(1, addMap.size.toLong())
        Assert.assertTrue(addMap.containsKey(-1107245616))
        Assert.assertEquals("add-item", addMap[-1107245616])
        val removeMap = kitConfiguration.eventAttributesRemoveFromUser
        Assert.assertEquals(1, removeMap.size.toLong())
        Assert.assertTrue(removeMap.containsKey(238178128))
        Assert.assertEquals("remove-item", removeMap[238178128])
        val singleMap = kitConfiguration.eventAttributesSingleItemUser
        Assert.assertEquals(1, singleMap.size.toLong())
        Assert.assertTrue(singleMap.containsKey(1784066270))
        Assert.assertEquals("single-item", singleMap[1784066270])
        kitConfig = JSONObject()
            .put("id", 10)
            .put("hs", JSONObject())
        kitConfiguration = MockKitConfiguration.createKitConfiguration(kitConfig)
        Assert.assertEquals(0, kitConfiguration.eventAttributesAddToUser.size.toLong())
        Assert.assertEquals(0, kitConfiguration.eventAttributesRemoveFromUser.size.toLong())
        Assert.assertEquals(0, kitConfiguration.eventAttributesSingleItemUser.size.toLong())
    }

    companion object {
        @BeforeClass
        fun setupAll() {
            val mockMp = Mockito.mock(MParticle::class.java)
            Mockito.`when`(mockMp.environment).thenReturn(MParticle.Environment.Development)
            MParticle.setInstance(mockMp)
        }

        var COMMERCE_FILTERS =
            "{\"id\":28, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }"
        var COMMERCE_FILTERS_2 =
            "{\"id\":28, \"as\":{\"apiKey\":\"1fd18e0e-22cd-4b86-a106-551ccc59175f\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"a8f63b1f-1bc2-4373-8947-8dacdd113ad4\", \"addEventAttributeList\":\"[{\\\"map\\\":\\\"Value\\\",\\\"value\\\":null,\\\"maptype\\\":\\\"AttributeSelector\\\"}]\", \"removeEventAttributeList\":\"[]\", \"singleItemEventAttributeList\":\"[]\"}, \"hs\":{\"et\":{\"1568\":0, \"1576\":0 }, \"cea\":{\"-1015386651\":0, \"-1091394645\":0, \"-2090340318\":0 }, \"afa\":{\"1\":{\"93997959\":0, \"-870793808\":0 }, \"2\":{\"1820422063\":0 } } }, \"pr\":[] }"

        // attribute hash 3288498 matches "key1", value hash 3611952 matches "val1"
        var ATTRIBUTE_VALUE_FILTERING_INCLUDE_TRUE =
            "{\"id\":28, \"avf\":{\"i\":true, \"a\":3288498, \"v\":3611952}, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }"
        var ATTRIBUTE_VALUE_FILTERING_INCLUDE_FALSE =
            "{\"id\":28, \"avf\":{\"i\":false, \"a\":3288498, \"v\":3611952}, \"as\":{\"apiKey\":\"2687a8d1-1022-4820-9327-48582e930098\", \"sendPushOpenedWhenAppInForeground\":\"False\", \"push_enabled\":\"True\", \"register_inapp\":\"True\", \"appGroupId\":\"\"}, \"hs\":{\"et\":{\"1568\":0 }, \"cea\":{\"-1015386651\":0, \"-2090340318\":0, \"-1091394645\":0 }, \"ent\":{\"1\":0 }, \"afa\":{\"2\":{\"1820422063\":0 } } }, \"pr\":[] }"
    }
}
