package com.mparticle;

import com.mparticle.commerce.CommerceApi;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MessageManager;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.mock.MockContext;

import org.mockito.Mockito;

public class MockMParticle extends MParticle {

    public MockMParticle() {
        mInternal = new Internal(Mockito.mock(ConfigManager.class));
        mKitManager = Mockito.mock(KitFrameworkWrapper.class);
        mAppStateManager = Mockito.mock(AppStateManager.class);
        mMessageManager = Mockito.mock(MessageManager.class);
        mMessaging = Mockito.mock(MPMessagingAPI.class);
        mMedia = Mockito.mock(MPMediaAPI.class);
        mCommerce = Mockito.mock(CommerceApi.class);
        mIdentityApi = new IdentityApi(new MockContext(), mAppStateManager, mMessageManager, mInternal.getConfigManager(), mKitManager);
    }

}
