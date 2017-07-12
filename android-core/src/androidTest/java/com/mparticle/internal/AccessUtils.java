package com.mparticle.internal;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.mparticle.MParticle;
import com.mparticle.MParticleTest;
import com.mparticle.internal.database.services.MParticleDBManager;

public class AccessUtils {

    public static void clearMpId(Context context) {
        ConfigManager.clearMpid(context);
    }

    public static MParticle.InstallType getInstallType(MessageManager messageManager) {
        return messageManager.mInstallType;
    }

    public static void deleteAllUserConfigs(Context context) {
        UserConfig.deleteAllUserConfigs(context);
    }
}
