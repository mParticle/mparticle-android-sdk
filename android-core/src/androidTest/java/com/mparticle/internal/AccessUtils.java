package com.mparticle.internal;

import android.content.Context;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
import com.mparticle.internal.networking.BaseNetworkConnection;

import org.json.JSONObject;

import java.io.IOException;

public class AccessUtils {

    public static final int STORE_MESSAGE = MessageHandler.STORE_MESSAGE;
    public static final int REMOVE_USER_ATTRIBUTE = MessageHandler.REMOVE_USER_ATTRIBUTE;
    public static final int SET_USER_ATTRIBUTE = MessageHandler.SET_USER_ATTRIBUTE;

    public static final int UPLOAD_MESSAGES = UploadHandler.UPLOAD_MESSAGES;
    public static final int UPLOAD_TRIGGER_MESSAGES = UploadHandler.UPLOAD_TRIGGER_MESSAGES;

    public static void clearMpId(Context context) {
        ConfigManager.clearMpid(context);
    }

    public static MParticle.InstallType getInstallType(MessageManager messageManager) {
        return messageManager.mInstallType;
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

    public static void setMParticleApiClient(MParticleApiClient client) {
        getUploadHandler().setApiClient(client);
    }

    public static void setMParticleApiClientProtocol(String protocol) {
        ((MParticleApiClientImpl)getApiClient()).overrideProtocol(protocol);
    }

    public static void setAppStateManagerHandler(Handler handler) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().getAppStateManager().delayedBackgroundCheckHandler = handler;
        }
    }

    public static class EmptyMParticleApiClient implements MParticleApiClient {
        @Override
        public void fetchConfig() throws IOException, MParticleApiClientImpl.MPConfigException {
        }

        @Override
        public void fetchConfig(boolean force) throws IOException, MParticleApiClientImpl.MPConfigException {

        }

        @Override
        public int sendMessageBatch(String message) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
            return 0;
        }

        @Override
        public JSONObject fetchAudiences() {
            return null;
        }

        @Override
        public boolean isThrottled() {
            return false;
        }

        @Override
        public JSONObject getCookies() {
            return null;
        }

        @Override
        public BaseNetworkConnection getRequestHandler() {
            return null;
        }

        @Override
        public void setRequestHandler(BaseNetworkConnection handler) {

        }
    }

    public static MParticleApiClient getApiClient() {
        return com.mparticle.AccessUtils.getMessageManager().mUploadHandler.mApiClient;
    }
}
