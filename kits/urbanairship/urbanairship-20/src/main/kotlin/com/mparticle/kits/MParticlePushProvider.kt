package com.mparticle.kits

import android.content.Context
import com.urbanairship.Platform
import com.urbanairship.push.PushProvider

/**
 * Used to register for push in the Urban Airship SDK.
 */
internal class MParticlePushProvider private constructor() : PushProvider {
    private var token: String? = null

    override val platform: Platform = Platform.ANDROID

    override val deliveryType: PushProvider.DeliveryType = PushProvider.DeliveryType.FCM

    override fun getRegistrationToken(context: Context): String? = token

    override fun isAvailable(context: Context): Boolean = true

    override fun isSupported(context: Context): Boolean = true

    fun setRegistrationToken(token: String?) {
        this.token = token
    }

    companion object {
        val instance = MParticlePushProvider()
    }
}
