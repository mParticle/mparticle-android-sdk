package com.mparticle.networking

import org.junit.Assert
import org.junit.Test

class EndpointTest {
    @Test
    fun parseEnumTest() {
        for (endpoint in MParticleBaseClientImpl.Endpoint.values()) {
            Assert.assertEquals(endpoint, MParticleBaseClientImpl.Endpoint.parseInt(endpoint.value))
        }
    }
}
