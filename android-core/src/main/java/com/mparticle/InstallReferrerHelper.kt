package com.mparticle

import android.content.Context
import android.os.Looper
import android.os.RemoteException
import androidx.annotation.Nullable
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.mparticle.internal.Constants
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility

/**
 * Helper class to fetch install referrer via reflection.
 *
 * To test the install referrer functionality:
 * 1. Set test application `applicationId` to a real app's Play Store package name, e.g. "com.medium.reader".
 *
 * 2. Ensure the app is fully uninstalled from the test device:
 *    `adb uninstall com.medium.reader`
 *
 * 3. Broadcast the test referrer to the Play Store. This will open the Play Store app:
 *    `adb shell "am start -a android.intent.action.VIEW -d 'https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3Dgoogle%26utm_medium%3Dcpc%26utm_term%3Drunning%252Bshoes%26utm_content%3Dcontent%26utm_campaign%3Dpromo'"`
 *
 * 4. Install the debug version of your test app.
 *
 * 5. Launch the app and check the logs for the referrer:
 *    `adb logcat -d | grep 'Install Referrer received'`
 *
 * 6. Verify the referrer is logged in the test app's logs by checking for the utm_* parameters passed in during step 3.
 *    e.g. `Install Referrer received: utm_source=google&utm_medium=cpc&utm_term=running%2Bshoes&utm_content=content&utm_campaign=promo`
 *
 * @see <a href="https://developer.android.com/reference/com/android/installreferrer/api/InstallReferrerClient">InstallReferrerClient</a>
 * @see <a href="https://medium.com/@madicdjordje/how-to-test-the-play-store-install-referrer-api-78a63d59945b">How to Test the Play Store Install Referrer API</a>
 */
open class InstallReferrerHelper {
    interface InstallReferrerCallback {
        fun onReceived(@Nullable installReferrer: String?)

        fun onFailed()
    }

    companion object {
        @Nullable
        @JvmStatic
        fun getInstallReferrer(context: Context?): String? {
            if (context == null) {
                return null
            }
            return context.getSharedPreferences(Constants.PREFS_FILE, 0).getString(Constants.PrefKeys.INSTALL_REFERRER, null)
        }

        @JvmStatic
        fun setInstallReferrer(context: Context?, @Nullable referrer: String?) {
            if (context != null) {
                val preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)
                preferences.edit().putString(Constants.PrefKeys.INSTALL_REFERRER, referrer).apply()
                val instance = MParticle.getInstance()
                if (instance != null) {
                    instance.installReferrerUpdated()
                }
            }
        }

        @JvmStatic
        fun fetchInstallReferrer(context: Context?, callback: InstallReferrerCallback) {
            if (getInstallReferrer(context) != null) {
                return
            }
            if (MPUtility.isInstallRefApiAvailable()) {
                val runnable =
                    Runnable {
                        try {
                            val mReferrerClient = InstallReferrerClient.newBuilder(context).build()
                            val listener =
                                object : InstallReferrerStateListener {
                                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                                        when (responseCode) {
                                            InstallReferrerClient.InstallReferrerResponse.OK -> {
                                                try {
                                                    val response = mReferrerClient.installReferrer
                                                    if (response != null) {
                                                        callback.onReceived(response.installReferrer)
                                                    } else {
                                                        callback.onFailed()
                                                    }
                                                    mReferrerClient.endConnection()
                                                } catch (e: RemoteException) {
                                                    Logger.warning("InstallReferrer Remote Exception, using InstallReferrer from intent.")
                                                    callback.onFailed()
                                                } catch (e: Exception) {
                                                    Logger.warning("InstallReferrer Exception: ${e.message}, using InstallReferrer from intent")
                                                    callback.onFailed()
                                                }
                                            }
                                            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                                                Logger.warning("InstallReferrer not supported, using InstallReferrer from intent.")
                                                callback.onFailed()
                                            }
                                            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                                                Logger.warning("Unable to connect to InstallReferrer service, using InstallReferrer from intent.")
                                                callback.onFailed()
                                            }
                                            else -> {
                                                Logger.warning("InstallReferrer responseCode not found, using InstallReferrer from intent.")
                                                callback.onFailed()
                                            }
                                        }
                                    }

                                    override fun onInstallReferrerServiceDisconnected() {
                                    }
                                }
                            mReferrerClient.startConnection(listener)
                        } catch (e: Exception) {
                            Logger.error("Exception while fetching install referrer: ${e.message}")
                            callback.onFailed()
                        }
                    }
                try {
                    if (Looper.getMainLooper() == Looper.myLooper()) {
                        Thread(runnable).start()
                    } else {
                        runnable.run()
                    }
                } catch (ignored: Exception) {
                    callback.onFailed()
                }
            } else {
                callback.onFailed()
            }
        }
    }
}
