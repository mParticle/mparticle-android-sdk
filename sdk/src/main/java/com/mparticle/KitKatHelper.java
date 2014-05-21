package com.mparticle;

import android.os.Build;

import org.json.JSONArray;

/**
 * Created by sdozor on 5/21/14.
 */
public class KitKatHelper {
    static void remove(JSONArray array, int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            array.remove(index);
        }
    }
}
