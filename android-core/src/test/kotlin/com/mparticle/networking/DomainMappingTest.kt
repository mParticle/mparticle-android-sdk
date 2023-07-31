package com.mparticle.networking

import com.mparticle.testutils.RandomUtils
import org.junit.Assert
import org.junit.Test
import java.util.Random

class DomainMappingTest {
    private var random = Random()
    var randomUtils = RandomUtils()

    @Test
    fun nullCertificatesNotAdded() {
        val certificates: MutableList<Certificate?> = ArrayList()
        certificates.add(null)
        val domainMapping = DomainMapping.eventsMapping("test")
            .setCertificates(certificates)
            .build()
        Assert.assertEquals(0, domainMapping.certificates.size.toLong())
    }

    @Test
    fun malformedCertificateNotAdded() {
        val domainMapping = DomainMapping.eventsMapping("test")
            .addCertificate("", "")
            .build()
        Assert.assertEquals(0, domainMapping.certificates.size.toLong())
    }

    @Test
    fun addCertificateTest() {
        val certificate = Certificate.with("alias", "certificate")
        val domainMapping = DomainMapping.eventsMapping("test")
            .addCertificate(certificate!!.alias, certificate.certificate)
            .build()
        Assert.assertEquals(1, domainMapping.certificates.size.toLong())
        Assert.assertEquals(certificate.alias, domainMapping.certificates[0].alias)
        Assert.assertEquals(certificate.certificate, domainMapping.certificates[0].certificate)
    }

    @Test
    fun addMultipleCertificatesTest() {
        val certificates = HashMap<String, Certificate?>()
        for (i in 0 until random.nextInt() % 10) {
            val alias = randomUtils.getAlphaNumericString(10)
            certificates[alias] = Certificate.with(alias, randomUtils.getAlphaNumericString(124))
        }
        var builder = DomainMapping.eventsMapping("test")
        for (certificate in certificates.values) {
            if (certificate != null) {
                builder.addCertificate(certificate)
            }
        }
        var domainMapping = builder.build()
        assertContainsCertificates(certificates, domainMapping)
        domainMapping = DomainMapping.eventsMapping("test")
            .setCertificates(ArrayList(certificates.values))
            .build()
        assertContainsCertificates(certificates, domainMapping)
        builder = DomainMapping.eventsMapping("test")
        for (certificate in certificates.values) {
            certificate?.alias?.let { builder.addCertificate(it, certificate.certificate) }
        }
        domainMapping = builder.build()
        assertContainsCertificates(certificates, domainMapping)
    }

    private fun assertContainsCertificates(
        certificates: Map<String, Certificate?>,
        domainMapping: DomainMapping
    ) {
        Assert.assertEquals(
            certificates.values.size.toLong(),
            domainMapping.certificates.size.toLong()
        )
        for (certificate in domainMapping.certificates) {
            val match = certificates[certificate.alias]
            Assert.assertNotNull(match)
            Assert.assertEquals(match?.alias, certificate.alias)
            Assert.assertEquals(match?.certificate, certificate.certificate)
        }
    }
}
