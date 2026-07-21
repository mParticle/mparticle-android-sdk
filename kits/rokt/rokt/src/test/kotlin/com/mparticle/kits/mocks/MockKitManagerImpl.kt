package com.mparticle.kits.mocks

import android.content.Context
import com.mparticle.MParticleOptions
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import com.mparticle.internal.ReportingManager
import com.mparticle.kits.KitConfiguration
import com.mparticle.kits.KitManagerImpl
import org.json.JSONException
import org.json.JSONObject
import org.mockito.Mockito

class MockKitManagerImpl(context: Context?, reportingManager: ReportingManager?, coreCallbacks: CoreCallbacks?) :
    KitManagerImpl(
        context,
        reportingManager,
        coreCallbacks,
        Mockito.mock(
            MParticleOptions::class.java,
        ),
    ) {
    constructor() : this(
        MockContext(),
        Mockito.mock<ReportingManager>(ReportingManager::class.java),
        Mockito.mock<CoreCallbacks>(
            CoreCallbacks::class.java,
        ),
    ) {
        Mockito.`when`(mCoreCallbacks.getKitListener()).thenReturn(
            object : KitListener {
                override fun kitFound(kitId: Int) {}
                override fun kitConfigReceived(kitId: Int, configuration: String?) {}
                override fun kitExcluded(kitId: Int, reason: String?) {}
                override fun kitStarted(kitId: Int) {}
                override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {}
                override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {}
            },
        )
    }

    @Throws(JSONException::class)
    override fun createKitConfiguration(configuration: JSONObject): KitConfiguration =
        MockKitConfiguration.createKitConfiguration(configuration)

    override fun getUserBucket(): Int = 50
}
