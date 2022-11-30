package com.mparticle.kits

import com.mparticle.MParticle.IdentityType
import com.mparticle.identity.IdentityApiRequest
import com.mparticle.kits.MockitoHelper.anyObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class FilteredApiRequestTest {
    @Test
    fun testEmptyApiRequest() {
        val request = IdentityApiRequest.withEmptyUser().build()
        val mockIntegration = Mockito.mock(
            KitIntegration::class.java
        )
        val mockConfiguration = Mockito.mock(
            KitConfiguration::class.java
        )
        mockIntegration.setConfiguration(mockConfiguration)
        `when`(mockConfiguration.shouldSetIdentity(anyObject())).thenReturn(true)
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

        mockIntegration.setConfiguration(mockConfiguration)
        `when`(mockConfiguration.shouldSetIdentity(IdentityType.Email)).thenReturn(true)
        `when`(mockConfiguration.shouldSetIdentity(IdentityType.Alias)).thenReturn(true)
        `when`(mockConfiguration.shouldSetIdentity(IdentityType.CustomerId))
            .thenReturn(false)
        `when`(mockConfiguration.shouldSetIdentity(IdentityType.Google)).thenReturn(false)
        `when`(mockIntegration.configuration).thenReturn(mockConfiguration)
        val filteredRequest = FilteredIdentityApiRequest(request, mockIntegration)
        Assert.assertEquals(2, filteredRequest.userIdentities.size.toLong())
    }
}
