package com.mparticle;

import android.content.Context;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.BeforeClass;

import static junit.framework.Assert.fail;

public abstract class BaseAbstractTest {
    protected static Context mContext;

    private static boolean beforeClassCalled = false;

    @BeforeClass
    public static void beforeClassImpl() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
    }

    @Before
    public void beforeImpl() throws Exception {
        if (!beforeClassCalled) {
            beforeClassCalled = true;
            beforeClassBase();
            beforeClass();
        }
        mContext = InstrumentationRegistry.getContext();
        beforeBase();
        before();
    }

    protected abstract void beforeClassBase() throws Exception;
    protected abstract void beforeClass() throws Exception;
    protected abstract void beforeBase() throws Exception;
    protected abstract void before() throws Exception;

}
