package com.mparticle;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by sdozor on 1/9/14.
 */ 

class MPUtility {

    public static String getCpuUsage() {
        String str1 = "unknown";
        String str2 = String.valueOf(android.os.Process.myPid());
        java.lang.Process localProcess = null;
        BufferedReader localBufferedReader = null;
        String str3 = null;
        try {
            localProcess = Runtime.getRuntime().exec("top -d 1 -n 1");
            localBufferedReader = new BufferedReader(new InputStreamReader(localProcess.getInputStream()));
            while ((str3 = localBufferedReader.readLine()) != null)
                if (str3.contains(str2)) {
                    String[] arrayOfString = str3.split(" ");
                    if (arrayOfString != null)
                        for (int i = 0; i < arrayOfString.length; i++){
                            if ((arrayOfString[i] != null) && (arrayOfString[i].contains("%"))){
                                str1 = arrayOfString[i];
                                str1 = str1.substring(0, str1.length() - 1);
                                return str1;
                            }
                        }
                }
        }
        catch (IOException localIOException2) {
            Log.w(Constants.LOG_TAG, "Error computing CPU usage");
            localIOException2.printStackTrace();
        }
        finally {
            try {
                 if (localBufferedReader != null)
                    localBufferedReader.close();
                if (localProcess != null)
                    localProcess.destroy();
            }
            catch (IOException localIOException4) {
                Log.w(Constants.LOG_TAG, "Error computing CPU usage");
                localIOException4.printStackTrace();
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
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)){
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return Boolean.toString(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        }else{
            return "unknown";
        }
    }

    public static long getAvailableInternalDisk() {
        long availableSpace = -1L;
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1){
            availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        }else{
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        return availableSpace;
    }

    public static long getAvailableExternalDisk() {
        long availableSpace = -1L;
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1){
            availableSpace = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        }else{
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        return availableSpace;
    }

    public static int getOrientation(Context context)
    {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display getOrient = windowManager.getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if(getOrient.getWidth()==getOrient.getHeight()){
            orientation = Configuration.ORIENTATION_SQUARE;
        } else{
            if(getOrient.getWidth() < getOrient.getHeight()){
                orientation = Configuration.ORIENTATION_PORTRAIT;
            }else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    public static long getTotalMemory(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
            return getTotalMemoryJB(context);
        }else{
            return getTotalMemoryPreJB();
        }
    }

    public static long getTotalMemoryJB(Context context){
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
            for (String num : arrayOfString) {
                Log.i(str2, num + "\t");
            }
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;
            localBufferedReader.close();
            return initial_memory;
        }
        catch (IOException e){
            return -1;
        }
    }
}
