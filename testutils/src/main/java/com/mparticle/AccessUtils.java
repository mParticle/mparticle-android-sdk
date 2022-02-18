package com.mparticle;

import android.content.Context;
import android.os.Build;
import android.os.Message;
import android.os.MessageQueue;

import androidx.annotation.RequiresApi;

import com.mparticle.identity.BaseIdentityTask;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.MPUtility;
import com.mparticle.internal.MessageManager;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AccessUtils {

    public static void reset(Context context, boolean deleteDatabase) {
        MParticle.reset(context, deleteDatabase);
    }

    /**
     * This is a way less than ideal implementation, but I think the insight is very important.
     *
     * This method returns an ordered list of the pending Messages in the UploadHandler queue. This
     * gives us the ability to test the UploadHandler's true "state" when looking closely at how
     * our Upload loop is performing
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static Set<Message> getUploadHandlerMessageQueue() {
        return getMessageManager().mUploadHandler.getMessageQueue();
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
