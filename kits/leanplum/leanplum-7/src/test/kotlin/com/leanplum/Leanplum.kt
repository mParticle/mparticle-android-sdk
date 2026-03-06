package com.leanplum

object Leanplum {
    var mMode: LeanplumDeviceIdMode? = null
    var mAppID: String? = null
    var mAccessKey: String? = null

    @JvmStatic
    var deviceId: String? = null

    @JvmStatic
    fun setDeviceIdMode(mode: LeanplumDeviceIdMode?) {
        mMode = mode
    }

    @JvmStatic
    fun setAppIdForProductionMode(
        appId: String?,
        accessKey: String?,
    ) {
        mAppID = appId
        mAccessKey = accessKey
    }

    fun clear() {
        mMode = null
        deviceId = null
        mAppID = null
        mAccessKey = null
    }
}
