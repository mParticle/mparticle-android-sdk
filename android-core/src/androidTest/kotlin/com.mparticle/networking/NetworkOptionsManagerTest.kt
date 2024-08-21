package com.mparticle.networking

import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import org.junit.Assert
import org.junit.Test

class NetworkOptionsManagerTest {
    @Test
    fun testEmptyNetworkOptions() {
        val networkOptions = NetworkOptions.builder().build()
        var refinedNetworkOptions = NetworkOptionsManager.validateAndResolve(networkOptions)
        val toString = refinedNetworkOptions.toString()
        Logger.error(toString)
        Assert.assertTrue(
            AccessUtils.equals(
                refinedNetworkOptions,
                NetworkOptionsManager.defaultNetworkOptions()
            )
        )
        refinedNetworkOptions = NetworkOptionsManager.validateAndResolve(null)
        Assert.assertTrue(
            AccessUtils.equals(
                refinedNetworkOptions,
                NetworkOptionsManager.defaultNetworkOptions()
            )
        )
        for (
        certificate in refinedNetworkOptions.domainMappings[MParticleBaseClientImpl.Endpoint.IDENTITY]!!
            .certificates
        ) {
            if (certificate.alias == "godaddy_root_g2") {
                Assert.assertEquals(certificate.certificate, Constants.GODADDY_ROOT_G2_CRT)
            } else if (certificate.alias == "godaddy_root_class2") {
                Assert.assertEquals(certificate.certificate, Constants.GODADDY_CLASS_2_ROOT_CRT)
            } else if (certificate.alias == "lets_encrypt_root_x1") {
                Assert.assertEquals(certificate.certificate, Constants.LETS_ENCRYPTS_ROOT_X1_CRT)
            } else if (certificate.alias == "lets_encrypt_root_x2_self") {
                Assert.assertEquals(certificate.certificate, Constants.LETS_ENCRYPTS_ROOT_X2_SELF_SIGN_CRT)
            } else if (certificate.alias == "lets_encrypt_root_x2_cross") {
                Assert.assertEquals(certificate.certificate, Constants.LETS_ENCRYPTS_ROOT_X2_CROSS_SIGN_CRT)
            } else {
                Assert.fail("unknown certificate")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun partialNetworkOptionTest() {
        val options = NetworkOptions.builder()
            .addDomainMapping(DomainMapping.eventsMapping("www.events.com").build())
            .build()
    }
}
