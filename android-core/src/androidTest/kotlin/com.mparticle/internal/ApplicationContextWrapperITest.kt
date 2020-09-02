package com.mparticle.internal

import android.app.Activity
import android.app.Application
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.support.test.InstrumentationRegistry
import com.mparticle.OrchestratorOnly
import com.mparticle.testutils.BaseAbstractTest
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertNull
import org.junit.Test


class ApplicationContextWrapperITest {


    /**
     * This test specifically addresses a problem we had where a callback was being registered on a
     * new thread which was not initialized as a looper. This setup (not extending BaseAbstractTest)
     * allows us to recreate this scenario
     */
    @OrchestratorOnly
    @Test
    fun testRegisterListenerBackgroundThread() {
        val applicationContextWrapper = ApplicationContextWrapper(InstrumentationRegistry.getContext().applicationContext as Application)
        var exception: Exception? = null
        assertNull(Looper.myLooper())
        try {
            applicationContextWrapper.registerActivityLifecycleCallbacks(MockCallbacks())
            //call it again to make sure we are not initializing the Looper twice
            applicationContextWrapper.registerActivityLifecycleCallbacks(MockCallbacks())
        } catch (e: Exception) {
            exception = e
        }
        assertNull(exception);
    }


    class MockCallbacks: Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(p0: Activity) {}
        override fun onActivityResumed(p0: Activity) {}
        override fun onActivityStarted(p0: Activity) {}
        override fun onActivityDestroyed(p0: Activity) {}
        override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
        override fun onActivityStopped(p0: Activity) {}
        override fun onActivityCreated(p0: Activity, savedInstanceState: Bundle?) {}
    }
}