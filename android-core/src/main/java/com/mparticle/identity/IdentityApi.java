package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.BaseHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.DatabaseTables;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.services.MParticleDBManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that is used to access Identity endpoints to manage User's Attributes and Identities
 */
public class IdentityApi {
    public static int UNKNOWN_ERROR = -1;
    public static int THROTTLE_ERROR = 429;
    public static int BAD_REQUEST = 400;
    public static int SERVER_ERROR = 500;

    private Context mContext;
    private BaseHandler mBackgroundHandler;
    private BaseHandler mMainHandler;
    ConfigManager mConfigManager;
    MessageManager mMessageManager;
    KitManager mKitManager;

    MParticleUserDelegate mUserDelegate;
    private MParticleIdentityClient mApiClient;

    private Set<IdentityStateListener> identityStateListeners = new HashSet<IdentityStateListener>();
    private static Object lock = new Object();

    IdentityApi() {}

    public IdentityApi(Context context, AppStateManager appStateManager, MessageManager messageManager, ConfigManager configManager, KitManager kitManager) {
        this.mContext = context;
        this.mBackgroundHandler = messageManager.mUploadHandler;
        this.mUserDelegate = new MParticleUserDelegate(appStateManager, configManager, messageManager, kitManager, new MParticleDBManager(context, DatabaseTables.getInstance(context)));
        this.mConfigManager = configManager;
        this.mMessageManager = messageManager;
        this.mKitManager = kitManager;
        configManager.addMpIdChangeListener(new IdentityStateListenerManager());
        setApiClient(new MParticleIdentityClientImpl(context, configManager));
    }

    /**
     * return the MParticleUser with the current MPID, even if an Identity request,
     * which might cause the MPID to change, is currently in progress
     *
     * @see IdentityStateListener
     */
    @Nullable
    public MParticleUser getCurrentUser() {
        long mpid = mConfigManager.getMpid();
        if (Constants.TEMPORARY_MPID == mpid) {
            return null;
        } else {
            return MParticleUserImpl.getInstance(mContext, mpid, mUserDelegate);
        }
    }

    /**
     * return the MParticleUser with the specified MPID, if it exists. If an MParticleUser with the
     * specified MPID does not exist, or the specifid MPID is 0, this will return null
     *
     * @param mpid the desired MParticleUser's MPID
     */
    @Nullable
    public MParticleUser getUser(Long mpid) {
            if (mConfigManager.mpidExists(mpid)) {
                return MParticleUserImpl.getInstance(mContext, mpid, mUserDelegate);
            } else {
            return null;
        }
    }

    public List<MParticleUser> getUsers() {
        List<MParticleUser> users = new ArrayList<MParticleUser>();
        Set<Long> mpids = mConfigManager.getMpids();
        mpids.remove(0L);
        for (Long mpid: mpids) {
            users.add(MParticleUserImpl.getInstance(mContext, mpid, mUserDelegate));
        }
        return users;
    }

    /**
     * adds a global listener for any changes in Identity State. This will give you a callback when
     * a user is identified
     * @param listener callback for Identity State changes
     *
     * @see IdentityStateListener
     */
    public void addIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.add(listener);
    }

    /**
     * removes an instance of a global listener by reference
     * @param listener callback for Identity State changes
     *
     * @see IdentityStateListener
     */
    public void removeIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.remove(listener);
    }

    /**
     * calls the Identity Logout endpoint with an empty IdentityApiRequest
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     *
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    public MParticleTask<IdentityApiResult> logout() {
        return logout(null);
    }

    /**
     * @see IdentityApiRequest
     *
     * calls the Identity Logout endpoint
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     *
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    public MParticleTask<IdentityApiResult> logout(final IdentityApiRequest logoutRequest) {
        return makeIdentityRequest(logoutRequest, new IdentityNetworkRequestRunnable() {
            @Override
            public IdentityHttpResponse request(IdentityApiRequest request) throws Exception {
                return getApiClient().logout(request);
            }

            @Override
            public void onPostExecute(IdentityApiResult result) {
                mKitManager.onLogoutCompleted(result.getUser(), logoutRequest);
            }
        });
    }

    /**
     * calls the Identity Login endpoint with an empty IdentityApiRequest
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     *
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    /**
     * @see IdentityApiRequest
     *
     * calls the Identity Login endpoint
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     *
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    public MParticleTask<IdentityApiResult> login(@Nullable final IdentityApiRequest loginRequest) {
        return makeIdentityRequest(loginRequest, new IdentityNetworkRequestRunnable() {
            @Override
            public IdentityHttpResponse request(IdentityApiRequest request) throws Exception {
                return getApiClient().login(request);
            }

            @Override
            public void onPostExecute(IdentityApiResult result) {
                mKitManager.onLoginCompleted(result.getUser(), loginRequest);
            }
        });
    }

    /**
     * @see IdentityApiRequest
     *
     * calls the Identity Identify endpoint
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     *
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    public MParticleTask<IdentityApiResult> identify(final IdentityApiRequest identifyRequest) {
        return makeIdentityRequest(identifyRequest, new IdentityNetworkRequestRunnable() {
            @Override
            public IdentityHttpResponse request(IdentityApiRequest request) throws Exception {
                return getApiClient().identify(request);
            }

            @Override
            public void onPostExecute(IdentityApiResult result) {
                mKitManager.onIdentifyCompleted(result.getUser(), identifyRequest);
            }
        });
    }

    /**
     * @see IdentityApiRequest
     *
     * calls the Identity Modify endpoint. This should be used in place of the pre-version-5
     * MParticle.setUserAttribute() and MParticle.setUserIdentity() methods
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     *
     * @see BaseIdentityTask
     */
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
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mKitManager.onModifyCompleted(getCurrentUser(), updateRequest);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        task.setFailed(new IdentityHttpResponse(IdentityApi.UNKNOWN_ERROR, ex.toString()));
                    }
                }
            });
        }
        return task;
    }

    public void reset() {
        identityStateListeners = new HashSet<IdentityStateListener>();
        mBackgroundHandler.removeCallbacksAndMessages(null);
        mBackgroundHandler.disable(true);
        if (mMainHandler != null) {
            mMainHandler.disable(true);
            mMainHandler.removeCallbacksAndMessages(null);
        }
    }

    private BaseIdentityTask makeIdentityRequest(IdentityApiRequest request, final IdentityNetworkRequestRunnable networkRequest) {
        if (request == null) {
            request = IdentityApiRequest.withEmptyUser().build();
        }
        final BaseIdentityTask task = new BaseIdentityTask();
        ConfigManager.setIdentityRequestInProgress(true);
        final IdentityApiRequest identityApiRequest = request;
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        long startingMpid = mConfigManager.getMpid();
                        final IdentityHttpResponse result = networkRequest.request(identityApiRequest);

                        if (!result.isSuccessful()) {
                            ConfigManager.setIdentityRequestInProgress(false);
                            task.setFailed(result);
                        } else {
                            long newMpid = result.getMpId();
                            ConfigManager.setIdentityRequestInProgress(false);
                            mUserDelegate.setUser(mContext, startingMpid, newMpid, identityApiRequest.getUserIdentities(), identityApiRequest.getUserAliasHandler());
                            task.setSuccessful(new IdentityApiResult(getCurrentUser()));
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    networkRequest.onPostExecute(new IdentityApiResult(getCurrentUser()));
                                }
                            });
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
        if (mApiClient == null) {
            mApiClient = new MParticleIdentityClientImpl(mContext, mConfigManager);
        }
        return mApiClient;
    }

    /**
     * this should only be used for testing
     */
    void setApiClient(MParticleIdentityClient client) {
        mApiClient = client;
    }

    interface IdentityNetworkRequestRunnable {
        IdentityHttpResponse request(IdentityApiRequest request) throws Exception;
        void onPostExecute(IdentityApiResult result);
    }

    public interface MpIdChangeListener {
        void onMpIdChanged(long mpid);
    }

    class IdentityStateListenerManager implements MpIdChangeListener {

        @Override
        public void onMpIdChanged(long mpid) {
            final MParticleUser user = MParticleUserImpl.getInstance(mContext, mpid, mUserDelegate);
            if (identityStateListeners != null && identityStateListeners.size() > 0) {
                if (mMainHandler == null) {
                    mMainHandler = new BaseHandler(Looper.getMainLooper());
                }
                mMainHandler.post(new Runnable() {

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