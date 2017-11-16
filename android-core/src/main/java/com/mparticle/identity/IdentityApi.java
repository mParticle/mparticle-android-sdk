package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.DatabaseTables;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MParticleApiClient;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.services.MParticleDBManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class IdentityApi {
    public static int UNKNOWN_ERROR = -1;
    public static int THROTTLE_ERROR = 429;
    public static int BAD_REQUEST = 400;
    public static int SERVER_ERROR = 500;

    private Context mContext;
    private Handler mBackgroundHandler;
    ConfigManager mConfigManager;
    MessageManager mMessageManager;

    MParticleUserDelegate mUserDelegate;
    private static MParticleIdentityClient sApiClient;
    private static boolean sTestAlreadySet = false;

    private Set<IdentityStateListener> identityStateListeners = new HashSet<IdentityStateListener>();
    private static Object lock = new Object();

    IdentityApi() {}

    public IdentityApi(Context context, AppStateManager appStateManager, MessageManager messageManager, ConfigManager configManager, KitManager kitManager) {
        this.mContext = context;
        this.mBackgroundHandler = messageManager.mUploadHandler;
        this.mUserDelegate = new MParticleUserDelegate(appStateManager, configManager, messageManager, kitManager, new MParticleDBManager(context, DatabaseTables.getInstance(context)));
        this.mConfigManager = configManager;
        this.mMessageManager = messageManager;
        configManager.addMpIdChangeListener(new IdentityStateListenerManager());
        if (sTestAlreadySet) {
            sTestAlreadySet = false;
        } else {
            setApiClient(new MParticleIdentityClientImpl(configManager, context), false);
        }
    }

    /**
     * return the current MPID, even if an Identity request, which might cause the MPID to change, is
     * currently in progress
     */
    @Nullable
    public MParticleUser getCurrentUser() {
        long mpid = mConfigManager.getMpid();
        if (Constants.TEMPORARY_MPID == mpid) {
            return null;
        } else {
            return MParticleUser.getInstance(mContext, mpid, mUserDelegate);
        }
    }

    public void addIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.add(listener);
    }

    public void removeIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.remove(listener);
    }

    public MParticleTask<IdentityApiResult> logout() {
        return logout(null);
    }

    public MParticleTask<IdentityApiResult> logout(final IdentityApiRequest logoutRequest) {
        return makeIdentityRequest(logoutRequest, new IdentityNetworkRequestRunnable() {
            @Override
            public IdentityHttpResponse request(IdentityApiRequest request) throws Exception {
                return getApiClient().logout(request);
            }
        });
    }

    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    public MParticleTask<IdentityApiResult> login(@Nullable final IdentityApiRequest loginRequest) {
        return makeIdentityRequest(loginRequest, new IdentityNetworkRequestRunnable() {
            @Override
            public IdentityHttpResponse request(IdentityApiRequest request) throws Exception {
                return getApiClient().login(request);
            }
        });
    }

    public MParticleTask<IdentityApiResult> identify(final IdentityApiRequest identifyRequest) {
        return makeIdentityRequest(identifyRequest, new IdentityNetworkRequestRunnable() {
            @Override
            public IdentityHttpResponse request(IdentityApiRequest request) throws Exception {
                return getApiClient().identify(request);
            }
        });
    }

    public BaseIdentityTask modify(@NonNull final IdentityApiRequest updateRequest) {
        boolean devMode = MPUtility.isDevEnv() || MPUtility.isAppDebuggable(mContext);
        final BaseIdentityTask task = new BaseIdentityTask();

        if (updateRequest == null) {
            String message = "modify() requires a valid IdentityApiRequest";
            if (devMode) {
                throw new IllegalArgumentException(message);
            } else {
                Logger.error(message);
            }
            task.setFailed(new IdentityHttpResponse(IdentityApi.UNKNOWN_ERROR, message));
        } else {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        final IdentityHttpResponse result = getApiClient().modify(updateRequest);
                        if (!result.isSuccessful()) {
                            task.setFailed(result);
                        } else {
                            task.setSuccessful(new IdentityApiResult(getCurrentUser()));
                            if (updateRequest.getUserIdentities() != null) {
                                for (Map.Entry<MParticle.IdentityType, String> entry : updateRequest.getUserIdentities().entrySet()) {
                                    mUserDelegate.setUserIdentity(entry.getValue(), entry.getKey(), mConfigManager.getMpid());
                                }
                            }
                        }
                    } catch (Exception ex) {
                        task.setFailed(new IdentityHttpResponse(IdentityApi.UNKNOWN_ERROR, ex.toString()));
                    }
                }
            });
        }
        return task;
    }


    private BaseIdentityTask makeIdentityRequest(IdentityApiRequest request, final IdentityNetworkRequestRunnable networkRequest) {
        if (request == null) {
            request = IdentityApiRequest.withEmptyUser().build();
        }
        final BaseIdentityTask task = new BaseIdentityTask();
        final long startingMpid = mConfigManager.getMpid();
        ConfigManager.setIdentityRequestInProgress(true);
        final IdentityApiRequest identityApiRequest = request;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        final IdentityHttpResponse result = networkRequest.request(identityApiRequest);

                        if (!result.isSuccessful()) {
                            ConfigManager.setIdentityRequestInProgress(false);
                            task.setFailed(result);
                        } else {
                            long newMpid = result.getMpId();
                            ConfigManager.setIdentityRequestInProgress(false);
                            mUserDelegate.setUser(mContext, startingMpid, newMpid, identityApiRequest.getUserIdentities(), identityApiRequest.getUserAliasHandler());
                            task.setSuccessful(new IdentityApiResult(getCurrentUser()));
                        }
                    } catch (Exception ex) {
                        ConfigManager.setIdentityRequestInProgress(false);
                        task.setFailed(new IdentityHttpResponse(IdentityApi.UNKNOWN_ERROR, ex.toString()));
                    }
                }
            }
        });
        return task;
    }

    MParticleIdentityClient getApiClient() {
        if (sApiClient == null) {
            sApiClient = new MParticleIdentityClientImpl(mConfigManager, mContext);
        }
        return sApiClient;
    }

    /**
     * @param overrideNextInstance - retain this client for the next instance to be constructed, testing method
     */
    static void setApiClient(MParticleIdentityClient client, boolean overrideNextInstance) {
        sTestAlreadySet = overrideNextInstance;
        sApiClient = client;
    }

    interface IdentityNetworkRequestRunnable {
        IdentityHttpResponse request(IdentityApiRequest request) throws Exception;
    }

    public interface MpIdChangeListener {
        void onMpIdChanged(long mpid);
    }

    class IdentityStateListenerManager implements MpIdChangeListener {

        @Override
        public void onMpIdChanged(long mpid) {
            final MParticleUser user = MParticleUser.getInstance(mContext, mpid, mUserDelegate);
            if (identityStateListeners != null && identityStateListeners.size() > 0) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            for (IdentityStateListener listener : new HashSet<IdentityStateListener>(identityStateListeners)) {
                                if (listener != null) {
                                    listener.onUserIdentified(user);
                                }
                            }
                        } catch (Exception e) {
                            Logger.error(e.toString());
                        }
                    }
                });
            }
        }
    }
}