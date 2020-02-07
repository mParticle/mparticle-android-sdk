package com.mparticle.networking;

import com.mparticle.testutils.RandomUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CertificateTest {
    RandomUtils randomUtils = new RandomUtils();
    String alias = randomUtils.getAlphaNumericString(10);
    String certificateString = randomUtils.getAlphaNumericString(124);

    @Test
    public void buildCertificateTest() {
        Certificate certificate = Certificate.with(alias, certificateString);
        assertEquals(alias, certificate.getAlias());
        assertEquals(certificateString, certificate.getCertificate());
    }

    @Test
    public void rejectMalformedCertificate() {
        Certificate certificate = Certificate.with("", certificateString);
        assertNull(certificate);

        certificate = Certificate.with(alias, "");
        assertNull(certificate);

    }
}
