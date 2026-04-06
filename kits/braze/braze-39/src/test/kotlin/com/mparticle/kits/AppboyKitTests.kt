package com.mparticle.kits

import android.util.SparseBooleanArray
import com.braze.Braze
import com.braze.models.outgoing.BrazeProperties
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticle.IdentityType
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.kits.mocks.MockAppboyKit
import com.mparticle.kits.mocks.MockContextApplication
import com.mparticle.kits.mocks.MockKitConfiguration
import com.mparticle.kits.mocks.MockUser
import junit.framework.TestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.lang.reflect.Method
import java.math.BigDecimal
import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale

class AppboyKitTests {
    private var random = SecureRandom()

    private lateinit var braze: Braze.Companion

    @Mock
    private val mTypeFilters: SparseBooleanArray? = null

    @Mock
    lateinit var user: MParticleUser

    private val kit: AppboyKit
        get() = AppboyKit()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Braze.clearPurchases()
        Braze.clearEvents()
        Braze.currentUser.customUserAttributes.clear()
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()!!.Identity()).thenReturn(
            Mockito.mock(
                IdentityApi::class.java,
            ),
        )
        braze = Braze
    }

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name.isNotEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Exception? = null
        try {
            val kit: KitIntegration = kit
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings, MockContextApplication())
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

    private var hostName = "aRandomHost"

    @Test
    @Throws(Exception::class)
    fun testHostSetting() {
        val settings = HashMap<String, String>()
        settings[AppboyKit.HOST] = hostName
        settings[AppboyKit.APPBOY_KEY] = "key"
        val kit = MockAppboyKit()
        kit.onKitCreate(settings, MockContextApplication())
        Assert.assertTrue(kit.calledAuthority[0] == hostName)
    }

    @Test
    @Throws(Exception::class)
    fun testHostSettingNull() {
        // test that the key is set when it is passed in by the settings map
        val missingSettings = HashMap<String, String>()
        missingSettings[AppboyKit.APPBOY_KEY] = "key"
        val kit = MockAppboyKit()
        try {
            kit.onKitCreate(missingSettings, MockContextApplication())
        } catch (e: Exception) {
        }
        Assert.assertTrue(kit.calledAuthority[0] == null)
    }

    @Test
    @Throws(Exception::class)
    fun testHostSettingEmpty() {
        var nullSettings = HashMap<String, String?>()
        nullSettings[AppboyKit.HOST] = null
        nullSettings[AppboyKit.APPBOY_KEY] = "key"
        var kit = MockAppboyKit()
        try {
            kit.onKitCreate(nullSettings, MockContextApplication())
        } catch (e: Exception) {
        }
        Assert.assertTrue(kit.calledAuthority[0] == null)
        nullSettings = HashMap()
        nullSettings[AppboyKit.HOST] = ""
        nullSettings[AppboyKit.APPBOY_KEY] = "key"
        kit = MockAppboyKit()
        try {
            kit.onKitCreate(nullSettings, MockContextApplication())
        } catch (e: Exception) {
        }
        Assert.assertTrue(kit.calledAuthority[0] == null)
    }

    @Test
    fun testOnModify() {
        // make sure it doesn't crash if there is no email or customerId
        var e: Exception? = null
        try {
            AppboyKit().onModifyCompleted(MockUser(HashMap()), null)
        } catch (ex: Exception) {
            e = ex
        }
        Assert.assertNull(e)
        for (i in 0..3) {
            val values = arrayOfNulls<String>(2)
            val mockEmail = "mockEmail$i"
            val mockCustomerId = "12345$i"
            val kit: AppboyKit =
                object : AppboyKit() {
                    override fun setId(customerId: String) {
                        values[0] = customerId
                    }

                    override fun setEmail(email: String) {
                        if (values[0] == null) {
                            Assert.fail("customerId should have been set first")
                        }
                        values[1] = email
                    }
                }
            kit.identityType = IdentityType.CustomerId
            val map = HashMap<IdentityType, String>()
            map[IdentityType.Email] = mockEmail
            map[IdentityType.Alias] = "alias"
            map[IdentityType.Facebook] = "facebook"
            map[IdentityType.Facebook] = "fb"
            map[IdentityType.CustomerId] = mockCustomerId
            when (i) {
                0 -> {
                    kit.onModifyCompleted(MockUser(map), null)
                    kit.onIdentifyCompleted(MockUser(map), null)
                    kit.onLoginCompleted(MockUser(map), null)
                    kit.onLogoutCompleted(MockUser(map), null)
                }
                1 -> {
                    kit.onIdentifyCompleted(MockUser(map), null)
                    kit.onLoginCompleted(MockUser(map), null)
                    kit.onLogoutCompleted(MockUser(map), null)
                }
                2 -> {
                    kit.onLoginCompleted(MockUser(map), null)
                    kit.onLogoutCompleted(MockUser(map), null)
                }
                3 -> kit.onLogoutCompleted(MockUser(map), null)
            }
            Assert.assertEquals(mockCustomerId, values[0])
            Assert.assertEquals(mockEmail, values[1])
        }
    }

    @Test
    fun testAgeToDob() {
        val kit: AppboyKit = MockAppboyKit()
        val currentYear = Calendar.getInstance()[Calendar.YEAR]
        var calendar = kit.getCalendarMinusYears("5")
        calendar
            ?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 5).toLong(), it.toLong()) }
        calendar = kit.getCalendarMinusYears(22)
        calendar
            ?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 22).toLong(), it.toLong()) }

//        round down doubles
        calendar = kit.getCalendarMinusYears("5.001")
        calendar
            ?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 5).toLong(), it.toLong()) }
        calendar = kit.getCalendarMinusYears("5.9")
        calendar
            ?.get(Calendar.YEAR)
            ?.let { Assert.assertEquals((currentYear - 5).toLong(), it.toLong()) }

        // invalid ages (negative, non numeric), don't get set
        Assert.assertNull(kit.getCalendarMinusYears("asdv"))
        Assert.assertNull(kit.getCalendarMinusYears(-1))
    }

    @Test
    fun testSetSubscriptionGroupIds() {
        val settings = HashMap<String, String>()
        settings[AppboyKit.APPBOY_KEY] = "key"
        settings[AppboyKit.HOST] = hostName
        settings["subscriptionGroupMapping"] =
            "[{\"jsmap\":null,\"map\":\"test1\",\"maptype\":\"UserAttributeClass.Name\"," +
            "\"value\":\"00000000-0000-0000-0000-000000000000\"}," +
            "{\"jsmap\":null,\"map\":\"test2\",\"maptype\":\"UserAttributeClass.Name\"," +
            "\"value\":\"00000000-0000-0000-0000-000000000001\"}," +
            "{\"jsmap\":null,\"map\":\"test3\",\"maptype\":\"UserAttributeClass.Name\"," +
            "\"value\":\"00000000-0000-0000-0000-000000000002\"}]"
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser

        kit.onKitCreate(settings, MockContextApplication())
        kit.onSetUserAttribute("test1", "true")
        kit.onSetUserAttribute("test2", "false")
        kit.onSetUserAttribute("test3", "notABoolean")
        Assert.assertEquals(2, currentUser.getCustomUserAttribute().size.toLong())
    }

//    @Test
//    fun testSetUserAttributeAge() {
//        val currentYear = Calendar.getInstance()[Calendar.YEAR]
//        val kit: AppboyKit = MockAppboyKit()
//        val currentUser = Braze.currentUser
//        Assert.assertEquals(-1, currentUser.dobDay.toLong())
//        Assert.assertEquals(-1, currentUser.dobYear.toLong())
//        Assert.assertNull(currentUser.dobMonth)
//        kit.setUserAttribute(MParticle.UserAttributes.AGE, "100")
//        Assert.assertEquals((currentYear - 100).toLong(), currentUser.dobYear.toLong())
//        Assert.assertEquals(1, currentUser.dobDay.toLong())
//        Assert.assertEquals(Month.JANUARY, currentUser.dobMonth)
//    }

//    @Test
//    fun testSetUserDoB() {
//        val kit = MockAppboyKit()
//        val currentUser = Braze.currentUser
//        val errorMessage = arrayOfNulls<String>(1)
//        Logger.setLogHandler(object : DefaultLogHandler() {
//            override fun log(priority: LogLevel, error: Throwable?, messages: String) {
//                if (priority == LogLevel.WARNING) {
//                    errorMessage[0] = messages
//                }
//            }
//        })
//
//        //valid
//        kit.setUserAttribute("dob", "1999-11-05")
//        Assert.assertEquals(1999, currentUser.dobYear.toLong())
//        Assert.assertEquals(5, currentUser.dobDay.toLong())
//        Assert.assertEquals(Month.NOVEMBER, currentUser.dobMonth)
//        Assert.assertNull(errorMessage[0])
//
//        //future
//        kit.setUserAttribute("dob", "2999-2-15")
//        Assert.assertEquals(2999, currentUser.dobYear.toLong())
//        Assert.assertEquals(15, currentUser.dobDay.toLong())
//        Assert.assertEquals(Month.FEBRUARY, currentUser.dobMonth)
//        Assert.assertNull(errorMessage[0])
//
//
//        //bad format (shouldn't crash, but should message)
//        var ex: Exception? = null
//        try {
//            kit.setUserAttribute("dob", "2kjb.21h045")
//            Assert.assertEquals(2999, currentUser.dobYear.toLong())
//            Assert.assertEquals(15, currentUser.dobDay.toLong())
//            Assert.assertEquals(Month.FEBRUARY, currentUser.dobMonth)
//            Assert.assertNotNull(errorMessage[0])
//        } catch (e: Exception) {
//            ex = e
//        }
//        Assert.assertNull(ex)
//    }

    @Test
    fun setIdentityType() {
        val possibleValues =
            arrayOf(
                "Other",
                "CustomerId",
                "Facebook",
                "Twitter",
                "Google",
                "Microsoft",
                "Yahoo",
                "Email",
                "Alias",
            )
        val mpid = "MPID"
        for (`val` in possibleValues) {
            val kit = kit
            val settings = HashMap<String, String>()
            settings[AppboyKit.USER_IDENTIFICATION_TYPE] = `val`
            kit.setIdentityType(settings)
            Assert.assertNotNull(kit.identityType)
            Assert.assertEquals(
                `val`.lowercase(Locale.getDefault()),
                kit.identityType?.name?.lowercase(Locale.getDefault()),
            )
            Assert.assertFalse(kit.isMpidIdentityType)
        }
        val settings = HashMap<String, String>()
        settings[AppboyKit.USER_IDENTIFICATION_TYPE] = mpid
        val kit = kit
        kit.setIdentityType(settings)
        Assert.assertNull(kit.identityType)
        Assert.assertTrue(kit.isMpidIdentityType)
    }

    @Test
    fun setId() {
        val userIdentities = HashMap<IdentityType, String>()
        val user = Mockito.mock(MParticleUser::class.java)
        Mockito.`when`(user.userIdentities).thenReturn(userIdentities)
        val mockId = random.nextLong()
        Mockito.`when`(user.id).thenReturn(mockId)
        Assert.assertEquals(mockId.toString(), kit.getIdentity(true, null, user))
        for (identityType in IdentityType.values()) {
            val identityValue = random.nextLong().toString()
            userIdentities[identityType] = identityValue
            Assert.assertEquals(identityValue, kit.getIdentity(false, identityType, user))
        }
        Assert.assertNull(kit.getIdentity(false, null, null))
    }

//    @Test
//    fun addRemoveAttributeFromEventTest() {
//        val kit = MockAppboyKit()
//        val currentUser = Braze.currentUser
//        kit.configuration = object : MockKitConfiguration() {
//
//            override fun getEventAttributesAddToUser(): Map<Int, String> {
//                val map = HashMap<Int, String>()
//                map[KitUtils.hashForFiltering(
//                    MParticle.EventType.Navigation.toString() + "Navigation Event" + "key1"
//                )] = "output"
//                return map
//            }
//
//            override fun getEventAttributesRemoveFromUser(): Map<Int, String> {
//                val map = HashMap<Int, String>()
//                map[KitUtils.hashForFiltering(
//                    MParticle.EventType.Location.toString() + "location event" + "key1"
//                )] = "output"
//                return map
//            }
//
//        }
//        val customAttributes = HashMap<String, String>()
//        customAttributes["key1"] = "value1"
//        kit.logEvent(
//            MPEvent.Builder("Navigation Event", MParticle.EventType.Navigation)
//                .customAttributes(customAttributes)
//                .build()
//        )
//        var attributes = currentUser.customAttributeArray["output"]
//        if (attributes != null) {
//            Assert.assertEquals(1, attributes.size)
//            Assert.assertEquals("value1", attributes[0])
//        }
//        kit.logEvent(
//            MPEvent.Builder("location event", MParticle.EventType.Location)
//                .customAttributes(customAttributes)
//                .build()
//        )
//        attributes = currentUser.customAttributeArray["output"]
//
//        if (attributes != null) {
//            Assert.assertEquals(0, attributes.size)
//        }
//    }

    @Test
    fun testPurchaseCurrency() {
        val kit = MockAppboyKit()
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.CHECKOUT, product)
                .currency("Moon Dollars")
                .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("Moon Dollars", purchase.currency)
        Assert.assertNull(
            purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE],
        )
    }

    @Test
    fun testPurchaseDefaultCurrency() {
        val kit = MockAppboyKit()
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.CHECKOUT, product)
                .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals(CommerceEventUtils.Constants.DEFAULT_CURRENCY_CODE, purchase.currency)
        Assert.assertNull(
            purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE],
        )
    }

    @Test
    fun testPurchase() {
        val kit = MockAppboyKit()
        val customAttributes = HashMap<String, String>()
        customAttributes["key1"] = "value1"
        customAttributes["key #2"] = "value #3"
        val productCustomAttributes = HashMap<String, String>()
        productCustomAttributes["productKey1"] = "value1"
        productCustomAttributes["productKey2"] = "value2"
        val transactionAttributes =
            TransactionAttributes("the id")
                .setTax(100.0)
                .setShipping(12.0)
                .setRevenue(99.0)
                .setCouponCode("coupon code")
                .setAffiliation("the affiliation")
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .quantity(5.0)
                .brand("testBrand")
                .variant("testVariant")
                .position(1)
                .category("testCategory")
                .customAttributes(productCustomAttributes)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.PURCHASE, product)
                .currency("Moon Dollars")
                .productListName("product list name")
                .productListSource("the source")
                .customAttributes(customAttributes)
                .transactionAttributes(transactionAttributes)
                .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("Moon Dollars", purchase.currency)
        Assert.assertEquals(5.0, purchase.quantity.toDouble(), 0.01)
        Assert.assertEquals("sku1", purchase.sku)
        Assert.assertEquals(BigDecimal(4.5), purchase.unitPrice)
        Assert.assertNotNull(purchase.purchaseProperties)
        val properties = purchase.purchaseProperties.properties
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_SHIPPING), 12.0)
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_ACTION_PRODUCT_LIST_SOURCE),
            "the source",
        )
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TAX), 100.0)
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TOTAL), 99.0)
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_ACTION_PRODUCT_ACTION_LIST),
            "product list name",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_COUPON_CODE),
            "coupon code",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_TRANSACTION_ID),
            "the id",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_AFFILIATION),
            "the affiliation",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_NAME),
            "product name",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_CATEGORY),
            "testCategory",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_BRAND),
            "testBrand",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_POSITION),
            1,
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_VARIANT),
            "testVariant",
        )

        // Custom Attributes
        Assert.assertEquals(properties.remove("key1"), "value1")
        Assert.assertEquals(properties.remove("key #2"), "value #3")

        // Product Custom Attributes
        Assert.assertEquals(properties.remove("productKey1"), "value1")
        Assert.assertEquals(properties.remove("productKey2"), "value2")

        val emptyAttributes = HashMap<String, String>()
        Assert.assertEquals(emptyAttributes, properties)
    }

    @Test
    fun testEnhancedPurchase() {
        val emptyAttributes = HashMap<String, String>()
        val kit = MockAppboyKit()
        val customAttributes = HashMap<String, String>()
        customAttributes["key1"] = "value1"
        customAttributes["key #2"] = "value #3"
        val transactionAttributes =
            TransactionAttributes("the id")
                .setTax(100.0)
                .setShipping(12.0)
                .setRevenue(99.0)
                .setCouponCode("coupon code")
                .setAffiliation("the affiliation")
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .quantity(5.0)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.PURCHASE, product)
                .currency("Moon Dollars")
                .productListName("product list name")
                .productListSource("the source")
                .customAttributes(customAttributes)
                .transactionAttributes(transactionAttributes)
                .build()
        kit.logOrderLevelTransaction(commerceEvent)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("Moon Dollars", purchase.currency)
        Assert.assertEquals(1.0, purchase.quantity.toDouble(), 0.01)
        Assert.assertEquals("eCommerce - purchase", purchase.sku)
        Assert.assertEquals(BigDecimal(99.0), purchase.unitPrice)
        Assert.assertNotNull(purchase.purchaseProperties)
        val properties = purchase.purchaseProperties.properties
        val productArray = properties.remove(AppboyKit.PRODUCT_KEY)
        Assert.assertTrue(productArray is JSONArray)
        if (productArray is Array<*>) {
            Assert.assertEquals(1, productArray.size.toLong())
            val productBrazeProperties = productArray[0]
            if (productBrazeProperties is BrazeProperties) {
                val productProperties = productBrazeProperties.properties
                Assert.assertEquals(
                    productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_TOTAL_AMOUNT),
                    22.5,
                )
                Assert.assertEquals(
                    productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_PRICE),
                    4.5,
                )
                Assert.assertEquals(
                    productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_QUANTITY),
                    5.0,
                )
                Assert.assertEquals(
                    productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_ID),
                    "sku1",
                )
                Assert.assertEquals(
                    productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_NAME),
                    "product name",
                )
                Assert.assertEquals(emptyAttributes, productProperties)
            }
        }
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_SHIPPING), 12.0)
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TAX), 100.0)
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_COUPON_CODE),
            "coupon code",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_AFFILIATION),
            "the affiliation",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_ACTION_PRODUCT_LIST_SOURCE),
            "the source",
        )
        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TOTAL), 99.0)
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_ACTION_PRODUCT_ACTION_LIST),
            "product list name",
        )
        Assert.assertEquals(
            properties.remove(CommerceEventUtils.Constants.ATT_TRANSACTION_ID),
            "the id",
        )

        val brazeCustomAttributesDictionary = properties.remove(AppboyKit.CUSTOM_ATTRIBUTES_KEY)
        if (brazeCustomAttributesDictionary is BrazeProperties) {
            val customAttributesDictionary = brazeCustomAttributesDictionary.properties
            Assert.assertEquals(customAttributesDictionary.remove("key1"), "value1")
            Assert.assertEquals(customAttributesDictionary.remove("key #2"), "value #3")
            Assert.assertEquals(emptyAttributes, customAttributesDictionary)
        }
        Assert.assertEquals(emptyAttributes, properties)
    }

//    @Test
//    fun testPromotion() {
//        val emptyAttributes = HashMap<String, String>()
//        val kit = MockAppboyKit()
//        kit.configuration = MockKitConfiguration()
//        val customAttributes = HashMap<String, String>()
//        customAttributes["key1"] = "value1"
//        customAttributes["key #2"] = "value #3"
//        val promotion = Promotion().apply {
//            id = "my_promo_1"
//            creative = "sale_banner_1"
//            name = "App-wide 50% off sale"
//            position ="dashboard_bottom"
//        }
//        val commerceEvent = CommerceEvent.Builder(Promotion.VIEW, promotion)
//            .customAttributes(customAttributes)
//            .build()
//        kit.logEvent(commerceEvent)
//
//        val braze = Braze
//        val events = braze.events
//        Assert.assertEquals(1, events.size.toLong())
//        val event = events.values.iterator().next()
//        Assert.assertNotNull(event.properties)
//        val properties = event.properties
//
//        Assert.assertEquals(properties.remove("Id"), "my_promo_1")
//        Assert.assertEquals(properties.remove("Name"), "App-wide 50% off sale")
//        Assert.assertEquals(properties.remove("Position"), "dashboard_bottom")
//        Assert.assertEquals(properties.remove("Creative"), "sale_banner_1")
//        Assert.assertEquals(properties.remove("key1"), "value1")
//        Assert.assertEquals(properties.remove("key #2"), "value #3")
//
//        Assert.assertEquals(emptyAttributes, properties)
//    }

    @Test
    fun testEnhancedPromotion() {
        val emptyAttributes = HashMap<String, String>()
        val kit = MockAppboyKit()
        kit.configuration = MockKitConfiguration()
        val customAttributes = HashMap<String, String>()
        customAttributes["key1"] = "value1"
        customAttributes["key #2"] = "value #3"
        val promotion =
            Promotion().apply {
                id = "my_promo_1"
                creative = "sale_banner_1"
                name = "App-wide 50% off sale"
                position = "dashboard_bottom"
            }
        val commerceEvent =
            CommerceEvent
                .Builder(Promotion.VIEW, promotion)
                .customAttributes(customAttributes)
                .build()
        kit.logOrderLevelTransaction(commerceEvent)
        val braze = Braze
        val events = braze.events
        Assert.assertEquals(1, events.size.toLong())
        val event = events.values.iterator().next()
        Assert.assertNotNull(event.properties)
        val properties = event.properties

        val promotionArray = properties.remove(AppboyKit.PROMOTION_KEY)
        Assert.assertTrue(promotionArray is JSONArray)
        if (promotionArray is Array<*>) {
            Assert.assertEquals(1, promotionArray.size.toLong())
            val promotionBrazeProperties = promotionArray[0]
            if (promotionBrazeProperties is BrazeProperties) {
                val promotionProperties = promotionBrazeProperties.properties
                Assert.assertEquals(
                    promotionProperties.remove(CommerceEventUtils.Constants.ATT_PROMOTION_ID),
                    "my_promo_1",
                )
                Assert.assertEquals(
                    promotionProperties.remove(CommerceEventUtils.Constants.ATT_PROMOTION_NAME),
                    "App-wide 50% off sale",
                )
                Assert.assertEquals(
                    promotionProperties.remove(CommerceEventUtils.Constants.ATT_PROMOTION_POSITION),
                    "dashboard_bottom",
                )
                Assert.assertEquals(
                    promotionProperties.remove(CommerceEventUtils.Constants.ATT_PROMOTION_CREATIVE),
                    "sale_banner_1",
                )
                Assert.assertEquals(emptyAttributes, promotionProperties)
            }
        }

        val brazeCustomAttributesDictionary = properties.remove(AppboyKit.CUSTOM_ATTRIBUTES_KEY)
        if (brazeCustomAttributesDictionary is BrazeProperties) {
            val customAttributesDictionary = brazeCustomAttributesDictionary.properties
            Assert.assertEquals(customAttributesDictionary.remove("key1"), "value1")
            Assert.assertEquals(customAttributesDictionary.remove("key #2"), "value #3")
            Assert.assertEquals(emptyAttributes, customAttributesDictionary)
        }

        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TOTAL), 0.0)

        Assert.assertEquals(emptyAttributes, properties)
    }

//    @Test
//    fun testImpression() {
//        val kit = MockAppboyKit()
//        kit.configuration = MockKitConfiguration()
//        val customAttributes = HashMap<String, String>()
//        customAttributes["key1"] = "value1"
//        customAttributes["key #2"] = "value #3"
//        val product = Product.Builder("product name", "sku1", 4.5)
//            .quantity(5.0)
//            .build()
//        val impression = Impression("Suggested Products List", product).let {
//            CommerceEvent.Builder(it).build()
//        }
//        val commerceEvent = CommerceEvent.Builder(impression)
//            .customAttributes(customAttributes)
//            .build()
//
//        kit.logEvent(commerceEvent)
//
//        val braze = Braze
//        val events = braze.events
//        Assert.assertEquals(1, events.size.toLong())
//        val event = events.values.iterator().next()
//        Assert.assertNotNull(event.properties)
//        val properties = event.properties
//
//        Assert.assertEquals(
//            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_TOTAL_AMOUNT), "22.5"
//        )
//        Assert.assertEquals(
//            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_NAME), "product name"
//        )
//        Assert.assertEquals(
//            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_QUANTITY), "5.0"
//        )
//        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_ID), "sku1")
//        Assert.assertEquals(
//            properties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_PRICE), "4.5"
//        )
//        Assert.assertEquals(
//            properties.remove("Product Impression List"), "Suggested Products List"
//        )
//        Assert.assertEquals(properties.remove("key1"), "value1")
//        Assert.assertEquals(properties.remove("key #2"), "value #3")
//
//        val emptyAttributes = HashMap<String, String>()
//        Assert.assertEquals(emptyAttributes, properties)
//    }

    @Test
    fun testEnhancedImpression() {
        val emptyAttributes = HashMap<String, String>()
        val kit = MockAppboyKit()
        kit.configuration = MockKitConfiguration()
        val customAttributes = HashMap<String, String>()
        customAttributes["key1"] = "value1"
        customAttributes["key #2"] = "value #3"
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .quantity(5.0)
                .customAttributes(customAttributes)
                .build()
        val impression =
            Impression("Suggested Products List", product).let {
                CommerceEvent.Builder(it).build()
            }
        val commerceEvent =
            CommerceEvent
                .Builder(impression)
                .customAttributes(customAttributes)
                .build()
        kit.logOrderLevelTransaction(commerceEvent)
        val braze = Braze
        val events = braze.events
        Assert.assertEquals(1, events.size.toLong())
        val event = events.values.iterator().next()
        Assert.assertNotNull(event.properties)
        val properties = event.properties

        val impressionArray = properties.remove(AppboyKit.IMPRESSION_KEY)
        Assert.assertTrue(impressionArray is JSONArray)
        if (impressionArray is Array<*>) {
            Assert.assertEquals(1, impressionArray.size.toLong())
            val impressionBrazeProperties = impressionArray[0]
            if (impressionBrazeProperties is BrazeProperties) {
                val impressionProperties = impressionBrazeProperties.properties
                Assert.assertEquals(
                    impressionProperties.remove("Product Impression List"),
                    "Suggested Products List",
                )
                val productArray = impressionProperties.remove(AppboyKit.PRODUCT_KEY)
                Assert.assertTrue(productArray is Array<*>)
                if (productArray is Array<*>) {
                    Assert.assertEquals(1, productArray.size.toLong())
                    val productBrazeProperties = productArray[0]
                    if (productBrazeProperties is BrazeProperties) {
                        val productProperties = productBrazeProperties.properties
                        Assert.assertEquals(
                            productProperties.remove(
                                CommerceEventUtils.Constants.ATT_PRODUCT_TOTAL_AMOUNT,
                            ),
                            22.5,
                        )
                        Assert.assertEquals(
                            productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_NAME),
                            "product name",
                        )
                        Assert.assertEquals(
                            productProperties.remove(
                                CommerceEventUtils.Constants.ATT_PRODUCT_QUANTITY,
                            ),
                            5.0,
                        )
                        Assert.assertEquals(
                            productProperties.remove(CommerceEventUtils.Constants.ATT_PRODUCT_ID),
                            "sku1",
                        )
                        Assert.assertEquals(
                            productProperties.remove(
                                CommerceEventUtils.Constants.ATT_PRODUCT_PRICE,
                            ),
                            4.5,
                        )
                        val brazeProductCustomAttributesDictionary =
                            productProperties.remove(AppboyKit.CUSTOM_ATTRIBUTES_KEY)
                        if (brazeProductCustomAttributesDictionary is BrazeProperties) {
                            val customProductAttributesDictionary =
                                brazeProductCustomAttributesDictionary.properties
                            Assert.assertEquals(
                                customProductAttributesDictionary.remove("key1"),
                                "value1",
                            )
                            Assert.assertEquals(
                                customProductAttributesDictionary.remove("key #2"),
                                "value #3",
                            )
                            Assert.assertEquals(emptyAttributes, customProductAttributesDictionary)
                        }
                        Assert.assertEquals(emptyAttributes, productProperties)
                    }
                    Assert.assertEquals(emptyAttributes, impressionProperties)
                }
            }
        }

        val brazeCustomAttributesDictionary = properties.remove(AppboyKit.CUSTOM_ATTRIBUTES_KEY)
        if (brazeCustomAttributesDictionary is BrazeProperties) {
            val customAttributesDictionary = brazeCustomAttributesDictionary.properties
            Assert.assertEquals(customAttributesDictionary.remove("key1"), "value1")
            Assert.assertEquals(customAttributesDictionary.remove("key #2"), "value #3")
            Assert.assertEquals(emptyAttributes, customAttributesDictionary)
        }

        Assert.assertEquals(properties.remove(CommerceEventUtils.Constants.ATT_TOTAL), 0.0)

        Assert.assertEquals(emptyAttributes, properties)
    }

//    @Test
//    fun setUserAttributeTyped() {
//        val kit = MockAppboyKit()
//        kit.enableTypeDetection = true
//        val currentUser = Braze.currentUser
//        kit.setUserAttribute("foo", "true")
//        Assert.assertTrue(currentUser.customUserAttributes["foo"] is Boolean)
//        Assert.assertEquals(currentUser.customUserAttributes["foo"], true)
//        kit.setUserAttribute("foo", "1")
//        Assert.assertTrue(currentUser.customUserAttributes["foo"] is Int)
//        Assert.assertEquals(currentUser.customUserAttributes["foo"], 1)
//        kit.setUserAttribute("foo", "1.1")
//        Assert.assertTrue(currentUser.customUserAttributes["foo"] is Double)
//        Assert.assertEquals(currentUser.customUserAttributes["foo"], 1.1)
//        kit.setUserAttribute("foo", "bar")
//        Assert.assertTrue(currentUser.customUserAttributes["foo"] is String)
//        Assert.assertEquals(currentUser.customUserAttributes["foo"], "bar")
//    }

//    @Test
//    fun testEventStringType() {
//        val kit = MockAppboyKit()
//        kit.configuration = MockKitConfiguration()
//        val customAttributes = HashMap<String, String?>()
//        customAttributes["foo"] = "false"
//        customAttributes["bar"] = "1"
//        customAttributes["baz"] = "1.5"
//        customAttributes["fuzz?"] = "foobar"
//        val customEvent = MPEvent.Builder("testEvent", MParticle.EventType.Location)
//            .customAttributes(customAttributes)
//            .build()
//        kit.enableTypeDetection = true
//        kit.logEvent(customEvent)
//        val braze = Braze
//        val events = braze.events
//        Assert.assertEquals(1, events.values.size.toLong())
//        val event = events.values.iterator().next()
//        val properties = event.properties
//        Assert.assertEquals(properties.remove("foo"), false)
//        Assert.assertEquals(properties.remove("bar"), 1)
//        Assert.assertEquals(properties.remove("baz"), 1.5)
//        Assert.assertEquals(properties.remove("fuzz?"), "foobar")
//        Assert.assertEquals(0, properties.size.toLong())
//    }

//    @Test
//    fun testLogCommerceEvent() {
//        val kit = MockAppboyKit()
//
//        val product: Product = Product.Builder("La Enchilada", "13061043670", 12.5)
//            .quantity(1.0)
//            .build()
//
//        val txAttributes = TransactionAttributes()
//            .setRevenue(product.getTotalAmount())
//
//        kit.configuration = MockKitConfiguration()
//        val customAttributes: MutableMap<String, String> = HashMap()
//        customAttributes["currentLocationLongitude"] = "2.1811267"
//        customAttributes["country"] = "ES"
//        customAttributes["deliveryLocationLatitude"] = "41.4035798"
//        customAttributes["appVersion"] = "5.201.0"
//        customAttributes["city"] = "BCN"
//        customAttributes["deviceId"] = "1104442582"
//        customAttributes["platform"] = "android"
//        customAttributes["isAuthorized"] = "true"
//        customAttributes["productSelectionOrigin"] = "Catalogue"
//        customAttributes["currentLocationLatitude"] = "41.4035798"
//        customAttributes["collectionId"] = "1180889389"
//        customAttributes["multiplatformVersion"] = "1.0.288"
//        customAttributes["deliveryLocationTimestamp"] = "1675344636685"
//        customAttributes["productId"] = "13061043670"
//        customAttributes["storeAddressId"] = "300482"
//        customAttributes["currentLocationAccuracy"] = "19.278"
//        customAttributes["productAddedOrigin"] = "Item Detail Add to Order"
//        customAttributes["deliveryLocationLongitude"] = "2.1811267"
//        customAttributes["currentLocationTimestamp"] = "1675344636685"
//        customAttributes["dynamicSessionId"] = "67f8fb8d-8d14-4f0e-bf1a-73fb8e6eed95"
//        customAttributes["deliveryLocationAccuracy"] = "19.278"
//        customAttributes["categoryId"] = "1"
//        customAttributes["isSponsored"] = "false"
//
//        val commerceEvent: CommerceEvent = CommerceEvent.Builder(Product.ADD_TO_CART, product)
//            .currency("EUR")
//            .customAttributes(customAttributes)
//            .transactionAttributes(txAttributes)
//            .build()
//        kit.logEvent(commerceEvent)
//
//        val braze = Braze
//        val events = braze.events
//        Assert.assertEquals(1, events.size.toLong())
//        val event = events.values.iterator().next()
//        Assert.assertNotNull(event.properties)
//        val properties = event.properties
//
//        Assert.assertEquals(properties.remove("Name"), "La Enchilada")
//        Assert.assertEquals(properties.remove("Total Product Amount"), "12.5")
//        Assert.assertEquals(properties.remove("Id"), "13061043670")
//        for (item in customAttributes) {
//            Assert.assertTrue(properties.containsKey(item.key))
//            Assert.assertTrue(properties.containsValue(item.value))
//        }
//    }

//    @Test
//    fun testEventStringTypeNotEnabled() {
//        val kit = MockAppboyKit()
//        kit.configuration = MockKitConfiguration()
//        val customAttributes = HashMap<String, String?>()
//        customAttributes["foo"] = "false"
//        customAttributes["bar"] = "1"
//        customAttributes["baz"] = "1.5"
//        customAttributes["fuzz?"] = "foobar"
//        val customEvent = MPEvent.Builder("testEvent", MParticle.EventType.Location)
//            .customAttributes(customAttributes)
//            .build()
//        kit.enableTypeDetection = false
//        kit.logEvent(customEvent)
//        val braze = Braze
//        val events = braze.events
//        Assert.assertEquals(1, events.values.size.toLong())
//        val event = events.values.iterator().next()
//        val properties = event.properties
//        Assert.assertEquals(properties.remove("foo"), "false")
//        Assert.assertEquals(properties.remove("bar"), "1")
//        Assert.assertEquals(properties.remove("baz"), "1.5")
//        Assert.assertEquals(properties.remove("fuzz?"), "foobar")
//        Assert.assertEquals(0, properties.size.toLong())
//    }

    @Test
    fun testCustomAttributes_log_add_attribute_event() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser

        kit.configuration = MockKitConfiguration()

        val jsonObject = JSONObject()
        val mapValue = JSONObject()
        // this is hash for event attribute i.e combination of eventType + eventName + attribute key
        mapValue.put("888169310", "testEvent")
        val eaaObject = JSONObject()
        eaaObject.put("eaa", mapValue)
        jsonObject.put("hs", eaaObject)

        Mockito.`when`(mTypeFilters!!.size()).thenReturn(0)

        var kitConfiguration = MockKitConfiguration.createKitConfiguration(jsonObject)
        kit.configuration = kitConfiguration
        val customAttributes: MutableMap<String, String> = HashMap()
        customAttributes["destination"] = "Shop"
        val event =
            MPEvent
                .Builder("AndroidTEST", MParticle.EventType.Navigation)
                .customAttributes(customAttributes)
                .build()
        val instance = MParticle.getInstance()
        instance?.logEvent(event)
        kit.logEvent(event)
        Assert.assertEquals(1, braze.events.size.toLong())
        Assert.assertEquals(1, currentUser.getCustomAttribute().size.toLong())
        var outputKey = ""
        for (keys in currentUser.getCustomAttribute().keys) {
            outputKey = keys
            break
        }
        Assert.assertEquals("testEvent", outputKey)
    }

    @Test
    fun testCustomAttributes_log_remove_attribute_event() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser

        kit.configuration = MockKitConfiguration()

        val jsonObject = JSONObject()
        val mapValue = JSONObject()
        // this is hash for event attribute i.e combination of eventType + eventName + attribute key
        mapValue.put("888169310", "testEvent")
        val eaaObject = JSONObject()
        eaaObject.put("eaa", mapValue)
        eaaObject.put("ear", mapValue)
        jsonObject.put("hs", eaaObject)

        Mockito.`when`(mTypeFilters!!.size()).thenReturn(0)

        var kitConfiguration = MockKitConfiguration.createKitConfiguration(jsonObject)
        kit.configuration = kitConfiguration
        val customAttributes: MutableMap<String, String> = HashMap()
        customAttributes["destination"] = "Shop"
        val event =
            MPEvent
                .Builder("AndroidTEST", MParticle.EventType.Navigation)
                .customAttributes(customAttributes)
                .build()
        val instance = MParticle.getInstance()
        instance?.logEvent(event)
        kit.logEvent(event)
        Assert.assertEquals(1, braze.events.size.toLong())
        Assert.assertEquals(0, currentUser.getCustomAttribute().size.toLong())
    }

    @Test
    fun testCustomAttributes_log_add_customUserAttribute_event() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser

        kit.configuration = MockKitConfiguration()

        val jsonObject = JSONObject()
        val mapValue = JSONObject()
        // this is hash for event attribute i.e combination of eventType + eventName + attribute key
        mapValue.put("888169310", "testEvent")
        val eaaObject = JSONObject()
        eaaObject.put("eas", mapValue)
        jsonObject.put("hs", eaaObject)

        Mockito.`when`(mTypeFilters!!.size()).thenReturn(0)

        var kitConfiguration = MockKitConfiguration.createKitConfiguration(jsonObject)
        kit.configuration = kitConfiguration
        val customAttributes: MutableMap<String, String> = HashMap()
        customAttributes["destination"] = "Shop"
        val event =
            MPEvent
                .Builder("AndroidTEST", MParticle.EventType.Navigation)
                .customAttributes(customAttributes)
                .build()
        val instance = MParticle.getInstance()
        instance?.logEvent(event)
        kit.logEvent(event)
        Assert.assertEquals(1, braze.events.size.toLong())
        Assert.assertEquals(1, currentUser.getCustomUserAttribute().size.toLong())
        var outputKey = ""
        for (keys in currentUser.getCustomUserAttribute().keys) {
            outputKey = keys
            break
        }
        Assert.assertEquals("testEvent", outputKey)
    }

    @Test
    fun testParseToNestedMap_When_JSON_Is_INVALID() {
        val kit = MockAppboyKit()
        var jsonInput =
            "{'GDPR':{'marketing':'{:false,'timestamp':1711038269644:'Test consent'," +
                "'location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}," +
                "'performance':'{'consented':true,'timestamp':1711038269644," +
                "'document':'parental_consent_agreement_v2','location':'17 Cherry Tree Lan 3'," +
                "'hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}," +
                "'CCPA':'{'consented':true,'timestamp':1711038269644," +
                "'document':'ccpa_consent_agreement_v3','location':'17 Cherry Tree Lane'," +
                "'hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}"

        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "parseToNestedMap",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, jsonInput)
        Assert.assertEquals(mutableMapOf<String, Any>(), result)
    }

    @Test
    fun testParseToNestedMap_When_JSON_Is_Empty() {
        val kit = MockAppboyKit()
        var jsonInput = ""

        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "parseToNestedMap",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, jsonInput)
        Assert.assertEquals(mutableMapOf<String, Any>(), result)
    }

    @Test
    fun testSearchKeyInNestedMap_When_Input_Key_Is_Empty_String() {
        val kit = MockAppboyKit()
        val map =
            mapOf(
                "FeatureEnabled" to true,
                "settings" to
                    mapOf(
                        "darkMode" to false,
                        "notifications" to
                            mapOf(
                                "email" to false,
                                "push" to true,
                                "lastUpdated" to 1633046400000L,
                            ),
                    ),
            )
        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "searchKeyInNestedMap",
                Map::class.java,
                Any::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, map, "")
        Assert.assertEquals(null, result)
    }

    @Test
    fun testSearchKeyInNestedMap_When_Input_Is_Empty_Map() {
        val kit = MockAppboyKit()
        val emptyMap: Map<String, Int> = emptyMap()
        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "searchKeyInNestedMap",
                Map::class.java,
                Any::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, emptyMap, "1")
        Assert.assertEquals(null, result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_Empty_Json() {
        val kit = MockAppboyKit()
        val emptyJson = ""
        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, emptyJson)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_Invalid_Json() {
        val kit = MockAppboyKit()
        var jsonInput =
            "{'GDPR':{'marketing':'{:false,'timestamp':1711038269644:'Test consent'," +
                "'location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}," +
                "'performance':'{'consented':true,'timestamp':1711038269644," +
                "'document':'parental_consent_agreement_v2','location':'17 Cherry Tree Lan 3'," +
                "'hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}," +
                "'CCPA':'{'consented':true,'timestamp':1711038269644," +
                "'document':'ccpa_consent_agreement_v3','location':'17 Cherry Tree Lane'," +
                "'hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}"
        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, jsonInput)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_NULL() {
        val kit = MockAppboyKit()
        val method: Method =
            AppboyKit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, null)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun onConsentStateUpdatedTest() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser
        kit.configuration = MockKitConfiguration()
        val map = java.util.HashMap<String, String>()

        map["consentMappingSDK"] =
            "        [{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\"," +
            "\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"google_ad_user_data\\\"}," +
            "{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\"," +
            "\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"google_ad_personalization\\\"}]"

        var kitConfiguration =
            MockKitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))
        kit.configuration = kitConfiguration

        val marketingConsent =
            GDPRConsent
                .builder(false)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Marketing", marketingConsent)
                .build()
        kit.onConsentStateUpdated(state, state)
        TestCase.assertEquals(
            false,
            currentUser.getCustomUserAttribute()["\$google_ad_personalization"],
        )
    }

    @Test
    fun onConsentStateUpdatedTest_When_Both_The_consents_Are_True() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser
        kit.configuration = MockKitConfiguration()
        val map = java.util.HashMap<String, String>()

        map["consentMappingSDK"] =
            "        [{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\"," +
            "\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"google_ad_user_data\\\"}," +
            "{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\"," +
            "\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"google_ad_personalization\\\"}]"

        var kitConfiguration =
            MockKitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))
        kit.configuration = kitConfiguration

        val marketingConsent =
            GDPRConsent
                .builder(true)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val performanceConsent =
            GDPRConsent
                .builder(true)
                .document("parental_consent_agreement_v2")
                .location("17 Cherry Tree Lan 3")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Marketing", marketingConsent)
                .addGDPRConsentState("Performance", performanceConsent)
                .build()
        kit.onConsentStateUpdated(state, state)
        TestCase.assertEquals(true, currentUser.getCustomUserAttribute()["\$google_ad_user_data"])
        TestCase.assertEquals(
            true,
            currentUser.getCustomUserAttribute()["\$google_ad_personalization"],
        )
    }

    @Test
    fun onConsentStateUpdatedTest_When_No_DATA_From_Server() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser
        kit.configuration = MockKitConfiguration()
        val marketingConsent =
            GDPRConsent
                .builder(true)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val performanceConsent =
            GDPRConsent
                .builder(true)
                .document("parental_consent_agreement_v2")
                .location("17 Cherry Tree Lan 3")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Marketing", marketingConsent)
                .addGDPRConsentState("Performance", performanceConsent)
                .build()
        kit.onConsentStateUpdated(state, state)
        TestCase.assertEquals(0, currentUser.getCustomUserAttribute().size)
    }

    @Test
    fun testOnConsentStateUpdatedTest_No_consentMappingSDK() {
        val kit = MockAppboyKit()
        val currentUser = braze.currentUser
        kit.configuration = MockKitConfiguration()
        val map = java.util.HashMap<String, String>()
        map["includeEnrichedUserAttributes"] = "True"
        map["userIdentificationType"] = "MPID"
        map["ABKDisableAutomaticLocationCollectionKey"] = "False"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

        val marketingConsent =
            GDPRConsent
                .builder(true)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val performanceConsent =
            GDPRConsent
                .builder(true)
                .document("parental_consent_agreement_v2")
                .location("17 Cherry Tree Lan 3")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Marketing", marketingConsent)
                .addGDPRConsentState("Performance", performanceConsent)
                .build()
        kit.onConsentStateUpdated(state, state)

        TestCase.assertEquals(0, currentUser.getCustomUserAttribute().size)
    }

    @Test
    fun testPurchase_Forward_product_name() {
        var settings = HashMap<String, String?>()
        settings[AppboyKit.APPBOY_KEY] = "key"
        settings[AppboyKit.REPLACE_SKU_AS_PRODUCT_NAME] = "True"
        val kit = MockAppboyKit()

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", settings))
        kit.onKitCreate(settings, MockContextApplication())
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.CHECKOUT, product)
                .currency("Moon Dollars")
                .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("product name", purchase.sku)
        Assert.assertNull(
            purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE],
        )
    }

    @Test
    fun testPurchase_Forward_product_name_When_flag_IS_FALSE() {
        var settings = HashMap<String, String?>()
        settings[AppboyKit.APPBOY_KEY] = "key"
        settings[AppboyKit.REPLACE_SKU_AS_PRODUCT_NAME] = "False"
        val kit = MockAppboyKit()

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", settings))
        kit.onKitCreate(settings, MockContextApplication())
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.CHECKOUT, product)
                .currency("Moon Dollars")
                .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("sku1", purchase.sku)
        Assert.assertNull(
            purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE],
        )
    }

    @Test
    fun testPurchase_Forward_product_name_When_flag_IS_Null() {
        var settings = HashMap<String, String?>()
        settings[AppboyKit.APPBOY_KEY] = "key"
        val kit = MockAppboyKit()

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", settings))
        kit.onKitCreate(settings, MockContextApplication())
        val product =
            Product
                .Builder("product name", "sku1", 4.5)
                .build()
        val commerceEvent =
            CommerceEvent
                .Builder(Product.CHECKOUT, product)
                .currency("Moon Dollars")
                .build()
        kit.logTransaction(commerceEvent, product)
        val braze = Braze
        val purchases = braze.purchases
        Assert.assertEquals(1, purchases.size.toLong())
        val purchase = purchases[0]
        Assert.assertEquals("sku1", purchase.sku)
        Assert.assertNull(
            purchase.purchaseProperties.properties[CommerceEventUtils.Constants.ATT_ACTION_CURRENCY_CODE],
        )
    }
}
