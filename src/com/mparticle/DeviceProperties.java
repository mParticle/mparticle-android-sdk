package com.mparticle;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.PrefKeys;

public class DeviceProperties {

    /**
     * Generates a collection of application properties
     * @param appContext the application context
     * @return a JSONObject of application-specific attributes
     */
    public static JSONObject collectAppInfo(Context appContext) {
        JSONObject properties = new JSONObject();

        try {
            PackageManager packageManager = appContext.getPackageManager();
            String packageName = appContext.getPackageName();
            properties.put(MessageKey.APP_PACKAGE_NAME, packageName);
            String installerPackageName = packageManager.getInstallerPackageName(packageName);
            if (null!=installerPackageName) {
                properties.put(MessageKey.APP_INSTALLER_NAME, installerPackageName);
            }
            try {
                ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName,0);
                properties.put(MessageKey.APP_NAME, packageManager.getApplicationLabel(appInfo));
            } catch (PackageManager.NameNotFoundException e) {
                // ignore missing data
            }
            try {
                PackageInfo pInfo = packageManager.getPackageInfo(packageName, 0);
                properties.put(MessageKey.APP_VERSION, pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                // ignore missing data
            }
            SharedPreferences preferences = appContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
            properties.put(MessageKey.MPARTICLE_INSTALL_TIME, preferences.getLong(PrefKeys.INSTALL_TIME, 0));
        } catch (JSONException e) {
            // ignore JSON exceptions
        }
        return properties;
    }

    /**
     * Generates a collection of device properties
     * @param appContext the application context
     * @return a JSONObject of device-specific attributes
     */
    public static JSONObject collectDeviceInfo(Context appContext) {
        JSONObject properties = new JSONObject();

        try {
            // device ID
            properties.put(MessageKey.DEVICE_ID, Settings.Secure.getString(appContext.getContentResolver(),
                    Settings.Secure.ANDROID_ID));

            // device/OS properties
            properties.put(MessageKey.BUILD_ID, android.os.Build.ID);
            properties.put(MessageKey.BRAND, android.os.Build.BRAND);
            properties.put(MessageKey.PRODUCT, android.os.Build.PRODUCT);
            properties.put(MessageKey.DEVICE, android.os.Build.DEVICE);
            properties.put(MessageKey.MANUFACTURER, android.os.Build.MANUFACTURER);
            properties.put(MessageKey.PLATFORM, "Android");
            properties.put(MessageKey.OS_VERSION, android.os.Build.VERSION.SDK_INT);
            properties.put(MessageKey.MODEL, android.os.Build.MODEL);

            // screen height/width
            WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            properties.put(MessageKey.SCREEN_HEIGHT, metrics.heightPixels);
            properties.put(MessageKey.SCREEN_WIDTH, metrics.widthPixels);

            // locales
            Locale locale = Locale.getDefault();
            properties.put(MessageKey.DEVICE_COUNTRY, locale.getDisplayCountry());
            properties.put(MessageKey.DEVICE_LOCALE_COUNTRY, locale.getCountry());
            properties.put(MessageKey.DEVICE_LOCALE_LANGUAGE, locale.getLanguage());

            // network
            TelephonyManager telephonyManager = (TelephonyManager)appContext.getSystemService(Context.TELEPHONY_SERVICE);
            int phoneType = telephonyManager.getPhoneType();
            if (phoneType!=TelephonyManager.PHONE_TYPE_NONE) {
                // NOTE: network properties can be empty if phone is in airplane mode and will not be set
                String networkCarrier = telephonyManager.getNetworkOperatorName();
                if (0!=networkCarrier.length()) {
                    properties.put(MessageKey.NETWORK_CARRIER, networkCarrier);
                }
                String networkCountry = telephonyManager.getNetworkCountryIso();
                if (0!=networkCountry.length()) {
                    properties.put(MessageKey.NETWORK_COUNTRY, networkCountry);
                }
                // android combines MNC+MCC into network operator
                String networkOperator = telephonyManager.getNetworkOperator();
                if (6==networkOperator.length()) {
                    properties.put(MessageKey.MOBILE_NETWORK_CODE, networkOperator.substring(0, 3));
                    properties.put(MessageKey.MOBILE_COUNTRY_CODE, networkOperator.substring(3));
                }
            }

            // timezone
            properties.put(MessageKey.TIMEZONE, TimeZone.getDefault().getRawOffset()/(1000*60*60));
        } catch (JSONException e) {
            // ignore JSON exceptions
        }

        return properties;
    }

}
