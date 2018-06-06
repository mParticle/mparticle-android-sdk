package com.mparticle;

import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.KitManager;
import com.mparticle.internal.MessageManager;

public class AccessUtils {

    public static MessageManager getMessageManager() {
        return MParticle.getInstance().mMessageManager;
    }

    public static void setKitManager(KitFrameworkWrapper kitManager) {
        MParticle.getInstance().mKitManager = kitManager;
    }
}
