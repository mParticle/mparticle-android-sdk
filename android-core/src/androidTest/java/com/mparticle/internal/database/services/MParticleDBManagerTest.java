package com.mparticle.internal.database.services;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.DatabaseTables;

import org.junit.BeforeClass;
import org.junit.Test;

public class MParticleDBManagerTest {
    Context mContext;
    MParticleDBManager dbManager;

    @BeforeClass
    public static void setup() {
        Looper.prepare();
    }

    @BeforeClass
    public void preConditions() {
        mContext = InstrumentationRegistry.getContext();
        dbManager = new MParticleDBManager(mContext, DatabaseTables.getInstance(mContext));
    }

    public static void setMessageListener(MParticleDBManager.MessageListener listener) {
        MParticleDBManager.setMessageListener(listener);
    }

}
