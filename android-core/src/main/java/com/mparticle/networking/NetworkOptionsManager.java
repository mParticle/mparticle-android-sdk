package com.mparticle.networking;

import static com.mparticle.networking.MParticleBaseClientImpl.Endpoint;

import com.mparticle.BuildConfig;
import com.mparticle.internal.Constants;
import com.mparticle.internal.MPUtility;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class NetworkOptionsManager {
    public static String MP_CONFIG_URL = "config2.mparticle.com";
    public static String MP_URL = "nativesdks.mparticle.com";
    public static String MP_IDENTITY_URL_PREFIX = "identity";
    public static String MP_URL_PREFIX = "nativesdks";

    public static NetworkOptions validateAndResolve(NetworkOptions networkOptions) {
        if (networkOptions == null) {
            return defaultNetworkOptions();
        }
        //Only take the endpoints we care about.
        for (Endpoint endpoint : Endpoint.values()) {
            DomainMapping domainMapping = networkOptions.domainMappings.get(endpoint);
            if (domainMapping == null) {
                networkOptions.domainMappings.put(endpoint, DomainMapping.withEndpoint(endpoint)
                        .setCertificates(defaultCertificates)
                        .build());
            } else {
                if (MPUtility.isEmpty(domainMapping.getUrl())) {
                    domainMapping.setUrl(getDefaultUrl(domainMapping.getType()));
                }
                //if there are no certificates, give the default ones
                if (MPUtility.isEmpty(domainMapping.getCertificates())) {
                    domainMapping.setCertificates(defaultCertificates);
                }
            }
        }
        return networkOptions;
    }


    static NetworkOptions defaultNetworkOptions() {
        return NetworkOptions.builder()
                .addDomainMapping(DomainMapping.identityMapping(getDefaultUrl(Endpoint.IDENTITY))
                        .setCertificates(defaultCertificates)
                        .build())
                .addDomainMapping(DomainMapping.configMapping(getDefaultUrl(Endpoint.CONFIG))
                        .setCertificates(defaultCertificates)
                        .build())
                .addDomainMapping(DomainMapping.eventsMapping(getDefaultUrl(Endpoint.EVENTS))
                        .setCertificates(defaultCertificates)
                        .build())
                .addDomainMapping(DomainMapping.audienceMapping(getDefaultUrl(Endpoint.AUDIENCE))
                        .setCertificates(defaultCertificates)
                        .build())
                .build();
    }

    static List<Certificate> getDefaultCertificates() {
        return defaultCertificates;
    }

    private static final List<Certificate> defaultCertificates = Collections.unmodifiableList(new LinkedList<Certificate>() {
        {
            add(Certificate.with("godaddy_root_g2", Constants.GODADDY_ROOT_G2_CRT));
            add(Certificate.with("godaddy_root_class2", Constants.GODADDY_CLASS_2_ROOT_CRT));
            add(Certificate.with("lets_encrypt_root_x1", Constants.LETS_ENCRYPTS_ROOT_X1_CRT));
            add(Certificate.with("lets_encrypt_root_x2_self", Constants.LETS_ENCRYPTS_ROOT_X2_SELF_SIGN_CRT));
            add(Certificate.with("lets_encrypt_root_x2_cross", Constants.LETS_ENCRYPTS_ROOT_X2_CROSS_SIGN_CRT));
        }
    });

    // https://go.mparticle.com/work/SQDSDKS-6621
    static String getDefaultUrl(Endpoint type) {
        switch (type) {
            case CONFIG:
                return MPUtility.isEmpty(BuildConfig.MP_CONFIG_URL) ? MP_CONFIG_URL : BuildConfig.MP_CONFIG_URL;
            case IDENTITY:
                return MPUtility.isEmpty(BuildConfig.MP_IDENTITY_URL) ? MP_IDENTITY_URL_PREFIX : BuildConfig.MP_IDENTITY_URL;
            case EVENTS:
            case ALIAS:
            case AUDIENCE:
                return MPUtility.isEmpty(BuildConfig.MP_URL) ? MP_URL_PREFIX : BuildConfig.MP_URL;
            default:
                throw new IllegalArgumentException("Missing a Url for type " + type.name());
        }
    }
}
