package com.mparticle;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.jar.JarFile;

/**
 * Created by sdozor on 1/9/14.
 */

class MPUtility {

    static final String NO_BLUETOOTH = "none";
    private static String sOpenUDID;
    private static String sBuildUUID;

    public static String getCpuUsage() {
        String str1 = "unknown";
        String str2 = String.valueOf(android.os.Process.myPid());
        java.lang.Process localProcess = null;
        BufferedReader localBufferedReader = null;
        String str3 = null;
        try {
            String[] command = {"top", "-d", "1", "-n", "1"};
            localProcess = new ProcessBuilder().command(command).redirectErrorStream(true).start();
            localBufferedReader = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
            while ((str3 = localBufferedReader.readLine()) != null)
                if (str3.contains(str2)) {
                    String[] arrayOfString = str3.split(" ");
                    if (arrayOfString != null) {
                        for (int i = 0; i < arrayOfString.length; i++) {
                            if ((arrayOfString[i] != null) && (arrayOfString[i].contains("%"))) {
                                str1 = arrayOfString[i];
                                str1 = str1.substring(0, str1.length() - 1);
                                return str1;
                            }
                        }
                    }
                }
        } catch (IOException localIOException2) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Error computing CPU usage");
        } finally {
            try {
                if (localBufferedReader != null) {
                    localBufferedReader.close();
                }
                if (localProcess != null) {
                    try {
                        // use exitValue() to determine if process is still running.
                        localProcess.exitValue();

                    } catch (IllegalThreadStateException e) {
                        // process is still running, kill it.
                        localProcess.destroy();
                    }
                }
            } catch (IOException localIOException4) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Error computing CPU usage");
            }
        }
        return str1;
    }

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

    public static String getGpsEnabled(Context context) {
        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return Boolean.toString(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        } else {
            return null;
        }
    }

    @TargetApi(18)
    public static long getAvailableInternalDisk() {
        long availableSpace = -1L;
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        } else {
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        return availableSpace;
    }

    @TargetApi(18)
    public static long getAvailableExternalDisk() {
        long availableSpace = -1L;
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        } else {
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        return availableSpace;
    }

    public static synchronized String getAndroidID(Context paramContext) {
        String str = Settings.Secure.getString(paramContext.getContentResolver(), "android_id");
        return (str == null) || (str.equals("9774d56d682e549c")) || (str.equals("0000000000000000")) || (str.length() < 15) ? null : str;
    }

    public static String getTimeZone() {
        return TimeZone.getDefault().getDisplayName(false, 0);
    }

    public static int getOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display getOrient = windowManager.getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if (getOrient.getWidth() == getOrient.getHeight()) {
            orientation = Configuration.ORIENTATION_SQUARE;
        } else {
            if (getOrient.getWidth() < getOrient.getHeight()) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            } else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
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

    public static synchronized String getOpenUDID(Context context) {
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
                editor.commit();
            }
        }
        return sOpenUDID;
    }

    static String getGeneratedUdid() {
        SecureRandom localSecureRandom = new SecureRandom();
        return new BigInteger(64, localSecureRandom).toString(16);
    }

    static String getBuildUUID(Context context) {
        TreeMap localTreeMap = new TreeMap();
        Object localObject1;
        Object localObject2;
        Object localObject3;
        Object localObject4;
        try {
            String str = context.getApplicationInfo().sourceDir;
            JarFile localJarFile = new JarFile(str);
            localObject1 = localJarFile.getManifest();
            localJarFile.close();
            localObject2 = ((java.util.jar.Manifest) localObject1).getEntries();
            localObject3 = new java.util.jar.Attributes.Name("SHA1-Digest");
            localObject4 = ((Map) localObject2).entrySet().iterator();
            while (((Iterator) localObject4).hasNext()) {
                Map.Entry localEntry = (Map.Entry) ((Iterator) localObject4).next();
                java.util.jar.Attributes localAttributes = (java.util.jar.Attributes) localEntry.getValue();
                if (localAttributes.containsKey(localObject3)) {
                    localTreeMap.put(localEntry.getKey(), localAttributes.getValue("SHA1-Digest"));
                }
            }
        } catch (Exception localException) {
        }
        if (localTreeMap.size() == 0) {
            sBuildUUID = "";
        } else {
            byte[] arrayOfByte = new byte[16];
            int i = 0;
            localObject1 = localTreeMap.entrySet().iterator();
            while (((Iterator) localObject1).hasNext()) {
                localObject2 = (Map.Entry) ((Iterator) localObject1).next();
                localObject3 = android.util.Base64.decode((String) ((Map.Entry) localObject2).getValue(), 0);
                for (int m : (byte[]) localObject3) {
                    arrayOfByte[i] = ((byte) (arrayOfByte[i] ^ m));
                    i = (i + 1) % 16;
                }
            }
            sBuildUUID = convertBytesToUUID(arrayOfByte, false);
        }
        return sBuildUUID;
    }

    private static String convertBytesToUUID(byte[] paramArrayOfByte, boolean paramBoolean) {
        String str = "";
        for (int i = 0; i < 16; i++) {
            str = str + String.format("%02x", new Object[]{Byte.valueOf(paramArrayOfByte[i])});
            if ((paramBoolean) && ((i == 3) || (i == 5) || (i == 7) || (i == 9))) {
                str = str + '-';
            }
        }
        return str;
    }

    public static boolean jsonObjsAreEqual(JSONObject js1, JSONObject js2) throws JSONException {
        if (js1 == null || js2 == null) {
            return (js1 == js2);
        }

        List<String> l1 = new ArrayList<String>();
        JSONArray a1 = js1.names();
        for (int i = 0; i < a1.length(); i++) {
            l1.add(a1.getString(i));
        }

        Collections.sort(l1);
        List<String> l2 = new ArrayList<String>();
        JSONArray a2 = js2.names();
        for (int i = 0; i < a2.length(); i++) {
            l2.add(a2.getString(i));
        }
        Collections.sort(l2);
        if (!l1.equals(l2)) {
            Log.d(Constants.LOG_TAG, "Difference detected: ECHO " + l1.toString() + " is different than: " + l2.toString());
            return false;
        }
        for (String key : l1) {
            Object val1 = js1.get(key);
            Object val2 = js2.get(key);
            if (val1 instanceof JSONObject) {
                if (!(val2 instanceof JSONObject)) {
                    Log.d(Constants.LOG_TAG, "Difference detected while inspecting key: " + key);
                    return false;
                }
                if (!jsonObjsAreEqual((JSONObject) val1, (JSONObject) val2)) {
                    Log.d(Constants.LOG_TAG, "Difference detected while inspecting key: " + key);
                    return false;
                }
            }

            if (val1 == null) {
                if (val2 != null) {
                    Log.d(Constants.LOG_TAG, "Difference detected while inspecting key, value: " + key + ", " + val2);
                    return false;

                }
            } else if (!val1.equals(val2)) {
                if (val2 == null) {
                    Log.d(Constants.LOG_TAG, "Difference detected while inspecting value: " + val1);
                } else {
                    Log.d(Constants.LOG_TAG, "Difference detected while inspecting value: " + val1 + ", does not match :" + val2);
                }
                return false;
            }
        }
        return true;
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static Object getAccessibleObject(Field paramField, Object paramObject) {
        Object localObject = null;
        if (paramField == null)
            return null;
        if (paramField != null) {
            paramField.setAccessible(true);
            try {
                localObject = paramField.get(paramObject);
            } catch (Exception e) {

            }
        }
        return localObject;
    }

    public static Field getAccessibleField(Class paramClass1, Class paramClass2) {
        Field[] paramfields = paramClass1.getDeclaredFields();
        Field localField = null;
        for (int i = 0; i < paramfields.length; i++)
            if (paramClass2.isAssignableFrom(paramfields[i].getType())) {
                // if (localField != null)
                //throw new MPException(cd.l);
                localField = paramfields[i];
            }

        localField.setAccessible(true);
        return localField;
    }

    public static Constructor getConstructor(String classStr, String[] params) throws ClassNotFoundException {
        Constructor<?>[] constructors = Class.forName(classStr).getDeclaredConstructors();
        for (int i = 0; i < constructors.length; i++) {
            Class[] classes = constructors[i].getParameterTypes();
            for (int j = 0; j < classes.length; j++) {
                if ((!classes[j].getName().equals(params[j]) ? 0 : classes.length != params.length ? 0 : 1) != 0)
                    return constructors[i];
            }
        }
        return null;
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

    public static boolean isBluetoothEnabled(Context context) {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && checkPermission(context, Manifest.permission.BLUETOOTH)) {
            return mBluetoothAdapter.isEnabled();
        }
        return false;
    }

    public static boolean checkPermission(Context context, String permission) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isGooglePlayServicesAvailable() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            return false;
        }
        try {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            return true;
        } catch (ClassNotFoundException cnfe) {

        }
        return false;
    }

    public static BigInteger hashDeviceIdForRamping(byte[] data) {
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
        if (resolveInfo.size() > 0) {
            return true;
        }
        return false;
    }

    static JSONObject wrapExtras(Bundle extras) {
        if (extras != null && !extras.isEmpty()) {
            JSONObject parameters = new JSONObject();
            for (String key : extras.keySet()) {
                Object value;
                if ((value = extras.getBundle(key)) != null) {
                    try {
                        parameters.put(key, wrapExtras((Bundle) value));
                    } catch (JSONException e) {

                    }
                } else if ((value = extras.get(key)) != null) {
                    String stringVal = value.toString();
                    if ((stringVal.length() < 500)) {
                        try {
                            parameters.put(key, stringVal);
                        } catch (JSONException e) {

                        }
                    }
                }
            }
            return parameters;
        }else{
            return null;
        }
    }
}
