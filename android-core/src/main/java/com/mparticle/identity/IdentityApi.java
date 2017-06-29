package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticleTask;
import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.services.MParticleDBManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IdentityApi {
    private Handler mHandler;
    private MParticleUserDelegate mUserDelegate;
    private MParticleDBManager mMParticleDBManager;
    private MParticleIdentityClient mMParticleApiClient;
    private Set<WeakReference<IdentityStateListener>> identityStateListeners = new HashSet<WeakReference<IdentityStateListener>>();
    private Object lock = new Object();
    private static IdentityApi instance;

    public static IdentityApi getInstance(Context context, MessageManager messageManager, ConfigManager configManager) {
        if (instance == null) {
            instance = new IdentityApi(context, messageManager, configManager);
        }
        return instance;
    }

    private IdentityApi(Context context, MessageManager messageManager, ConfigManager configManager) {
        this.mHandler = messageManager.mUploadHandler;
        this.mUserDelegate = new MParticleUserDelegate(messageManager);
        this.mMParticleDBManager = messageManager.getMParticleDBManager();
        mMParticleDBManager.setIdentityStateListener(new IdentityStateListenerManager());
        this.mMParticleApiClient = new MParticleIdentityClientImpl(configManager, context);
    }

    @Nullable
    public MParticleUser getCurrentUser() {
        MParticleUser currentUser = mMParticleDBManager.getCurrentUser();
        if (currentUser!= null) {
            currentUser.setUserDelegate(mUserDelegate);
        }
        return currentUser;
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
            final BaseIdentityTask task = new IdentityApiResultTask();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.setSuccessful(mMParticleApiClient.logout(logoutRequest));
                    } catch (Exception ex) {
                        task.setFailed(ex);
                    }
                }
            });
            return task;
        }
    }

    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    public MParticleTask<IdentityApiResult> login(@Nullable final IdentityApiRequest loginRequest) {
        synchronized (lock) {
            final BaseIdentityTask<IdentityApiResult> task = new IdentityApiResultTask();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.setSuccessful(mMParticleApiClient.login(loginRequest));
                    } catch (Exception ex) {
                        task.setFailed(ex);
                    }
                }
            });
            return task;
        }
    }

    public synchronized MParticleTask<Void> modify(@NonNull final IdentityApiRequest updateRequest) {
        final BaseIdentityTask<Void> task = new BaseIdentityTask<Void>() {
            @Override
            Void buildResult(Object o) {
                return null;
            }
        };
        mHandler.post(new Runnable() {
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

    public synchronized MParticleTask<IdentityApiResult> identify(final IdentityApiRequest identifyRequest) {
        synchronized (lock) {
            final BaseIdentityTask<IdentityApiResult> task = new IdentityApiResultTask();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        task.setSuccessful(mMParticleApiClient.identify(identifyRequest));
                    } catch (Exception ex) {
                        task.setFailed(ex);
                    }
                }
            });
            return task;
        }
    }

    class IdentityApiResultTask extends BaseIdentityTask<IdentityApiResult> {

            @Override
            IdentityApiResult buildResult(final Object o) {
                return new IdentityApiResult() {
                    @Override
                    public MParticleUser getUser() {
                        return ((MParticleUser)o).setUserDelegate(mUserDelegate);
                    }
                };
            }
    }

    class IdentityStateListenerManager implements IdentityStateListener {

        @Override
        public void onUserIdentified(MParticleUser user) {
            List<WeakReference<IdentityStateListener>> toRemove = new ArrayList<WeakReference<IdentityStateListener>>();
            for (WeakReference<IdentityStateListener> listenerRef: identityStateListeners) {
                IdentityStateListener listener = listenerRef.get();
                if (listener != null) {
                    listener.onUserIdentified(user.setUserDelegate(mUserDelegate));
                } else {
                    toRemove.add(listenerRef);
                }
            }
            identityStateListeners.removeAll(toRemove);
        }
    }
}