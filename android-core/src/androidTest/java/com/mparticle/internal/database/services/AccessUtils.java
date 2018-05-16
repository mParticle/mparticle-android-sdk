package com.mparticle.internal.database.services;

public final class AccessUtils {

    public static void setMessageStoredListener(MParticleDBManager.MessageListener listener) {
        MParticleDBManager.setMessageListener(listener);
    }
}