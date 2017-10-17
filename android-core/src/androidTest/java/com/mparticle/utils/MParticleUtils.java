package com.mparticle.utils;

import android.os.Handler;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;

import com.mparticle.MParticle;
import com.mparticle.internal.AccessUtils;
import com.mparticle.internal.Logger;

public class MParticleUtils {

    public static void awaitStoreMessage() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageHandler();
        while(messageHandler.hasMessages(AccessUtils.STORE_MESSAGE)) {
            Thread.sleep(100);
            //do nothing, just block
        }
        return;
    }

    public static void awaitSetUserAttribute() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageHandler();
        while (messageHandler.hasMessages(AccessUtils.SET_USER_ATTRIBUTE)) {
            Thread.sleep(100);
            //do nothing, just block
        }
        return;
    }

    public static void awaitRemoveUserAttribute() throws InterruptedException {
        Handler messageHandler = AccessUtils.getMessageHandler();
        while (messageHandler.hasMessages(AccessUtils.REMOVE_USER_ATTRIBUTE)) {
            Thread.sleep(100);
            //do nothing, just block
        }
        return;
    }

    public static void awaitUploadRunnables() throws InterruptedException {
        Handler uploadHandler = AccessUtils.getUploadHandler();
        do {
            Thread.sleep(1000);
        }
        while (uploadHandler.hasMessages(0));
        return;
    }

    public static void clear() {
        MParticle.setInstance(null);
        AccessUtils.deleteDatabase();
        AccessUtils.deleteConfigManager(InstrumentationRegistry.getContext());
    }

}
