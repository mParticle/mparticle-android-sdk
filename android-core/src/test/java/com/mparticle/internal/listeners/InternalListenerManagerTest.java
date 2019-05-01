package com.mparticle.internal.listeners;

import android.content.pm.ApplicationInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mparticle.SdkListener;
import com.mparticle.identity.AliasResponse;
import com.mparticle.internal.InternalSession;
import com.mparticle.internal.MPUtility;
import com.mparticle.mock.MockContext;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
public class InternalListenerManagerTest {

    @Test
    @PrepareForTest({MPUtility.class})
    public void testStartup() {
        DevStateMockContext mockContext = new DevStateMockContext();

        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.isDevEnv()).thenReturn(true);
        Mockito.when(MPUtility.getProp(Mockito.anyString())).thenReturn(mockContext.getPackageName());


        assertNotNull(InternalListenerManager.getListener());
        assertEquals(InternalListenerManager.getListener(), InternalListener.EMPTY);
        assertFalse(InternalListenerManager.isEnabled());

        mockContext.setDebuggable(true);
        InternalListenerManager manager = InternalListenerManager.start(mockContext);
        assertNotNull(manager);

        //manager is started, but should still be a brick until an SdkListener is added
        assertNotNull(InternalListenerManager.getListener());
        assertEquals(InternalListenerManager.getListener(), InternalListener.EMPTY);
        assertFalse(InternalListenerManager.isEnabled());

        SdkListener listener = new SdkListener();
        manager.addListener(listener);

        //manager should now be active, since a listener was added
        assertNotNull(InternalListenerManager.getListener());
        assertNotEquals(InternalListenerManager.getListener(), InternalListener.EMPTY);
        assertTrue(InternalListenerManager.isEnabled());

        manager.removeListener(listener);

        //manager should go back to being a brick, since it's listener was removed
        assertNotNull(InternalListenerManager.getListener());
        assertEquals(InternalListenerManager.getListener(), InternalListener.EMPTY);
        assertFalse(InternalListenerManager.isEnabled());
    }

    @Test
    @PrepareForTest({MPUtility.class})
    public void testUnableToStart() {
        DevStateMockContext context = new DevStateMockContext();
        context.setDebuggable(false);

        PowerMockito.mockStatic(MPUtility.class);
        Mockito.when(MPUtility.isDevEnv()).thenReturn(false);

        InternalListenerManager manager = InternalListenerManager.start(context);

        //brick instance of InternalListenerManager should act like a brick
        assertNotNull(InternalListenerManager.getListener());
        assertEquals(InternalListenerManager.getListener(), InternalListener.EMPTY);
        assertFalse(InternalListenerManager.isEnabled());

        assertNull(manager);
    }


    @Test
    public void assertAppDebuggable() {
        DevStateMockContext context = new DevStateMockContext();
        context.setDebuggable(true);
        assertTrue(MPUtility.isAppDebuggable(context));
        context.setDebuggable(false);
        assertFalse(MPUtility.isAppDebuggable(context));
        context.setDebuggable(true);
        assertTrue(MPUtility.isAppDebuggable(context));
    }



    class DevStateMockContext extends MockContext {
        boolean isDebuggable;

        public void setDebuggable(boolean isDebuggable) {
            this.isDebuggable = isDebuggable;
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            ApplicationInfo applicationInfo = super.getApplicationInfo();
            if (isDebuggable) {
                applicationInfo.flags += ApplicationInfo.FLAG_DEBUGGABLE;
            } else {
                applicationInfo.flags = 0;
            }
            return applicationInfo;
        }

        @Override
        public String getPackageName() {
            return "test.package.name";
        }
    }
}



