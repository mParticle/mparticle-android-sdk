package com.mparticle.internal;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import com.mparticle.MParticle;

public class AccessUtils {

    public static void setAppStateManagerHandler(Handler handler) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().getAppStateManager().delayedBackgroundCheckHandler = handler;
        }
    }

    public static void deleteConfigManager(Context mContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mContext.deleteSharedPreferences("mp_preferences");

        } else {
            mContext.getSharedPreferences("mp_preferences", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }
}
