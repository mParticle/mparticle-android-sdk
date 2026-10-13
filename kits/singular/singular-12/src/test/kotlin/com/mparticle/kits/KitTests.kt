package com.mparticle.kits

import com.mparticle.MPEvent
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.consent.CCPAConsent
import com.mparticle.consent.ConsentState
import com.singular.sdk.Singular
import com.singular.sdk.SingularConfig
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.modules.junit4.rule.PowerMockRule

@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*")
class KitTests {
    //region Tests Setup
    private var kit: MockSingularKit? = null
    private lateinit var settings: HashMap<String, String>
    private val config: SingularConfig? = null

    @get:Rule
    var rule = PowerMockRule()

    @get:Rule
    var exception: ExpectedException = ExpectedException.none()

    @Mock
    private val reportingMessage: ReportingMessage? = null

    @Before
    fun setUp() {
        // MockitoAnnotations.initMocks(this);
        kit = MockSingularKit()
        settings = HashMap()
        settings[API_KEY] = "Test"
        settings[API_SECRET] = "Test"
    }

    //endregion
    //region Config Tests
    @Test
    fun buildConfigWithoutSettings() {
        val testConfig = kit?.buildSingularConfig(null)
        Assert.assertNull(testConfig)
    }

    @Test
    fun buildConfigWithEmptySettings() {
        val testConfig = kit?.buildSingularConfig(HashMap())
        Assert.assertNull(testConfig)
    }

    //endregion
    //region Log MPEvent Tests
    @Test
    fun logEventWithInfo() {
        var result: List<ReportingMessage?>? = null
        try {
            // Creating the event
            val eventJson = JSONObject()
            eventJson.put("eventName", "Testing")
            eventJson.put("eventType", "Unknown")
            val event = MPEvent.Builder.parseString(eventJson.toString())?.build()

            // Mocking the Kit Methods
            Singular.acceptEvent = true
            result = event?.let { kit?.logEvent(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(
                String.format(
                    "logEventWithInfo failed with exception message:%s",
                    e.message,
                ),
            )
        } finally {
            Assert.assertTrue(!result.isNullOrEmpty())
        }
    }

    @Test
    fun logEventWithoutInfo() {
        var result: List<ReportingMessage?>? = null
        try {
            // Creating the event
            val eventJson = JSONObject()
            eventJson.put("eventName", "Testing")
            eventJson.put("eventType", "Unknown")
            val event = MPEvent.Builder.parseString(eventJson.toString())?.build()
            if (event != null) {
                event.customAttributes?.clear()

                // Mocking the kit Methods
                Singular.acceptEvent = true
                result = kit?.logEvent(event)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(
                String.format(
                    "logEventWithInfo failed with exception message:%s",
                    e.message,
                ),
            )
        } finally {
            Assert.assertTrue(!result.isNullOrEmpty())
        }
    }

    @Test
    fun logEventWithInvalidData() {
        val event: MPEvent? = null
        val result = event?.let { kit?.logEvent(it) }
        if (result != null) {
            Assert.assertTrue(result.isEmpty())
        }
    }

    //endregion
    //region Log CommerceEvent Tests
    @Test
    fun logCommercePurchaseEvents() {
        var result: List<ReportingMessage?>? = null
        try {
            val commerceEvent =
                CommerceEvent
                    .Builder(
                        Product.PURCHASE,
                        Product
                            .Builder("Testing", "Unknown", 2.0)
                            .quantity(1.0)
                            .category("Category")
                            .build(),
                    ).addProduct(Product.Builder("Unknown", "b", 1.0).build())
                    .build()
            result = kit?.logEvent(commerceEvent)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(
                String.format(
                    "logCommercePurchaseEvents failed with exception message:%s",
                    e.message,
                ),
            )
        } finally {
            Assert.assertTrue(!result.isNullOrEmpty())
        }
    }

    @Test
    fun logCommerceNonPurchaseEvents() {
        var result: List<ReportingMessage?>? = null
        try {
            val commerceEvent =
                CommerceEvent
                    .Builder(
                        Product.DETAIL,
                        Product
                            .Builder("Testing", "Unknown", 2.0)
                            .quantity(1.0)
                            .category("Category")
                            .build(),
                    ).addProduct(Product.Builder("Unknown", "b", 1.0).build())
                    .build()
            result = kit?.logEvent(commerceEvent)
        } catch (e: Exception) {
            e.printStackTrace()
            Assert.fail(
                String.format(
                    "logCommerceNonPurchaseEvents failed with exception message:%s",
                    e.message,
                ),
            )
        } finally {
            Assert.assertTrue(!result.isNullOrEmpty())
        }
    }

    //endregion
    //region Consent Tests
    @Test
    fun onConsentStateUpdatedForwardsNewCcpaState() {
        val oldState =
            ConsentState
                .builder()
                .setCCPAConsentState(CCPAConsent.builder(true).build())
                .build()
        val newState =
            ConsentState
                .builder()
                .setCCPAConsentState(CCPAConsent.builder(false).build())
                .build()
        val initialized = SingularKit::class.java.getDeclaredField("isInitialized")
        initialized.isAccessible = true
        initialized.set(kit, true)
        Singular.limitDataSharingValue = null

        kit?.onConsentStateUpdated(oldState, newState)

        Assert.assertEquals(false, Singular.limitDataSharingValue)
    }

    //endregion
    //region MParticle Kit Factory Tests
    //endregion
    @Throws(Exception::class)
    @Test
    fun isSingularIntegrationInFactory() {
        val options = Mockito.mock(MParticleOptions::class.java)
        val factory = KitIntegrationFactory(options)
        val integrations = factory.supportedKits.values
        val className = kit?.javaClass?.name.orEmpty()
        for (integration in integrations) {
            if (integration.name.replace("SingularKit", "MockSingularKit") == className) {
                return
            }
        }
        Assert.fail("$className not found as a known integration.")
    }

    companion object {
        private const val API_KEY = "apiKey"
        private const val API_SECRET = "secret"
        private const val DDL_TIME_OUT = "ddlTimeout"
    }
}
