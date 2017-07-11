package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.BaseIdentityTask;
import com.mparticle.MParticleTask;

import com.mparticle.internal.AppStateManager;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.dto.MParticleUserDTO;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IdentityApi {
    private Handler mBackgroundHandler, mMainHandler;
    ConfigManager mConfigManager;
    MessageManager mMessageManager;

    MParticleUserDelegate mUserDelegate;
    MParticleIdentityClient mMParticleApiClient;

    private Set<WeakReference<IdentityStateListener>> identityStateListeners = new HashSet<WeakReference<IdentityStateListener>>();
    private Object lock = new Object();
    private static IdentityApi instance;

    public static IdentityApi getInstance(Context context, AppStateManager appStateManager, MessageManager messageManager, ConfigManager configManager, KitManager kitManager) {
        if (instance == null) {
            instance = new IdentityApi(context, appStateManager, messageManager, configManager, kitManager);
        }
        return instance;
    }

    public IdentityApi(Context context, AppStateManager appStateManager, MessageManager messageManager, ConfigManager configManager, KitManager kitManager) {
        this.mBackgroundHandler = messageManager.mUploadHandler;
        this.mMainHandler = new Handler(Looper.myLooper());
        this.mUserDelegate = new MParticleUserDelegate(appStateManager, configManager, messageManager, kitManager);
        this.mConfigManager = configManager;
        this.mMessageManager = messageManager;
        configManager.setMpIdChangeListener(new IdentityStateListenerManager());
        this.mMParticleApiClient = new MParticleIdentityClientImpl(configManager, context);
    }

    //for testing only
    public IdentityApi(Context context, AppStateManager appStateManager, MessageManager messageManager, ConfigManager configManager, KitManager kitManager, Looper looper) {
        this.mBackgroundHandler = messageManager.mUploadHandler;
        this.mMainHandler = new Handler(looper);
        this.mUserDelegate = new MParticleUserDelegate(appStateManager, configManager, messageManager, kitManager);
        this.mConfigManager = configManager;
        this.mMessageManager = messageManager;
        configManager.setMpIdChangeListener(new IdentityStateListenerManager());
        this.mMParticleApiClient = new MParticleIdentityClientImpl(configManager, context);
    }


    /**
     * returns the current MParticleUser. There are times, while requests which might change the MPID
     * are in progress, that the MPID is "in flux" signifying that the MPID might have already changed
     * based on the information in the request, but we are not yet aware of the new MPID, since the
     * request has not returned. In this case, the internal MPID, provided by the ConfigManager, will
     * be a temporary placeholder value, but the MParticleUser returned by this method, will reflect
     * the current MParticleUser, before the MPID became "in flux"
     */
    public MParticleUser getCurrentUser() {
        long mpid = mConfigManager.getMpid();
        // if the internal MPID is temporary, we need to check if that is because we are using the
        // temporary MPID as a placeholder because a request is in progress, or if it is because we
        // just do not have a current MPID
        if (Constants.TEMPORARY_MPID == mpid) {
            if (mUserDelegate.hasMpIdInFlux()) {
                return MParticleUser.getInstance(mUserDelegate.getMpIdInFlux(), mUserDelegate);
            } else {
                return null;
            }
        } else {
            return MParticleUser.getInstance(mpid, mUserDelegate);
        }
    }

    public void addIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.add(new ComparableWeakReference<IdentityStateListener>(listener));
    }

    public void removeIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.remove(new ComparableWeakReference<IdentityStateListener>(listener));
    }

    public MParticleTask<IdentityApiResult> logout() {
        return logout(null);
    }

    public MParticleTask<IdentityApiResult> logout(final IdentityApiRequest logoutRequest) {
        synchronized (lock) {
            return makeIdentityRequest(logoutRequest, new IdentityNetworkRequestRunnable() {
                @Override
                public MParticleUserDTO request(IdentityApiRequest request) throws Exception {
                    return mMParticleApiClient.logout(request);
                }
            });
        }
    }

    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    public MParticleTask<IdentityApiResult> login(@Nullable final IdentityApiRequest loginRequest) {
        synchronized (lock) {
            return makeIdentityRequest(loginRequest, new IdentityNetworkRequestRunnable() {
                @Override
                public MParticleUserDTO request(IdentityApiRequest request) throws Exception {
                    return mMParticleApiClient.login(request);
                }
            });
        }
    }

    public synchronized MParticleTask<IdentityApiResult> identify(final IdentityApiRequest identifyRequest) {
        synchronized (lock) {
            return makeIdentityRequest(identifyRequest, new IdentityNetworkRequestRunnable() {
                @Override
                public MParticleUserDTO request(IdentityApiRequest request) throws Exception {
                    return mMParticleApiClient.identify(request);
                }
            });
        }
    }

    public synchronized MParticleTask<Void> modify(@NonNull final IdentityApiRequest updateRequest) {
        if (updateRequest == null) {
            throw new IllegalArgumentException("modify() requires a valid IdentityApiRequest");
        }
        final BaseIdentityTask<Void> task = new BaseIdentityTask<Void>() {
            @Override
            public Void buildResult(Object o) {
                return null;
            }
        };
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    task.setSuccessful(mMParticleApiClient.modify(updateRequest));
                }
                catch (Exception ex) {
                    task.setFailed(ex);
                }
            }
        });
        return task;
    }


    private BaseIdentityTask makeIdentityRequest(final IdentityApiRequest identityApiRequest, final IdentityNetworkRequestRunnable networkRequest) {
        final BaseIdentityTask task = new IdentityApiResultTask();
        final long startingMpid = mConfigManager.getMpid();
        mUserDelegate.useTemporaryMpId(startingMpid);
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final MParticleUserDTO result = networkRequest.request(identityApiRequest);
//                    mMainHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
                            if (result.hasError()) {
                                // If the requst has an error, set the MPID back
                                mUserDelegate.migrateTemporaryToMpId(startingMpid);
                                task.setFailed(result.getError());
                            } else {
                                // when the request succeeds, set new MPID
                                boolean userChanged = result.getMpId() != startingMpid;
                                mUserDelegate.setUser(result, userChanged);
                                mUserDelegate.migrateTemporaryToMpId(result.getMpId());
                                task.setSuccessful(result.getMpId());
                            }
//                        }
//                    });
                } catch (Exception ex) {
                    // If we get an exception, set the MPID back
                    mUserDelegate.migrateTemporaryToMpId(startingMpid);
                    task.setFailed(ex);
                }
            }
        });
        return task;
    }

    class IdentityApiResultTask extends BaseIdentityTask<IdentityApiResult> {

            @Override
            public IdentityApiResult buildResult(final Object o) {
                return new IdentityApiResult() {
                    @Override
                    public MParticleUser getUser() {
                        return MParticleUser.getInstance((Long)o, mUserDelegate);
                    }
                };
            }
    }

    void setApiClient(MParticleIdentityClient client) {
        this.mMParticleApiClient = client;
    }

    interface IdentityNetworkRequestRunnable {
        MParticleUserDTO request(IdentityApiRequest request) throws Exception;
    }

    public interface MpIdChangeListener {
        void onMpIdChanged(long mpid);
    }

    class IdentityStateListenerManager implements MpIdChangeListener {

        @Override
        public void onMpIdChanged(long mpid) {
            MParticleUser user = MParticleUser.getInstance(mpid, mUserDelegate);
            List<WeakReference<IdentityStateListener>> toRemove = new ArrayList<WeakReference<IdentityStateListener>>();
            for (WeakReference<IdentityStateListener> listenerRef : identityStateListeners) {
                IdentityStateListener listener = listenerRef.get();
                if (listener != null) {
                    listener.onUserIdentified(user);
                } else {
                    toRemove.add(listenerRef);
                }
            }
            identityStateListeners.removeAll(toRemove);
        }
    }
}