package com.mparticle.kits

import android.content.Context
import androidx.annotation.WorkerThread

internal object IterableDeviceIdHelper {
    @WorkerThread
    fun getGoogleAdId(context: Context?): String? {
        try {
            val advertisingIdClient =
                Class
                    .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            val getAdvertisingInfo =
                advertisingIdClient.getMethod(
                    "getAdvertisingIdInfo",
                    Context::class.java,
                )
            val advertisingInfo = getAdvertisingInfo.invoke(null, context)
            val isLimitAdTrackingEnabled =
                advertisingInfo.javaClass.getMethod(
                    "isLimitAdTrackingEnabled",
                )
            val limitAdTrackingEnabled =
                isLimitAdTrackingEnabled
                    .invoke(advertisingInfo) as Boolean
            val getId = advertisingInfo.javaClass.getMethod("getId")
            val advertisingId = getId.invoke(advertisingInfo) as String
            if (!limitAdTrackingEnabled) {
                return advertisingId
            }
        } catch (ignored: Exception) {
        }
        return null
    }
}
