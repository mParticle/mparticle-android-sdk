package com.mparticle.test;


import android.content.Context;
import android.content.SharedPreferences;

import com.mparticle.internal.Constants;

public class TestUtils {

    public static SharedPreferences getSharedPrefs(Context context){
        return context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
    }
}
