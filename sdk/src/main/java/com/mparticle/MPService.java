package com.mparticle;

import android.annotation.SuppressLint;
import android.app.IntentService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code IntentService } used internally by the SDK to process incoming broadcast messages in the background. Required for push notification functionality.
 * <p/>
 * This {@code IntentService} must be specified within the {@code <application>} block of your application's {@code AndroidManifest.xml} file:
 * <p/>
 * <pre>
 * {@code
 * <service android:name="com.mparticle.MPService" />}
 * </pre>
 */
@SuppressLint("Registered")
public class MPService extends IntentService {

    private static final String TAG = Constants.LOG_TAG;
    private static final String MPARTICLE_NOTIFICATION_OPENED = "com.mparticle.push.notification_opened";
    private static final Object LOCK = MPService.class;

    private static PowerManager.WakeLock sWakeLock;
    private static final String APP_STATE = "com.mparticle.push.appstate";
    private static final String PUSH_ORIGINAL_PAYLOAD = "com.mparticle.push.originalpayload";
    private static final String PUSH_REDACTED_PAYLOAD = "com.mparticle.push.redactedpayload";
    private static final String BROADCAST_PERMISSION = ".mparticle.permission.NOTIFICATIONS";

    public MPService() {
        super("com.mparticle.MPService");
    }

    static void runIntentInService(Context context, Intent intent) {
        synchronized (LOCK) {
            if (sWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            }
        }
        sWakeLock.acquire();
        intent.setClass(context, MPService.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MPService", "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public final void onHandleIntent(final Intent intent) {
        boolean release = true;
        try {

            MParticle.start(getApplicationContext());
            MParticle.getInstance().mEmbeddedKitManager.handleIntent(intent);

            String action = intent.getAction();
            Log.i("MPService", "Handling action: " + action);
            if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
                handleRegistration(intent);
            } else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
                release = false;
                (new AsyncTask<Intent, Void, Notification>() {
                    @Override
                    protected Notification doInBackground(Intent... params) {
                        return handleMessage(params[0]);
                    }

                    @Override
                    protected void onPostExecute(Notification notification) {
                        super.onPostExecute(notification);
                        if (notification != null) {
                            NotificationManager mNotifyMgr =
                                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            mNotifyMgr.notify(notification.hashCode(), notification);

                        }
                        synchronized (LOCK) {
                            if (sWakeLock != null && sWakeLock.isHeld()) {
                                sWakeLock.release();
                            }
                        }
                    }
                }).execute(intent);

            } else if (action.equals("com.google.android.c2dm.intent.UNREGISTER")) {
                intent.putExtra("unregistered", "true");
                handleRegistration(intent);
            } else if (action.equals(MPARTICLE_NOTIFICATION_OPENED)) {
                handleNotificationClick(intent);
            } else {
                handeReRegistration();
            }
        } finally {
            synchronized (LOCK) {
                if (release && sWakeLock != null && sWakeLock.isHeld()) {
                    sWakeLock.release();
                }
            }
        }
    }

    private void handeReRegistration() {
        MParticle.start(getApplicationContext());
        MParticle.getInstance();
    }

    private void handleNotificationClick(Intent intent) {

        broadcastNotificationClicked(intent.getBundleExtra(PUSH_ORIGINAL_PAYLOAD));

        try {
            MParticle.start(getApplicationContext());
            MParticle mMParticle = MParticle.getInstance();
            Bundle extras = intent.getExtras();
            String appState = extras.getString(APP_STATE);
            mMParticle.logNotification(intent.getExtras().getBundle(PUSH_REDACTED_PAYLOAD),
                                        appState);
        } catch (Throwable t) {

        }
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getPackageName());
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(launchIntent);
    }

    private void handleRegistration(Intent intent) {
        try {
            MParticle mMParticle = MParticle.getInstance();
            String registrationId = intent.getStringExtra("registration_id");
            String unregistered = intent.getStringExtra("unregistered");
            String error = intent.getStringExtra("error");

            if (registrationId != null) {
                // registration succeeded
                mMParticle.setPushRegistrationId(registrationId);
            } else if (unregistered != null) {
                // unregistration succeeded
                mMParticle.clearPushNotificationId();
            } else if (error != null) {
                // Unrecoverable error, log it
                Log.i(TAG, "GCM registration error: " + error);
            }
        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the
            // mparticle.properties file is not correct
            // and a warning message will already have been logged
            return;
        }
    }

    private Notification handleMessage(Intent intent) {


        try {

            Bundle newExtras = new Bundle();
            newExtras.putBundle(PUSH_ORIGINAL_PAYLOAD, intent.getExtras());
            newExtras.putBundle(PUSH_REDACTED_PAYLOAD, intent.getExtras());

            if (!MParticle.appRunning) {
                newExtras.putString(APP_STATE, AppStateManager.APP_STATE_NOTRUNNING);
            }

            MParticle mMParticle = MParticle.getInstance();
            if (!newExtras.containsKey(Constants.MessageKey.APP_STATE)) {
                if (mMParticle.mAppStateManager.isBackgrounded()) {
                    newExtras.putString(APP_STATE, AppStateManager.APP_STATE_BACKGROUND);
                } else {
                    newExtras.putString(APP_STATE, AppStateManager.APP_STATE_FOREGROUND);
                }
            }

            String fallbackTitle = null;
            int titleResId = MParticle.getInstance().mConfigManager.getPushTitle();
            if (titleResId > 0){
                try{
                    fallbackTitle = getString(titleResId);
                }catch(Resources.NotFoundException e){

                }
            }else{
                try {
                    int stringId = getApplicationInfo().labelRes;
                    fallbackTitle = getResources().getString(stringId);
                } catch (Resources.NotFoundException ex) {

                }
            }

            int smallIcon = MParticle.getInstance().mConfigManager.getPushIcon();
            try{
                Drawable draw = getResources().getDrawable(smallIcon);
            }catch (Resources.NotFoundException nfe){
                smallIcon = 0;
            }

            if (smallIcon == 0){
                try {
                    smallIcon = getPackageManager().getApplicationInfo(getPackageName(), 0).icon;
                } catch (NameNotFoundException e) {
                    // use the ic_dialog_alert icon if the app's can not be found
                }
                if (0 == smallIcon) {
                    smallIcon = android.R.drawable.ic_dialog_alert;
                }
            }


            if (intent.getExtras().containsKey("mp_data")){
                JSONObject mpData = new JSONObject(intent.getExtras().getString("mp_data"));
                NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
                int smallIconResId = 0;
                if (mpData.has("si") &&
                        (smallIconResId = this.getResources().getIdentifier(mpData.getString("si"), "drawable", this.getPackageName())) > 0){
                    notification.setSmallIcon(smallIconResId);
                }else{
                    notification.setSmallIcon(smallIcon);
                }

                notification.setContentTitle(mpData.optString("t",fallbackTitle));
                notification.setContentText(mpData.optString("m",fallbackTitle));
                newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD).putString(MParticlePushUtility.PUSH_ALERT_EXTRA, mpData.optString("m",fallbackTitle));

                if (mpData.has("sm")){
                    notification.setSubText(mpData.getString("sm"));
                }
                /*
                  waiting for v21 of support library to be released.
                if (mpData.containsKey("g")){
                   notification.setGroup(mpData.getString("g));
                }
                */
                if (mpData.has("li")){
                    String largeIcon = mpData.getString("li");
                    if (largeIcon.contains("http")){
                        try {
                            InputStream in = new java.net.URL(largeIcon).openStream();
                            notification.setLargeIcon(BitmapFactory.decodeStream(in));
                        } catch (Exception e) {


                        }
                    }else{
                        int drawableResourceId = this.getResources().getIdentifier(largeIcon, "drawable", this.getPackageName());
                        if (drawableResourceId > 0) {
                            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                                    drawableResourceId);
                            notification.setLargeIcon(icon);
                        }
                    }
                }
                if (mpData.has("l")){
                    JSONObject lightData = mpData.getJSONObject("l");
                    int argbColor = lightData.getInt("c");
                    int off = lightData.getInt("off");
                    int on = lightData.getInt("on");
                    notification.setLights(argbColor, on, off);
                }
                if (mpData.has("n")){
                    notification.setNumber(mpData.getInt("n"));
                }
                if (mpData.has("ao")){
                    notification.setOnlyAlertOnce(mpData.getBoolean("ao"));
                }
                if (mpData.has("p")){
                    notification.setPriority(mpData.getInt("p"));
                }
                if (mpData.has("s")){
                    try {
                        notification.setSound(Uri.parse("android.resource://" + getPackageName() + "/raw/" + mpData.getString("s")));
                    }catch (Exception e){

                    }
                }
                if (mpData.has("bi")){
                    try {
                        InputStream in = new java.net.URL(mpData.getString("bi")).openStream();
                        NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
                        style.bigPicture(BitmapFactory.decodeStream(in));
                        if (mpData.has("xt")){
                            style.setBigContentTitle(mpData.getString("xt"));
                        }
                        notification.setStyle(style);
                    } catch (Exception e) {


                    }
                }else if (mpData.has("bt")){
                    NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle().bigText(mpData.getString("bt"));
                    if (mpData.has("xt")){
                        style.setBigContentTitle(mpData.getString("xt"));
                    }
                    notification.setStyle(style);
                }else if (mpData.has("ib")){
                    NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                    JSONArray lines = mpData.getJSONArray("ib");
                    for (int i = 0; i < lines.length(); i++){
                        style.addLine(lines.getString(i));
                    }
                    if (mpData.has("xt")){
                        style.setBigContentTitle(mpData.getString("xt"));
                    }
                    notification.setStyle(style);
                }
                if (mpData.has("v")){
                    JSONArray pattern = mpData.getJSONArray("v");
                    List<Long> list = new ArrayList<Long>();
                    for (int i=0; i<pattern.length(); i++) {
                        list.add( pattern.getLong(i) );
                    }
                    Long[] vArray = new Long[list.size()];
                    vArray = list.toArray(vArray);
                    long[] primitives = new long[vArray.length];
                    for (int i = 0; i < vArray.length; i++)
                        primitives[i] = vArray[i];

                    notification.setVibrate(primitives);
                }
                if (mpData.has("a1")){
                    addAction(notification, mpData.getJSONObject("a1"));
                }
                if (mpData.has("a2")){
                    addAction(notification, mpData.getJSONObject("a2"));
                }
                if (mpData.has("a3")){
                    addAction(notification, mpData.getJSONObject("a3"));
                }
                Intent launchIntent = new Intent(getApplicationContext(), MPService.class);
                launchIntent.setAction(MPARTICLE_NOTIFICATION_OPENED);

                launchIntent.putExtras(newExtras);
                PendingIntent notifyIntent = PendingIntent.getService(getApplicationContext(), 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                notification.setContentIntent(notifyIntent);
                notification.setAutoCancel(true);
                broadcastNotificationReceived(newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD));
                return notification.build();

            }else{
                String[] possibleKeys = mMParticle.mConfigManager.getPushKeys();
                String key = findMessageKey(possibleKeys, newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD));
                String message = newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD).getString(key);
                newExtras.getBundle(PUSH_REDACTED_PAYLOAD).putString(key, "");
                newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD).putString(MParticlePushUtility.PUSH_ALERT_EXTRA, message);
                broadcastNotificationReceived(newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD));
                return showBasicPush(smallIcon, fallbackTitle, message, newExtras);
            }


        } catch (Throwable t) {
            // failure to instantiate mParticle likely means that the
            // mparticle.properties file is not correct
            // and a warning message will already have been logged
            return null;
        }
    }

    private void addAction(NotificationCompat.Builder notification, JSONObject actionData) {
        try {
            int drawableResourceId = this.getResources().getIdentifier(actionData.getString("ai"), "drawable", this.getPackageName());
            notification.addAction(drawableResourceId, actionData.getString("at"), null);
        }catch (JSONException e){}
    }

    private void broadcastNotificationReceived(Bundle originalPayload) {
        Intent intent = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_RECEIVED);
        intent.putExtras(originalPayload);
        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);
    }

    private void broadcastNotificationClicked(Bundle originalPayload) {
        Intent intent = new Intent(MParticlePushUtility.BROADCAST_NOTIFICATION_TAPPED);
        intent.putExtras(originalPayload);
        String packageName = getPackageName();
        sendBroadcast(intent, packageName + BROADCAST_PERMISSION);
    }

    private String findMessageKey(String[] possibleKeys, Bundle extras){
        if (possibleKeys != null) {
            for (String key : possibleKeys) {
                String message = extras.getString(key);
                if (message != null && message.length() > 0) {
                    return key;
                }
            }
        }
        Log.w(Constants.LOG_TAG, "Failed to extract push alert message using configuration.");
        return null;
    }

    private Notification showBasicPush(int iconId, String title, String message, Bundle newExtras) {
        MParticle.start(getApplicationContext());
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        MParticle mMParticle = MParticle.getInstance();
        Intent launchIntent = new Intent(getApplicationContext(), MPService.class);
        launchIntent.setAction(MPARTICLE_NOTIFICATION_OPENED);
        launchIntent.putExtras(newExtras);
        PendingIntent notifyIntent = PendingIntent.getService(getApplicationContext(), 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setContentIntent(notifyIntent)
                .setSmallIcon(iconId).setTicker(message).setContentTitle(title).setContentText(message).build();

        if (mMParticle.mConfigManager.isPushSoundEnabled()) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }
        if (mMParticle.mConfigManager.isPushVibrationEnabled()) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        return notification;
    }

}