package com.mparticle;

import android.support.annotation.NonNull;

import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SdkListenerTest extends BaseCleanStartedEachTest {

    @Test
    public void testOnApiCalled() throws InterruptedException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final CountDownLatch latch = new MPLatch(1);

        final Mutable<String> apiNameResult = new Mutable<String>(null);
        final Mutable<List<Object>> objectsResult = new Mutable<List<Object>>(null);
        final Mutable<Boolean> isExternalResult = new Mutable<Boolean>(null);

        SdkListener sdkListener = new SdkListener() {
            @Override
            public void onApiCalled(@NonNull String apiName, @NonNull List<Object> objects, boolean isExternal) {
                if (apiNameResult.value == null) {
                    apiNameResult.value = apiName;
                    objectsResult.value = objects;
                    isExternalResult.value = isExternal;
                    latch.countDown();
                }
            }
        };

        MParticle.addListener(mContext, sdkListener);

        MPEvent mpEvent = TestingUtils.getInstance().getRandomMPEventRich();

        //invoke an API method from an external package ;)
        MParticle mParticle = MParticle.getInstance();
        Method method = mParticle.getClass().getMethod("logEvent", BaseEvent.class);
        try {
            method.invoke(mParticle, mpEvent);
        } catch (Exception ignore) {
            //probably going to crash, nbd
        }

        latch.await();

        assertEquals("MParticle.logEvent()", apiNameResult.value);
        assertEquals(1, objectsResult.value.size());
        assertEquals(mpEvent, objectsResult.value.get(0));
        assertTrue(isExternalResult.value);

        apiNameResult.value = null;

        try {
            MParticle.getInstance().upload();
        }
        catch (Exception ignore) {
            //probably going to crash, nbd
        }

        assertEquals("MParticle.upload()", apiNameResult.value);
        assertEquals(0, objectsResult.value.size());
        assertFalse(isExternalResult.value);

        apiNameResult.value = null;
    }


}
