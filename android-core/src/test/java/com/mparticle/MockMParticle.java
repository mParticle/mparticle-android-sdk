package com.mparticle;

import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.KitsLoadedCallback;
import com.mparticle.internal.MessageManager;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.mock.MockContext;

import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class MockMParticle extends MParticle {

    public MockMParticle() {
        mAppContext = new MockContext();
        mInternal = Mockito.mock(Internal.class);
        mConfigManager = Mockito.mock(ConfigManager.class);
        mKitManager = Mockito.mock(KitFrameworkWrapper.class);
        mAppStateManager = Mockito.mock(AppStateManager.class);

        mConfigManager = Mockito.mock(ConfigManager.class);
        mKitManager = Mockito.mock(KitFrameworkWrapper.class);
        mMessageManager = Mockito.mock(MessageManager.class);
        mMessaging = Mockito.mock(MPMessagingAPI.class);
        mMedia = Mockito.mock(MPMediaAPI.class);
        Mockito.when(mKitManager.updateKits(Mockito.any())).thenReturn(new KitsLoadedCallback());
        Mockito.when(mInternal.getAppStateManager()).thenReturn(mAppStateManager);
        Mockito.when(mInternal.getConfigManager()).thenReturn(mConfigManager);
        Mockito.when(mInternal.getKitManager()).thenReturn(mKitManager);
        Mockito.when(mInternal.getMessageManager()).thenReturn(mMessageManager);
        mIdentityApi = new IdentityApi(new MockContext(), mAppStateManager, mMessageManager, Internal().getConfigManager(), mKitManager, OperatingSystem.ANDROID);

    }



}
