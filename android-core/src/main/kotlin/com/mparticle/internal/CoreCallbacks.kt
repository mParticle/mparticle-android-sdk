package com.mparticle.internal

import android.app.Activity
import android.net.Uri
import androidx.annotation.WorkerThread
import com.mparticle.MParticleOptions
import org.json.JSONArray
import java.lang.ref.WeakReference

interface CoreCallbacks {
    val isBackgrounded: Boolean

    val userBucket: Int

    val isEnabled: Boolean

    fun setIntegrationAttributes(kitId: Int, integrationAttributes: Map<String, String>)

    fun getIntegrationAttributes(kitId: Int): Map<String, String>?

    val currentActivity: WeakReference<Activity>

    @get:WorkerThread
    val latestKitConfiguration: JSONArray?

    val dataplanOptions: MParticleOptions.DataplanOptions?

    val isPushEnabled: Boolean

    val pushSenderId: String?

    val pushInstanceId: String?

    val launchUri: Uri?

    val launchAction: String?

    val kitListener: KitListener

    interface KitListener {
        fun kitFound(kitId: Int)

        fun kitConfigReceived(kitId: Int, configuration: String?)

        fun kitExcluded(kitId: Int, reason: String?)

        fun kitStarted(kitId: Int)

        fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?)

        fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?)

        companion object {
            @JvmField
            val EMPTY: KitListener = object : KitListener {
                override fun kitFound(kitId: Int) {}
                override fun kitConfigReceived(kitId: Int, configuration: String?) {}
                override fun kitExcluded(kitId: Int, reason: String?) {}
                override fun kitStarted(kitId: Int) {}
                override fun onKitApiCalled(kitId: Int, used: Boolean?, vararg objects: Any?) {}
                override fun onKitApiCalled(methodName: String?, kitId: Int, used: Boolean?, vararg objects: Any?) {}
            }
        }
    }
}
