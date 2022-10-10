package com.mparticle

import com.mparticle.identity.IdentityApi
import com.mparticle.internal.AppStateManager
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.KitFrameworkWrapper
import com.mparticle.internal.KitsLoadedCallback
import com.mparticle.internal.MessageManager
import com.mparticle.media.MPMediaAPI
import com.mparticle.messaging.MPMessagingAPI
import com.mparticle.mock.MockContext
import org.mockito.Mockito

class MockMParticle : MParticle() {
    init {
        mConfigManager = Mockito.mock(ConfigManager::class.java)
        mKitManager = Mockito.mock(KitFrameworkWrapper::class.java)
        mAppStateManager = Mockito.mock(AppStateManager::class.java)
        mConfigManager = Mockito.mock(ConfigManager::class.java)
        mKitManager = Mockito.mock(KitFrameworkWrapper::class.java)
        mMessageManager = Mockito.mock(MessageManager::class.java)
        mMessaging = Mockito.mock(MPMessagingAPI::class.java)
        mMedia = Mockito.mock(MPMediaAPI::class.java)
        mIdentityApi = IdentityApi(
            MockContext(),
            mAppStateManager,
            mMessageManager,
            mInternal.configManager,
            mKitManager,
            OperatingSystem.ANDROID
        )
        Mockito.`when`(mKitManager.updateKits(Mockito.any())).thenReturn(KitsLoadedCallback())
        val event = MPEvent.Builder("this")
            .customAttributes(HashMap<String, String?>())
            .build()
        val attributes = event.customAttributes
    }
}
