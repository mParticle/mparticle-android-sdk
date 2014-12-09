package com.mparticle.messaging;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mparticle.MParticle;
import com.mparticle.messaging.MPCloudNotificationMessage;

public class MPCloudBackgroundMessage {

    public static boolean processSilentPush(Context context, Bundle extras) {
        if (extras != null &&
                extras.containsKey(MPCloudNotificationMessage.COMMAND)){
            int command = Integer.parseInt(extras.getString(MPCloudNotificationMessage.COMMAND));
            if (command == MPCloudNotificationMessage.COMMAND_ALERT_CONFIG_REFRESH){
                try {
                    MParticle.start(context);
                    MParticle.getInstance().internal().refreshConfiguration();
                }catch (Exception e){

                }
                return true;
            }

        }
        return false;
    }
}
