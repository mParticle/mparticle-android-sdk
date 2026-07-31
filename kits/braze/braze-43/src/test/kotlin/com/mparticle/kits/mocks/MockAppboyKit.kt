package com.mparticle.kits.mocks

import android.content.Context
import com.mparticle.internal.ReportingManager
import com.mparticle.kits.AppboyKit
import org.mockito.Mockito

class MockAppboyKit : AppboyKit() {
    val calledAuthority = arrayOfNulls<String>(1)

    override fun setAuthority(authority: String?) {
        calledAuthority[0] = authority
    }

    override fun queueDataFlush() {
        // do nothing
    }

    init {
        kitManager =
            MockKitManagerImpl(
                Mockito.mock(Context::class.java),
                Mockito.mock(
                    ReportingManager::class.java,
                ),
                MockCoreCallbacks(),
            )
    }
}
