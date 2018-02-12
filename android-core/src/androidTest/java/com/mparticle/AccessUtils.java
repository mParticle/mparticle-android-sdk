package com.mparticle;

import com.mparticle.internal.MessageManager;

public class AccessUtils {

    public static MessageManager getMessageManager() {
        return MParticle.getInstance().mMessageManager;
    }

    public static void logEvent(ConsentEvent consentEvent) {
        MParticle.getInstance().logEvent(consentEvent);
    }
}
