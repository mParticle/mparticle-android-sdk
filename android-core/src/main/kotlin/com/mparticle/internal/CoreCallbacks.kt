package com.mparticle.internal

import android.app.Activity
import android.net.Uri
import androidx.annotation.WorkerThread
import com.mparticle.MParticleOptions.DataplanOptions
import org.json.JSONArray
import java.lang.ref.WeakReference

interface CoreCallbacks {
    fun isBackgrounded(): Boolean

    fun getUserBucket(): Int

    fun isEnabled(): Boolean

    fun setIntegrationAttributes(kitId: Int, integrationAttributes: Map<String, String>)

    fun getIntegrationAttributes(kitId: Int): Map<String, String>?

    fun getCurrentActivity(): WeakReference<Activity>?

    @WorkerThread
    fun getLatestKitConfiguration(): JSONArray?

    fun getDataplanOptions(): DataplanOptions?

    fun isPushEnabled(): Boolean

    fun getPushSenderId(): String?

    fun getPushInstanceId(): String?

    fun getLaunchUri(): Uri?

    fun getLaunchAction(): String?

    fun getKitListener(): KitListener?

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