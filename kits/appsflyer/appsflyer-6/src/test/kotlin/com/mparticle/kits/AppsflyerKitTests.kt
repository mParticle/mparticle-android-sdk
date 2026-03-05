package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.appsflyer.AppsFlyerLib
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import junit.framework.Assert.assertEquals
import junit.framework.TestCase
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import java.lang.ref.WeakReference
import java.lang.reflect.Method

class AppsflyerKitTests {
    private var kit = AppsFlyerKit()
    private var appsflyer = AppsFlyerLib()

    @Mock
    lateinit var filteredMParticleUser: FilteredMParticleUser

    @Mock
    lateinit var user: MParticleUser

    @Before
    @Throws(JSONException::class)
    fun before() {
        AppsFlyerLib.clearInstance()
        kit = AppsFlyerKit()
        MockitoAnnotations.initMocks(this)
        MParticle.setInstance(mock(MParticle::class.java))
        Mockito
            .`when`(MParticle.getInstance()?.Identity())
            .thenReturn(
                mock(
                    IdentityApi::class.java,
                ),
            )
        val kitManager =
            KitManagerImpl(
                mock(
                    Context::class.java,
                ),
                null,
                emptyCoreCallbacks,
                mock(MParticleOptions::class.java),
            )
        kit.kitManager = kitManager
        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("id", "-1"))
        appsflyer = AppsFlyerLib.getInstance(null)!!
    }

    @Test
    @Throws(Exception::class)
    fun testGetName() {
        val name = kit.name
        Assert.assertTrue(name.isNotEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testParseSharingFilterForPartners_returnsListForValidJson() {
        val method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseSharingFilterForPartners",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, """["partner_1", "partner_2"]""")
        Assert.assertEquals(listOf("partner_1", "partner_2"), result)
    }

    @Test
    @Throws(Exception::class)
    fun testParseSharingFilterForPartners_returnsNullForEmptyInput() {
        val method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseSharingFilterForPartners",
                String::class.java,
            )
        method.isAccessible = true
        Assert.assertNull(method.invoke(kit, ""))
        Assert.assertNull(method.invoke(kit, null))
    }

    @Test
    @Throws(Exception::class)
    fun testParseSharingFilterForPartners_returnsNullForInvalidJson() {
        val method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseSharingFilterForPartners",
                String::class.java,
            )
        method.isAccessible = true
        Assert.assertNull(method.invoke(kit, "not a json array"))
    }

    @Test
    @Throws(Exception::class)
    fun testParseSharingFilterForPartners_stripsBackslashes() {
        val method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseSharingFilterForPartners",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, """[\"test_1\", \"test_2\"]""")
        Assert.assertEquals(listOf("test_1", "test_2"), result)
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
    @Throws(Exception::class)
    fun testOnKitCreate() {
        var e: Throwable? = null
        try {
            val settings = HashMap<String, String>()
            settings["fake setting"] = "fake"
            kit.onKitCreate(settings as Map<String?, String?>, mock(Context::class.java))
        } catch (ex: Throwable) {
            e = ex
        }
        Assert.assertNotNull(e)
    }

    @Test
    @Throws(Exception::class)
    fun testClassName() {
        val options = mock(MParticleOptions::class.java)
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
    fun testGenerateSkuString() {
        MParticle.setInstance(mock(MParticle::class.java))
        Mockito
            .`when`(MParticle.getInstance()?.environment)
            .thenReturn(MParticle.Environment.Production)
        Assert.assertNull(AppsFlyerKit.generateProductIdList(null))
        val product = Product.Builder("foo-name", "foo-sku", 50.0).build()
        val event =
            CommerceEvent
                .Builder(Product.PURCHASE, product)
                .transactionAttributes(TransactionAttributes("foo"))
                .build()
        assertEquals(mutableListOf("foo-sku"), AppsFlyerKit.generateProductIdList(event))
        val product2 = Product.Builder("foo-name-2", "foo-sku-2", 50.0).build()
        val event2 =
            CommerceEvent
                .Builder(Product.PURCHASE, product)
                .addProduct(product2)
                .transactionAttributes(TransactionAttributes("foo"))
                .build()
        assertEquals(
            mutableListOf("foo-sku", "foo-sku-2"),
            AppsFlyerKit.generateProductIdList(event2),
        )
        val product3 = Product.Builder("foo-name-3", "foo-sku-,3", 50.0).build()
        val event3 =
            CommerceEvent
                .Builder(Product.PURCHASE, product)
                .addProduct(product2)
                .addProduct(product3)
                .transactionAttributes(TransactionAttributes("foo"))
                .build()
        assertEquals(
            mutableListOf("foo-sku", "foo-sku-2", "foo-sku-%2C3"),
            AppsFlyerKit.generateProductIdList(event3),
        )
    }

    @Test
    @Throws(Exception::class)
    fun testConsentWhenGDPRNotApplied() {
        val map = HashMap<String?, String?>()
        map["defaultAdStorageConsent"] = "Granted"
        map["gdprApplies"] = "false"
        map["consentMapping"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsent"] = "Denied"
        map["defaultAdPersonalizationConsent"] = "Denied"

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(false, expectedConsentValue)

        val notExpectedConsentKey =
            afConsentResults.containsKey("hasConsentForDataUsage")
        TestCase.assertEquals(false, notExpectedConsentKey)

        val notExpectedConsentKey2 =
            afConsentResults.containsKey("hasConsentForAdsPersonalization")
        TestCase.assertEquals(false, notExpectedConsentKey2)

        val notExpectedConsentKey3 =
            afConsentResults.containsKey("hasConsentForAdStorage")
        TestCase.assertEquals(false, notExpectedConsentKey3)
    }

    @Test
    @Throws(Exception::class)
    fun testConsentWhenGDPRAppliedWithoutConsentDefaults() {
        val map = HashMap<String?, String?>()
        map["defaultAdStorageConsent"] = "Unspecified"
        map["gdprApplies"] = "true"
        map["consentMapping"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsent"] = "Unspecified"
        map["defaultAdPersonalizationConsent"] = "Unspecified"

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

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
                .addGDPRConsentState("test1", marketingConsent)
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(true, expectedConsentValue)

        val notExpectedConsentKey =
            afConsentResults.containsKey("hasConsentForDataUsage")
        TestCase.assertEquals(false, notExpectedConsentKey)

        val notExpectedConsentKey2 =
            afConsentResults.containsKey("hasConsentForAdsPersonalization")
        TestCase.assertEquals(false, notExpectedConsentKey2)

        val notExpectedConsentKey3 =
            afConsentResults.containsKey("hasConsentForAdStorage")
        TestCase.assertEquals(false, notExpectedConsentKey3)
    }

    @Test
    @Throws(Exception::class)
    fun testConsentWhenGDPRAppliedWithConsentDefaults() {
        val map = HashMap<String?, String?>()
        map["defaultAdStorageConsent"] = "Granted"
        map["gdprApplies"] = "true"
        map["consentMapping"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsent"] = "Denied"
        map["defaultAdPersonalizationConsent"] = "Granted"

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

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
                .addGDPRConsentState("test1", marketingConsent)
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(true, expectedConsentValue)

        val expectedConsentValue2 =
            afConsentResults
                .getValue("hasConsentForDataUsage")
        TestCase.assertEquals(false, expectedConsentValue2)

        val expectedConsentValue3 =
            afConsentResults
                .getValue("hasConsentForAdsPersonalization")
        TestCase.assertEquals(true, expectedConsentValue3)

        val expectedConsentValue4 =
            afConsentResults
                .getValue("hasConsentForAdStorage")
        TestCase.assertEquals(true, expectedConsentValue4)
    }

    @Test
    @Throws(Exception::class)
    fun testConsentMapping() {
        val map = HashMap<String?, String?>()
        map["defaultAdStorageConsent"] = "Granted"
        map["gdprApplies"] = "true"
        map["consentMapping"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsent"] = "Denied"
        map["defaultAdPersonalizationConsent"] = "Granted"

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

        val performanceConsent =
            GDPRConsent
                .builder(true)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()
        val marketingConsent =
            GDPRConsent
                .builder(false)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()
        val testConsent =
            GDPRConsent
                .builder(false)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()
        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Performance", performanceConsent)
                .addGDPRConsentState("Marketing", marketingConsent)
                .addGDPRConsentState("testconsent", testConsent)
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(true, expectedConsentValue)

        val expectedConsentValue2 =
            afConsentResults
                .getValue("hasConsentForDataUsage")
        TestCase.assertEquals(true, expectedConsentValue2)

        val expectedConsentValue3 =
            afConsentResults
                .getValue("hasConsentForAdsPersonalization")
        TestCase.assertEquals(false, expectedConsentValue3)

        val expectedConsentValue4 =
            afConsentResults
                .getValue("hasConsentForAdStorage")
        TestCase.assertEquals(false, expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTestPerformance_And_Marketing_are_true() {
        val map = HashMap<String?, String?>()
        map["defaultAdStorageConsent"] = "Granted"
        map["gdprApplies"] = "true"
        map["consentMapping"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsent"] = "Denied"
        map["defaultAdPersonalizationConsent"] = "Granted"

        kit.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

        val performanceConsent =
            GDPRConsent
                .builder(true)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val marketingConsent =
            GDPRConsent
                .builder(true)
                .document("Test consent")
                .location("17 Cherry Tree Lane")
                .hardwareId("IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702")
                .build()

        val state =
            ConsentState
                .builder()
                .addGDPRConsentState("Performance", performanceConsent)
                .addGDPRConsentState("Marketing", marketingConsent)
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(true, expectedConsentValue)

        val expectedConsentValue2 =
            afConsentResults
                .getValue("hasConsentForDataUsage")
        TestCase.assertEquals(true, expectedConsentValue2)

        val expectedConsentValue3 =
            afConsentResults
                .getValue("hasConsentForAdsPersonalization")
        TestCase.assertEquals(true, expectedConsentValue3)

        val expectedConsentValue4 =
            afConsentResults
                .getValue("hasConsentForAdStorage")
        TestCase.assertEquals(true, expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTest_When_No_Defaults_Values() {
        val map = HashMap<String, String>()
        map["gdprApplies"] = "true"
        map["consentMapping"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"

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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)
        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(true, expectedConsentValue)

        val expectedConsentValue2 =
            afConsentResults
                .getValue("hasConsentForDataUsage")
        TestCase.assertEquals(true, expectedConsentValue2)

        val expectedConsentValue3 =
            afConsentResults
                .getValue("hasConsentForAdsPersonalization")
        TestCase.assertEquals(true, expectedConsentValue3)

        val notExpectedConsentKey =
            afConsentResults.containsKey("hasConsentForAdStorage")
        TestCase.assertEquals(false, notExpectedConsentKey)
    }

    @Test
    fun onConsentStateUpdatedTest_When_No_DATA_From_Server() {
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        TestCase.assertEquals(0, appsflyer.getConsentState().size)
    }

    @Test
    fun onConsentStateUpdatedTest_No_consentMappingSDK() {
        val map = HashMap<String, String>()
        map["gdprApplies"] = "true"
        map["defaultAdStorageConsent"] = "Granted"
        map["defaultAdUserDataConsent"] = "Denied"
        map["defaultAdPersonalizationConsent"] = "Denied"

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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(true, expectedConsentValue)

        val expectedConsentValue2 =
            afConsentResults
                .getValue("hasConsentForDataUsage")
        TestCase.assertEquals(false, expectedConsentValue2)

        val expectedConsentValue3 =
            afConsentResults
                .getValue("hasConsentForAdsPersonalization")
        TestCase.assertEquals(false, expectedConsentValue3)

        val expectedConsentValue4 =
            afConsentResults
                .getValue("hasConsentForAdStorage")
        TestCase.assertEquals(true, expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTest_When_default_is_Unspecified_And_No_consentMappingSDK_And_GDPR_Not_Applied() {
        val map = HashMap<String, String>()
        map["gdprApplies"] = "false"
        map["defaultAdStorageConsent"] = "Unspecified"
        map["defaultAdUserDataConsent"] = "Unspecified"
        map["defaultAdPersonalizationConsent"] = "Unspecified"

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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kit)

        kit.onConsentStateUpdated(state, state, filteredMParticleUser)

        TestCase.assertEquals(1, appsflyer.getConsentState().size)
        val afConsentResults = appsflyer.getConsentState()
        val expectedConsentValue =
            afConsentResults
                .getValue("isUserSubjectToGDPR")
        TestCase.assertEquals(false, expectedConsentValue)
    }

    @Test
    fun testParseToNestedMap_When_JSON_Is_INVALID() {
        var jsonInput =
            "{'GDPR':{'marketing':'{:false,'timestamp':1711038269644:'Test consent','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}','performance':'{'consented':true,'timestamp':1711038269644,'document':'parental_consent_agreement_v2','location':'17 Cherry Tree Lan 3','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'},'CCPA':'{'consented':true,'timestamp':1711038269644,'document':'ccpa_consent_agreement_v3','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}"

        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseToNestedMap",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, jsonInput)
        Assert.assertEquals(mutableMapOf<String, Any>(), result)
    }

    @Test
    fun testParseToNestedMap_When_JSON_Is_Empty() {
        var jsonInput = ""

        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseToNestedMap",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, jsonInput)
        Assert.assertEquals(mutableMapOf<String, Any>(), result)
    }

    @Test
    fun testSearchKeyInNestedMap_When_Input_Key_Is_Empty_String() {
        val map =
            mapOf(
                "GDPR" to true,
                "marketing" to
                    mapOf(
                        "consented" to false,
                        "document" to
                            mapOf(
                                "timestamp" to 1711038269644,
                            ),
                    ),
            )
        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
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
        val emptyMap: Map<String, Int> = emptyMap()
        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
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
        val emptyJson = ""
        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, emptyJson)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_Invalid_Json() {
        var jsonInput =
            "{'GDPR':{'marketing':'{:false,'timestamp':1711038269644:'Test consent','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}','performance':'{'consented':true,'timestamp':1711038269644,'document':'parental_consent_agreement_v2','location':'17 Cherry Tree Lan 3','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'},'CCPA':'{'consented':true,'timestamp':1711038269644,'document':'ccpa_consent_agreement_v3','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}"
        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, jsonInput)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_NULL() {
        val method: Method =
            AppsFlyerKit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kit, null)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    private var emptyCoreCallbacks: CoreCallbacks =
        object : CoreCallbacks {
            var activity = Activity()

            override fun isBackgrounded(): Boolean = false

            override fun getUserBucket(): Int = 0

            override fun isEnabled(): Boolean = false

            override fun setIntegrationAttributes(
                i: Int,
                map: Map<String, String>,
            ) {}

            override fun getIntegrationAttributes(i: Int): Map<String, String>? = null

            override fun getCurrentActivity(): WeakReference<Activity> = WeakReference(activity)

            override fun getLatestKitConfiguration(): JSONArray? = null

            override fun getDataplanOptions(): MParticleOptions.DataplanOptions? = null

            override fun isPushEnabled(): Boolean = false

            override fun getPushSenderId(): String? = null

            override fun getPushInstanceId(): String? = null

            override fun getLaunchUri(): Uri? = null

            override fun getLaunchAction(): String? = null

            override fun getKitListener(): KitListener =
                object : KitListener {
                    override fun kitFound(kitId: Int) {}

                    override fun kitConfigReceived(
                        kitId: Int,
                        configuration: String?,
                    ) {}

                    override fun kitExcluded(
                        kitId: Int,
                        reason: String?,
                    ) {}

                    override fun kitStarted(kitId: Int) {}

                    override fun onKitApiCalled(
                        kitId: Int,
                        used: Boolean?,
                        vararg objects: Any?,
                    ) {}

                    override fun onKitApiCalled(
                        methodName: String?,
                        kitId: Int,
                        used: Boolean?,
                        vararg objects: Any?,
                    ) {
                    }
                }
        }
}
