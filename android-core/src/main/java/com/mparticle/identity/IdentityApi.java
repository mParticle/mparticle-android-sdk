package com.mparticle.identity;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.MParticleTask;
import com.mparticle.internal.MParticleApiClient;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.UploadHandler;
import com.mparticle.internal.database.services.MParticleDBManager;

import java.util.HashSet;
import java.util.Set;

public final class IdentityApi {
    private Handler mHandler;
    private MParticleUserDelegate mUserDelegate;
    private MParticleDBManager mMParticleDBManager;
    private MParticleApiClient mMParticleApiClient;
    private Set<IdentityStateListener> identityStateListeners = new HashSet<IdentityStateListener>();

    public IdentityApi(UploadHandler uploadHandler, MessageManager messageManager, MParticleDBManager dbManager, MParticleApiClient mParticleApiClient) {
        this.mHandler = uploadHandler;
        this.mUserDelegate = new MParticleUserDelegate(messageManager);
        this.mMParticleDBManager = dbManager;
        mMParticleDBManager.setIdentityStateListener(new IdentityStateListener() {
            @Override
            public void onUserIdentified(MParticleUser user) {
                for (IdentityStateListener listener: identityStateListeners) {
                    if (listener != null) {
                        listener.onUserIdentified(user.setUserDelegate(mUserDelegate));
                    }
                }
            }
        });
        this.mMParticleApiClient = mParticleApiClient;
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
        MParticleUser user;
        if ((user = getCurrentUser()) != null) {
            listener.onUserIdentified(user);
        }
        identityStateListeners.add(listener);
    }

    public void removeIdentityStateListener(IdentityStateListener listener) {
        identityStateListeners.remove(listener);
    }

    public MParticleTask<IdentityApiResult> logout() {
        return logout(null);
    }

    public MParticleTask<IdentityApiResult> logout(final IdentityApiRequest logoutRequest) {
        final BaseIdentityTask task = new IdentityApiResultTask();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    task.setSuccessful(mMParticleApiClient.logout(logoutRequest));
                }
                catch (Exception ex) {
                    task.setFailed(ex);
                }
            }
        });
        return task;
    }

    public MParticleTask<IdentityApiResult> login() {
        return login(null);
    }

    public MParticleTask<IdentityApiResult> login(@Nullable final IdentityApiRequest loginRequest) {
        final BaseIdentityTask<IdentityApiResult> task = new IdentityApiResultTask();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    task.setSuccessful(mMParticleApiClient.login(loginRequest));
                }
                catch (Exception ex) {
                    task.setFailed(ex);
                }
            }
        });
        return task;
    }

    public MParticleTask<Void> modify(@NonNull final IdentityApiRequest updateRequest) {
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

    public MParticleTask<IdentityApiResult> identify(final IdentityApiRequest identifyRequest) {
        final BaseIdentityTask<IdentityApiResult> task = new IdentityApiResultTask();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    task.setSuccessful(mMParticleApiClient.identify(identifyRequest));
                }
                catch (Exception ex) {
                    task.setFailed(ex);
                }
            }
        });
        return task;
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
}