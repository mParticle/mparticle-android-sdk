package com.singular.sdk

object Singular {
    var acceptEventJson = false
    var acceptEvent = false
    var limitDataSharingValue: Boolean? = null

    @JvmStatic
    fun limitDataSharing(shouldLimitDataSharing: Boolean) {
        limitDataSharingValue = shouldLimitDataSharing
    }
}
