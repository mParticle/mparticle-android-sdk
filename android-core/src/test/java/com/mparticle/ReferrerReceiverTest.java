package com.mparticle;

import android.content.Intent;

import com.mparticle.internal.Constants;
import com.mparticle.mock.MockContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;


public class ReferrerReceiverTest {

    @Before
    public void setup() {
        MParticle.setInstance(new MockMParticle());
    }

    @Test
    public void testOnReceive() throws Exception {
        MockContext context = new MockContext();
        ReferrerReceiver receiver = new ReferrerReceiver();
        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getAction()).thenReturn("com.android.vending.INSTALL_REFERRER");
        Mockito.when(intent.getStringExtra("referrer")).thenReturn("test referrer");
        receiver.onReceive(context, intent);
        assertEquals("test referrer", context.getSharedPreferences(null, 0).getString(Constants.PrefKeys.INSTALL_REFERRER, null));
        Intent intent2 = Mockito.mock(Intent.class);
        Mockito.when(intent2.getAction()).thenReturn("not the right action");
        Mockito.when(intent2.getStringExtra("referrer")).thenReturn("test referrer 2");
        receiver.onReceive(context, intent2);
        assertEquals("test referrer", context.getSharedPreferences(null, 0).getString(Constants.PrefKeys.INSTALL_REFERRER, null));
        receiver.onReceive(null, null);
        receiver.onReceive(context, null);
        receiver.onReceive(null, intent);
    }

    @Test
    public void testSetInstallReferrer() throws Exception {
        MockContext context = new MockContext();
        ReferrerReceiver receiver = new ReferrerReceiver();
        Intent intent = Mockito.mock(Intent.class);
        Mockito.when(intent.getAction()).thenReturn("com.android.vending.INSTALL_REFERRER");
        Mockito.when(intent.getStringExtra("referrer")).thenReturn("test referrer");
        receiver.setInstallReferrer(context, intent);
        assertEquals("test referrer", context.getSharedPreferences(null, 0).getString(Constants.PrefKeys.INSTALL_REFERRER, null));
        Intent intent2 = Mockito.mock(Intent.class);
        Mockito.when(intent2.getAction()).thenReturn("not the right action");
        Mockito.when(intent2.getStringExtra("referrer")).thenReturn("test referrer 2");
        receiver.setInstallReferrer(context, intent2);
        assertEquals("test referrer", context.getSharedPreferences(null, 0).getString(Constants.PrefKeys.INSTALL_REFERRER, null));
        receiver.setInstallReferrer(null, "");
        receiver.setInstallReferrer(context, "");
        receiver.setInstallReferrer(null, intent);
    }
}