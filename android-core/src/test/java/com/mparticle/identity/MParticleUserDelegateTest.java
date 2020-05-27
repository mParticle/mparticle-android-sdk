package com.mparticle.identity;

import com.mparticle.MParticle;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.MessageManager;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MParticleUserDelegateTest {

    @Test
    public void setUserIdentitiesNull() {
        MParticleUserDelegate mockDelegate = Mockito.mock(MParticleUserDelegate.class);
        MParticleUserDelegate.setUserIdentities(mockDelegate, null, 123L);
        Mockito.verify(mockDelegate, Mockito.never())
                .setUserIdentity(Mockito.anyString(), Mockito.any(MParticle.IdentityType.class), Mockito.anyLong());
    }

    @Test
    public void setUserIdentitiesCustomerIdEmailInOrder() {
        MParticleUserDelegate mockDelegate = Mockito.mock(MParticleUserDelegate.class);
        Map<MParticle.IdentityType, String> identities = new HashMap<MParticle.IdentityType, String>();
        for (MParticle.IdentityType type : MParticle.IdentityType.values()) {
            identities.put(type, "foo-"+type.name());
        }
        MParticleUserDelegate.setUserIdentities(mockDelegate, identities, 123L);
        InOrder orderVerifier = Mockito.inOrder(mockDelegate);

        //first customer id
        orderVerifier.verify(mockDelegate)
                .setUserIdentity(
                        Mockito.eq("foo-" + MParticle.IdentityType.CustomerId),
                        Mockito.eq(MParticle.IdentityType.CustomerId),
                        Mockito.eq(123L));
        //then email
        orderVerifier.verify(mockDelegate)
                .setUserIdentity(
                        Mockito.eq("foo-" + MParticle.IdentityType.Email),
                        Mockito.eq(MParticle.IdentityType.Email),
                        Mockito.eq(123L));

        //then verify everything
        for (MParticle.IdentityType type : MParticle.IdentityType.values()) {
            Mockito.verify(mockDelegate)
                    .setUserIdentity(
                            Mockito.eq("foo-" + type),
                            Mockito.eq(type),
                            Mockito.eq(123L));
        }
    }

    @Test
    public void setUserIdentitiesNoCustomerIdEmail() {
        MParticleUserDelegate mockDelegate = Mockito.mock(MParticleUserDelegate.class);
        Map<MParticle.IdentityType, String> identities = new HashMap<MParticle.IdentityType, String>();
        for (MParticle.IdentityType type : MParticle.IdentityType.values()) {
            if (type != MParticle.IdentityType.CustomerId && type != MParticle.IdentityType.Email) {
                identities.put(type, "foo-" + type.name());
            }
        }
        MParticleUserDelegate.setUserIdentities(mockDelegate, identities, 123L);

        for (MParticle.IdentityType type : MParticle.IdentityType.values()) {
            if (type != MParticle.IdentityType.CustomerId && type != MParticle.IdentityType.Email) {
                Mockito.verify(mockDelegate)
                        .setUserIdentity(
                                Mockito.eq("foo-" + type),
                                Mockito.eq(type),
                                Mockito.eq(123L));
            }
        }
    }

}