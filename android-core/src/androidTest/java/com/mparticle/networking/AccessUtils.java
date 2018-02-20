package com.mparticle.networking;

import java.util.Map;

public class AccessUtils {

    public static NetworkOptions getDefaultNetworkOptions() {
        return NetworkOptionsManager.defaultNetworkOptions();
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
