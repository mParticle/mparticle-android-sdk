package com.mparticle.internal.database.services;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.DatabaseTables;

import org.junit.BeforeClass;
import org.junit.Test;

public class AccessUtils {

    public static void setMessageStoredListener(MParticleDBManager.MessageListener listener) {
        MParticleDBManager.setMessageListener(listener);
    }

}
