package com.mparticle

enum class WrapperSdk(wrapperSdkId: Int, wrapperSdkName: String) {
    WrapperNone(0, "None"),
    WrapperSdkUnity(1, "Unity"),
    WrapperSdkReactNative(2, "React Native"),
    WrapperSdkCordova(3, "Cordova"),
    WrapperXamarin(4, "Xamarin"),
    WrapperFlutter(5, "Flutter"),
    WrapperMaui(6, "Maui");
}

/**
 * @param sdk represent the wrapper sdk. If not configured will be [WrapperSdk.WrapperNone]
 * @param version represents the configured version for the wrapper sdk. Will return null if
 * [WrapperSdk.WrapperNone] is set as the sdk wrapper
 */
data class WrapperSdkVersion(val sdk: WrapperSdk, val version: String?)