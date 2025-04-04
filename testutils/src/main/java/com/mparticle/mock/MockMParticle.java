package com.mparticle.mock;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityApi;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MessageManager;
import com.mparticle.media.MPMediaAPI;
import com.mparticle.messaging.MPMessagingAPI;

import org.mockito.Mockito;

import java.util.Random;

public class MockMParticle extends MParticle {

    public MockMParticle() {
        mConfigManager = Mockito.mock(ConfigManager.class);
        Mockito.when(mConfigManager.getMpid()).thenReturn(new Random().nextLong());
        mKitManager = Mockito.mock(KitFrameworkWrapper.class);
        mMessageManager = Mockito.mock(MessageManager.class);
        mMessaging = Mockito.mock(MPMessagingAPI.class);
        mMedia = Mockito.mock(MPMediaAPI.class);
        mIdentityApi = new IdentityApi(new MockContext(), mAppStateManager, mMessageManager, Internal().getConfigManager(), mKitManager, OperatingSystem.ANDROID);
        mPreferences = new MockSharedPreferences();
    }

    public void setIdentityApi(IdentityApi identityApi) {
        this.mIdentityApi = identityApi;
    }

    class MockInternal extends Internal {

        MockInternal() {
            super();
        }
    }

    class MockRokt extends Rokt {

        MockRokt() {
            super();
        }
    }
}
