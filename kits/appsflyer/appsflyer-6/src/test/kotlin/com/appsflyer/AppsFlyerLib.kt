package com.appsflyer

import android.content.Context

class AppsFlyerLib {
    private var consentData: AppsFlyerConsent? = null

    fun setConsentData(consent: AppsFlyerConsent) {
        consentData = consent
    }

    fun getConsentData(): AppsFlyerConsent? = consentData

    fun getConsentState(): MutableMap<Any, Any> {
        val stateMap = mutableMapOf<Any, Any>()
        consentData?.let { consent ->
            // Use property names directly instead of getter methods
            consent.isUserSubjectToGDPR?.let { stateMap["isUserSubjectToGDPR"] = it }
            consent.hasConsentForDataUsage?.let { stateMap["hasConsentForDataUsage"] = it }
            consent.hasConsentForAdsPersonalization?.let { stateMap["hasConsentForAdsPersonalization"] = it }
            consent.hasConsentForAdStorage?.let { stateMap["hasConsentForAdStorage"] = it }
        }
        return stateMap
    }

    companion object {
        private var _instance: AppsFlyerLib? = null

        @JvmStatic
        fun getInstance(): AppsFlyerLib? {
            if (_instance == null) {
                _instance = AppsFlyerLib()
            }
            return _instance
        }

        @JvmStatic
        fun getInstance(context: Context?): AppsFlyerLib? = getInstance()

        /**
         * Access Methods
         */
        fun clearInstance() {
            _instance = null
        }
    }
}
