package com.mparticle;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * {@code IntentService } used internally by the SDK to process incoming broadcast messages in the background. Required for push notification functionality.
 * <p></p>
 * This {@code IntentService} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 * <p></p>
 * <pre>
 * {@code
 * <service android:exported="false" android:name="com.mparticle.MPService" />}
 * </pre>
 * <p></p>
 * The SDK starts this service only with explicit intents from your own application, so do not export it.
 * An exported service with no permission lets any other app on the device send it push intents.
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
    public final void onHandleIntent(@NonNull final Intent intent) {
        new MPServiceUtil(this).onHandleIntent(intent);
    }

}