package com.mparticle.networking;

import com.mparticle.testutils.RandomUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DomainMappingTest {
    Random random = new Random();
    RandomUtils randomUtils = new RandomUtils();

    @Test
    public void nullCertificatesNotAdded() {
        List<Certificate> certificates = new ArrayList<Certificate>();
        certificates.add(null);
        DomainMapping domainMapping = DomainMapping.eventsMapping("test")
                .setCertificates(certificates)
                .build();
        assertEquals(0, domainMapping.getCertificates().size());
    }

    @Test
    public void malformedCertificateNotAdded() {
        DomainMapping domainMapping = DomainMapping.eventsMapping("test")
                .addCertificate("", "")
                .build();
        assertEquals(0, domainMapping.getCertificates().size());
    }

    @Test
    public void addCertificatTest() {
        Certificate certificate = Certificate.with("alias", "certificate");
        DomainMapping domainMapping = DomainMapping.eventsMapping("test")
                .addCertificate(certificate.getAlias(), certificate.getCertificate())
                .build();

        assertEquals(1, domainMapping.getCertificates().size());
        assertEquals(certificate.getAlias(), domainMapping.getCertificates().get(0).getAlias());
        assertEquals(certificate.getCertificate(), domainMapping.getCertificates().get(0).getCertificate());
    }

    @Test
    public void addMultipleCertificatesTest() {
        Map<String, Certificate> certificates = new HashMap<String, Certificate>();

        for (int i = 0; i < random.nextInt() % 10; i++) {
            String alias = randomUtils.getAlphaNumericString(10);
            certificates.put(alias, Certificate.with(alias, randomUtils.getAlphaNumericString(124)));
        }

        DomainMapping.Builder builder = DomainMapping.eventsMapping("test");
        for (Certificate certificate: certificates.values()) {
            builder.addCertificate(certificate);
        }

        DomainMapping domainMapping = builder.build();

        assertContainsCertificates(certificates, domainMapping);

        domainMapping = DomainMapping.eventsMapping("test")
                .setCertificates(new ArrayList<Certificate>(certificates.values()))
                .build();

        assertContainsCertificates(certificates, domainMapping);

        builder = DomainMapping.eventsMapping("test");
        for (Certificate certificate: certificates.values()) {
            builder.addCertificate(certificate.getAlias(), certificate.getCertificate());
        }
        domainMapping = builder.build();

        assertContainsCertificates(certificates, domainMapping);
    }

    private void assertContainsCertificates(Map<String, Certificate> certificates, DomainMapping domainMapping) {
        assertEquals(certificates.values().size(), domainMapping.getCertificates().size());
        for(Certificate certificate: domainMapping.getCertificates()) {
            Certificate match = certificates.get(certificate.getAlias());
            assertNotNull(match);
            assertEquals(match.getAlias(), certificate.getAlias());
            assertEquals(match.getCertificate(), certificate.getCertificate());
        }
    }
}
