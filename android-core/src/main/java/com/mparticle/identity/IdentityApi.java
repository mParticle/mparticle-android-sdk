package com.mparticle.identity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mparticle.MParticle;
import com.mparticle.MParticleTask;
import com.mparticle.SdkListener;
import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.BaseHandler;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.listeners.ApiClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class that is used to access Identity endpoints to manage User's Attributes and Identities.
 */
@ApiClass
public class IdentityApi {
    public static int UNKNOWN_ERROR = -1;
    public static int THROTTLE_ERROR = 429;
    public static int BAD_REQUEST = 400;
    public static int SERVER_ERROR = 500;

    private Context mContext;
    private BaseHandler mBackgroundHandler;
    private BaseHandler mMainHandler;
    private MParticle.OperatingSystem mOperatingSystem;
    ConfigManager mConfigManager;
    MessageManager mMessageManager;
    KitManager mKitManager;
    private Internal mInternal = new Internal();

    MParticleUserDelegate mUserDelegate;
    private MParticleIdentityClient mApiClient;

    Set<IdentityStateListener> identityStateListeners = new HashSet<IdentityStateListener>();
    private static Object lock = new Object();

    protected IdentityApi() {
    }

    @SuppressLint("UnknownNullness")
    public IdentityApi(Context context, AppStateManager appStateManager, MessageManager messageManager, ConfigManager configManager, KitManager kitManager, MParticle.OperatingSystem operatingSystem) {
        this.mContext = context;
        this.mBackgroundHandler = messageManager.mUploadHandler;
        this.mUserDelegate = new MParticleUserDelegate(appStateManager, configManager, messageManager, kitManager);
        this.mConfigManager = configManager;
        this.mMessageManager = messageManager;
        this.mKitManager = kitManager;
        this.mOperatingSystem = operatingSystem;
        configManager.addMpIdChangeListener(new IdentityStateListenerManager());
        setApiClient(new MParticleIdentityClientImpl(context, configManager, operatingSystem));
    }

    /**
     * return the MParticleUser with the current MPID, even if an Identity request,
     * which might cause the MPID to change, is currently in progress.
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
     * Return the MParticleUser with the specified MPID, if it exists. If an MParticleUser with the
     * specified MPID does not exist, or the specifid MPID is 0, this will return null.
     *
     * @param mpid the desired MParticleUser's MPID
     */
    @Nullable
    public MParticleUser getUser(@NonNull Long mpid) {
        if (Constants.TEMPORARY_MPID != mpid && mConfigManager.mpidExists(mpid)) {
            return MParticleUserImpl.getInstance(mContext, mpid, mUserDelegate);
        } else {
            return null;
        }
    }

    /**
     * returns a list of {@link MParticleUser}s that have been seen by this device. The collection
     * is ordered by {@link MParticleUser#getLastSeenTime()}, from most to least recent
     *
     * @return a collection of {@link MParticleUser} ordered by descending {@link MParticleUser#getLastSeenTime()}
     */
    @NonNull
    public List<MParticleUser> getUsers() {
        List<MParticleUser> users = new ArrayList<MParticleUser>();
        Set<Long> mpids = mConfigManager.getMpids();
        mpids.remove(Constants.TEMPORARY_MPID);
        for (Long mpid : mpids) {
            users.add(MParticleUserImpl.getInstance(mContext, mpid, mUserDelegate));
        }
        sortUsers(users);
        return users;
    }

    @NonNull
    public String getDeviceApplicationStamp() {
        return mConfigManager.getDeviceApplicationStamp();
    }

    /**
     * Adds a global listener which will be invoked when the MPID or "logged in" status changes for the current user.
     *
     * @param listener callback for Identity State changes
     * @see IdentityStateListener
     */
    public void addIdentityStateListener(@NonNull IdentityStateListener listener) {
        identityStateListeners.add(listener);
    }

    /**
     * Removes an instance of a global listener by reference.
     *
     * @param listener callback for Identity State changes
     * @see IdentityStateListener
     */
    public void removeIdentityStateListener(@NonNull IdentityStateListener listener) {
        identityStateListeners.remove(listener);
    }

    /**
     * Calls the Identity Logout endpoint with an empty IdentityApiRequest.
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    @NonNull
    public MParticleTask<IdentityApiResult> logout() {
        return logout(null);
    }

    /**
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     * @see IdentityApiRequest
     *
     * calls the Identity Logout endpoint
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    @NonNull
    public MParticleTask<IdentityApiResult> logout(@Nullable final IdentityApiRequest logoutRequest) {
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
     * Calls the Identity Login endpoint with an empty IdentityApiRequest.
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    @NonNull
    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    /**
     * Calls the Identity Login endpoint.
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     * @see IdentityApiRequest
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    @NonNull
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
     * Calls the Identity Identify endpoint.
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     * @see IdentityApiRequest
     * @see MParticleTask and
     * @see IdentityApiResult
     */
    @NonNull
    public MParticleTask<IdentityApiResult> identify(@Nullable final IdentityApiRequest identifyRequest) {
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
     * Calls the Identity Modify endpoint. This should be used in place of the pre-version-5
     * MParticle.setUserAttribute() and MParticle.setUserIdentity() methods.
     *
     * @return an MParticleTask<IdentityApiResult> to handle the Asynchronous results
     * @see IdentityApiRequest
     * @see BaseIdentityTask
     */
    @NonNull
    public BaseIdentityTask modify(@NonNull final IdentityApiRequest updateRequest) {
        boolean devMode = MPUtility.isDevEnv() || MPUtility.isAppDebuggable(mContext);
        final BaseIdentityTask task = new BaseIdentityTask();

        if (updateRequest.mpid == null) {
            updateRequest.mpid = mConfigManager.getMpid();
        }
        if (Constants.TEMPORARY_MPID == updateRequest.mpid) {
            String message = "modify() requires a non-zero MPID, please make sure a MParticleUser is present before making a modify request.";
            if (devMode) {
                throw new IllegalArgumentException(message);
            } else {
                Logger.error(message);
            }
            task.setFailed(new IdentityHttpResponse(IdentityApi.UNKNOWN_ERROR, message));
            return task;
        }
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final IdentityHttpResponse result = getApiClient().modify(updateRequest);
                    if (!result.isSuccessful()) {
                        task.setFailed(result);
                    } else {
                        MParticleUserDelegate.setUserIdentities(mUserDelegate, updateRequest.getUserIdentities(), updateRequest.mpid);
                        task.setSuccessful(new IdentityApiResult(MParticleUserImpl.getInstance(mContext, updateRequest.mpid, mUserDelegate), null));
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mKitManager.onModifyCompleted(getUser(updateRequest.mpid), updateRequest);
                            }
                        });
                    }
                } catch (Exception ex) {
                    task.setFailed(new IdentityHttpResponse(IdentityApi.UNKNOWN_ERROR, ex.toString()));
                }
            }
        });
        return task;
    }

    /**
     * Initiate an Alias request. Retries are handled internally, so one an {@link AliasRequest} is submitted,
     * it will be retried unless there are unrecoverable errors. To listen for updates submit an
     * implementation of {@link com.mparticle.SdkListener} to {@link com.mparticle.MParticle#addListener(Context, SdkListener)}
     *
     * @param aliasRequest
     * @return
     */
    public boolean aliasUsers(@NonNull AliasRequest aliasRequest) {
        if (aliasRequest.getDestinationMpid() == 0 || aliasRequest.getSourceMpid() == 0) {
            Logger.error("AliasRequest does not have a valid destinationMpid and a valid sourceMpid");
            return false;
        }
        if (aliasRequest.getDestinationMpid() == aliasRequest.getSourceMpid()) {
            Logger.error("AliasRequest cannot have the same value for destinationMpid and sourceMpid");
            return false;
        }
        if (aliasRequest.getEndTime() == 0 || aliasRequest.getStartTime() == 0) {
            Logger.error("AliasRequest must have both a startTime and an endTime");
            return false;
        }
        if (aliasRequest.getEndTime() < aliasRequest.getStartTime()) {
            Logger.error("AliasRequest cannot have an startTime that is greater than its endTime");
            return false;
        }
        mMessageManager.logAliasRequest(aliasRequest);
        return true;
    }

    @NonNull
    public Internal Internal() {
        return mInternal;
    }

    void sortUsers(List<MParticleUser> users) {
        Collections.sort(users, new Comparator<MParticleUser>() {
            @Override
            public int compare(MParticleUser user1, MParticleUser user2) {
                long lst1 = user1.getLastSeenTime();
                long lst2 = user2.getLastSeenTime();
                if (lst1 > lst2) {
                    return -1;
                } else if (lst1 < lst2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
    }

    private void reset() {
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
                    if (mBackgroundHandler.isDisabled()) {
                        return;
                    }
                    try {
                        long startingMpid = mConfigManager.getMpid();
                        final IdentityHttpResponse result = networkRequest.request(identityApiRequest);

                        if (!result.isSuccessful()) {
                            ConfigManager.setIdentityRequestInProgress(false);
                            task.setFailed(result);
                        } else {
                            long newMpid = result.getMpId();
                            boolean isLoggedIn = result.isLoggedIn();
                            ConfigManager.setIdentityRequestInProgress(false);
                            mUserDelegate.setUser(mContext, startingMpid, newMpid, identityApiRequest.getUserIdentities(), identityApiRequest.getUserAliasHandler(), isLoggedIn);
                            final MParticleUser previousUser = startingMpid != newMpid ? getUser(startingMpid) : null;
                            task.setSuccessful(new IdentityApiResult(MParticleUserImpl.getInstance(mContext, newMpid, mUserDelegate), previousUser));
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    networkRequest.onPostExecute(new IdentityApiResult(getCurrentUser(), previousUser));
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
            mApiClient = new MParticleIdentityClientImpl(mContext, mConfigManager, mOperatingSystem);
        }
        return mApiClient;
    }

    /**
     * This should only be used for testing.
     */
    void setApiClient(MParticleIdentityClient client) {
        mApiClient = client;
    }

    interface IdentityNetworkRequestRunnable {
        IdentityHttpResponse request(IdentityApiRequest request) throws Exception;

        void onPostExecute(IdentityApiResult result);
    }

    public interface MpIdChangeListener {
        void onMpIdChanged(long newMpid, long previousMpid);
    }

    class IdentityStateListenerManager implements MpIdChangeListener {

        @Override
        public void onMpIdChanged(long newMpid, final long previousMpid) {
            final MParticleUser user = MParticleUserImpl.getInstance(mContext, newMpid, mUserDelegate);
            if (identityStateListeners != null && identityStateListeners.size() > 0) {
                if (mMainHandler == null) {
                    mMainHandler = new BaseHandler(Looper.getMainLooper());
                }
                MParticleUser previousUser = previousMpid != 0 ? MParticleUserImpl.getInstance(mContext, previousMpid, mUserDelegate) : null;
                mMainHandler.post(new BroadcastRunnable(identityStateListeners, user, previousUser));
            }
        }

        private class BroadcastRunnable implements Runnable {
            Set<IdentityStateListener> identityListeners;
            MParticleUser newUser;
            MParticleUser previousUser;

            private BroadcastRunnable(Set<IdentityStateListener> listeners, MParticleUser newUser, MParticleUser previousUser) {
                this.identityListeners = new HashSet<IdentityStateListener>(listeners);
                this.newUser = newUser;
                this.previousUser = previousUser;
            }

            public void run() {
                try {
                    for (IdentityStateListener listener : identityListeners) {
                        listener.onUserIdentified(newUser, previousUser);
                    }
                } catch (Exception e) {
                    Logger.error(e.toString());
                }
            }
        }
    }

    public static abstract class SingleUserIdentificationCallback implements IdentityStateListener {

        @Override
        public void onUserIdentified(MParticleUser user, MParticleUser previousUser) {
            MParticle.getInstance().Identity().removeIdentityStateListener(this);
            onUserFound(user);
        }

        public abstract void onUserFound(MParticleUser user);

    }

    /**
     * @hidden
     */
    public class Internal {
        public void reset() {
            IdentityApi.this.reset();
        }
    }
}