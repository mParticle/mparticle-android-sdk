package com.mparticle;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.mparticle.messaging.MPCloudNotificationMessage;

/**
 * Created by sdozor on 10/21/14.
 */
class MPCloudBackgroundMessage {

    private static final String COMMAND_CONFIG_REFRESH = "CONFIG_REFRESH";

    public static boolean processSilentPush(Context context, Bundle extras) {
        if (extras != null &&
                extras.containsKey(MPCloudNotificationMessage.COMMAND)){
            String command = extras.getString(MPCloudNotificationMessage.COMMAND);
            if (!TextUtils.isEmpty(command)){
                if (COMMAND_CONFIG_REFRESH.equals(command)){
                    try {
                        MParticle.start(context);
                        MParticle.getInstance().refreshConfiguration();
                    }catch (Exception e){

                    }
                    return true;
                }
            }
        }
        return false;
    }
}
