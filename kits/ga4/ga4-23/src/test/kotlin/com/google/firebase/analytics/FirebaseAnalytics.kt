package com.google.firebase.analytics

import android.app.Activity
import android.content.Context
import android.os.Bundle
import java.util.AbstractMap.SimpleEntry
import java.util.LinkedList

class FirebaseAnalytics {
    var loggedEvents: LinkedList<Map.Entry<String, Bundle>> = LinkedList()
    var currentScreenName: String? = null
    var consentStateMap: MutableMap<Any, Any> = mutableMapOf()

    object Event {
        const val ADD_PAYMENT_INFO = "add_payment_info"
        const val ADD_SHIPPING_INFO = "add_shipping_info"
    }

    fun logEvent(
        key: String,
        bundle: Bundle,
    ) {
        loggedEvents.add(SimpleEntry(key, bundle))
    }

    fun setCurrentScreen(
        currentActivity: Activity?,
        screenName: String?,
        classOverride: String?,
    ) {
        currentScreenName = screenName
    }

    fun setConsent(var1: MutableMap<Any, Any>) {
        consentStateMap.putAll(var1)
    }

    fun getConsentState(): MutableMap<Any, Any> = consentStateMap

    fun setUserProperty(
        key: String?,
        value: String?,
    ) {}

    fun getLoggedEvents(): List<Map.Entry<String, Bundle>> = loggedEvents

    fun clearLoggedEvents() {
        loggedEvents = LinkedList()
    }

    companion object {
        var firebaseInstanceId: String? = null
        var instance: FirebaseAnalytics? = null

        @JvmStatic
        fun getInstance(context: Context?): FirebaseAnalytics? {
            if (instance == null) {
                instance = FirebaseAnalytics()
            }
            return instance
        }

        /**
         * Access Methods
         */
        fun clearInstance() {
            instance = null
        }

        fun setFirebaseId(firebaseId: String?) {
            firebaseInstanceId = firebaseId
        }
    }
}
