package com.mparticle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.CloudAction;

public class MPPushAnalyticsReceiver extends BroadcastReceiver {
    private Context mContext = null;
    @Override
    public final void onReceive(Context context, Intent intent) {
        mContext = context;
        if (MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED.equalsIgnoreCase(intent.getAction())){
            AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
            CloudAction action = intent.getParcelableExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA);
            if (!onNotificationTapped(message, action)){
                intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
                intent.putExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA, action);
                MPService.runIntentInService(context, intent);
            }
            return;
        } else if (MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED.equalsIgnoreCase(intent.getAction())){
            AbstractCloudMessage message = intent.getParcelableExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA);
            if (!onNotificationReceived(message)){
                intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
                MPService.runIntentInService(context, intent);
            }
            return;
        }

    }

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
