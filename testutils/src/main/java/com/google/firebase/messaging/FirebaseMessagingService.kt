package com.google.firebase.messaging

import android.content.Context

object FirebaseMessagingServiceTestContext {
    var appContext: Context? = null
}

open class FirebaseMessagingService {
    var applicationContext: Context?
        get() = FirebaseMessagingServiceTestContext.appContext
        set(value) {
            FirebaseMessagingServiceTestContext.appContext = value
        }

    open fun onNewToken(token: String?) {}
}