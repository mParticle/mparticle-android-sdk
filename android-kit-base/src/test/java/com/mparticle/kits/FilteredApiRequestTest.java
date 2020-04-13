package com.mparticle.kits;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApiRequest;

import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class FilteredApiRequestTest {

    @Test
    public void testEmptyApiRequest() {
        IdentityApiRequest request = IdentityApiRequest.withEmptyUser().build();
        KitIntegration mockIntegration = Mockito.mock(KitIntegration.class);
        KitConfiguration mockConfiguration = Mockito.mock(KitConfiguration.class);
        Mockito.when(mockConfiguration.shouldSetIdentity(Mockito.any(MParticle.IdentityType.class))).thenReturn(true);
        Mockito.when(mockIntegration.getConfiguration()).thenReturn(mockConfiguration);
        FilteredIdentityApiRequest filteredRequest = new FilteredIdentityApiRequest(request, mockIntegration);

        assertEquals(0, filteredRequest.userIdentities.size());
    }

    @Test
    public void testApiRequestFilter() {
        IdentityApiRequest request = IdentityApiRequest.withEmptyUser()
                .email("testEmail")
                .customerId("1234")
                .userIdentity(MParticle.IdentityType.Alias, "alias")
                .userIdentity(MParticle.IdentityType.Google, "mparticle@gmail")
                .build();
        KitIntegration mockIntegration = Mockito.mock(KitIntegration.class);
        KitConfiguration mockConfiguration = Mockito.mock(KitConfiguration.class);
        Mockito.when(mockConfiguration.shouldSetIdentity(MParticle.IdentityType.Email)).thenReturn(true);
        Mockito.when(mockConfiguration.shouldSetIdentity(MParticle.IdentityType.Alias)).thenReturn(true);
        Mockito.when(mockConfiguration.shouldSetIdentity(MParticle.IdentityType.CustomerId)).thenReturn(false);
        Mockito.when(mockConfiguration.shouldSetIdentity(MParticle.IdentityType.Google)).thenReturn(false);
        Mockito.when(mockIntegration.getConfiguration()).thenReturn(mockConfiguration);
        FilteredIdentityApiRequest filteredRequest = new FilteredIdentityApiRequest(request, mockIntegration);

        assertEquals(4, filteredRequest.userIdentities.size());
        assertEquals(2, filteredRequest.getUserIdentities().size());
    }
}
