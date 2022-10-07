package com.mparticle.identity

import com.mparticle.MParticle.IdentityType
import org.junit.Test
import org.mockito.Mockito

class MParticleUserDelegateTest {
    @Test
    fun setUserIdentitiesNull() {
        val mockDelegate = Mockito.mock(
            MParticleUserDelegate::class.java
        )
        MParticleUserDelegate.setUserIdentities(mockDelegate, null, 123L)
        Mockito.verify(
            mockDelegate,
            Mockito.never()
        )
            .setUserIdentity(
                Mockito.anyString(),
                Mockito.any(
                    IdentityType::class.java
                ),
                Mockito.anyLong()
            )
    }

    @Test
    fun setUserIdentitiesCustomerIdEmailInOrder() {
        val mockDelegate = Mockito.mock(
            MParticleUserDelegate::class.java
        )
        val identities = HashMap<IdentityType, String>()
        for (type in IdentityType.values()) {
            identities[type] = "foo-" + type.name
        }
        MParticleUserDelegate.setUserIdentities(mockDelegate, identities, 123L)
        val orderVerifier = Mockito.inOrder(mockDelegate)

        // first customer id
        orderVerifier.verify(mockDelegate)
            .setUserIdentity(
                Mockito.eq("foo-" + IdentityType.CustomerId),
                Mockito.eq(IdentityType.CustomerId),
                Mockito.eq(123L)
            )
        // then email
        orderVerifier.verify(mockDelegate)
            .setUserIdentity(
                Mockito.eq("foo-" + IdentityType.Email),
                Mockito.eq(IdentityType.Email),
                Mockito.eq(123L)
            )

        // then verify everything
        for (type in IdentityType.values()) {
            Mockito.verify(mockDelegate)
                .setUserIdentity(
                    Mockito.eq("foo-$type"),
                    Mockito.eq(type),
                    Mockito.eq(123L)
                )
        }
    }

    @Test
    fun setUserIdentitiesNoCustomerIdEmail() {
        val mockDelegate = Mockito.mock(
            MParticleUserDelegate::class.java
        )
        val identities = HashMap<IdentityType, String>()
        for (type in IdentityType.values()) {
            if (type != IdentityType.CustomerId && type != IdentityType.Email) {
                identities[type] = "foo-" + type.name
            }
        }
        MParticleUserDelegate.setUserIdentities(mockDelegate, identities, 123L)
        for (type in IdentityType.values()) {
            if (type != IdentityType.CustomerId && type != IdentityType.Email) {
                Mockito.verify(mockDelegate)
                    .setUserIdentity(
                        Mockito.eq("foo-$type"),
                        Mockito.eq(type),
                        Mockito.eq(123L)
                    )
            }
        }
    }
}
