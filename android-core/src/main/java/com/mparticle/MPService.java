package com.mparticle;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.mparticle.internal.ConfigManager;
import com.mparticle.internal.Constants;
import com.mparticle.internal.KitFrameworkWrapper;
import com.mparticle.internal.KitsLoadedListener;
import com.mparticle.internal.Logger;
import com.mparticle.messaging.MPMessagingAPI;
import com.mparticle.messaging.ProviderCloudMessage;

import java.util.List;

/**
 * {@code IntentService } used internally by the SDK to process incoming broadcast messages in the background. Required for push notification functionality.
 * <p></p>
 * This {@code IntentService} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 * <p></p>
 * <pre>
 * {@code
 * <service android:name="com.mparticle.MPService" />}
 * </pre>
 */
@SuppressLint("Registered")
public class MPService extends IntentService {

    public MPService() {
        super("com.mparticle.MPService");
    }



    /**
     *
     */
    @Override
    public final void onHandleIntent(final Intent intent) {
        new MPServiceUtil(this).onHandleIntent(intent);
    }

}