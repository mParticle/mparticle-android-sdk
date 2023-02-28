package com.mparticle.networking

import com.mparticle.mock.utils.RandomUtils
import org.junit.Assert
import org.junit.Test

class NetworkOptionsTest {
    @Test
    @Throws(Exception::class)
    fun testNetworkOptionsSerialization() {
        val random = RandomUtils.getInstance()
        val options = NetworkOptions.builder().addDomainMapping(
            DomainMapping.configMapping("www.configUrl.com")
                .addCertificate("godaddy", random.getAlphaNumericString(24, 256))
                .addCertificate(
                    Certificate.with(
                        "random",
                        random.getAlphaNumericString(24, 256)
                    )!!
                ).build()
        ).addDomainMapping(
            DomainMapping.identityMapping("www.identityUrl.com")
                .addCertificate("random1", random.getAlphaNumericString(24, 256))
                .addCertificate(
                    Certificate.with(
                        "random2",
                        random.getAlphaNumericString(24, 256)
                    )!!
                ).addCertificate("random3", random.getAlphaNumericString(24, 256)).build()
        ).addDomainMapping(
            DomainMapping.eventsMapping("www.eventsUrl.com")
                .addCertificate("random3", random.getAlphaNumericString(24, 256))
                .addCertificate("random4", random.getAlphaNumericString(24, 256))
                .addCertificate("random5", random.getAlphaNumericString(24, 256)).build()
        ).setPinningDisabledInDevelopment(true).build()
        val optionsString = options.toString()
        val optionsDeserialized = NetworkOptions.withNetworkOptions(optionsString)
        Assert.assertTrue(equals(options, optionsDeserialized))
    }

    companion object {
        fun equals(networkOptions1: NetworkOptions, networkOptions2: NetworkOptions?): Boolean {
            if (networkOptions1 === networkOptions2) {
                return true
            }
            if (networkOptions1.pinningDisabledInDevelopment != networkOptions2?.pinningDisabledInDevelopment) {
                return false
            }
            for (entry in networkOptions1.domainMappings) {
                val other = networkOptions2.domainMappings[entry.key]
                if (other == null || !equals(entry.value, other)) {
                    return false
                }
            }
            return true
        }

        fun equals(domainMapping1: DomainMapping, domainMapping2: DomainMapping): Boolean {
            if (domainMapping1 === domainMapping2) {
                return true
            }
            if (domainMapping1.url == domainMapping2.url && domainMapping1.type == domainMapping2.type) {
                for (i in domainMapping1.certificates.indices) {
                    if (!equals(
                            domainMapping1.certificates[i],
                            domainMapping2.certificates[i]
                        )
                    ) {
                        return false
                    }
                }
            }
            return true
        }

        fun equals(certificate1: Certificate, certificate2: Certificate): Boolean {
            if (certificate1 == certificate2) {
                return true
            }
            return ((certificate1.certificate === certificate2.certificate || certificate1.certificate == certificate2.certificate) && (certificate1.alias === certificate2.alias || certificate1.alias == certificate2.alias))
        }
    }
}
