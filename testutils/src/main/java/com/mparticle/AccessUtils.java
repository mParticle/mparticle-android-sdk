package com.mparticle;

import android.content.Context;

import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;

public class AccessUtils {

    public static void reset(Context context, boolean deleteDatabase) {
        MParticle.reset(context, deleteDatabase);
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
}
