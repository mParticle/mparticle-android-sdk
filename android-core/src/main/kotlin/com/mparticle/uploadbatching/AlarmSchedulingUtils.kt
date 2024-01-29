package com.mparticle.uploadbatching

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mparticle.internal.Logger
import com.mparticle.millisToLoggingDate

fun scheduleUploadBatchAlarm(context: Context, delay: Long) {
    val intent = Intent(context, UploadBatchReceiver::class.java).apply {
        action = UploadBatchReceiver.ACTION_UPLOAD_BATCH
    }
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    var alarmDelay = delay
    //Setting alarm delay to 2min MINIMUM to prevent collision with end session message triggers
    if (delay < 120000) {
        alarmDelay = 120000L
    }
    val time = System.currentTimeMillis() + alarmDelay
    val alarmType = AlarmManager.RTC_WAKEUP
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager?)?.let { manager ->

        PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            manager.cancel(it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.setAndAllowWhileIdle(alarmType, time, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.setAndAllowWhileIdle(alarmType, time, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            manager.set(alarmType, time, pendingIntent)
        } else {
            manager.set(alarmType, time, pendingIntent)
        }
    }
    Logger.debug("Upload batch alarm set at ${millisToLoggingDate(time)}")
}



