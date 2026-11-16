package com.leanplum

object Leanplum {
    var mMode: LeanplumDeviceIdMode? = null
    var mAppID: String? = null
    var mAccessKey: String? = null

    @JvmStatic
    var deviceId: String? = null

    private var currentUserId: String? = null
    private var testModeEnabled = false

    @JvmStatic
    fun getUserId(): String? = currentUserId

    @JvmStatic
    fun setUserId(userId: String?) {
        currentUserId = userId
    }

    @JvmStatic
    fun isTestModeEnabled(): Boolean = testModeEnabled

    @JvmStatic
    fun setIsTestModeEnabled(isTestModeEnabled: Boolean) {
        testModeEnabled = isTestModeEnabled
    }

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
        currentUserId = null
        testModeEnabled = false
    }
}
