package com.mparticle.internal;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.MParticleTest;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

public class AccessUtils {

    public static final int STORE_MESSAGE = MessageHandler.STORE_MESSAGE;
    public static final int REMOVE_USER_ATTRIBUTE = MessageHandler.REMOVE_USER_ATTRIBUTE;
    public static final int SET_USER_ATTRIBUTE = MessageHandler.SET_USER_ATTRIBUTE;

    public static void clearMpId(Context context) {
        ConfigManager.clearMpid(context);
    }

    public static MParticle.InstallType getInstallType(MessageManager messageManager) {
        return messageManager.mInstallType;
    }

    public static void deleteAllUserConfigs(Context context) {
        UserConfig.deleteAllUserConfigs(context);
    }

    public static void deleteConfigManager(Context context) {
        ConfigManager.deleteConfigManager(context);
    }

    public static void clearMessages(MessageManager messageManager) {
        messageManager.mMessageHandler.removeCallbacksAndMessages(null);
        messageManager.mUploadHandler.removeCallbacksAndMessages(null);
    }

    public static void deleteDatabase() {
        InstrumentationRegistry.getTargetContext().deleteDatabase(MParticleDatabaseHelper.DB_NAME);
        DatabaseTables.setInstance(null);
    }

    public static MessageHandler getMessageHandler() {
        return com.mparticle.AccessUtils.getMessageManager().mMessageHandler;
    }

    public static UploadHandler getUploadHandler() {
        return com.mparticle.AccessUtils.getMessageManager().mUploadHandler;
    }
}
