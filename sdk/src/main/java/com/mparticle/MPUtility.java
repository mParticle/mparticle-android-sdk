package com.mparticle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by sdozor on 1/9/14.
 */ 

class MPUtility {

    public static double getCpuUsage() {
        return 0;
    }

    public static long getAvailableMemory() {
        return 0;
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

    public static long getAvailableDisk() {
        return 0;
    }

    public static long getAppMemoryUsage() {
        return 0;
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

    public static long getTotalMemory() {
        return 0;
    }
}
