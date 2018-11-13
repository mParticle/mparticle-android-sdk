package com.mparticle.mock;

import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MessageManager;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.messaging.MPMessagingAPI;

import org.mockito.Mockito;

import java.util.Random;

public class MockMParticle extends MParticle {

    public MockMParticle() {
        mInternal = new MockInternal(Mockito.mock(ConfigManager.class));
        Mockito.when(mInternal.getConfigManager().getMpid()).thenReturn(new Random().nextLong());
        mKitManager = Mockito.mock(KitFrameworkWrapper.class);
        mAppStateManager = Mockito.mock(AppStateManager.class);
        mMessageManager = Mockito.mock(MessageManager.class);
        mMessaging = Mockito.mock(MPMessagingAPI.class);
        mMedia = Mockito.mock(MPMediaAPI.class);
        mCommerce = Mockito.mock(CommerceApi.class);
        mIdentityApi = new IdentityApi(new MockContext(), mAppStateManager, mMessageManager, mInternal.getConfigManager(), mKitManager);
        mPreferences = new MockSharedPreferences();
    }

    class MockInternal extends Internal {

        MockInternal(ConfigManager configManager) {
            super(configManager);
        }
    }
}
