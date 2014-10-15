package com.mparticle;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Core {@code BroadcastReceiver} used to support push notification and installer referrers. Handles the following Intent actions:
 *
 * <ul>
 *     <li>{@code com.android.vending.INSTALL_REFERRER}</li>
 *     <li>{@code com.google.android.c2dm.intent.REGISTRATION}</li>
 *     <li>{@code com.google.android.c2dm.intent.RECEIVE}</li>
 *     <li>{@code com.google.android.c2dm.intent.UNREGISTER}</li>
 *     <li>{@code android.intent.action.PACKAGE_REPLACED}</li>
 *     <li>{@code android.intent.action.BOOT_COMPLETED}</li>
 * </ul>
 *
 * This {@code BroadcastReceiver} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 *
 *
 * <pre>
 * {@code
 *  <receiver android:name="com.mparticle.MPReceiver"
 *      android:permission="com.google.android.c2dm.permission.SEND" >
 *      <intent-filter>
 *          <action android:name="com.android.vending.INSTALL_REFERRER"/>
 *          <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
 *          <action android:name="com.google.android.c2dm.intent.RECEIVE" />
 *          <action android:name="com.google.android.c2dm.intent.UNREGISTER"/>
 *          <!-- Use your application's package name as the category -->
 *          <category android:name="<YOUR_PACKAGE_NAME_HERE>" />
 *      </intent-filter>
 *      <!-- The GCM registration ID could change on an app upgrade, this filter will allow mParticle to re-register for push notifications if necessary. -->
 *      <intent-filter>
 *          <action android:name="android.intent.action.PACKAGE_REPLACED" />
 *          <!-- Use your application's package name as the path -->
 *          <data android:path="<YOUR_PACKAGE_NAME_HERE>"
 *              android:scheme="package" />
 *      </intent-filter>
 *      <!-- The GCM registration ID could change if the device's Android version changes, this filter will allow mParticle to re-register for push notifications if necessary. -->
 *      <intent-filter>
 *          <action android:name="android.intent.action.BOOT_COMPLETED" />
 *      </intent-filter>
 * </receiver> }</pre>
 *
 *
 */
public class MPReceiver extends BroadcastReceiver {

    static final String MPARTICLE_IGNORE = "mparticle_ignore";

    public MPReceiver() {
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        if (!MPARTICLE_IGNORE.equals(intent.getAction())) {
            if ("com.android.vending.INSTALL_REFERRER".equals(intent.getAction())) {
                String referrer = intent.getStringExtra("referrer");
                SharedPreferences preferences = context.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE);
                preferences.edit().putString(Constants.PrefKeys.INSTALL_REFERRER, referrer).commit();
            } else if (MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED.equalsIgnoreCase(intent.getAction())){
                AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
                AbstractCloudMessage.CloudAction action = intent.getParcelableExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA);
                if (!onNotificationTapped(message, action)){
                    MPService.runIntentInService(context, intent);
                }
            } else if (MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED.equalsIgnoreCase(intent.getAction())){
                AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
                if (!onNotificationReceived(message)){
                    MPService.runIntentInService(context, intent);
                }
            } else {
                MPService.runIntentInService(context, intent);
            }
        }
        setResult(Activity.RESULT_OK, null, null);
    }

    /**
     * Override this method to listen for when a notification has been received.
     *
     *
     * @param message The message that was received. Depending on the push provider, could be either a {@link com.mparticle.MPCloudMessage} or a {@link com.mparticle.ProviderCloudMessage}
     * @return True if you would like to handle this notification, False if you would like the mParticle to generate and show a {@link android.app.Notification}.
     */
    protected boolean onNotificationReceived(AbstractCloudMessage message){
        return false;
    }

    /**
     * Override this method to listen for when a notification has been tapped or acted on.
     *
     *
     * @param message The message that was tapped. Depending on the push provider, could be either a {@link com.mparticle.MPCloudMessage} or a {@link com.mparticle.ProviderCloudMessage}
     * @param action The action that the user acted on.
     * @return True if you would like to consume this tap/action, False if the mParticle SDK should attempt to handle it.
     */
    protected boolean onNotificationTapped(AbstractCloudMessage message, AbstractCloudMessage.CloudAction action){
        return false;
    }

}
