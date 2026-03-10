package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Impression
import com.mparticle.commerce.Product
import com.mparticle.commerce.Promotion
import com.mparticle.commerce.TransactionAttributes
import com.mparticle.consent.ConsentState
import com.mparticle.consent.GDPRConsent
import com.mparticle.identity.IdentityApi
import com.mparticle.identity.MParticleUser
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import com.mparticle.testutils.TestingUtils
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
import java.lang.reflect.Modifier
import java.security.SecureRandom
import java.util.HashMap

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class GoogleAnalyticsFirebaseGA4KitTest {
    private lateinit var kitInstance: GoogleAnalyticsFirebaseGA4Kit
    private lateinit var firebaseSdk: FirebaseAnalytics
    private var random = SecureRandom()

    @Mock
    lateinit var user: MParticleUser

    @Mock
    lateinit var filteredMParticleUser: FilteredMParticleUser

    @Before
    @Throws(JSONException::class)
    fun before() {
        FirebaseAnalytics.clearInstance()
        FirebaseAnalytics.setFirebaseId("firebaseId")
        kitInstance = GoogleAnalyticsFirebaseGA4Kit()
        MockitoAnnotations.initMocks(this)
        MParticle.setInstance(Mockito.mock(MParticle::class.java))
        Mockito.`when`(MParticle.getInstance()?.Identity()).thenReturn(
            Mockito.mock(
                IdentityApi::class.java,
            ),
        )
        val kitManager =
            KitManagerImpl(
                Mockito.mock(
                    Context::class.java,
                ),
                null,
                emptyCoreCallbacks,
                mock(MParticleOptions::class.java),
            )
        kitInstance.kitManager = kitManager
        kitInstance.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("id", "-1"))
        firebaseSdk = FirebaseAnalytics.getInstance(null)!!
        GoogleAnalyticsFirebaseGA4Kit.setClientStandardizationCallback(null)
    }

    /**
     * make sure that all MPEvents are getting translating their getInfo() value to the bundle of the Firebase event.
     * MPEvent.getName() should be the firebase event name in all cases, except when the MPEvent.type is MPEvent.Search
     */
    @Test
    fun testEmptyEvent() {
        kitInstance.logEvent(MPEvent.Builder("eventName", MParticle.EventType.Other).build())
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        var firebaseEvent = firebaseSdk.loggedEvents[0]
        TestCase.assertEquals("eventName", firebaseEvent.key)
        TestCase.assertEquals(0, firebaseEvent.value.size())

        for (i in 0..9) {
            val event = TestingUtils.getInstance().randomMPEventRich
            firebaseSdk.clearLoggedEvents()
            kitInstance.logEvent(event)
            TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
            firebaseEvent = firebaseSdk.loggedEvents[0]
            if (event.eventType != MParticle.EventType.Search) {
                TestCase.assertEquals(
                    kitInstance.standardizeName(event.eventName, true),
                    firebaseEvent.key,
                )
            } else {
                TestCase.assertEquals("search", firebaseEvent.key)
            }
            event.customAttributeStrings?.let {
                TestCase.assertEquals(
                    it.size,
                    firebaseEvent.value.size(),
                )
                for (customAttEvent in it) {
                    val key = kitInstance.standardizeName(customAttEvent.key, true)
                    val value = kitInstance.standardizeValue(customAttEvent.value, true)
                    if (key != null) {
                        TestCase.assertEquals(
                            value,
                            firebaseEvent.value.getString(
                                key,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testPromotionCommerceEvent() {
        val promotion = Promotion()
        promotion.creative = "asdva"
        promotion.id = "1234"
        promotion.name = "1234asvd"
        promotion.position = "2"
        val event = CommerceEvent.Builder(Promotion.CLICK, promotion).build()
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
    }

    @Test
    fun onConsentStateUpdatedTest() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Granted"
        map["defaultAnalyticsStorageConsentSDK"] = "Granted"
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsentSDK"] = "Denied"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kitInstance.configuration =
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue2)
        val expectedConsentValue3 =
            firebaseSdk.getConsentState().getKeyByValue("ANALYTICS_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue3)
        val expectedConsentValue4 =
            firebaseSdk.getConsentState().getKeyByValue("AD_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTest_When_Marketing_true() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Granted"
        map["defaultAnalyticsStorageConsentSDK"] = "Granted"
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsentSDK"] = "Denied"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kitInstance.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

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
                .addGDPRConsentState("Marketing", marketingConsent)
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue2)
        val expectedConsentValue3 =
            firebaseSdk.getConsentState().getKeyByValue("ANALYTICS_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue3)
        val expectedConsentValue4 =
            firebaseSdk.getConsentState().getKeyByValue("AD_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTest_When_Performance_true() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Granted"
        map["defaultAnalyticsStorageConsentSDK"] = "Granted"
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsentSDK"] = "Denied"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kitInstance.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

        val performanceConsent =
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
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue2)
        val expectedConsentValue3 =
            firebaseSdk.getConsentState().getKeyByValue("ANALYTICS_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue3)
        val expectedConsentValue4 =
            firebaseSdk.getConsentState().getKeyByValue("AD_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTest_When_Performance_false() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Granted"
        map["defaultAnalyticsStorageConsentSDK"] = "Granted"
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsentSDK"] = "Denied"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kitInstance.configuration =
            KitConfiguration.createKitConfiguration(JSONObject().put("as", map.toMutableMap()))

        val performanceConsent =
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
                .build()
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue2)
        val expectedConsentValue3 =
            firebaseSdk.getConsentState().getKeyByValue("ANALYTICS_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue3)
        val expectedConsentValue4 =
            firebaseSdk.getConsentState().getKeyByValue("AD_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTestPerformance_And_Marketing_are_true() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Granted"
        map["defaultAnalyticsStorageConsentSDK"] = "Granted"
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsentSDK"] = "Denied"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kitInstance.configuration =
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue2)
        val expectedConsentValue3 =
            firebaseSdk.getConsentState().getKeyByValue("ANALYTICS_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue3)
        val expectedConsentValue4 =
            firebaseSdk.getConsentState().getKeyByValue("AD_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue4)
    }

    @Test
    fun onConsentStateUpdatedTest_When_No_Defaults_Values() {
        val map = HashMap<String, String>()
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"

        kitInstance.configuration =
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue2)
        TestCase.assertEquals(2, firebaseSdk.getConsentState().size)
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        TestCase.assertEquals(0, firebaseSdk.getConsentState().size)
    }

    @Test
    fun onConsentStateUpdatedTest_No_consentMappingSDK() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Granted"
        map["defaultAnalyticsStorageConsentSDK"] = "Granted"
        map["defaultAdUserDataConsentSDK"] = "Denied"
        map["defaultAdPersonalizationConsentSDK"] = "Denied"

        kitInstance.configuration =
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("ANALYTICS_STORAGE").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue2)
        val expectedConsentValue3 =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue3)
        val expectedConsentValue4 =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("DENIED", expectedConsentValue4)
        TestCase.assertEquals(4, firebaseSdk.getConsentState().size)
    }

    @Test
    fun onConsentStateUpdatedTest_When_default_is_Unspecified_And_No_consentMappingSDK() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Unspecified"
        map["defaultAnalyticsStorageConsentSDK"] = "Unspecified"
        map["defaultAdUserDataConsentSDK"] = "Unspecified"
        map["defaultAdPersonalizationConsentSDK"] = "Unspecified"

        kitInstance.configuration =
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        TestCase.assertEquals(0, firebaseSdk.getConsentState().size)
    }

    @Test
    fun onConsentStateUpdatedTest_When_default_is_Unspecified() {
        val map = HashMap<String, String>()
        map["defaultAdStorageConsentSDK"] = "Unspecified"
        map["defaultAnalyticsStorageConsentSDK"] = "Unspecified"
        map["consentMappingSDK"] =
            "[{\\\"jsmap\\\":null,\\\"map\\\":\\\"Performance\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_user_data\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"Marketing\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_personalization\\\"},{\\\"jsmap\\\":null,\\\"map\\\":\\\"testconsent\\\",\\\"maptype\\\":\\\"ConsentPurposes\\\",\\\"value\\\":\\\"ad_storage\\\"}]"
        map["defaultAdUserDataConsentSDK"] = "Unspecified"
        map["defaultAdPersonalizationConsentSDK"] = "Unspecified"

        kitInstance.configuration =
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
        filteredMParticleUser = FilteredMParticleUser.getInstance(user, kitInstance)

        kitInstance.onConsentStateUpdated(state, state, filteredMParticleUser)

        val expectedConsentValue =
            firebaseSdk.getConsentState().getKeyByValue("AD_USER_DATA").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue)
        val expectedConsentValue2 =
            firebaseSdk.getConsentState().getKeyByValue("AD_PERSONALIZATION").toString()
        TestCase.assertEquals("GRANTED", expectedConsentValue2)
        TestCase.assertEquals(2, firebaseSdk.getConsentState().size)
    }

    fun MutableMap<Any, Any>.getKeyByValue(inputKey: String): Any? {
        for ((key, mapValue) in entries) {
            if (key.toString() == inputKey) {
                return mapValue
            }
        }
        return null
    }

    @Test
    fun testParseToNestedMap_When_JSON_Is_INVALID() {
        var jsonInput =
            "{'GDPR':{'marketing':'{:false,'timestamp':1711038269644:'Test consent','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}','performance':'{'consented':true,'timestamp':1711038269644,'document':'parental_consent_agreement_v2','location':'17 Cherry Tree Lan 3','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'},'CCPA':'{'consented':true,'timestamp':1711038269644,'document':'ccpa_consent_agreement_v3','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}"

        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "parseToNestedMap",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, jsonInput)
        Assert.assertEquals(mutableMapOf<String, Any>(), result)
    }

    @Test
    fun testParseToNestedMap_When_JSON_Is_Empty() {
        var jsonInput = ""

        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "parseToNestedMap",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, jsonInput)
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
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "searchKeyInNestedMap",
                Map::class.java,
                Any::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, map, "")
        Assert.assertEquals(null, result)
    }

    @Test
    fun testSearchKeyInNestedMap_When_Input_Is_Empty_Map() {
        val emptyMap: Map<String, Int> = emptyMap()
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "searchKeyInNestedMap",
                Map::class.java,
                Any::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, emptyMap, "1")
        Assert.assertEquals(null, result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_Empty_Json() {
        val emptyJson = ""
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, emptyJson)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_Invalid_Json() {
        var jsonInput =
            "{'GDPR':{'marketing':'{:false,'timestamp':1711038269644:'Test consent','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}','performance':'{'consented':true,'timestamp':1711038269644,'document':'parental_consent_agreement_v2','location':'17 Cherry Tree Lan 3','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'},'CCPA':'{'consented':true,'timestamp':1711038269644,'document':'ccpa_consent_agreement_v3','location':'17 Cherry Tree Lane','hardware_id':'IDFA:a5d934n0-232f-4afc-2e9a-3832d95zc702'}'}"
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, jsonInput)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testParseConsentMapping_When_Input_Is_NULL() {
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "parseConsentMapping",
                String::class.java,
            )
        method.isAccessible = true
        val result = method.invoke(kitInstance, null)
        Assert.assertEquals(emptyMap<String, String>(), result)
    }

    @Test
    fun testShippingInfoCommerceEvent() {
        val event =
            CommerceEvent
                .Builder(
                    Product.CHECKOUT_OPTION,
                    Product.Builder("asdv", "asdv", 1.3).build(),
                ).addCustomFlag(
                    "GA4.CommerceEventType",
                    FirebaseAnalytics.Event.ADD_SHIPPING_INFO,
                ).addCustomFlag("GA4.ShippingTier", "overnight")
                .build()
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals("add_shipping_info", firebaseSdk.loggedEvents[0].key)
        TestCase.assertEquals(
            "overnight",
            firebaseSdk.loggedEvents[0].value.getString("shipping_tier"),
        )
    }

    @Test
    fun testPaymentInfoCommerceEvent() {
        val commerceCustomAttributes =
            mapOf(
                "event::country" to "US",
            )
        val event =
            CommerceEvent
                .Builder(
                    Product.CHECKOUT_OPTION,
                    Product.Builder("asdv", "asdv", 1.3).build(),
                ).addCustomFlag(
                    "GA4.CommerceEventType",
                    FirebaseAnalytics.Event.ADD_PAYMENT_INFO,
                ).addCustomFlag("GA4.PaymentType", "visa")
                .build()
        event.customAttributes = commerceCustomAttributes
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals("add_payment_info", firebaseSdk.loggedEvents[0].key)
        TestCase.assertEquals("visa", firebaseSdk.loggedEvents[0].value.getString("payment_type"))
        TestCase.assertEquals("US", firebaseSdk.loggedEvents[0].value.getString("event__country"))
    }

    @Test
    fun testCheckoutOptionCommerceEvent() {
        val customEventTypes =
            arrayOf(
                FirebaseAnalytics.Event.ADD_PAYMENT_INFO,
                FirebaseAnalytics.Event.ADD_SHIPPING_INFO,
            )
        for (customEventType in customEventTypes) {
            val event =
                CommerceEvent
                    .Builder(
                        Product.CHECKOUT_OPTION,
                        Product.Builder("asdv", "asdv", 1.3).build(),
                    ).addCustomFlag(
                        "GA4.CommerceEventType",
                        customEventType,
                    ).build()
            kitInstance.logEvent(event)
            TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
            TestCase.assertEquals(customEventType, firebaseSdk.loggedEvents[0].key)
            firebaseSdk.clearLoggedEvents()
        }
    }

    @Test
    @Throws(IllegalAccessException::class)
    fun testCommerceEvent() {
        for (field in Product::class.java.fields) {
            if (Modifier.isPublic(field.modifiers) && Modifier.isStatic(field.modifiers)) {
                firebaseSdk.clearLoggedEvents()
                val eventType = field?.get(null).toString()
                if (eventType != "remove_from_wishlist" && eventType != "checkout_option") {
                    val event =
                        CommerceEvent
                            .Builder(
                                eventType,
                                Product.Builder("asdv", "asdv", 1.3).build(),
                            ).transactionAttributes(
                                TransactionAttributes()
                                    .setId("235")
                                    .setRevenue(23.3)
                                    .setAffiliation("231"),
                            ).build()
                    kitInstance.logEvent(event)
                    TestCase.assertEquals(
                        "failed for event type: $eventType",
                        1,
                        firebaseSdk.loggedEvents.size,
                    )
                }
            }
        }
    }

    @Test
    fun testNameStandardization() {
        val badPrefixes = arrayOf("firebase_event_name", "google_event_name", "ga_event_name")
        for (badPrefix in badPrefixes) {
            val clean = kitInstance.standardizeName(badPrefix, true)
            TestCase.assertEquals("event_name", clean)
        }
        val emptySpace1 = "event name"
        val emptySpace2 = "event_name "
        val emptySpace3 = "event  name "
        val emptySpace4 = "event - name "
        TestCase.assertEquals(
            "event_name",
            kitInstance.standardizeName(emptySpace1, true),
        )
        TestCase.assertEquals(
            "event_name_",
            kitInstance.standardizeName(emptySpace2, true),
        )
        TestCase.assertEquals(
            "event__name_",
            kitInstance.standardizeName(emptySpace3, true),
        )
        TestCase.assertEquals(
            "event___name_",
            kitInstance.standardizeName(emptySpace4, true),
        )
        TestCase.assertEquals(
            "event_name ",
            kitInstance.standardizeName(emptySpace2, false),
        )
        TestCase.assertEquals(
            "event  name ",
            kitInstance.standardizeName(emptySpace3, false),
        )
        TestCase.assertEquals(
            "event - name ",
            kitInstance.standardizeName(emptySpace4, false),
        )
        TestCase.assertEquals(
            "!event - name !",
            kitInstance.standardizeName("!event - name !", false),
        )
        TestCase.assertEquals(
            "!@#\$%^&*()_+=[]{}|'\"?>",
            kitInstance.standardizeName("!@#\$%^&*()_+=[]{}|'\"?>", false),
        )

        val badStarts =
            arrayOf(
                "!@#$%^&*()_+=[]{}|'\"?><:;event_name",
                "_event_name",
                "   event_name",
                "_event_name",
            )
        for (badStart in badStarts) {
            val clean = kitInstance.standardizeName(badStart, true)
            TestCase.assertEquals("event_name", clean)
        }
        val justFine =
            "abcdefghijklmnopqrstuvwx"
        var sanitized: String = kitInstance.standardizeName(justFine, true).toString()
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))
        sanitized = kitInstance.standardizeName(justFine, false).toString()
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(justFine, true)
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(justFine, false)
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(justFine.startsWith(sanitized))

        val tooLong =
            "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890abcdefghijklmnopqrstuvwxyz1234567890"
        sanitized = kitInstance.standardizeName(tooLong, true).toString()
        TestCase.assertEquals(40, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))
        sanitized = kitInstance.standardizeName(tooLong, false).toString()
        TestCase.assertEquals(24, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(tooLong, true)
        TestCase.assertEquals(500, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))
        sanitized = kitInstance.standardizeValue(tooLong, false)
        TestCase.assertEquals(36, sanitized.length)
        TestCase.assertTrue(tooLong.startsWith(sanitized))

        val emptyStrings =
            arrayOf(
                "!@#$%^&*()_+=[]{}|'\"?><:;",
                "_1234567890",
                " ",
                "",
            )
        for (emptyString in emptyStrings) {
            val empty = kitInstance.standardizeName(emptyString, true)
            TestCase.assertEquals("invalid_ga4_key", empty)
        }

        var callback =
            object : GoogleAnalyticsFirebaseGA4Kit.MPClientStandardization {
                override fun nameStandardization(name: String): String = "test"
            }

        GoogleAnalyticsFirebaseGA4Kit.setClientStandardizationCallback(callback)

        val clientTestStrings =
            arrayOf(
                "this",
                "is",
                "not",
                "a",
                "test",
            )
        for (clientString in clientTestStrings) {
            val client = kitInstance.standardizeName(clientString, true)
            TestCase.assertEquals("test", client)
        }

        var callbackCheck =
            object : GoogleAnalyticsFirebaseGA4Kit.MPClientStandardization {
                override fun nameStandardization(name: String): String = name
            }

        GoogleAnalyticsFirebaseGA4Kit.setClientStandardizationCallback(callbackCheck)

        val client = kitInstance.standardizeName("clientString", true)
        TestCase.assertEquals("clientString", client)
    }

    @Test
    fun testMaxStandardization() {
        val testSucccessAttributes =
            mapOf(
                "test1" to "parameter",
                "test2" to "parameter",
                "test3" to "parameter",
                "test4" to "parameter",
                "test5" to "parameter",
                "test6" to "parameter",
                "test7" to "parameter",
                "test8" to "parameter",
                "test9" to "parameter",
                "test10" to "parameter",
                "test11" to "parameter",
                "test12" to "parameter",
                "test13" to "parameter",
                "test14" to "parameter",
                "test15" to "parameter",
                "test16" to "parameter",
                "test17" to "parameter",
                "test18" to "parameter",
                "test19" to "parameter",
                "test20" to "parameter",
                "test21" to "parameter",
                "test22" to "parameter",
                "test23" to "parameter",
                "event::country" to "US",
            )
        val testTruncatedAttributes =
            mapOf(
                "test1" to "parameter",
                "test2" to "parameter",
                "test3" to "parameter",
                "test4" to "parameter",
                "test5" to "parameter",
                "test6" to "parameter",
                "test7" to "parameter",
                "test8" to "parameter",
                "test9" to "parameter",
                "test10" to "parameter",
                "test11" to "parameter",
                "test12" to "parameter",
                "test13" to "parameter",
                "test14" to "parameter",
                "test15" to "parameter",
                "test16" to "parameter",
                "test17" to "parameter",
                "test18" to "parameter",
                "test19" to "parameter",
                "test20" to "parameter",
                "test21" to "parameter",
                "test22" to "parameter",
                "test23" to "parameter",
                "event::country" to "US",
                "z1" to "parameter",
                "z2" to "parameter",
                "z3" to "parameter",
                "z4" to "parameter",
            )
        val testFinalAttributes =
            mapOf(
                "test1" to "parameter",
                "test2" to "parameter",
                "test3" to "parameter",
                "test4" to "parameter",
                "test5" to "parameter",
                "test6" to "parameter",
                "test7" to "parameter",
                "test8" to "parameter",
                "test9" to "parameter",
                "test10" to "parameter",
                "test11" to "parameter",
                "test12" to "parameter",
                "test13" to "parameter",
                "test14" to "parameter",
                "test15" to "parameter",
                "test16" to "parameter",
                "test17" to "parameter",
                "test18" to "parameter",
                "test19" to "parameter",
                "test20" to "parameter",
                "test21" to "parameter",
                "test22" to "parameter",
                "test23" to "parameter",
                "event__country" to "US",
                "currency" to "USD",
            )
        val event =
            CommerceEvent
                .Builder(
                    Product.ADD_TO_CART,
                    Product.Builder("asdv", "asdv", 1.3).build(),
                ).build()
        event.customAttributes = testSucccessAttributes
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals(
            testSucccessAttributes.size + 3,
            firebaseSdk.loggedEvents[0].value.size(),
        )
        firebaseSdk.clearLoggedEvents()

        event.customAttributes = testTruncatedAttributes
        kitInstance.logEvent(event)
        TestCase.assertEquals(1, firebaseSdk.loggedEvents.size)
        TestCase.assertEquals(
            testTruncatedAttributes.size + 3,
            firebaseSdk.loggedEvents[0].value.size(),
        )
        firebaseSdk.clearLoggedEvents()
    }

    @Test
    fun testProductCustomAttributes() {
        val attributes = hashMapOf<String, String>()
        attributes["productCustomAttribute"] = "potato"
        attributes["store"] = "Target"
        val product =
            Product
                .Builder("expensivePotato", "SKU123", 40.00)
                .quantity(1.0)
                .brand("LV")
                .category("vegetable")
                .position(4)
                .customAttributes(attributes)
                .build()

        Impression("Suggested Products List", product)
            .let {
                CommerceEvent.Builder(it).build()
            }.let {
                kitInstance.logEvent(it)
            }
        val firebaseImpressionEvent = firebaseSdk.loggedEvents[0]
        val firebaseProducts = firebaseImpressionEvent.value.get("items") as? Array<Bundle>
        val firebaseProduct = firebaseProducts?.get(0)

        // test the count of parameters for product, even though we only pass 2 custom attributes we
        // expect the total to be 9 to include name, price, quantity, sku, brand, category and position
        TestCase.assertEquals(
            9,
            firebaseProduct?.size(),
        )
    }

    @Test
    fun testStandardizeAttributes() {
        val attributeCopy =
            hashMapOf(
                "test" to "test",
                "test1" to "test1",
                "test2" to "test2",
                "test3" to "test4",
                "test5" to "test5",
            )
        val eventMaxParameterProperty = 3
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "limitAttributes",
                HashMap::class.java,
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true

        method.invoke(kitInstance, attributeCopy, eventMaxParameterProperty)
        Assert.assertEquals(eventMaxParameterProperty, attributeCopy.size)
    }

    @Test
    fun testStandardizeAttributes_attribute_isNull() {
        val attributeCopy = HashMap<String, String>()

        val eventMaxParameterProperty = 3
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "limitAttributes",
                HashMap::class.java,
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(kitInstance, null, eventMaxParameterProperty)
        Assert.assertEquals(0, attributeCopy.size)
    }

    @Test
    fun testStandardizeAttributes_attribute_empty() {
        val attributeCopy = HashMap<String, String>()
        val eventMaxParameterProperty = 3
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "limitAttributes",
                HashMap::class.java,
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(kitInstance, attributeCopy, eventMaxParameterProperty)
        Assert.assertEquals(0, attributeCopy.size)
    }

    @Test
    fun testStandardizeAttributes_attribute_less_than_max_size() {
        val attributeCopy =
            hashMapOf(
                "test" to "test",
                "test1" to "test1",
            )
        val eventMaxParameterProperty = 3
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "limitAttributes",
                HashMap::class.java,
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(kitInstance, attributeCopy, eventMaxParameterProperty)
        Assert.assertEquals(2, attributeCopy.size)
    }

    @Test
    fun testStandardizeAttributes_attribute_equal_to_max_size() {
        val attributeCopy =
            hashMapOf(
                "test" to "test",
                "test1" to "test1",
                "test2" to "test4",
            )
        val eventMaxParameterProperty = 3
        val method: Method =
            GoogleAnalyticsFirebaseGA4Kit::class.java.getDeclaredMethod(
                "limitAttributes",
                HashMap::class.java,
                Int::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(kitInstance, attributeCopy, eventMaxParameterProperty)
        Assert.assertEquals(eventMaxParameterProperty, attributeCopy.size)
    }

    @Test
    fun testScreenNameSanitized() {
        kitInstance.logScreen("Some long Screen name", null)
        val firebaseScreenViewEvent = firebaseSdk.loggedEvents[0]
        TestCase.assertEquals(
            "Some_long_Screen_name",
            firebaseScreenViewEvent.value.getString("screen_name"),
        )
    }

    @Test
    fun testScreenNameAttributes() {
        val attributes = hashMapOf<String, String>()
        attributes["testAttributeKey"] = "testAttributeValue"
        kitInstance.logScreen("testScreenName", attributes)
        val firebaseScreenViewEvent = firebaseSdk.loggedEvents[0]
        // even though we are passing one attribute, it should contain two including the screen_name
        TestCase.assertEquals(
            2,
            firebaseScreenViewEvent.value.size(),
        )
        // make sure the even name is correct with Firebase's constant SCREEN_NAME value
        TestCase.assertEquals(
            "screen_view",
            firebaseScreenViewEvent.key,
        )
        // make sure that the Params include the screenName value
        TestCase.assertEquals(
            "testScreenName",
            firebaseScreenViewEvent.value.getString("screen_name"),
        )
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

            override fun getDataplanOptions(): DataplanOptions? = null

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
                    ) {}
                }
        }
}
