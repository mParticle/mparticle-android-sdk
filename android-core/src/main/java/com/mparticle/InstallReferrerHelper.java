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

