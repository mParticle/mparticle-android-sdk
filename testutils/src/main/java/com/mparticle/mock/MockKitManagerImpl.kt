package com.mparticle.mock

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

open class MockKitManagerImpl(
    context: Context,
    reportingManager: ReportingManager,
    val coreCallbacks: CoreCallbacks
) : KitManagerImpl(
    context, reportingManager, coreCallbacks,
    Mockito.mock(MParticleOptions::class.java)
) {
    constructor() : this(
        MockContext(),
        Mockito.mock<ReportingManager>(ReportingManager::class.java),
        Mockito.mock<CoreCallbacks>(CoreCallbacks::class.java)
    ) {
        Mockito.`when`(mCoreCallbacks.kitListener).thenReturn(KitListener.EMPTY)
    }

    @Throws(JSONException::class)
    override fun createKitConfiguration(configuration: JSONObject): KitConfiguration {
        return MockKitConfiguration.createKitConfiguration(configuration)
    }

    override val userBucket: Int
        get() = 50

    override fun runOnKitThread(runnable: Runnable?) {
        runnable!!.run()
    }

    override fun runOnMainThread(runnable: Runnable) {
        runnable.run()
    }
}
