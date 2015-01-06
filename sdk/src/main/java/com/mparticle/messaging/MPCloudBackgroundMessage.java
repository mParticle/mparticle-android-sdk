package com.mparticle.messaging;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mparticle.MParticle;
import com.mparticle.messaging.MPCloudNotificationMessage;

/**
 * @hide
 */
public class MPCloudBackgroundMessage {

    public static boolean processSilentPush(Context context, Bundle extras) {
        if (extras != null &&
                extras.containsKey(MPCloudNotificationMessage.COMMAND)){
            int command = Integer.parseInt(extras.getString(MPCloudNotificationMessage.COMMAND));
            switch (command){
                case MPCloudNotificationMessage.COMMAND_ALERT_CONFIG_REFRESH:
                    MParticle.start(context);
                    MParticle.getInstance().internal().refreshConfiguration();
                case MPCloudNotificationMessage.COMMAND_DONOTHING:
                    return true;
                default:
                    return false;
            }


        }
        return false;
    }
}
