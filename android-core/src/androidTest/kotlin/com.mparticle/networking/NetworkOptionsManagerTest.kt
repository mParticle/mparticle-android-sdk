package com.mparticle.networking

import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import org.junit.Assert
import org.junit.Test

class NetworkOptionsManagerTest {
    @Test
    fun testEmptyNetworkOptions() {
        val networkOptions = NetworkOptions.builder().build()
        var refinedNetworkOptions = NetworkOptionsManager.validateAndResolve(networkOptions, "us1", true)
        val toString = refinedNetworkOptions.toString()
        Logger.error(toString)
        Assert.assertTrue(
            AccessUtils.equals(
                refinedNetworkOptions,
                NetworkOptionsManager.defaultNetworkOptions("us1", true)
            )
        )
        refinedNetworkOptions = NetworkOptionsManager.validateAndResolve(null, "us1", true)
        Assert.assertTrue(
            AccessUtils.equals(
                refinedNetworkOptions,
                NetworkOptionsManager.defaultNetworkOptions("us1", true)
            )
        )
        for (
        certificate in refinedNetworkOptions.domainMappings[MParticleBaseClientImpl.Endpoint.IDENTITY]!!
            .certificates
        ) {
            if (certificate.alias == "intca") {
                Assert.assertEquals(certificate.certificate, Constants.GODADDY_INTERMEDIATE_CRT)
            } else if (certificate.alias == "rootca") {
                Assert.assertEquals(certificate.certificate, Constants.GODADDY_ROOT_CRT)
            } else if (certificate.alias == "fiddlerroot") {
                Assert.assertEquals(certificate.certificate, Constants.FIDDLER_ROOT_CRT)
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
