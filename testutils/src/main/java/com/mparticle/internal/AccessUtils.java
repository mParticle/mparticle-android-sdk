package com.mparticle.internal;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.audience.BaseAudienceTask;
import com.mparticle.identity.IdentityStateListener;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.kits.KitManagerImpl;
import com.mparticle.networking.BaseNetworkConnection;
import com.mparticle.testutils.MPLatch;

import org.json.JSONArray;
import org.json.JSONException;
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
        InstrumentationRegistry.getInstrumentation().getTargetContext().deleteDatabase(MParticleDatabaseHelper.getDbName());
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

    public static void setAppStateManagerHandler(Handler handler) {
        if (MParticle.getInstance() != null) {
            MParticle.getInstance().Internal().getAppStateManager().delayedBackgroundCheckHandler = handler;
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
     * or are UploadTriggerMessages, are cleared from the Handler's queue.
     *
     * Upload essages which are tied to an MPID, are ones originating from MParticle.getInstance().upload() calls,
     * and initial upload messages.
     *
     * the fact that these messages are tied into an MPID is an artifact from a defunct implementation
     * of the UploadHandler, but it is really useful for this use case.
     *
     * @throws InterruptedException
     */
    public static void awaitUploadHandler() throws InterruptedException {
        BaseHandler uploadHandler = getUploadHandler();
        CountDownLatch latch = new MPLatch(1);
        uploadHandler.await(latch);
        latch.await();
        return;
    }

    public static void forceFetchConfig() throws IOException, MParticleApiClientImpl.MPConfigException {
        getUploadHandler().mApiClient.fetchConfig(true);
    }

    public static class EmptyMParticleApiClient implements MParticleApiClient {
        @Override
        public void fetchConfig() throws IOException, MParticleApiClientImpl.MPConfigException {
        }

        @Override
        public void fetchConfig(boolean force) throws IOException, MParticleApiClientImpl.MPConfigException {

        }

        @Override
        public int sendMessageBatch(@NonNull String message, @NonNull UploadSettings uploadSettings) throws IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
            return 0;
        }


        @Override
        public void fetchUserAudience(BaseAudienceTask Task, long mpId) {
        }

        @Override
        public JSONObject getCookies() {
            return null;
        }

        @NonNull
        @Override
        public AliasNetworkResponse sendAliasRequest(@NonNull String request, @NonNull UploadSettings uploadSettings) throws JSONException, IOException, MParticleApiClientImpl.MPThrottleException, MParticleApiClientImpl.MPRampException {
            return new AliasNetworkResponse(0);
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

    public static KitManagerImpl getKitManager() {
        return (KitManagerImpl) MParticle.getInstance().Internal().getKitManager().mKitManager;
    }

    public static void setKitManager(final KitManager kitManager) throws InterruptedException {
        final KitFrameworkWrapper kitFrameworkWrapper = MParticle.getInstance().Internal().getKitManager();
        kitFrameworkWrapper.loadKitLibrary();
        MParticle.getInstance().Identity().removeIdentityStateListener((IdentityStateListener) kitFrameworkWrapper.mKitManager);
        final CountDownLatch kitManagerLoadedLatch = new MPLatch(1);
        //Need to do this since the KitManager instance in KitFrameworkWrapper is not threadsafe. If
        //it is in mid-loadKitLibrary, then the instance you set could be overwritten.
        getUploadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (!kitFrameworkWrapper.getFrameworkLoadAttempted()) {
                    kitFrameworkWrapper.loadKitLibrary();
                }
                kitFrameworkWrapper.setKitManager(kitManager);
                kitManagerLoadedLatch.countDown();
            }
        });
        kitManagerLoadedLatch.await();
        final CountDownLatch kitsLoadedLatch = new MPLatch(1);
        kitFrameworkWrapper.setKitsLoaded(false);
        kitFrameworkWrapper.addKitsLoadedListener(new KitsLoadedListener() {
            @Override
            public void onKitsLoaded() {
                kitsLoadedLatch.countDown();
            }
        });
        JSONArray configuration = MParticle.getInstance().Internal().getConfigManager().getLatestKitConfiguration();
        Logger.debug("Kit Framework loaded. IN TEST");
        if (configuration != null) {
            Logger.debug("Restoring previous Kit configuration IN TEST.");
            kitManager.updateKits(configuration);
        }
        kitsLoadedLatch.await();
    }

    public static void setPushInPushRegistrationHelper(Context context, String instanceId, String senderId) {
        PushRegistrationHelper.setPushRegistration(context, instanceId, senderId);
    }

    public static long getActivityDelay() {
        return AppStateManager.ACTIVITY_DELAY;
    }
}
