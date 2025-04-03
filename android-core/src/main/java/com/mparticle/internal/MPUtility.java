package com.mparticle.internal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.instantapps.InstantApps;
import com.mparticle.MParticle;
import com.mparticle.networking.MPConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mixin utility class responsible for generating all sorts of device information, mostly
 * used by the DeviceInfo and AppInfo dictionaries within batch messages.
 */
public class MPUtility {

    static final String NO_BLUETOOTH = "none";
    private static String sOpenUDID;
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final String TAG = MPUtility.class.toString();
    private static AdIdInfo adInfoId = null;

    public static long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.availMem;
    }

    public static boolean isSystemMemoryLow(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    public static long getSystemMemoryThreshold(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.threshold;
    }

    public static long getRemainingHeapInBytes() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemBytes = (runtime.totalMemory() - runtime.freeMemory());
        final long maxHeapSizeInBytes = runtime.maxMemory();
        return maxHeapSizeInBytes - usedMemBytes;
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean isEmpty(JSONArray jsonArray) {
        return jsonArray == null || jsonArray.length() == 0;
    }

    public static boolean isEmpty(JSONObject jsonObject) {
        return jsonObject == null || jsonObject.length() == 0;
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.size() == 0;
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    @WorkerThread
    @Nullable
    public static AdIdInfo getAdIdInfo(Context context) {
        if (adInfoId != null) {
            return adInfoId;
        }
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        String installerName = packageManager.getInstallerPackageName(packageName);
        if ((installerName != null && installerName.contains("com.amazon.venezia")) ||
                "Amazon".equals(android.os.Build.MANUFACTURER)) {
            adInfoId = getAmazonAdIdInfo(context);
            if (adInfoId == null) {
                return getGoogleAdIdInfo(context);
            }
            return adInfoId;
        } else {
            adInfoId = getGoogleAdIdInfo(context);
            if (adInfoId == null) {
                return getAmazonAdIdInfo(context);
            }
            return adInfoId;
        }
    }

    private static AdIdInfo getGoogleAdIdInfo(Context context) {
        try {
            Class AdvertisingIdClient = Class
                    .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo",
                    Context.class);
            Object advertisingInfo = getAdvertisingInfo.invoke(null, context);
            Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod(
                    "isLimitAdTrackingEnabled");
            Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled
                    .invoke(advertisingInfo);
            Method getId = advertisingInfo.getClass().getMethod("getId");
            String advertisingId = (String) getId.invoke(advertisingInfo);
            return new AdIdInfo(advertisingId, limitAdTrackingEnabled, AdIdInfo.Advertiser.GOOGLE);
        } catch (Exception e) {
            Logger.info(TAG, "Could not locate Google Play Ads Identifier library");
        }
        return null;
    }

    private static AdIdInfo getAmazonAdIdInfo(Context context) {
        // https://developer.amazon.com/public/solutions/platforms/fire-os/docs/fire-os-advertising-id
        // https://forums.developer.amazon.com/articles/18194/using-the-advertising-id-in-your-app.html
        String advertisingID = "";
        boolean limitAdTracking;
        try {
            ContentResolver cr = context.getContentResolver();
            limitAdTracking = (Settings.Secure.getInt(cr, "limit_ad_tracking", 0) == 0) ? false : true;
            advertisingID = Settings.Secure.getString(cr, "advertising_id");
            if (advertisingID != null) {
                return new AdIdInfo(advertisingID, limitAdTracking, AdIdInfo.Advertiser.AMAZON);
            }
        } catch (Exception e) {
            Logger.info(TAG, "Could not locate Amazon ID on device: " + e.getMessage());
        }
        return null;
    }

    public static boolean isInDaylightSavings() {
        return Boolean.valueOf(TimeZone.getDefault().inDaylightTime(new Date()));
    }

    public static boolean isEqual(Object field, Object field1) {
        return field == field1 || (field != null && field.equals(field1));
    }

    public static class AdIdInfo {
        public enum Advertiser {
            AMAZON("Amazon"),
            GOOGLE("Google Play Store");

            public String descriptiveName;

            Advertiser(String name) {
                this.descriptiveName = name;
            }
        }

        public final String id;
        public final boolean isLimitAdTrackingEnabled;
        public final Advertiser advertiser;

        public AdIdInfo(String id, boolean isLimitAdTrackingEnabled, Advertiser advertiser) {
            this.id = id;
            this.isLimitAdTrackingEnabled = isLimitAdTrackingEnabled;
            this.advertiser = advertiser;
        }
    }

    public static String getGpsEnabled(Context context) {
        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return Boolean.toString(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        } else {
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    public static Integer getNetworkType(Context context, TelephonyManager telephonyManager) {
        if (telephonyManager != null && MPUtility.checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            return telephonyManager.getNetworkType();
        } else {
            return null;
        }
    }

    public static long getAvailableInternalDisk(Context context) {
        File path = Environment.getDataDirectory();
        return getDiskSpace(context, path);
    }

    public static long getAvailableExternalDisk(Context context) {
        File path = context.getExternalFilesDir(null);
        if (path == null) {
            return 0;
        }
        return getDiskSpace(context, path);
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            // ignore missing data
        }
        return "unknown";
    }

    public static String hmacSha256Encode(String key, String data) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("utf-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return asHex(sha256_HMAC.doFinal(data.getBytes("utf-8")));
    }

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    public static JSONObject getJsonResponse(MPConnection connection) {
        try {
            return getJsonResponse(connection.getInputStream());
        } catch (IOException ex) {
            return getJsonResponse(connection.getErrorStream());
        }
    }

    public static JSONObject getJsonResponse(HttpURLConnection connection) {
        try {
            return getJsonResponse(connection.getInputStream());
        } catch (IOException ex) {
            return getJsonResponse(connection.getErrorStream());
        }
    }


    public static JSONObject getJsonResponse(InputStream is) {
        try {
            StringBuilder responseBuilder = new StringBuilder();
            if (is == null) {
                return null;
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line + '\n');
            }
            in.close();
            return new JSONObject(responseBuilder.toString());
        } catch (IOException ex) {

        } catch (JSONException jse) {

        }
        return null;
    }

    public static long getDiskSpace(Context context, File path) {
        if (MPUtility.isInstantApp(context)) {
            return 0L;
        }
        long availableSpace = -1L;
        StatFs stat = new StatFs(path.getPath());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            availableSpace = JellybeanHelper.getAvailableMemory(stat);
        }
        if (availableSpace == 0) {
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        return availableSpace;
    }

    public static String getErrorMessage(HttpURLConnection connection) {
        InputStream is = connection.getErrorStream();
        if (is == null) {
            return null;
        }
        StringBuilder responseBuilder = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line + '\n');
            }
            in.close();
            return responseBuilder.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public static long millitime() {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Nullable
    public static String getAndroidID(Context context) {
        if (MParticle.isAndroidIdEnabled()) {
            return Settings.Secure.getString(context.getContentResolver(), "android_id");
        } else {
            return null;
        }
    }

    public static String getTimeZone() {
        try {
            //Some Android 8 devices crash here for no clear reason.
            return TimeZone.getDefault().getDisplayName(false, 0);
        } catch (Exception ignored) {
        } catch (AssertionError e) {
        }
        return null;
    }

    public static int getOrientation(Context context) {
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if (context != null) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            if (displayMetrics.widthPixels == displayMetrics.heightPixels) {
                orientation = Configuration.ORIENTATION_SQUARE;
            } else {
                if (displayMetrics.widthPixels < displayMetrics.heightPixels) {
                    orientation = Configuration.ORIENTATION_PORTRAIT;
                } else {
                    orientation = Configuration.ORIENTATION_LANDSCAPE;
                }
            }
        }
        return orientation;
    }

    public static long getTotalMemory(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return getTotalMemoryJB(context);
        } else {
            return getTotalMemoryPreJB();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getTotalMemoryJB(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.totalMem;
    }

    public static long getTotalMemoryPreJB() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();//meminfo
            arrayOfString = str2.split("\\s+");
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;
            localBufferedReader.close();
            return initial_memory;
        } catch (IOException e) {
            return -1;
        }
    }

    public static String getOpenUDID(Context context) {
        if (sOpenUDID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    Constants.PREFS_FILE, Context.MODE_PRIVATE);
            sOpenUDID = sharedPrefs.getString(Constants.PrefKeys.OPEN_UDID, null);
            if (sOpenUDID == null) {
                sOpenUDID = getAndroidID(context);
                if (sOpenUDID == null)
                    sOpenUDID = getGeneratedUdid();

                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(Constants.PrefKeys.OPEN_UDID, sOpenUDID);
                editor.apply();
            }
        }
        return sOpenUDID;
    }

    public static String getRampUdid(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                Constants.PREFS_FILE, Context.MODE_PRIVATE);
        String rampUdid = sharedPrefs.getString(Constants.PrefKeys.DEVICE_RAMP_UDID, null);
        if (rampUdid == null) {
            rampUdid = getGeneratedUdid();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(Constants.PrefKeys.DEVICE_RAMP_UDID, rampUdid);
            editor.apply();
        }
        return rampUdid;
    }

    static String getGeneratedUdid() {
        SecureRandom localSecureRandom = new SecureRandom();
        return new BigInteger(64, localSecureRandom).toString(16);
    }

    static String getBuildUUID(String versionCode) {
        if (versionCode == null) {
            versionCode = DeviceAttributes.UNKNOWN;
        }
        try {
            return UUID.nameUUIDFromBytes(versionCode.getBytes()).toString();
        } catch (AssertionError e) {
            //Some devices do not have MD5 and will throw a NoSuchAlgorithmException.
            return DeviceAttributes.UNKNOWN;
        }
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean hasNfc(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    public static String getBluetoothVersion(Context context) {
        String bluetoothVersion = NO_BLUETOOTH;
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) && (context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le"))) {
            bluetoothVersion = "ble";
        } else if (context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            bluetoothVersion = "classic";
        }
        return bluetoothVersion;
    }

    public static boolean isPhoneRooted() {

        // Get from build customAttributes
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

    public static int mpHash(String input) {
        int hash = 0;

        if (input == null || input.length() == 0)
            return hash;

        char[] chars = input.toLowerCase().toCharArray();

        for (char c : chars) {
            hash = ((hash << 5) - hash) + c;
        }

        return hash;
    }

    public static boolean hasTelephony(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    @SuppressLint("MissingPermission")
    public static boolean isBluetoothEnabled(Context context) {
        if (checkPermission(context, Manifest.permission.BLUETOOTH)) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null) {
                try {
                    //noinspection MissingPermission
                    return mBluetoothAdapter.isEnabled();
                } catch (SecurityException se) {
                }
            }
        }
        return false;
    }

    public static boolean checkPermission(Context context, String permission) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isGmsAdIdAvailable() {
        try {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isSupportLibAvailable() {
        try {
            Class.forName("android.support.v4.app.FragmentActivity");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isFirebaseAvailable() {
        if (isFirebaseAvailablePostV21() || isFirebaseAvailablePreV21()) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean isFirebaseAvailablePostV21() {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static Boolean isFirebaseAvailablePreV21() {
        try {
            Class.forName("com.google.firebase.iid.FirebaseInstanceId");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isInstallRefApiAvailable() {
        try {
            Class.forName("com.android.installreferrer.api.InstallReferrerStateListener");
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static BigInteger hashFnv1A(byte[] data) {
        final BigInteger INIT64 = new BigInteger("cbf29ce484222325", 16);
        final BigInteger PRIME64 = new BigInteger("100000001b3", 16);
        final BigInteger MOD64 = new BigInteger("2").pow(64);

        BigInteger hash = INIT64;

        for (byte b : data) {
            hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
            hash = hash.multiply(PRIME64).mod(MOD64);
        }

        return hash;
    }

    public static boolean isServiceAvailable(Context context, Class<?> service) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(context, service);
        List resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo.size() > 0;
    }

    public static JSONObject wrapExtras(Bundle extras) {
        if (extras != null && !extras.isEmpty()) {
            JSONObject parameters = new JSONObject();
            for (String key : extras.keySet()) {
                Object value;
                if ((value = extras.getBundle(key)) != null) {
                    try {
                        parameters.put(key, wrapExtras((Bundle) value));
                    } catch (JSONException ignored) {

                    }
                } else if ((value = extras.get(key)) != null) {
                    String stringVal = value.toString();
                    if ((stringVal.length() < 500)) {
                        try {
                            parameters.put(key, stringVal);
                        } catch (JSONException ignored) {

                        }
                    }
                }
            }
            return parameters;
        } else {
            return null;
        }
    }

    public static JSONObject mapToJson(Map<String, ?> map) {
        if (map == null) {
            return null;
        }
        JSONObject attrs = new JSONObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            try {
                Object value = entry.getValue();
                String key = entry.getKey();
                if (value instanceof List) {
                    JSONArray array = new JSONArray();
                    for (Object v : (List) value) {
                        array.put(v);
                    }
                    attrs.put(key, array);
                } else if (value != null) {
                    attrs.put(key, value.toString());
                } else {
                    attrs.put(key, value);
                }
            } catch (JSONException ignore) {

            }
        }
        return attrs;
    }

    public static boolean isAppDebuggable(Context context) {
        return (0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static boolean isDevEnv() {
        return ConfigManager.getEnvironment().equals(MParticle.Environment.Development);
    }

    /**
     * This method makes sure the constraints on event attributes are enforced. A JSONObject version
     * of the attributes is return with data that exceeds the limits removed.
     * NOTE: Non-string attributes are not converted to strings, currently.
     *
     * @param attributes the user-provided JSONObject
     * @return a cleansed copy of the JSONObject
     */
    public static JSONObject enforceAttributeConstraints(Map<String, String> attributes) {
        if (null == attributes) {
            return null;
        }
        JSONObject checkedAttributes = new JSONObject();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setCheckedAttribute(checkedAttributes, key, value, false, false);
        }
        return checkedAttributes;
    }

    public static Boolean setCheckedAttribute(JSONObject attributes, String key, Object value, boolean increment, boolean userAttribute) {
        return setCheckedAttribute(attributes, key, value, false, increment, userAttribute);
    }

    public static Boolean setCheckedAttribute(JSONObject attributes, String key, Object value, Boolean caseInsensitive, boolean increment, boolean userAttribute) {
        if (null == attributes || null == key) {
            return false;
        }
        try {
            if (caseInsensitive) {
                key = findCaseInsensitiveKey(attributes, key);
            }
            if (value != null) {
                String stringValue = value.toString();
                if (stringValue.length() > Constants.LIMIT_ATTR_VALUE) {
                    Logger.error("Attribute value length exceeds limit. Discarding attribute: " + key);
                    return false;
                }
            }
            if (key.length() > Constants.LIMIT_ATTR_KEY) {
                Logger.error("Attribute name length exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (value == null) {
                value = JSONObject.NULL;
            }
            if (increment) {
                String oldValue = attributes.optString(key, "0");
                int oldInt = Integer.parseInt(oldValue);
                value = Integer.toString((Integer) value + oldInt);
            }
            attributes.put(key, value);
        } catch (JSONException e) {
            Logger.error("JSON error processing attributes. Discarding attribute: " + key);
            return false;
        } catch (NumberFormatException nfe) {
            Logger.error("Attempted to increment a key that could not be parsed as an integer: " + key);
            return false;
        } catch (Exception e) {
            Logger.error("Failed to add attribute: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static String findCaseInsensitiveKey(JSONObject jsonObject, String key) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String currentKey = keys.next();
            if (currentKey.equalsIgnoreCase(key)) {
                return currentKey;
            }
        }
        return key;
    }

    public static boolean isInstantApp(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.getPackageManager().isInstantApp();
        }
        try {
            Class.forName("com.google.android.instantapps.InstantApps");
            return new SyncRunnable<Boolean>() {
                @Override
                public Boolean run() {
                    return InstantApps.isInstantApp(context);
                }
            }.run();
        } catch (ClassNotFoundException ignored) {
            try {
                Class.forName("com.google.android.instantapps.supervisor.InstantAppsRuntime");
                return true;
            } catch (ClassNotFoundException a) {
                return false;
            }
        }
    }

    public static boolean containsNullKey(Map map) {
        try {
            return map.containsKey(null);
        } catch (RuntimeException ignore) {
            //At this point we should be able to conclude that the implementation of the map does
            //not allow for null keys, if you get an exception when you check for a null key, but
            //there is no guarantee in the Map documentation, so we still have to check by hand.
            for (Map.Entry entry : new ArrayList<Map.Entry>(map.entrySet())) {
                if (entry.getKey() == null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public static String getProp(String key) {
        try {
            Class SystemProperties = Class.forName("android.os.SystemProperties");
            Method get = SystemProperties.getMethod("get", new Class[]{String.class});
            return get.invoke(SystemProperties, new Object[]{key}).toString();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Number addNumbers(Number number1, Number number2) {
        if (number1 instanceof Double || number2 instanceof Double) {
            return number1.doubleValue() + number2.doubleValue();
        } else if (number1 instanceof Float || number2 instanceof Float) {
            return number1.floatValue() + number2.floatValue();
        } else if (number1 instanceof Long || number2 instanceof Long) {
            return number1.longValue() + number2.longValue();
        } else {
            return number1.intValue() + number2.intValue();
        }
    }

    public static Object toNumberOrString(String stringValue) {
        if (stringValue == null) {
            return null;
        }
        for (Character c : stringValue.toCharArray()) {
            if (!Character.isDigit(c) && c != '.' && c != '-') {
                return stringValue;
            }
        }
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ignored){}
        try {
           return Double.parseDouble(stringValue);
        } catch (NumberFormatException ignored){}

        return stringValue;
    }

    private interface SyncRunnable<T> {
        T run();
    }
}