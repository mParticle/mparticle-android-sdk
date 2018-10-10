package com.mparticle.internal;

import android.content.Context;
import android.os.Handler;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;
import com.mparticle.networking.BaseNetworkConnection;
import com.mparticle.networking.MParticleBaseClientImpl;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

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
        ((MParticleBaseClientImpl)getApiClient()).setScheme(protocol);
    }

    public static void setAppStateManagerHandler(Handler handler) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().getAppStateManager().delayedBackgroundCheckHandler = handler;
        }
    }

    public static void awaitMessageHandler() throws InterruptedException {
        BaseHandler messageHandler = getMessageHandler();
        CountDownLatch latch = new MPLatch(1);
        messageHandler.await(latch);
        latch.await();
        return;
    }

    /**
     * This method will block the current thread until Upload messages, which are tied to the mpid parameter,
     * or are UploadTriggerMessages, are cleared from the Handler's queue
     *
     * Upload essages which are tied to an MPID, are ones originating from MParticle.getInstance().upload() calls,
     * and initial upload messages
     *
     * the fact that these messages are tied into an MPID is an artifact from a defunct implementation
     * of the UploadHandler, but it is really useful for this use case,
     * @throws InterruptedException
     */
    public static void awaitUploadHandler() throws InterruptedException {
        BaseHandler uploadHandler = getUploadHandler();
        CountDownLatch latch = new MPLatch(1);
        uploadHandler.await(latch);
        latch.await();
        return;
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

    public static void setKitManager(final KitManager kitManager) {
        final KitFrameworkWrapper kitFrameworkWrapper = MParticle.getInstance().getKitManager();
        kitFrameworkWrapper.loadKitLibrary();
        MParticle.getInstance().Identity().removeIdentityStateListener((IdentityStateListener)kitFrameworkWrapper.mKitManager);
        final CountDownLatch latch = new MPLatch(1);
        //Need to do this since the KitManager instance in KitFrameworkWrapper is not threadsafe. If
        //it is in mid-loadKitLibrary, then the instance you set could be overwritten
        getUploadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!kitFrameworkWrapper.getFrameworkLoadAttempted()) {
                    kitFrameworkWrapper.loadKitLibrary();
                }
                kitFrameworkWrapper.setKitManager(kitManager);
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        JSONArray configuration = MParticle.getInstance().Internal().getConfigManager().getLatestKitConfiguration();
        Logger.debug("Kit Framework loaded.");
        if (configuration != null) {
            Logger.debug("Restoring previous Kit configuration.");
            kitManager.updateKits(configuration);
        }
    }

    public static long getActivityDelay() {
        return AppStateManager.ACTIVITY_DELAY;
    }
}
