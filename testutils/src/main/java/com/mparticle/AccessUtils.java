package com.mparticle;

import android.content.Context;
import android.os.Build;
import android.os.Message;

import androidx.annotation.RequiresApi;

import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;

import java.util.Set;

public class AccessUtils {

    public static void reset(Context context, boolean deleteDatabase, boolean switchingWorkspaces) {
        MParticle.reset(context, deleteDatabase, switchingWorkspaces);
    }

    public static MessageManager getMessageManager() {
        return MParticle.getInstance().mMessageManager;
    }

    public static void setKitManager(KitFrameworkWrapper kitManager) {
        MParticle.getInstance().mKitManager = kitManager;
    }

    public static BaseIdentityTask getIdentityTask(MParticleOptions.Builder builder) {
        return builder.identityTask;
    }

    public static MParticleOptions.Builder setCredentialsIfEmpty(MParticleOptions.Builder builder) {
        if (MPUtility.isEmpty(builder.apiKey) && MPUtility.isEmpty(builder.apiSecret)) {
            builder.credentials("key", "secret");
        }
        return builder;
    }

    public static MParticleOptions emptyMParticleOptions(Context context) {
        return MParticleOptions.builder(context).buildForInternalRestart();
    }
}
