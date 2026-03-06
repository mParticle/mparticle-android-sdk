package com.mparticle.kits.mocks

import android.app.Activity
import android.net.Uri
import com.mparticle.MParticleOptions.DataplanOptions
import com.mparticle.internal.CoreCallbacks
import com.mparticle.internal.CoreCallbacks.KitListener
import org.json.JSONArray
import java.lang.ref.WeakReference

class MockCoreCallbacks : CoreCallbacks {
    override fun isBackgrounded(): Boolean = false

    override fun getUserBucket(): Int = 0

    override fun isEnabled(): Boolean = false

    override fun setIntegrationAttributes(
        kitId: Int,
        integrationAttributes: Map<String, String>,
    ) {}

    override fun getIntegrationAttributes(kitId: Int): Map<String, String>? = null

    override fun getCurrentActivity(): WeakReference<Activity>? = null

    override fun getLatestKitConfiguration(): JSONArray? = null

    override fun getDataplanOptions(): DataplanOptions? = null

    override fun isPushEnabled(): Boolean = false

    override fun getPushSenderId(): String? = null

    override fun getPushInstanceId(): String? = null

    override fun getLaunchUri(): Uri? = null

    override fun getLaunchAction(): String? = null

    override fun getKitListener(): KitListener =
        object : KitListener {
            override fun kitFound(kitId: Int) {}

            override fun kitConfigReceived(
                kitId: Int,
                configuration: String?,
            ) {}

            override fun kitExcluded(
                kitId: Int,
                reason: String?,
            ) {}

            override fun kitStarted(kitId: Int) {}

            override fun onKitApiCalled(
                kitId: Int,
                used: Boolean?,
                vararg objects: Any?,
            ) {}

            override fun onKitApiCalled(
                methodName: String?,
                kitId: Int,
                used: Boolean?,
                vararg objects: Any?,
            ) {}
        }
}
