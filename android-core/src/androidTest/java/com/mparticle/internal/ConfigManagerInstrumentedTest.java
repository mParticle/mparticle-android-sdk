package com.mparticle.internal;

import android.os.Looper;
import android.support.test.InstrumentationRegistry;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class ConfigManagerInstrumentedTest {

    @BeforeClass
    public static void setup() {
        Looper.prepare();
    }

    @Test
    public void testUserConfigTimedCach() throws InterruptedException {
        //Test normal behavior, newly instantiate UserConfigHolder drops non-current instances when appropriatly stale
        ConfigManager.UserConfigHolder userConfigHolder = new ConfigManager.UserConfigHolder(InstrumentationRegistry.getContext(), 1);
        testTimedCacheDropping(userConfigHolder, 1L);

        //test it works after changing current MPID
        userConfigHolder.setCurrent(23);
        testTimedCacheDropping(userConfigHolder, 23);

    }

    private void testTimedCacheDropping(ConfigManager.UserConfigHolder userConfigHolder, long currentMpId) throws InterruptedException {
        UserConfig currentUserConfig = userConfigHolder.getCurrentInstance();

        assertTrue(userConfigHolder.isCached(currentMpId));
        assertEquals(currentUserConfig.getMpid(), currentMpId);

        UserConfig otherUserConfig = userConfigHolder.getInstance(2L);
        UserConfig other2UserConfig = userConfigHolder.getInstance(3L);

        assertTrue(userConfigHolder.isCached(2L));
        assertEquals(otherUserConfig.getMpid(), 2L);
        assertTrue(userConfigHolder.isCached(3L));
        assertEquals(other2UserConfig.getMpid(), 3L);

        CountDownLatch latch = new CountDownLatch(1);
        latch.await(1, TimeUnit.SECONDS);
        userConfigHolder.getInstance(2L);
        latch.await(1500, TimeUnit.MILLISECONDS);
        userConfigHolder.getInstance(2L);
        latch.await(1, TimeUnit.SECONDS);

        assertTrue(userConfigHolder.isCached(currentMpId));
        assertTrue(userConfigHolder.isCached(2L));
        assertFalse(userConfigHolder.isCached(3L));

        latch.await(3, TimeUnit.SECONDS);


        assertTrue(userConfigHolder.isCached(currentMpId));
        assertFalse(userConfigHolder.isCached(2L));
        assertFalse(userConfigHolder.isCached(3L));


    }
}
