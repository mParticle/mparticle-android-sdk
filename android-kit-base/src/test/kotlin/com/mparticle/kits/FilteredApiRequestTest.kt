package com.mparticle.kits

import com.mparticle.MParticle.IdentityType
import com.mparticle.identity.IdentityApiRequest
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class FilteredApiRequestTest {
    @Test
    fun testEmptyApiRequest() {
        val request = IdentityApiRequest.withEmptyUser().build()
        val mockIntegration = Mockito.mock(
            KitIntegration::class.java
        )
        val configMock = Mockito.mock(KitConfiguration::class.java)
        mockIntegration.setConfiguration(configMock)
        Mockito.`when`(
            configMock.shouldSetIdentity(
                anyObject(
                    IdentityType::class.java
                )
            )
        ).thenReturn(true)
        val filteredRequest = FilteredIdentityApiRequest(request, mockIntegration)
        Assert.assertEquals(0, filteredRequest.userIdentities.size.toLong())
    }

    @Test
    fun testApiRequestFilter() {
        val request = IdentityApiRequest.withEmptyUser()
            .email("testEmail")
            .customerId("1234")
            .userIdentity(IdentityType.Alias, "alias")
            .userIdentity(IdentityType.Google, "mparticle@gmail")
            .build()
        val mockIntegration = Mockito.mock(
            KitIntegration::class.java
        )
        val mockConfiguration = Mockito.mock(
            KitConfiguration::class.java
        )
        Mockito.`when`(mockConfiguration.shouldSetIdentity(IdentityType.Email)).thenReturn(true)
        Mockito.`when`(mockConfiguration.shouldSetIdentity(IdentityType.Alias)).thenReturn(true)
        Mockito.`when`(mockConfiguration.shouldSetIdentity(IdentityType.CustomerId))
            .thenReturn(false)
        Mockito.`when`(mockConfiguration.shouldSetIdentity(IdentityType.Google)).thenReturn(false)
        mockIntegration.setConfiguration(mockConfiguration)
        val filteredRequest = FilteredIdentityApiRequest(request, mockIntegration)
        Assert.assertEquals(4, filteredRequest.userIdentities.size.toLong())
        Assert.assertEquals(2, filteredRequest.getUserIdentities().size.toLong())
    }
}
