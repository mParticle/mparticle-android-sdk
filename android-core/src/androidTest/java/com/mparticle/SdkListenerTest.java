package com.mparticle;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mparticle.commerce.Cart;
import com.mparticle.commerce.CommerceApi;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.consent.ConsentState;
import com.mparticle.identity.IdentityApi;
import com.mparticle.identity.IdentityApiRequest;
import com.mparticle.identity.MParticleUserImpl;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.listeners.ApiClass;
import com.mparticle.testutils.AndroidUtils;
import com.mparticle.testutils.AndroidUtils.Mutable;
import com.mparticle.testutils.BaseAbstractTest;
import com.mparticle.testutils.BaseCleanStartedEachTest;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import dalvik.system.DexFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        Method method = mParticle.getClass().getMethod("logEvent", MPEvent.class);
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
