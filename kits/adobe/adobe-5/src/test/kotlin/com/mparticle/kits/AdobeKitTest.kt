package com.mparticle.kits

import android.content.Context
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.kits.AdobeKitBase
import com.mparticle.kits.KitIntegration
import com.mparticle.kits.KitIntegrationFactory
import com.mparticle.kits.KitManagerImpl
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import java.util.Arrays
import java.util.HashMap

class AdobeKitTest {
    private val kit: AdobeKit
        get() = AdobeKit()

    @Test
    fun testGetName() {
        val name = kit.name
        assertTrue(name.isNotEmpty())
    }

    /**
     * Kit *should* throw an exception when they're initialized with the wrong settings.
     *
     */
    @Test
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

        assertNotNull(e)
    }

    @Test
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
    fun testBuildUrl() {
        val url =
            kit.encodeIds(
                "<MCID>",
                "<ORG ID>",
                "<BLOB>",
                "<REGION>",
                "<PUSH TOKEN>",
                "<GAID>",
                HashMap(),
            )
        val testUrl1 =
            "d_mid=<MCID>&d_ver=2&d_orgid=<ORG ID>&d_cid=20914%01<GAID>&d_cid=20919%01<PUSH TOKEN>&dcs_region=<REGION>&d_blob=<BLOB>&d_ptfm=android"
        assertEqualUnorderedUrlParams(url, testUrl1)

        val userIdentities = HashMap<MParticle.IdentityType, String>()
        userIdentities[MParticle.IdentityType.CustomerId] = "<CUSTOMER ID>"
        userIdentities[MParticle.IdentityType.Email] = "<EMAIL>"
        val url2 =
            kit.encodeIds(
                "<MCID>",
                "<ORG ID>",
                "<BLOB>",
                "<REGION>",
                "<PUSH TOKEN>",
                "<GAID>",
                userIdentities,
            )
        val testUrls2 =
            "d_mid=<MCID>&d_ver=2&d_orgid=<ORG ID>&d_cid=20914%01<GAID>&d_cid=20919%01<PUSH TOKEN>&dcs_region=<REGION>&d_blob=<BLOB>&d_ptfm=android&d_cid_ic=customerid%01<CUSTOMER ID>&d_cid_ic=email%01<EMAIL>"
        assertEqualUnorderedUrlParams(url2, testUrls2)
    }

    @Test
    fun testGetUrlInstance() {
        val kit = kit
        kit.kitManager = Mockito.mock(KitManagerImpl::class.java)
        val integrationAttributes = HashMap<String, String>()
        integrationAttributes[AdobeKitBase.MARKETING_CLOUD_ID_KEY] = "foo"
        Mockito
            .`when`(
                kit.kitManager
                    .getIntegrationAttributes(
                        Mockito.any(KitIntegration::class.java),
                    ),
            ).thenReturn(integrationAttributes)

        val settings = HashMap<String, String>()
        settings[AdobeKitBase.AUDIENCE_MANAGER_SERVER] = "some.random.url"
        kit.onKitCreate(settings, Mockito.mock(Context::class.java))

        val url = kit.url
        assertEquals(url, "some.random.url")
    }

    private fun assertEqualUnorderedUrlParams(
        url1: String?,
        url2: String?,
    ) {
        if (url1 == null && url2 == null) {
            return
        }
        val url1Split =
            Arrays.asList(
                *url1!!
                    .split("&".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray(),
            )
        val url2Split =
            Arrays.asList(
                *url2!!
                    .split("&".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray(),
            )
        assertEquals(url1Split.size.toLong(), url2Split.size.toLong())
        url1Split.sort()
        url2Split.sort()
        for (i in url1Split.indices) {
            if (url1Split[i] != url2Split[i]) {
                assertTrue(false)
            }
        }
    }
}
