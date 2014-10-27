package com.mparticle;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mparticle.messaging.MPCloudNotificationMessage;

/**
 * Created by sdozor on 10/21/14.
 */
class MPCloudBackgroundMessage {

    public static boolean processSilentPush(Context context, Bundle extras) {
        if (extras != null &&
                extras.containsKey(MPCloudNotificationMessage.COMMAND)){
            int command = Integer.parseInt(extras.getString(MPCloudNotificationMessage.COMMAND));
            if (command == MPCloudNotificationMessage.COMMAND_ALERT_CONFIG_REFRESH){
                try {
                    MParticle.start(context);
                    MParticle.getInstance().refreshConfiguration();
                }catch (Exception e){

                }
                return true;
            }

        }
        return false;
    }
}
