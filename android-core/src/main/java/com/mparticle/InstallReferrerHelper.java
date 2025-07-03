package com.mparticle;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

/**
 * Helper class to fetch install referrer via reflection.
 * <p>
 * To test the install referrer functionality:
 * 1. Set test application `applicationId` to a real app's Play Store package name, e.g. "com.medium.reader".
 * <p>
 * 2. Ensure the app is fully uninstalled from the test device:
 *    `adb uninstall com.medium.reader`
 * <p>
 * 3. Broadcast the test referrer to the Play Store. This will open the Play Store app:
 *    `adb shell "am start -a android.intent.action.VIEW -d 'https://play.google.com/store/apps/details?id=com.medium.reader&referrer=utm_source%3Dgoogle%26utm_medium%3Dcpc%26utm_term%3Drunning%252Bshoes%26utm_content%3Dcontent%26utm_campaign%3Dpromo'"`
 * <p>
 * 4. Install the debug version of your test app.
 * <p>
 * 5. Launch the app and check the logs for the referrer:
 *    `adb logcat -d | grep 'Install Referrer received'`
 * <p>
 * 6. Verify the referrer is logged in the test app's logs by checking for the utm_* parameters passed in during step 3.
 *    e.g. `Install Referrer received: utm_source=google&utm_medium=cpc&utm_term=running%2Bshoes&utm_content=content&utm_campaign=promo`
 *
 * @see <a href="https://developer.android.com/reference/com/android/installreferrer/api/InstallReferrerClient">InstallReferrerClient</a>
 * @see <a href="https://medium.com/@madicdjordje/how-to-test-the-play-store-install-referrer-api-78a63d59945b">How to Test the Play Store Install Referrer API</a>
 * @noinspection JavadocLinkAsPlainText
 */
public class InstallReferrerHelper {

    @Nullable
    public static String getInstallReferrer(@NonNull Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(Constants.PREFS_FILE, 0).getString(Constants.PrefKeys.INSTALL_REFERRER, null);
    }


    public static void setInstallReferrer(@NonNull Context context, @Nullable String referrer) {
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            preferences.edit().putString(Constants.PrefKeys.INSTALL_REFERRER, referrer).apply();
            MParticle instance = MParticle.getInstance();
            if (instance != null) {
                instance.installReferrerUpdated();
            }
        }
    }

    public static void fetchInstallReferrer(@NonNull final Context context, @NonNull final InstallReferrerCallback callback) {
        if (InstallReferrerHelper.getInstallReferrer(context) != null) {
            return;
        }
        if (MPUtility.isInstallRefApiAvailable()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        final InstallReferrerClient mReferrerClient = InstallReferrerClient.newBuilder(context).build();
                        InstallReferrerStateListener listener = new InstallReferrerStateListener() {
                            @Override
                            public void onInstallReferrerSetupFinished(int responseCode) {
                                switch (responseCode) {
                                    case InstallReferrerClient.InstallReferrerResponse.OK:
                                        try {
                                            ReferrerDetails response = mReferrerClient.getInstallReferrer();
                                            if (response != null) {
                                                callback.onReceived(response.getInstallReferrer());
                                            } else {
                                                callback.onFailed();
                                            }
                                            mReferrerClient.endConnection();
                                        } catch (RemoteException e) {
                                            Logger.warning("InstallReferrer Remote Exception, using InstallReferrer from intent.");
                                            callback.onFailed();
                                        } catch (Exception e) {
                                            Logger.warning("InstallReferrer Exception: " + e.getMessage() + ", using InstallReferrer from intent");
                                            callback.onFailed();
                                        }
                                        break;
                                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                                        Logger.warning("InstallReferrer not supported, using InstallReferrer from intent.");
                                        callback.onFailed();
                                        break;
                                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                                        Logger.warning("Unable to connect to InstallReferrer service, using InstallReferrer from intent.");
                                        callback.onFailed();
                                        break;
                                    default:
                                        Logger.warning("InstallReferrer responseCode not found, using InstallReferrer from intent.");
                                        callback.onFailed();
                                }
                            }

                            @Override
                            public void onInstallReferrerServiceDisconnected() {

                            }
                        };
                        mReferrerClient.startConnection(listener);
                    } catch (Exception e) {
                        Logger.error("Exception while fetching install referrer: " + e.getMessage());
                        callback.onFailed();
                    }
                }
            };
            try {
                if (Looper.getMainLooper() == Looper.myLooper()) {
                    new Thread(runnable).start();
                } else {
                    runnable.run();
                }
            } catch (Exception ignored) {
                callback.onFailed();
            }
        } else {
            callback.onFailed();
        }
    }

    public interface InstallReferrerCallback {
        void onReceived(@Nullable String installReferrer);

        void onFailed();
    }

}

