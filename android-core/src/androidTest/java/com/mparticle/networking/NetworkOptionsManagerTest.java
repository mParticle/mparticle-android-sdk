package com.mparticle.networking;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class NetworkOptionsManagerTest {

    @Test
    public void testEmptyNetworkOptions() {
        NetworkOptions networkOptions = NetworkOptions.builder().build();
        NetworkOptions refinedNetworkOptions = NetworkOptionsManager.validateAndResolve(networkOptions);

        String toString = refinedNetworkOptions.toString();
        Logger.error(toString);

        assertTrue(AccessUtils.equals(refinedNetworkOptions, NetworkOptionsManager.defaultNetworkOptions()));

        refinedNetworkOptions = NetworkOptionsManager.validateAndResolve(null);
        assertTrue(AccessUtils.equals(refinedNetworkOptions, NetworkOptionsManager.defaultNetworkOptions()));
        for(Certificate certificate: refinedNetworkOptions.domainMappings.get(MParticleBaseClientImpl.Endpoint.IDENTITY).getCertificates()) {
            if (certificate.getAlias().equals("intca")) {
                assertEquals(certificate.getCertificate(), Constants.GODADDY_INTERMEDIATE_CRT);
            } else if (certificate.getAlias().equals("rootca")) {
                assertEquals(certificate.getCertificate(), Constants.GODADDY_ROOT_CRT);
            } else if (certificate.getAlias().equals("fiddlerroot")) {
                assertEquals(certificate.getCertificate(), Constants.FIDDLER_ROOT_CRT);
            } else {
                fail("unknown certificate");
            }
        }
    }

    @Test
    public void partialNetworkOptionTest() throws Exception {
        NetworkOptions options = NetworkOptions.builder()
                .addDomainMapping(DomainMapping.eventsMapping("www.events.com").build())
                .build();
    }
}
