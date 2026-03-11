package com.mparticle.kits

import android.content.Context
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class UrbanAirshipKitTests {
    private val kit: KitIntegration
        get() = UrbanAirshipKit()

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(!name.isNullOrEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings, Mockito.mock(Context::class.java))
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNotNull(e)
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val options = Mockito.mock(MParticleOptions::class.java)
        val factory = KitIntegrationFactory(options)
        val integrations = factory.supportedKits.values
        val className = kit.javaClass.name
        for (integration in integrations) {
            if (integration.name == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    @Test
    @Throws(Exception::class)
    fun testParsing() {
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"this is the app key\", \"applicationSecret\": \"this is the app secret\", \"applicationMasterSecret\": \"mySecret\", \"domain\": \"EU\", \"enableTags\": \"True\", \"includeUserAttributes\": \"False\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"pressed\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"-1394780343\\\",\\\"value\\\":\\\"screen1\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"-2010155734\\\",\\\"value\\\":\\\"cart\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"245922523\\\",\\\"value\\\":\\\"gesture\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"245922523\\\",\\\"value\\\":\\\"a2ctid\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"1112195452\\\",\\\"value\\\":\\\"hello\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-897761755\\\",\\\"value\\\":\\\"a\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-635338283\\\",\\\"value\\\":\\\"b\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1165857198\\\",\\\"value\\\":\\\"c\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"-2093257886\\\",\\\"value\\\":\\\"d\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"-599719438\\\",\\\"value\\\":\\\"e\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": {}, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        Assert.assertEquals("this is the app key", urbanAirshipConfiguration.applicationKey)
        Assert.assertEquals("this is the app secret", urbanAirshipConfiguration.applicationSecret)
        Assert.assertEquals("EU", urbanAirshipConfiguration.domain)
        Assert.assertEquals(true, urbanAirshipConfiguration.enableTags)
        Assert.assertEquals(false, urbanAirshipConfiguration.includeUserAttributes)
        Assert.assertEquals("Application Icon", urbanAirshipConfiguration.notificationIconName)
        Assert.assertEquals("System default", urbanAirshipConfiguration.notificationColor)
        Assert.assertEquals(
            MParticle.IdentityType.CustomerId,
            urbanAirshipConfiguration.userIdField,
        )
        var eventTags: MutableMap<Int, ArrayList<String>> = urbanAirshipConfiguration.eventClass

        Assert.assertTrue(eventTags[-1394780343]?.get(0) == "screen1")
        eventTags = urbanAirshipConfiguration.eventAttributeClass
        Assert.assertTrue(
            eventTags[245922523]?.contains("gesture") == true &&
                eventTags[245922523]
                    ?.contains("a2ctid") == true,
        )
        Assert.assertTrue(eventTags[-2093257886]?.get(0) == "d")
        Assert.assertTrue(eventTags[-1165857198]?.get(0) == "c")
        eventTags = urbanAirshipConfiguration.eventClassDetails
        Assert.assertTrue(eventTags[-2010155734]?.get(0) == "cart")
        Assert.assertTrue(eventTags[847138800]?.get(0) == "pressed")
        eventTags = urbanAirshipConfiguration.eventAttributeClassDetails
        Assert.assertTrue(eventTags[1112195452]?.get(0) == "hello")
        Assert.assertTrue(eventTags[-897761755]?.get(0) == "a")
        Assert.assertTrue(eventTags[-635338283]?.get(0) == "b")
        Assert.assertTrue(eventTags[-599719438]?.get(0) == "e")
    }

    @Test
    @Throws(Exception::class)
    fun testExtractEventName() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val event =
            MPEvent.Builder("Navigation 2").eventType(MParticle.EventType.Navigation).build()
        val set = kit.extractTags(event)
        Assert.assertEquals(1, set.size.toLong())
        Assert.assertEquals("test even tag", set.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun testExtractEventAttributes() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val attributes = HashMap<String, String?>()
        attributes["searchTerm"] = "anything"
        val event =
            MPEvent
                .Builder("search")
                .eventType(MParticle.EventType.Search)
                .customAttributes(attributes)
                .build()
        val set = kit.extractTags(event)
        Assert.assertEquals(2, set.size.toLong())
        Assert.assertTrue(set.contains("test event attribute"))
        Assert.assertTrue(set.contains("test event attribute-anything"))
    }

    @Test
    @Throws(Exception::class)
    fun testExtractScreenName() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val set = kit.extractScreenTags("Screen Layout B", HashMap())
        Assert.assertEquals(1, set.size.toLong())
        Assert.assertEquals("test screen tag", set.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun testExtractScreenAttribute() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val attributes = HashMap<String, String>()
        attributes["version"] = "anything"
        val set = kit.extractScreenTags("Main Screen", attributes)
        Assert.assertEquals(2, set.size.toLong())
        Assert.assertTrue(set.contains("test screen attribute"))
        Assert.assertTrue(set.contains("test screen attribute-anything"))
    }

    @Test
    @Throws(Exception::class)
    fun testExtractEcommEventType() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito
            .`when`(MParticle.getInstance()?.environment)
            .thenReturn(MParticle.Environment.Development)
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val event =
            CommerceEvent
                .Builder(Product.ADD_TO_CART, Product.Builder("name", "sku", 10.0).build())
                .build()
        val set = kit.extractCommerceTags(event)
        Assert.assertEquals(1, set.size.toLong())
        Assert.assertEquals("test ecomm add to cart tag", set.iterator().next())
    }

    @Test
    @Throws(Exception::class)
    fun testExtractEcommAttribute() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito
            .`when`(MParticle.getInstance()!!.environment)
            .thenReturn(MParticle.Environment.Development)
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val event =
            CommerceEvent
                .Builder(Product.PURCHASE, Product.Builder("name", "sku", 10.0).build())
                .transactionAttributes(
                    TransactionAttributes("id").setRevenue(10.0),
                ).build()
        val set = kit.extractCommerceTags(event)
        Assert.assertEquals(2, set.size.toLong())
        Assert.assertTrue(set.contains("test eComm attribute total amount"))
        Assert.assertTrue(set.toString(), set.contains("test eComm attribute total amount-10.0"))
    }

    @Test
    @Throws(Exception::class)
    fun testExtractEcommAttribute2() {
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito
            .`when`(MParticle.getInstance()?.environment)
            .thenReturn(MParticle.Environment.Development)
        val config =
            JSONObject(
                "{ \"id\": 25, \"as\": { \"applicationKey\": \"1234456\", \"applicationSecret\": \"123456\", \"applicationMasterSecret\": \"123456\", \"enableTags\": \"True\", \"includeUserAttributes\": \"True\", \"notificationIconName\": \"Application Icon\", \"notificationColor\": \"System default\", \"namedUserIdField\": \"customerId\", \"eventUserTags\": \"[{\\\"map\\\":\\\"1824528343\\\",\\\"value\\\":\\\"test even tag\\\",\\\"maptype\\\":\\\"EventClass.Id\\\"},{\\\"map\\\":\\\"847138800\\\",\\\"value\\\":\\\"test screen tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"},{\\\"map\\\":\\\"1567\\\",\\\"value\\\":\\\"test ecomm add to cart tag\\\",\\\"maptype\\\":\\\"EventClassDetails.Id\\\"}]\", \"eventAttributeUserTags\": \"[{\\\"map\\\":\\\"-241024017\\\",\\\"value\\\":\\\"test event attribute\\\",\\\"maptype\\\":\\\"EventAttributeClass.Id\\\"},{\\\"map\\\":\\\"861397237\\\",\\\"value\\\":\\\"test screen attribute\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1854578855\\\",\\\"value\\\":\\\"test eComm attribute total amount\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"},{\\\"map\\\":\\\"-1001670849\\\",\\\"value\\\":\\\"test eComm checkout promo code\\\",\\\"maptype\\\":\\\"EventAttributeClassDetails.Id\\\"}]\" }, \"hs\": { \"et\": { \"50\": 0, \"51\": 0 }, \"ec\": { \"-460386492\": 0, \"476338248\": 0, \"-1229406110\": 0, \"-1528980234\": 0, \"-138049017\": 0, \"360094366\": 0, \"-1711952615\": 0, \"1238657721\": 0, \"1057880655\": 0, \"-1415615126\": 0, \"-1573616412\": 0, \"-1417002190\": 0, \"1794482897\": 0, \"-1471969403\": 0, \"1981524391\": 0, \"1025548221\": 0, \"-956692642\": 0, \"-1535298586\": 0 }, \"ea\": { \"-1034789330\": 0, \"-820700541\": 0, \"454072115\": 0, \"1283264677\": 0, \"2132567239\": 0, \"644132244\": 0, \"-576148370\": 0, \"6478943\": 0, \"-1676187368\": 0, \"535860203\": 0, \"260811952\": 0, \"-2143124485\": 0, \"526806372\": 0, \"-261733467\": 0, \"-1809553213\": 0, \"1850278251\": 0 } }, \"pr\": [] }",
            )
        val kitConfig = MockKitConfiguration.createKitConfiguration(config)
        val urbanAirshipConfiguration = UrbanAirshipConfiguration(kitConfig.settings)
        val kit = UrbanAirshipKit()
        kit.configuration = kitConfig
        kit.setUrbanConfiguration(urbanAirshipConfiguration)
        val map = HashMap<String, String>()
        map["Promo Code"] = "this is a promo code"
        val event =
            CommerceEvent
                .Builder(
                    Product.CHECKOUT,
                    Product.Builder("name", "sku", 10.0).customAttributes(map).build(),
                ).build()
        val set = kit.extractCommerceTags(event)
        Assert.assertEquals(2, set.size.toLong())
        Assert.assertTrue(set.contains("test eComm checkout promo code"))
        Assert.assertTrue(
            set.toString(),
            set.contains("test eComm checkout promo code-this is a promo code"),
        )
    }
}
