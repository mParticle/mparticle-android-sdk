package com.mparticle.internal.listeners

import android.content.pm.ApplicationInfo
import com.mparticle.SdkListener
import com.mparticle.internal.MPUtility
import com.mparticle.mock.MockContext
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito

class InternalListenerManagerTest {
    @Test
    fun testStartup() {
        val mockContext = DevStateMockContext()
        Mockito.mockStatic(MPUtility::class.java).use {
            Mockito.`when`(MPUtility.isDevEnv()).thenReturn(true)
            Mockito.`when`(MPUtility.getProp(Mockito.anyString())).thenReturn(mockContext.packageName)
            Assert.assertNotNull(InternalListenerManager.listener)
            Assert.assertEquals(InternalListenerManager.listener, InternalListener.EMPTY)
            Assert.assertFalse(InternalListenerManager.isEnabled)
            mockContext.isDebuggable = true
            val manager = InternalListenerManager.start(mockContext)
            Assert.assertNotNull(manager)

            // Manager is started, but should still be a brick until an SdkListener is added.
            Assert.assertNotNull(InternalListenerManager.listener)
            Assert.assertEquals(InternalListenerManager.listener, InternalListener.EMPTY)
            Assert.assertFalse(InternalListenerManager.isEnabled)
            val listener = SdkListener()
            manager?.addListener(listener)

            // Manager should now be active, since a listener was added.
            Assert.assertNotNull(InternalListenerManager.listener)
            Assert.assertNotEquals(InternalListenerManager.listener, InternalListener.EMPTY)
            Assert.assertTrue(InternalListenerManager.isEnabled)
            manager?.removeListener(listener)

            // Manager should go back to being a brick, since it's listener was removed.
            Assert.assertNotNull(InternalListenerManager.listener)
            Assert.assertEquals(InternalListenerManager.listener, InternalListener.EMPTY)
            Assert.assertFalse(InternalListenerManager.isEnabled)
        }
    }

    @Test
    fun testUnableToStart() {
        val context = DevStateMockContext()
        context.isDebuggable = false
        Mockito.mockStatic(MPUtility::class.java).use {
            Mockito.`when`(MPUtility.isDevEnv()).thenReturn(false)
            val manager = InternalListenerManager.start(context)

            // B rick instance of InternalListenerManager should act like a brick.
            Assert.assertNotNull(InternalListenerManager.listener)
            Assert.assertEquals(InternalListenerManager.listener, InternalListener.EMPTY)
            Assert.assertFalse(InternalListenerManager.isEnabled)
            Assert.assertNull(manager)
        }
    }

    @Test
    fun assertAppDebuggable() {
        val context = DevStateMockContext()
        context.isDebuggable = true
        Assert.assertTrue(MPUtility.isAppDebuggable(context))
        context.isDebuggable = false
        Assert.assertFalse(MPUtility.isAppDebuggable(context))
        context.isDebuggable = true
        Assert.assertTrue(MPUtility.isAppDebuggable(context))
    }

    internal inner class DevStateMockContext : MockContext() {
        var isDebuggable = false

        override fun getApplicationInfo(): ApplicationInfo {
            val applicationInfo = super.getApplicationInfo()
            if (isDebuggable) {
                applicationInfo.flags += ApplicationInfo.FLAG_DEBUGGABLE
            } else {
                applicationInfo.flags = 0
            }
            return applicationInfo
        }

        override fun getPackageName(): String = "test.package.name"
    }
}
