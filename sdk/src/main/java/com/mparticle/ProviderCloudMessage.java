package com.mparticle;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by sdozor on 9/15/14.
 */
public class ProviderCloudMessage extends AbstractCloudMessage {
    public ProviderCloudMessage(Bundle extras) {
        mExtras = extras;
        mPrimaryText = findProviderMessage(extras);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    @Override
    public Notification buildNotification(Context context) {
        Notification notification = new NotificationCompat.Builder(context)
                .setContentIntent(getDefaultOpenIntent(context, this))
                .setSmallIcon(MParticlePushUtility.getFallbackIcon(context))
                .setTicker(mPrimaryText)
                .setContentTitle(MParticlePushUtility.getFallbackTitle(context))
                .setContentText(mPrimaryText).build();

        MParticle.start(context);
        MParticle mMParticle = MParticle.getInstance();

        if (mMParticle.mConfigManager.isPushSoundEnabled()) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }
        if (mMParticle.mConfigManager.isPushVibrationEnabled()) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        return notification;
    }

    private String findProviderMessage(Bundle extras){
        String[] possibleKeys = MParticle.getInstance().mConfigManager.getPushKeys();
        if (possibleKeys != null) {
            for (String key : possibleKeys) {
                String message = extras.getString(key);
                if (message != null && message.length() > 0) {
                    extras.remove(key);
                    return message;
                }
            }
        }
        Log.w(Constants.LOG_TAG, "Failed to extract 3rd party push message.");
        return "";
    }
}
