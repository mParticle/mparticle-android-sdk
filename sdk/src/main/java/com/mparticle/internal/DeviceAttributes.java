package com.mparticle.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mparticle.MParticle;
import com.mparticle.internal.Constants.MessageKey;
import com.mparticle.internal.Constants.PrefKeys;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/* package-private */class DeviceAttributes {

    //re-use this whenever an attribute can't be determined
    static final String UNKNOWN = "unknown";

    /**
     * Generates a collection of application attributes.  This will be lazy-loaded, and only ever called once per app run.
     *
     * @param appContext the application context
     * @return a JSONObject of application-specific attributes
     */
    public static JSONObject collectAppInfo(Context appContext) {
        JSONObject attributes = new JSONObject();
        SharedPreferences preferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        try {
            long now = System.currentTimeMillis();
            PackageManager packageManager = appContext.getPackageManager();
            String packageName = appContext.getPackageName();
            attributes.put(MessageKey.APP_PACKAGE_NAME, packageName);
            String versionCode = UNKNOWN;
            try {
                PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(packageName, 0);
                versionCode = Integer.toString(pInfo.versionCode);
                attributes.put(MessageKey.APP_VERSION, pInfo.versionName);
            } catch (PackageManager.NameNotFoundException nnfe) { }

            attributes.put(MessageKey.APP_VERSION_CODE, versionCode);

            String installerPackageName = packageManager.getInstallerPackageName(packageName);
            if (installerPackageName != null) {
                attributes.put(MessageKey.APP_INSTALLER_NAME, installerPackageName);
            }
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                attributes.put(MessageKey.APP_NAME, packageManager.getApplicationLabel(appInfo));
            } catch (PackageManager.NameNotFoundException e) {
                // ignore missing data
            }

            attributes.put(MessageKey.BUILD_ID, MPUtility.getBuildUUID(versionCode));
            attributes.put(MessageKey.APP_DEBUG_SIGNING, MPUtility.isAppDebuggable(appContext));
            attributes.put(MessageKey.APP_PIRATED, preferences.getBoolean(PrefKeys.PIRATED, false));

            attributes.put(MessageKey.MPARTICLE_INSTALL_TIME, preferences.getLong(PrefKeys.INSTALL_TIME, now));
            if (!preferences.contains(PrefKeys.INSTALL_TIME)) {
                editor.putLong(PrefKeys.INSTALL_TIME, now);
            }

            int totalRuns = preferences.getInt(PrefKeys.TOTAL_RUNS, 0) + 1;
            editor.putInt(PrefKeys.TOTAL_RUNS, totalRuns);
            attributes.put(MessageKey.LAUNCH_COUNT, totalRuns);

            long useDate = preferences.getLong(PrefKeys.LAST_USE, 0);
            attributes.put(MessageKey.LAST_USE_DATE, useDate);
            editor.putLong(PrefKeys.LAST_USE, now);
            try {
                PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
                int persistedVersion = preferences.getInt(PrefKeys.COUNTER_VERSION, -1);
                int countSinceUpgrade = preferences.getInt(PrefKeys.TOTAL_SINCE_UPGRADE, 0);
                long upgradeDate = preferences.getLong(PrefKeys.UPGRADE_DATE, now);

                if (persistedVersion < 0 || persistedVersion != pInfo.versionCode){
                    countSinceUpgrade = 0;
                    upgradeDate = now;
                    editor.putInt(PrefKeys.COUNTER_VERSION, pInfo.versionCode);
                    editor.putLong(PrefKeys.UPGRADE_DATE, upgradeDate);
                }
                countSinceUpgrade += 1;
                editor.putInt(PrefKeys.TOTAL_SINCE_UPGRADE, countSinceUpgrade);

                attributes.put(MessageKey.LAUNCH_COUNT_SINCE_UPGRADE, countSinceUpgrade);
                attributes.put(MessageKey.UPGRADE_DATE, upgradeDate);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore missing data
            }

            boolean install = preferences.getBoolean(PrefKeys.FIRST_RUN_INSTALL, true);
            attributes.put(MessageKey.FIRST_SEEN_INSTALL, install);

        } catch (JSONException e) {
            // again difference devices can do terrible things, make sure that we don't bail out completely
            // and return at least what we've built so far.
        } finally {
            editor.apply();
        }
        return attributes;
    }

    /**
     * Generates a collection of device attributes. This will be lazy-loaded, and only ever called once per app run.
     *
     * @param appContext the application context
     * @return a JSONObject of device-specific attributes
     */
    public static JSONObject collectDeviceInfo(Context appContext) {
        final JSONObject attributes = new JSONObject();

        try {
            // device/OS attributes
            attributes.put(MessageKey.BUILD_ID, android.os.Build.ID);
            attributes.put(MessageKey.BRAND, Build.BRAND);
            attributes.put(MessageKey.PRODUCT, android.os.Build.PRODUCT);
            attributes.put(MessageKey.DEVICE, android.os.Build.DEVICE);
            attributes.put(MessageKey.MANUFACTURER, android.os.Build.MANUFACTURER);
            attributes.put(MessageKey.PLATFORM, "Android");
            attributes.put(MessageKey.OS_VERSION, Build.VERSION.SDK);
            attributes.put(MessageKey.OS_VERSION_INT, Build.VERSION.SDK_INT);
            attributes.put(MessageKey.MODEL, android.os.Build.MODEL);
            attributes.put(MessageKey.RELEASE_VERSION, Build.VERSION.RELEASE);

            // device ID
            attributes.put(MessageKey.DEVICE_ID, Settings.Secure.getString(appContext.getContentResolver(),
                    Settings.Secure.ANDROID_ID));
            attributes.put(MessageKey.DEVICE_BLUETOOTH_ENABLED, MPUtility.isBluetoothEnabled(appContext));
            attributes.put(MessageKey.DEVICE_BLUETOOTH_VERSION, MPUtility.getBluetoothVersion(appContext));
            attributes.put(MessageKey.DEVICE_SUPPORTS_NFC, MPUtility.hasNfc(appContext));
            attributes.put(MessageKey.DEVICE_SUPPORTS_TELEPHONY, MPUtility.hasTelephony(appContext));



            JSONObject rootedObject = new JSONObject();
            rootedObject.put(MessageKey.DEVICE_ROOTED_CYDIA, MPUtility.isPhoneRooted());
            attributes.put(MessageKey.DEVICE_ROOTED, rootedObject);

            // screen height/width
            WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            attributes.put(MessageKey.SCREEN_HEIGHT, metrics.heightPixels);
            attributes.put(MessageKey.SCREEN_WIDTH, metrics.widthPixels);
            attributes.put(MessageKey.SCREEN_DPI, metrics.densityDpi);

            // locales
            Locale locale = Locale.getDefault();
            attributes.put(MessageKey.DEVICE_COUNTRY, locale.getDisplayCountry());
            attributes.put(MessageKey.DEVICE_LOCALE_COUNTRY, locale.getCountry());
            attributes.put(MessageKey.DEVICE_LOCALE_LANGUAGE, locale.getLanguage());
            attributes.put(MessageKey.DEVICE_TIMEZONE_NAME, MPUtility.getTimeZone());
            attributes.put(MessageKey.TIMEZONE, TimeZone.getDefault().getRawOffset() / (1000 * 60 * 60));
            // network
            TelephonyManager telephonyManager = (TelephonyManager) appContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            int phoneType = telephonyManager.getPhoneType();
            if (phoneType != TelephonyManager.PHONE_TYPE_NONE) {
                // NOTE: network attributes can be empty if phone is in airplane
                // mode and will not be set
                String networkCarrier = telephonyManager.getNetworkOperatorName();
                if (0 != networkCarrier.length()) {
                    attributes.put(MessageKey.NETWORK_CARRIER, networkCarrier);
                }
                String networkCountry = telephonyManager.getNetworkCountryIso();
                if (0 != networkCountry.length()) {
                    attributes.put(MessageKey.NETWORK_COUNTRY, networkCountry);
                }
                // android combines MNC+MCC into network operator
                String networkOperator = telephonyManager.getNetworkOperator();
                if (6 == networkOperator.length()) {
                    attributes.put(MessageKey.MOBILE_COUNTRY_CODE, networkOperator.substring(0, 3));
                    attributes.put(MessageKey.MOBILE_NETWORK_CODE, networkOperator.substring(3));
                }

            }
            attributes.put(MessageKey.DEVICE_IS_TABLET, MPUtility.isTablet(appContext));
            attributes.put(MessageKey.DEVICE_ANID, MPUtility.getAndroidID(appContext));
            attributes.put(MessageKey.DEVICE_OPEN_UDID, MPUtility.getOpenUDID(appContext));

            MPUtility.getGoogleAdIdInfo(appContext, new MPUtility.GoogleAdIdListener() {
                @Override
                public void onGoogleIdInfoRetrieved(String googleAdId, Boolean limitAdTrackingEnabled) {
                    if (limitAdTrackingEnabled == null) {
                        ConfigManager.log(MParticle.LogLevel.DEBUG, "Failed to collect Google Play Advertising ID, be sure to add Google Play services or com.google.android.gms:play-services-ads to your app's dependencies.");
                    }else {
                        try {
                            attributes.put(MessageKey.LIMIT_AD_TRACKING, limitAdTrackingEnabled);
                            if (limitAdTrackingEnabled) {
                                ConfigManager.log(MParticle.LogLevel.DEBUG, "Google Play Advertising ID available but ad tracking is disabled on this device.");
                            } else {
                                attributes.put(MessageKey.GOOGLE_ADV_ID, googleAdId);
                                ConfigManager.log(MParticle.LogLevel.DEBUG, "Successfully collected Google Play Advertising ID.");
                            }
                        }catch (JSONException jse) {

                        }
                    }
                }
            });


        } catch (Exception e) {
            //believe it or not, difference devices can be missing build.prop fields, or have otherwise
            //strange version/builds of Android that cause unpredictable behavior
        }

        return attributes;
    }
}
