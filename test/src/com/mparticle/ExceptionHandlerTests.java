package com.mparticle;

import static org.mockito.Mockito.*;

import java.lang.Thread.UncaughtExceptionHandler;

import android.test.AndroidTestCase;

public class ExceptionHandlerTests extends AndroidTestCase {

    private MessageManager mMockMessageManager;
    private MParticleAPI mMParticleAPI;
    private UncaughtExceptionHandler mOriginalUEH;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mOriginalUEH = Thread.getDefaultUncaughtExceptionHandler();
        mMockMessageManager = mock(MockableMessageManager.class);
        mMParticleAPI = new MParticleAPI(getContext(), "TestAppKey", mMockMessageManager);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        Thread.setDefaultUncaughtExceptionHandler(mOriginalUEH);
    }

    public void testEnableUncaughtExceptionHandling() {
        mMParticleAPI.enableUncaughtExceptionLogging();
        UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        assertTrue(ueh instanceof ExceptionHandler);
    }

    public void testDoubleEnableUncaughtExceptionHandling() {
        mMParticleAPI.enableUncaughtExceptionLogging();
        mMParticleAPI.enableUncaughtExceptionLogging();
        UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        assertTrue(ueh instanceof ExceptionHandler);
    }

    public void testDisableUncaughtExceptionHandling() {
        mMParticleAPI.enableUncaughtExceptionLogging();
        mMParticleAPI.disableUncaughtExceptionLogging();
        UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        assertFalse(ueh instanceof ExceptionHandler);
        assertSame(ueh, mOriginalUEH);
    }

    public void testDoubleDisableUncaughtExceptionHandling() {
        mMParticleAPI.disableUncaughtExceptionLogging();
        mMParticleAPI.disableUncaughtExceptionLogging();
        UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        assertFalse(ueh instanceof ExceptionHandler);
        assertSame(ueh, mOriginalUEH);
    }

}
