package com.mparticle.internal;

import android.annotation.TargetApi;
import android.os.Build;

import org.json.JSONArray;

/**
 * This is solely used to avoid logcat warnings that Android will generate when loading a class,
 * even if you use conditional execution based on VERSION.
 */
@TargetApi(19)
public class KitKatHelper {
    public static void remove(JSONArray array, int index) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            array.remove(index);
        }
    }
}
