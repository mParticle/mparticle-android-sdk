package com.mparticle.networking;

import com.mparticle.mock.utils.RandomUtils;

import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class NetworkOptionsTest {

    @Test
    public void testNetworkOptionsSerialization() throws Exception {
        RandomUtils random = RandomUtils.getInstance();
        NetworkOptions options = NetworkOptions.builder()
                .addDomainMapping(
                        DomainMapping.configMapping("www.configUrl.com")
                                .addCertificate("godaddy", random.getAlphaNumericString(24, 256))
                                .addCertificate(Certificate.with("random", random.getAlphaNumericString(24, 256)))
                                .build())
                .addDomainMapping(
                        DomainMapping.identityMapping("www.identityUrl.com")
                                .addCertificate("random1", random.getAlphaNumericString(24, 256))
                                .addCertificate(Certificate.with("random2", random.getAlphaNumericString(24, 256)))
                                .addCertificate("random3", random.getAlphaNumericString(24, 256))
                                .build())
                .addDomainMapping(
                        DomainMapping.eventsMapping("www.eventsUrl.com")
                                .addCertificate("random3", random.getAlphaNumericString(24, 256))
                                .addCertificate("random4", random.getAlphaNumericString(24, 256))
                                .addCertificate("random5", random.getAlphaNumericString(24, 256))
                                .build())
                .setPinningDisabledInDevelopment(true)
                .build();
        String optionsString = options.toString();
        NetworkOptions optionsDeserialized = NetworkOptions.withNetworkOptions(optionsString);
        assertTrue(equals(options, optionsDeserialized));

    }

    public static boolean equals(NetworkOptions networkOptions1, NetworkOptions networkOptions2) {
        if (networkOptions1 == networkOptions2) {
            return true;
        }
        if (networkOptions1.pinningDisabledInDevelopment != networkOptions2.pinningDisabledInDevelopment) {
            return false;
        }
        for (Map.Entry<MParticleBaseClientImpl.Endpoint, DomainMapping> entry : networkOptions1.domainMappings.entrySet()) {
            DomainMapping other = networkOptions2.domainMappings.get(entry.getKey());
            if (other == null || !equals(entry.getValue(), other)) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(DomainMapping domainMapping1, DomainMapping domainMapping2) {
        if (domainMapping1 == domainMapping2) {
            return true;
        }
        if (domainMapping1.getUrl().equals(domainMapping2.getUrl()) && domainMapping1.getType() == domainMapping2.getType()) {
            for (int i = 0; i < domainMapping1.getCertificates().size(); i++) {
                if (!equals(domainMapping1.getCertificates().get(i), (domainMapping2.getCertificates().get(i)))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean equals(Certificate certificate1, Certificate certificate2) {
        if (certificate1 == certificate2) {
            return true;
        }
        if (((certificate1.getCertificate() == certificate2.getCertificate()) || certificate1.getCertificate().equals(certificate2.getCertificate()))
                && ((certificate1.getAlias() == certificate2.getAlias()) || certificate1.getAlias().equals(certificate2.getAlias()))) {
            return true;
        }
        return false;
    }
}