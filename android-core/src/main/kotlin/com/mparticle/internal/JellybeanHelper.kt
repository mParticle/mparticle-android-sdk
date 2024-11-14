package com.mparticle.internal

import android.annotation.TargetApi
import android.os.Build
import android.os.StatFs

/**
 * This is solely used to avoid logcat warnings that Android will generate when loading a class,
 * even if you use conditional execution based on VERSION.
 */
@TargetApi(18)
object JellybeanHelper {
    @JvmStatic
    fun getAvailableMemory(stat: StatFs): Long {
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return stat.availableBlocksLong * stat.blockSizeLong
            }
        } catch (e: Exception) {
            //For some reason, it appears some devices even in jelly bean don't have this method.
        }

        return 0
    }
}
