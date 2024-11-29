package com.mparticle.internal

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.mparticle.MParticle
import com.mparticle.internal.Constants.MessageKey
import com.mparticle.internal.Constants.PrefKeys
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone

class DeviceAttributes
/**
 * package-private
 */ internal constructor(private val operatingSystem: MParticle.OperatingSystem?) {
    private var deviceInfo: JSONObject? = null
    private var appInfo: JSONObject? = null
    private var firstCollection = true

    private val sideloadedKitsCount: Int
        get() {
            try {
                val kits = MParticle.getInstance()?.Internal()?.kitManager?.supportedKits
                var count = 0
                if (kits != null) {
                    for (kitId in kits) {
                        if (kitId >= 1000000) {
                            count++
                        }
                    }
                }
                return count
            } catch (e: Exception) {
                Logger.debug("Exception while adding sideloadedKitsCount to Device Attribute")
                return 0
            }
        }

    /**
     * Generates a collection of application attributes that will not change during an app's process.
     *
     *
     * This contains logic that MUST only be called once per app run.
     *
     * @param appContext the application context
     * @return a JSONObject of application-specific attributes
     */
    fun getStaticApplicationInfo(appContext: Context): JSONObject {
        val attributes = JSONObject()
        val preferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        val editor = preferences.edit()
        try {
            val now = System.currentTimeMillis()
            val packageManager = appContext.packageManager
            val packageName = appContext.packageName
            attributes.put(MessageKey.APP_PACKAGE_NAME, packageName)
            attributes.put(MessageKey.SIDELOADED_KITS_COUNT, sideloadedKitsCount)
            var versionCode = UNKNOWN
            try {
                val pInfo = appContext.packageManager.getPackageInfo(packageName, 0)
                versionCode = pInfo.versionCode.toString()
                attributes.put(MessageKey.APP_VERSION, pInfo.versionName)
            } catch (nnfe: PackageManager.NameNotFoundException) {
            }

            attributes.put(MessageKey.APP_VERSION_CODE, versionCode)

            val installerPackageName = packageManager.getInstallerPackageName(packageName)
            if (installerPackageName != null) {
                attributes.put(MessageKey.APP_INSTALLER_NAME, installerPackageName)
            }
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                attributes.put(MessageKey.APP_NAME, packageManager.getApplicationLabel(appInfo))
            } catch (e: PackageManager.NameNotFoundException) {
                // ignore missing data
            }

            attributes.put(MessageKey.BUILD_ID, MPUtility.getBuildUUID(versionCode))
            attributes.put(MessageKey.APP_DEBUG_SIGNING, MPUtility.isAppDebuggable(appContext))
            attributes.put(MessageKey.APP_PIRATED, preferences.getBoolean(PrefKeys.PIRATED, false))

            attributes.put(MessageKey.MPARTICLE_INSTALL_TIME, preferences.getLong(PrefKeys.INSTALL_TIME, now))
            if (!preferences.contains(PrefKeys.INSTALL_TIME)) {
                editor.putLong(PrefKeys.INSTALL_TIME, now)
            }
            val userStorage = ConfigManager.getUserStorage(appContext)
            val totalRuns = userStorage.getTotalRuns(0) + 1
            userStorage.setTotalRuns(totalRuns)
            attributes.put(MessageKey.LAUNCH_COUNT, totalRuns)

            val useDate = userStorage.getLastUseDate(0)
            attributes.put(MessageKey.LAST_USE_DATE, useDate)
            userStorage.lastUseDate = now
            try {
                val pInfo = packageManager.getPackageInfo(packageName, 0)
                val persistedVersion = preferences.getInt(PrefKeys.COUNTER_VERSION, -1)
                var countSinceUpgrade = userStorage.launchesSinceUpgrade
                var upgradeDate = preferences.getLong(PrefKeys.UPGRADE_DATE, now)

                if (persistedVersion < 0 || persistedVersion != pInfo.versionCode) {
                    countSinceUpgrade = 0
                    upgradeDate = now
                    editor.putInt(PrefKeys.COUNTER_VERSION, pInfo.versionCode)
                    editor.putLong(PrefKeys.UPGRADE_DATE, upgradeDate)
                }
                countSinceUpgrade += 1
                userStorage.launchesSinceUpgrade = countSinceUpgrade

                attributes.put(MessageKey.LAUNCH_COUNT_SINCE_UPGRADE, countSinceUpgrade)
                attributes.put(MessageKey.UPGRADE_DATE, upgradeDate)
            } catch (e: PackageManager.NameNotFoundException) {
                // ignore missing data
            }
            val instance = MParticle.getInstance()
            if (instance != null) {
                attributes.put(MessageKey.ENVIRONMENT, ConfigManager.getEnvironment().value)
            }
            attributes.put(MessageKey.INSTALL_REFERRER, preferences.getString(PrefKeys.INSTALL_REFERRER, null))

            val install = preferences.getBoolean(PrefKeys.FIRST_RUN_INSTALL, true)
            attributes.put(MessageKey.FIRST_SEEN_INSTALL, install)
            editor.putBoolean(PrefKeys.FIRST_RUN_INSTALL, false)
        } catch (e: Exception) {
            // again different devices can do terrible things, make sure that we don't bail out completely
            // and return at least what we've built so far.
        } finally {
            editor.apply()
        }
        return attributes
    }

    fun updateInstallReferrer(context: Context, attributes: JSONObject) {
        val preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
        try {
            attributes.put(MessageKey.INSTALL_REFERRER, preferences.getString(PrefKeys.INSTALL_REFERRER, null))
        } catch (ignored: JSONException) {
            // this, hopefully, should never fail
        }
    }

    /**
     * Generates a collection of device attributes that will not change during an app's process.
     *
     *
     * This contains logic that MUST only be called once per app run.
     *
     * @param appContext the application context
     * @return a JSONObject of device-specific attributes
     */
    fun getStaticDeviceInfo(appContext: Context): JSONObject {
        val attributes = JSONObject()

        try {
            // device/OS attributes
            attributes.put(MessageKey.BUILD_ID, Build.ID)
            attributes.put(MessageKey.BRAND, Build.BRAND)
            attributes.put(MessageKey.PRODUCT, Build.PRODUCT)
            attributes.put(MessageKey.DEVICE, Build.DEVICE)
            attributes.put(MessageKey.MANUFACTURER, Build.MANUFACTURER)
            attributes.put(MessageKey.PLATFORM, operatingSystemString)
            attributes.put(MessageKey.OS_VERSION, Build.VERSION.SDK)
            attributes.put(MessageKey.OS_VERSION_INT, Build.VERSION.SDK_INT)
            attributes.put(MessageKey.MODEL, Build.MODEL)
            attributes.put(MessageKey.RELEASE_VERSION, Build.VERSION.RELEASE)

            val application = appContext as Application
            // device ID
            addAndroidId(attributes, application)

            attributes.put(MessageKey.DEVICE_BLUETOOTH_ENABLED, MPUtility.isBluetoothEnabled(appContext))
            attributes.put(MessageKey.DEVICE_BLUETOOTH_VERSION, MPUtility.getBluetoothVersion(appContext))
            attributes.put(MessageKey.DEVICE_SUPPORTS_NFC, MPUtility.hasNfc(appContext))
            attributes.put(MessageKey.DEVICE_SUPPORTS_TELEPHONY, MPUtility.hasTelephony(appContext))

            val rootedObject = JSONObject()
            rootedObject.put(MessageKey.DEVICE_ROOTED_CYDIA, MPUtility.isPhoneRooted())
            attributes.put(MessageKey.DEVICE_ROOTED, rootedObject)

            // screen height/width
            val displayMetrics = appContext.getResources().displayMetrics
            attributes.put(MessageKey.SCREEN_HEIGHT, displayMetrics.heightPixels)
            attributes.put(MessageKey.SCREEN_WIDTH, displayMetrics.widthPixels)
            attributes.put(MessageKey.SCREEN_DPI, displayMetrics.densityDpi)

            // locales
            val locale = Locale.getDefault()
            attributes.put(MessageKey.DEVICE_COUNTRY, locale.displayCountry)
            attributes.put(MessageKey.DEVICE_LOCALE_COUNTRY, locale.country)
            attributes.put(MessageKey.DEVICE_LOCALE_LANGUAGE, locale.language)
            attributes.put(MessageKey.DEVICE_TIMEZONE_NAME, MPUtility.getTimeZone())
            attributes.put(MessageKey.TIMEZONE, TimeZone.getDefault().rawOffset / (1000 * 60 * 60))
            // network
            val telephonyManager = appContext
                .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val phoneType = telephonyManager.phoneType
            if (phoneType != TelephonyManager.PHONE_TYPE_NONE) {
                // NOTE: network attributes can be empty if phone is in airplane
                // mode and will not be set
                val networkCarrier = telephonyManager.networkOperatorName
                if (0 != networkCarrier.length) {
                    attributes.put(MessageKey.NETWORK_CARRIER, networkCarrier)
                }
                val networkCountry = telephonyManager.networkCountryIso
                if (0 != networkCountry.length) {
                    attributes.put(MessageKey.NETWORK_COUNTRY, networkCountry)
                }
                // android combines MNC+MCC into network operator
                val networkOperator = telephonyManager.networkOperator
                if (6 == networkOperator.length) {
                    attributes.put(MessageKey.MOBILE_COUNTRY_CODE, networkOperator.substring(0, 3))
                    attributes.put(MessageKey.MOBILE_NETWORK_CODE, networkOperator.substring(3))
                }
            }
            attributes.put(MessageKey.DEVICE_IS_TABLET, MPUtility.isTablet(appContext))
            attributes.put(MessageKey.DEVICE_IS_IN_DST, MPUtility.isInDaylightSavings())

            if (!MPUtility.isEmpty(deviceImei)) {
                attributes.put(MessageKey.DEVICE_IMEI, deviceImei)
            }
        } catch (e: Exception) {
            // believe it or not, difference devices can be missing build.prop fields, or have otherwise
            // strange version/builds of Android that cause unpredictable behavior
        }

        return attributes
    }

    /**
     * For the following fields we always want the latest values
     */
    fun updateDeviceInfo(context: Context, deviceInfo: JSONObject) {
        deviceInfo.remove(MessageKey.LIMIT_AD_TRACKING)
        deviceInfo.remove(MessageKey.GOOGLE_ADV_ID)
        val adIdInfo = MPUtility.getAdIdInfo(context)
        var message =
            "Failed to collect Advertising ID, be sure to add Google Play services (com.google.android.gms:play-services-ads) or Amazon Ads (com.amazon.android:mobile-ads) to your app's dependencies."
        if (adIdInfo != null) {
            try {
                deviceInfo.put(MessageKey.LIMIT_AD_TRACKING, adIdInfo.isLimitAdTrackingEnabled)
                val instance = MParticle.getInstance()
                // check instance nullability here and decline to act if it is not available. Don't want to have the case where we are overriding isLimiAdTrackingEnabled
                // just because there was a timing issue with the singleton
                if (instance != null) {
                    if (adIdInfo.isLimitAdTrackingEnabled) {
                        message = adIdInfo.advertiser.descriptiveName + " Advertising ID tracking is disabled on this device."
                    } else {
                        when (adIdInfo.advertiser) {
                            MPUtility.AdIdInfo.Advertiser.AMAZON -> deviceInfo.put(MessageKey.AMAZON_ADV_ID, adIdInfo.id)
                            MPUtility.AdIdInfo.Advertiser.GOOGLE -> deviceInfo.put(MessageKey.GOOGLE_ADV_ID, adIdInfo.id)
                        }
                        message = "Successfully collected " + adIdInfo.advertiser.descriptiveName + " Advertising ID."
                    }
                }
            } catch (jse: JSONException) {
                Logger.debug("Failed while building device-customAttributes object: ", jse.toString())
            }
        }
        if (firstCollection) {
            Logger.debug(message)
            firstCollection = false
        }

        try {
            val mParticle = MParticle.getInstance()
            if (mParticle != null) {
                val configManager = mParticle.Internal().configManager
                val registration = configManager.pushRegistration
                if (registration != null && !MPUtility.isEmpty(registration.instanceId)) {
                    deviceInfo.put(MessageKey.PUSH_TOKEN, registration.instanceId)
                    deviceInfo.put(MessageKey.PUSH_TOKEN_TYPE, Constants.GOOGLE_GCM)
                }
            }
        } catch (jse: JSONException) {
            Logger.debug("Failed while building device-customAttributes object: ", jse.toString())
        }
    }

    fun getDeviceInfo(context: Context): JSONObject {
        if (deviceInfo == null) {
            deviceInfo = getStaticDeviceInfo(context)
        }
        deviceInfo?.let {
            updateDeviceInfo(context, it)
            return it
        } ?: run {
            return JSONObject()
        }
    }

    fun getAppInfo(context: Context): JSONObject {
        return getAppInfo(context, false)
    }

    fun getAppInfo(context: Context, forceUpdateInstallReferrer: Boolean): JSONObject {
        if (appInfo == null) {
            appInfo = getStaticApplicationInfo(context)
            appInfo?.let { updateInstallReferrer(context, it) }
        } else if (forceUpdateInstallReferrer) {
            appInfo?.let { updateInstallReferrer(context, it) }
        }
        return appInfo as JSONObject
    }

    val operatingSystemString: String
        get() = when (operatingSystem) {
            MParticle.OperatingSystem.ANDROID -> Constants.Platform.ANDROID
            MParticle.OperatingSystem.FIRE_OS -> Constants.Platform.FIRE_OS
            else -> Constants.Platform.ANDROID
        }

    companion object {
        // re-use this whenever an attribute can't be determined
        const val UNKNOWN: String = "unknown"

        @Volatile
        private var _deviceImei: String? = null

        @get:JvmStatic
        val deviceImei: String?
            get() = _deviceImei ?: null

        @JvmStatic
        fun setDeviceImei(deviceImei: String?) {
            _deviceImei = deviceImei
        }

        @Throws(JSONException::class)
        fun addAndroidId(attributes: JSONObject, context: Context) {
            val androidId = MPUtility.getAndroidID(context)
            if (!MPUtility.isEmpty(androidId)) {
                attributes.put(MessageKey.DEVICE_ID, androidId)
                attributes.put(MessageKey.DEVICE_ANID, androidId)
                attributes.put(MessageKey.DEVICE_OPEN_UDID, MPUtility.getOpenUDID(context))
            }
        }
    }
}
