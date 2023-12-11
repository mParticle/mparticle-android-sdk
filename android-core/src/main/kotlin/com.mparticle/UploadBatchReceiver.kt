package com.mparticle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.mparticle.internal.Logger

internal class UploadBatchReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UPLOAD_BATCH = "ACTION_UPLOAD_BATCH"
        fun getIntentFilter(): IntentFilter {
            return IntentFilter().apply { addAction(ACTION_UPLOAD_BATCH) }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Logger.debug("Received broadcast ${this::class.java.name}")
        intent?.let {
            if (it.action == ACTION_UPLOAD_BATCH) {
                try {
                    MParticle.getInstance()?.let {
                        //Do if there is a non-null mParticle instance, force upload messages
                        it.upload()
                        Logger.debug("Uploading events in upload batch receiver")
                    } ?: run {
                        Logger.debug("Batches cant be uploaded in receiver because MParticle instance is null")
                    }
                } catch (e: Exception) {
                    Logger.error("Error while uploading batches in upload batch reveiver")
                }
            }
        }
    }
}