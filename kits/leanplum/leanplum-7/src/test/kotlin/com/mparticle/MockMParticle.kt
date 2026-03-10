package com.mparticle

import com.mparticle.identity.IdentityApi
import org.mockito.Mockito

class MockMParticle : MParticle() {
    fun setAndroidIdDisabled(disabled: Boolean) {
        sAndroidIdEnabled = !disabled
    }

    init {
        mIdentityApi = Mockito.mock(IdentityApi::class.java)
    }
}
