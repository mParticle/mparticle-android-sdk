package com.mparticle;

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

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.PrefKeys;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Locale;
import java.util.TimeZone;

/* package-private */class DeviceAttributes {

    //re-use this whenever an attribute can't be determined
    private static final String UNKNOWN = "unknown";

    /**
     * Generates a collection of application attributes
     *
     * @param appContext the application context
     * @return a JSONObject of application-specific attributes
     */
    public static JSONObject collectAppInfo(Context appContext) {
        JSONObject attributes = new JSONObject();

        try {
            PackageManager packageManager = appContext.getPackageManager();
            String packageName = appContext.getPackageName();
            attributes.put(MessageKey.APP_PACKAGE_NAME, packageName);
            try {
                PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(packageName, 0);
                attributes.put(MessageKey.APP_VERSION_CODE, Integer.toString(pInfo.versionCode));
            } catch (PackageManager.NameNotFoundException nnfe) {
                attributes.put(MessageKey.APP_VERSION_CODE, UNKNOWN);
            }

            String installerPackageName = packageManager.getInstallerPackageName(packageName);
            if (null != installerPackageName) {
                attributes.put(MessageKey.APP_INSTALLER_NAME, installerPackageName);
            }
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
                attributes.put(MessageKey.APP_NAME, packageManager.getApplicationLabel(appInfo));
            } catch (PackageManager.NameNotFoundException e) {
                // ignore missing data
            }
            try {
                PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
                attributes.put(MessageKey.APP_VERSION, pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore missing data
            }
            SharedPreferences preferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            attributes.put(MessageKey.MPARTICLE_INSTALL_TIME, preferences.getLong(PrefKeys.INSTALL_TIME, 0));
            attributes.put(MessageKey.INSTALL_REFERRER, preferences.getString(PrefKeys.INSTALL_REFERRER, null));
            attributes.put(MessageKey.BUILD_ID, MPUtility.getBuildUUID(appContext));
            attributes.put(MessageKey.APP_DEBUG_SIGNING, MPUtility.isDebug(packageManager, packageName));
            attributes.put(MessageKey.APP_PIRATED, preferences.getBoolean(PrefKeys.PIRATED, false));
        } catch (JSONException e) {
            // ignore JSON exceptions
        }
        return attributes;
    }

    /**
     * Generates a collection of device attributes
     *
     * @param appContext the application context
     * @return a JSONObject of device-specific attributes
     */
    public static JSONObject collectDeviceInfo(Context appContext) {
        JSONObject attributes = new JSONObject();

        try {
            // device ID
            attributes.put(MessageKey.DEVICE_ID, Settings.Secure.getString(appContext.getContentResolver(),
                    Settings.Secure.ANDROID_ID));

            // device/OS attributes
            attributes.put(MessageKey.BUILD_ID, android.os.Build.ID);
            attributes.put(MessageKey.BRAND, Build.BRAND);
            attributes.put(MessageKey.PRODUCT, android.os.Build.PRODUCT);
            attributes.put(MessageKey.DEVICE, android.os.Build.DEVICE);
            attributes.put(MessageKey.MANUFACTURER, android.os.Build.MANUFACTURER);
            attributes.put(MessageKey.PLATFORM, "Android");
            attributes.put(MessageKey.OS_VERSION, android.os.Build.VERSION.SDK_INT);
            attributes.put(MessageKey.MODEL, android.os.Build.MODEL);

            JSONObject rootedObject = new JSONObject();
            rootedObject.put(MessageKey.DEVICE_ROOTED_CYDIA, isPhoneRooted());
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
                    attributes.put(MessageKey.MOBILE_NETWORK_CODE, networkOperator.substring(0, 3));
                    attributes.put(MessageKey.MOBILE_COUNTRY_CODE, networkOperator.substring(3));
                }

            }
            attributes.put(MessageKey.DEVICE_IS_TABLET, MPUtility.isTablet(appContext));
            attributes.put(MessageKey.DEVICE_ANID, MPUtility.getAndroidID(appContext));
            attributes.put(MessageKey.DEVICE_OPEN_UDID, MPUtility.getOpenUDID(appContext));

            /*
               Due to PII concerns, we are not currently sending this information.
            if (PackageManager.PERMISSION_GRANTED == appContext
                    .checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE)){
                attributes.put(MessageKey.DEVICE_IMEI, telephonyManager.getDeviceId());
            }else{
                attributes.put(MessageKey.DEVICE_IMEI, UNKNOWN);
            }

            if (PackageManager.PERMISSION_GRANTED == appContext
                    .checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE)){
                WifiManager wifiMan = (WifiManager) appContext.getSystemService(
                        Context.WIFI_SERVICE);
                WifiInfo wifiInf = wifiMan.getConnectionInfo();
                attributes.put(MessageKey.DEVICE_MAC_WIFI, wifiInf.getMacAddress());
            }else{
                attributes.put(MessageKey.DEVICE_MAC_WIFI, UNKNOWN);
            }

            if (PackageManager.PERMISSION_GRANTED == appContext
                    .checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH)){
                attributes.put(MessageKey.DEVICE_MAC_BLUETOOTH, BluetoothAdapter.getDefaultAdapter().getAddress());
            }else{
                attributes.put(MessageKey.DEVICE_MAC_BLUETOOTH, UNKNOWN);
            }*/
        } catch (JSONException e) {
            // ignore JSON exceptions
        }

        return attributes;
    }

    private static boolean isPhoneRooted() {

        // get from build info
        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        boolean bool = false;
        String[] arrayOfString1 = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String str : arrayOfString1) {
            File localFile = new File(str + "su");
            if (localFile.exists()) {
                bool = true;
                break;
            }
        }
        return bool;
    }
}
