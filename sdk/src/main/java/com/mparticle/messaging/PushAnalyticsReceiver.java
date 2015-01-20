package com.mparticle.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mparticle.MPService;

/**
 * <code>BroadcastReceiver</code> to be used to listen for, manipulate, and react to GCM notifications.
 *
 * In order to use this class:
 *
 * 1. Create your own class which extends this one, for example MyPushReceiver.java
 * 2. Add the following to your <code>AndroidManifest.xml</code>, replacing <code>YOURPACKAGENAME</code> and the name of your <code>BroadcastReceiver</code>:
 * <pre>
 *  {@code <receiver android:name="YOURPACKAGENAME.MyPushReceiver">
 *      <intent-filter>
 *          <action android:name="com.mparticle.push.RECEIVE" />
 *          <action android:name="com.mparticle.push.TAP" />
 *          <category android:name="YOURPACKAGENAME"/>
 *      </intent-filter>
 *  </receiver> }</pre>
 *
 * @see #onNotificationReceived(AbstractCloudMessage)
 * @see #onNotificationTapped(AbstractCloudMessage, CloudAction)
 *
 */
public class PushAnalyticsReceiver extends BroadcastReceiver {
    private Context mContext = null;

    /**
     * @hide
     */
    @Override
    public final void onReceive(Context context, Intent intent) {
        mContext = context;
        if (MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED.equalsIgnoreCase(intent.getAction())){
            AbstractCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
            CloudAction action = intent.getParcelableExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA);
            if (!onNotificationTapped(message, action)){
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                intent.putExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA, action);
                MPService.runIntentInService(context, intent);
            }
            return;
        } else if (MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED.equalsIgnoreCase(intent.getAction())){
            AbstractCloudMessage message = intent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
            if (!onNotificationReceived(message)){
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                MPService.runIntentInService(context, intent);
            }
            return;
        }

    }

    /**
     * Helper method to retrieve the Context that was passed into this <code>BroadcastReceiver</code>
     *
     * @return Context object
     */
    protected final Context getContext(){
        return mContext;
    }

    /**
     * Override this method to listen for when a notification has been received.
     *
     *
     * @param message The message that was received. Depending on the push provider, could be either a {@link com.mparticle.messaging.MPCloudNotificationMessage} or a {@link com.mparticle.messaging.ProviderCloudMessage}
     * @return True if you would like to handle this notification, False if you would like the mParticle to generate and show a {@link android.app.Notification}.
     */
    protected boolean onNotificationReceived(AbstractCloudMessage message){
        return false;
    }

    /**
     * Override this method to listen for when a notification has been tapped or acted on.
     *
     *
     * @param message The message that was tapped. Depending on the push provider, could be either a {@link com.mparticle.messaging.MPCloudNotificationMessage} or a {@link com.mparticle.messaging.ProviderCloudMessage}
     * @param action The action that the user acted on.
     * @return True if you would like to consume this tap/action, False if the mParticle SDK should attempt to handle it.
     */
    protected boolean onNotificationTapped(AbstractCloudMessage message, CloudAction action){
        return false;
    }

}
