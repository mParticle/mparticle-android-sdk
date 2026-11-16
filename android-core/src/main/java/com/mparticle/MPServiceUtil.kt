package com.mparticle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mparticle.internal.ConfigManager
import com.mparticle.internal.Constants
import com.mparticle.internal.KitsLoadedListener
import com.mparticle.internal.KitsLoadedListenerConfiguration
import com.mparticle.internal.Logger
import com.mparticle.messaging.MPMessagingAPI
import com.mparticle.messaging.ProviderCloudMessage

open class MPServiceUtil(context: Context?) {
    private var mContext: Context? = context

    init {
        // This a required workaround for a bug in AsyncTask in the Android framework.
        // AsyncTask.java has code that needs to run on the main thread,
        // but that is not guaranteed since it will be initialized on whichever
        // thread happens to cause the class to run its static initializers. The original
        // Android issue tracker link for this bug is no longer available.
        val looper = Looper.getMainLooper()
        val handler = Handler(looper)
        handler.post {
            try {
                Class.forName("android.os.AsyncTask")
            } catch (e: ClassNotFoundException) {
                Logger.error(e)
            }
        }
    }

    open fun onHandleIntent(intent: Intent?) {
        try {
            val action = intent?.action
            Logger.info("MPService", "Handling action: $action")
            if (action == null) {
                Logger.warning("MPService", "Ignoring intent with no action.")
                return
            }
            if (action == "com.google.android.c2dm.intent.REGISTRATION") {
                // Android will an Intent with this action, but we don't care - we capture the ID synchronously.
            } else if (action == "com.google.android.c2dm.intent.RECEIVE") {
                generateCloudMessage(intent)
            } else if (action.startsWith(INTERNAL_NOTIFICATION_TAP)) {
                handleNotificationTapInternal(intent)
            } else if (action == MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED) {
                handleNotificationTap(intent)
            } else if (action == MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED) {
                val message = getCloudMessage(intent)
                if (message != null) {
                    showNotification(message)
                }
            }
        } finally {
            synchronized(LOCK) {
                if (sWakeLock != null && sWakeLock!!.isHeld) {
                    sWakeLock!!.release()
                }
            }
        }
    }

    private fun showNotification(message: ProviderCloudMessage) {
        if (!message.displayed && ConfigManager.isDisplayPushNotifications(mContext)) {
            object : AsyncTask<ProviderCloudMessage?, Void?, Notification?>() {
                override fun doInBackground(vararg params: ProviderCloudMessage?): Notification? =
                    message.buildNotification(mContext!!, System.currentTimeMillis())

                override fun onPostExecute(notification: Notification?) {
                    super.onPostExecute(notification)
                    if (notification != null) {
                        val mNotifyMgr = mContext!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        if (Build.VERSION.SDK_INT >= 26) {
                            createNotificationChannel(mNotifyMgr)
                        }
                        mNotifyMgr.cancel(message.id)
                        mNotifyMgr.notify(message.id, notification)
                    }
                }
            }.execute(message)
        }
        val appState = MParticle.getAppState()
        val instance = MParticle.getInstance()
        if (instance != null) {
            instance.logNotification(message, false, appState)
        }
    }

    private fun handleNotificationTap(intent: Intent) {
        val message = getCloudMessage(intent) ?: return
        val pe = message.getDefaultOpenIntent(mContext, message)
        pe.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        pe.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(mContext, 0, pe, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(mContext, 0, pe, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        if (pendingIntent != null) {
            try {
                pendingIntent.send()
            } catch (e: PendingIntent.CanceledException) {
            }
        }
    }

    private fun generateCloudMessage(intent: Intent) {
        try {
            val kitsLoadedListener =
                object : KitsLoadedListener {
                    override fun onKitsLoaded() {
                        try {
                            val instance = MParticle.getInstance()
                            if (instance != null) {
                                val cloudMessage = ProviderCloudMessage.createMessage(intent, ConfigManager.getPushKeys(mContext))
                                val handled = instance.mKitManager.onMessageReceived(mContext!!.applicationContext, intent)
                                cloudMessage.displayed = handled
                                broadcastNotificationReceived(cloudMessage)
                            }
                        } catch (e: Exception) {
                            Logger.warning("FCM parsing error: $e")
                        }
                    }
                }
            val instance = MParticle.getInstance()
            if (instance != null) {
                instance.mKitManager.addKitsLoadedListener(kitsLoadedListener)
            } else {
                MParticle.start(
                    MParticleOptions
                        .builder(mContext!!)
                        .configuration(KitsLoadedListenerConfiguration(kitsLoadedListener))
                        .buildForInternalRestart(),
                )
            }
        } catch (e: Exception) {
            Logger.warning("FCM parsing error: $e")
        }
    }

    private fun broadcastNotificationReceived(message: ProviderCloudMessage) {
        val intent = Intent(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED)
        intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message)
        LocalBroadcastManager.getInstance(mContext!!).sendBroadcast(intent)
        onHandleIntent(intent)
    }

    private fun handleNotificationTapInternal(intent: Intent) {
        val message = getCloudMessage(intent) ?: return

        val manager = mContext!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(message.id)

        MParticle.start(MParticleOptions.builder(mContext!!.applicationContext).buildForInternalRestart())
        val broadcast = Intent(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED)
        broadcast.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message)
        LocalBroadcastManager.getInstance(mContext!!).sendBroadcast(broadcast)
        onHandleIntent(broadcast)
    }

    @RequiresApi(api = 26)
    private fun createNotificationChannel(mNotifyMgr: NotificationManager) {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL, "Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        mNotifyMgr.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL = "com.mparticle.default"
        const val INTERNAL_NOTIFICATION_TAP = "com.mparticle.push.notification_tapped"
        private var sWakeLock: PowerManager.WakeLock? = null
        private const val TAG = "${Constants.LOG_TAG}:wakeLock"
        private val LOCK: Any = MPService::class.java

        @JvmStatic
        fun runIntentInService(context: Context?, intent: Intent?) {
            synchronized(LOCK) {
                if (sWakeLock == null) {
                    val pm = context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
                    sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
                }
            }
            sWakeLock!!.acquire()
            intent!!.setClass(context!!, MPService::class.java)
            try {
                context.startService(intent)
            } catch (ignore: IllegalStateException) {
                MPServiceUtil(context).onHandleIntent(intent)
            }
        }

        private fun getCloudMessage(intent: Intent): ProviderCloudMessage? {
            var message: ProviderCloudMessage? = null
            try {
                message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA)
            } catch (e: Exception) {
                Logger.warning("MPService", "Unable to read the cloud message extra: $e")
            }
            if (message == null) {
                Logger.warning("MPService", "Ignoring action ${intent.action} with no cloud message extra.")
            }
            return message
        }
    }
}
