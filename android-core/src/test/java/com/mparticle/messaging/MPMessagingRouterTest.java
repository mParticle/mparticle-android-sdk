package com.mparticle.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;

import com.mparticle.MPServiceUtil;
import com.mparticle.MParticle;
import com.mparticle.internal.MessageManager;
import com.mparticle.mock.MockContext;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MPMessagingRouterTest {

    private static void stubStringExtras(Intent intent) {
        final Map<String, String> stringExtras = new HashMap<>();
        Mockito.when(intent.putExtra(Mockito.anyString(), Mockito.anyString())).thenAnswer(invocation -> {
            stringExtras.put(invocation.getArgument(0), invocation.getArgument(1));
            return intent;
        });
        Mockito.when(intent.getStringExtra(Mockito.anyString())).thenAnswer(invocation ->
                stringExtras.get(invocation.getArgument(0)));
    }

    @Test
    public void testRejectsUnauthenticatedForeignBroadcasts() {
        Context context = new MockContext();
        ProviderCloudMessage cloudMessage = new ProviderCloudMessage((Bundle) null, null);
        PushAnalyticsReceiverCallback callback = Mockito.mock(PushAnalyticsReceiverCallback.class);

        try (MockedStatic<MPServiceUtil> serviceUtilMock = Mockito.mockStatic(MPServiceUtil.class)) {
            Intent foreignReceiveIntent = Mockito.mock(Intent.class);
            Mockito.when(foreignReceiveIntent.getAction()).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED);
            Mockito.when(foreignReceiveIntent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA)).thenReturn(cloudMessage);

            assertFalse(MPMessagingRouter.onReceive(context, foreignReceiveIntent, callback));

            Intent forgedTokenTapIntent = Mockito.mock(Intent.class);
            Mockito.when(forgedTokenTapIntent.getAction()).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED);
            Mockito.when(forgedTokenTapIntent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA)).thenReturn(cloudMessage);
            Mockito.when(forgedTokenTapIntent.getStringExtra(MPMessagingRouter.INTERNAL_BROADCAST_AUTH_EXTRA))
                    .thenReturn("forged-external-token");

            assertFalse(MPMessagingRouter.onReceive(context, forgedTokenTapIntent, callback));

            final AtomicBoolean receiverInvoked = new AtomicBoolean(false);
            PushAnalyticsReceiver receiver = new PushAnalyticsReceiver() {
                @Override
                public boolean onNotificationReceived(ProviderCloudMessage message) {
                    receiverInvoked.set(true);
                    return false;
                }

                @Override
                public boolean onNotificationTapped(ProviderCloudMessage message) {
                    receiverInvoked.set(true);
                    return false;
                }
            };
            receiver.onReceive(context, foreignReceiveIntent);
            receiver.onReceive(context, forgedTokenTapIntent);
            assertFalse(receiverInvoked.get());

            Mockito.verifyNoInteractions(callback);
            serviceUtilMock.verifyNoInteractions();
        }
    }

    @Test
    public void testSafelyHandlesNonProviderCloudMessageAndMalformedParcelableExtra() {
        Context context = new MockContext();
        PushAnalyticsReceiverCallback callback = Mockito.mock(PushAnalyticsReceiverCallback.class);

        try (MockedStatic<MPServiceUtil> serviceUtilMock = Mockito.mockStatic(MPServiceUtil.class)) {
            Parcelable wrongTypeParcelable = Mockito.mock(Parcelable.class);
            Intent wrongTypeIntent = Mockito.mock(Intent.class);
            Mockito.when(wrongTypeIntent.getAction()).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED);
            Mockito.doReturn(wrongTypeParcelable).when(wrongTypeIntent).getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
            stubStringExtras(wrongTypeIntent);
            MPMessagingRouter.markInternalBroadcast(wrongTypeIntent);

            assertNull(MPMessagingRouter.extractCloudMessage(wrongTypeIntent));
            assertFalse(MPMessagingRouter.onReceive(context, wrongTypeIntent, callback));

            Intent classCastIntent = Mockito.mock(Intent.class);
            Mockito.when(classCastIntent.getAction()).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED);
            Mockito.when(classCastIntent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                    .thenThrow(new ClassCastException("Wrong parcelable type"));
            stubStringExtras(classCastIntent);
            MPMessagingRouter.markInternalBroadcast(classCastIntent);

            assertNull(MPMessagingRouter.extractCloudMessage(classCastIntent));
            assertFalse(MPMessagingRouter.onReceive(context, classCastIntent, callback));

            BadParcelableException badParcelable = Mockito.mock(BadParcelableException.class);
            Intent badParcelableIntent = Mockito.mock(Intent.class);
            Mockito.when(badParcelableIntent.getAction()).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED);
            Mockito.when(badParcelableIntent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                    .thenThrow(badParcelable);
            stubStringExtras(badParcelableIntent);
            MPMessagingRouter.markInternalBroadcast(badParcelableIntent);

            assertNull(MPMessagingRouter.extractCloudMessage(badParcelableIntent));
            assertFalse(MPMessagingRouter.onReceive(context, badParcelableIntent, callback));

            Intent badUnparcelAuthIntent = Mockito.mock(Intent.class);
            Mockito.when(badUnparcelAuthIntent.getAction()).thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED);
            Mockito.when(badUnparcelAuthIntent.getStringExtra(MPMessagingRouter.INTERNAL_BROADCAST_AUTH_EXTRA))
                    .thenThrow(badParcelable);

            assertFalse(MPMessagingRouter.isInternalBroadcast(badUnparcelAuthIntent));
            assertFalse(MPMessagingRouter.onReceive(context, badUnparcelAuthIntent, callback));

            Mockito.verifyNoInteractions(callback);
            serviceUtilMock.verifyNoInteractions();
        }
    }

    @Test
    public void testAcceptsGenuineInternalBroadcastsFromMPServiceUtil() throws Exception {
        Context context = new MockContext();
        final ProviderCloudMessage cloudMessage = new ProviderCloudMessage((Bundle) null, null);

        try (MockedConstruction<Intent> intentConstruction = Mockito.mockConstruction(Intent.class, (mock, ctx) -> {
            String action = (String) ctx.arguments().get(0);
            Mockito.when(mock.getAction()).thenReturn(action);
            Mockito.when(mock.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA)).thenReturn(cloudMessage);
            stubStringExtras(mock);
        })) {
            Intent receivedIntent = MPServiceUtil.createInternalPushBroadcast(
                    MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED,
                    cloudMessage
            );
            Intent tappedIntent = MPServiceUtil.createInternalPushBroadcast(
                    MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED,
                    cloudMessage
            );

            assertNotNull(receivedIntent.getStringExtra(MPMessagingRouter.INTERNAL_BROADCAST_AUTH_EXTRA));
            assertEquals(
                    receivedIntent.getStringExtra(MPMessagingRouter.INTERNAL_BROADCAST_AUTH_EXTRA),
                    tappedIntent.getStringExtra(MPMessagingRouter.INTERNAL_BROADCAST_AUTH_EXTRA)
            );
            assertTrue(MPMessagingRouter.isInternalBroadcast(receivedIntent));
            assertTrue(MPMessagingRouter.isInternalBroadcast(tappedIntent));

            try (MockedStatic<MPServiceUtil> serviceUtilMock = Mockito.mockStatic(MPServiceUtil.class)) {
                final AtomicReference<ProviderCloudMessage> receivedRef = new AtomicReference<>();
                final AtomicReference<ProviderCloudMessage> tappedRef = new AtomicReference<>();

                PushAnalyticsReceiverCallback forwardingCallback = new PushAnalyticsReceiverCallback() {
                    @Override
                    public boolean onNotificationReceived(ProviderCloudMessage message) {
                        receivedRef.set(message);
                        return false;
                    }

                    @Override
                    public boolean onNotificationTapped(ProviderCloudMessage message) {
                        tappedRef.set(message);
                        return true;
                    }
                };

                assertTrue(MPMessagingRouter.onReceive(context, receivedIntent, forwardingCallback));
                assertSame(cloudMessage, receivedRef.get());
                serviceUtilMock.verify(() -> MPServiceUtil.runIntentInService(context, receivedIntent));

                assertTrue(MPMessagingRouter.onReceive(context, tappedIntent, forwardingCallback));
                assertSame(cloudMessage, tappedRef.get());
                serviceUtilMock.verifyNoMoreInteractions();
            }

            try (MockedStatic<Looper> looperMock = Mockito.mockStatic(Looper.class);
                 MockedConstruction<Handler> handlerMock = Mockito.mockConstruction(Handler.class);
                 MockedStatic<PendingIntent> pendingIntentMock = Mockito.mockStatic(PendingIntent.class);
                 MockedStatic<MParticle> mparticleMock = Mockito.mockStatic(MParticle.class)) {
                looperMock.when(Looper::getMainLooper).thenReturn(Mockito.mock(Looper.class));
                MPServiceUtil serviceUtil = new MPServiceUtil(context);

                ProviderCloudMessage receiveServiceMessage = Mockito.mock(ProviderCloudMessage.class);
                Mockito.when(receiveServiceMessage.getDisplayed()).thenReturn(true);
                Intent unauthReceiveServiceIntent = Mockito.mock(Intent.class);
                Mockito.when(unauthReceiveServiceIntent.getAction())
                        .thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_RECEIVED);
                Mockito.when(unauthReceiveServiceIntent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                        .thenReturn(receiveServiceMessage);

                serviceUtil.onHandleIntent(unauthReceiveServiceIntent);
                Mockito.verifyNoInteractions(receiveServiceMessage);

                Mockito.doReturn(receiveServiceMessage)
                        .when(receivedIntent)
                        .getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
                serviceUtil.onHandleIntent(receivedIntent);
                Mockito.verify(receiveServiceMessage).getDisplayed();

                ProviderCloudMessage tapServiceMessage = Mockito.mock(ProviderCloudMessage.class);
                Intent openIntent = Mockito.mock(Intent.class);
                PendingIntent pendingIntent = Mockito.mock(PendingIntent.class);
                Mockito.when(tapServiceMessage.getDefaultOpenIntent(context, tapServiceMessage))
                        .thenReturn(openIntent);
                pendingIntentMock.when(() -> PendingIntent.getActivity(
                        Mockito.eq(context),
                        Mockito.eq(0),
                        Mockito.eq(openIntent),
                        Mockito.anyInt()
                )).thenReturn(pendingIntent);

                Intent unauthTapServiceIntent = Mockito.mock(Intent.class);
                Mockito.when(unauthTapServiceIntent.getAction())
                        .thenReturn(MPMessagingAPI.BROADCAST_NOTIFICATION_TAPPED);
                Mockito.when(unauthTapServiceIntent.getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA))
                        .thenReturn(tapServiceMessage);

                serviceUtil.onHandleIntent(unauthTapServiceIntent);
                Mockito.verifyNoInteractions(tapServiceMessage);
                Mockito.verify(pendingIntent, Mockito.never()).send();

                Mockito.doReturn(tapServiceMessage)
                        .when(tappedIntent)
                        .getParcelableExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA);
                serviceUtil.onHandleIntent(tappedIntent);
                Mockito.verify(tapServiceMessage).getDefaultOpenIntent(context, tapServiceMessage);
                Mockito.verify(pendingIntent).send();
            }
        }
    }

    @Test
    public void testHandlesNullBundleInProviderCloudMessageAndMessageManager() {
        JSONArray pushKeys = new JSONArray().put("alert");
        ProviderCloudMessage nullBundleMessage = new ProviderCloudMessage((Bundle) null, pushKeys);
        assertNull(nullBundleMessage.getExtras());
        JSONObject redactedFromConstructor = nullBundleMessage.getRedactedJsonPayload();
        assertNotNull(redactedFromConstructor);
        assertEquals("{}", redactedFromConstructor.toString());
        assertEquals(0, nullBundleMessage.getId());

        Parcel parcel = Mockito.mock(Parcel.class);
        Mockito.when(parcel.readBundle()).thenReturn(null);
        Mockito.when(parcel.readString()).thenReturn(null);
        ProviderCloudMessage parcelMessage = new ProviderCloudMessage(parcel);
        assertNull(parcelMessage.getExtras());
        assertEquals("{}", parcelMessage.getRedactedJsonPayload().toString());
        assertEquals(0, parcelMessage.getId());

        try (MockedStatic<Looper> looperMock = Mockito.mockStatic(Looper.class);
             MockedConstruction<HandlerThread> handlerThreadMock = Mockito.mockConstruction(HandlerThread.class)) {
            looperMock.when(Looper::getMainLooper).thenReturn(Mockito.mock(Looper.class));
            MessageManager messageManager = Mockito.mock(MessageManager.class, Mockito.CALLS_REAL_METHODS);

            assertEquals("{}", messageManager.getRedactedJsonPayload(nullBundleMessage));
            assertEquals("{}", messageManager.getRedactedJsonPayload(parcelMessage));
            assertEquals("{}", messageManager.getRedactedJsonPayload(null));
            messageManager.logNotification(null, "foreground");
        }
    }
}
