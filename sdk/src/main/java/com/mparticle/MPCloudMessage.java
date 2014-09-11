package com.mparticle;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sdozor on 9/11/14.
 */
public class MPCloudMessage {
    private final static String ROOT_KEY = "mp_data";
    private final static String SMALL_ICON = "si";
    private final static String TITLE = "t";
    private final static String PRIMARY_MESSAGE = "m";
    private final static String SECONDARY_MESSAGE = "sm";
    private final static String LARGE_ICON = "li";
    private final static String LIGHTS_ROOT = "l";
    private final static String LIGHTS_COLOR = "c";
    private final static String LIGHTS_OFF_MILLIS = "off";
    private final static String LIGHTS_ON_MILLIS = "on";
    private final static String NUMBER = "n";
    private final static String ALERT_ONCE = "ao";
    private final static String PRIORITY = "p";
    private final static String SOUND = "s";
    private final static String BIG_IMAGE = "bi";
    private final static String TITLE_EXPANDED = "xt";
    private final static String INBOX_STYLE_ROOT = "ib";
    private final static String BIG_TEXT = "bt";
    private final static String VIBRATION_PATTERN = "v";
    private final static String ACTION_1 = "a1";
    private final static String ACTION_2 = "a2";
    private final static String ACTION_3 = "a3";
    private final static String ACTION_ICON = "ai";
    private final static String ACTION_TITLE = "at";

    static String getFallbackTitle(Context context){
        String fallbackTitle = null;
        int titleResId = MParticle.getInstance().mConfigManager.getPushTitle();
        if (titleResId > 0){
            try{
                fallbackTitle = context.getString(titleResId);
            }catch(Resources.NotFoundException e){

            }
        }else{
            try {
                int stringId = context.getApplicationInfo().labelRes;
                fallbackTitle = context.getResources().getString(stringId);
            } catch (Resources.NotFoundException ex) {

            }
        }
        return fallbackTitle;
    }

    static int getFallbackIcon(Context context){
        int smallIcon = MParticle.getInstance().mConfigManager.getPushIcon();
        try{
            Drawable draw = context.getResources().getDrawable(smallIcon);
        }catch (Resources.NotFoundException nfe){
            smallIcon = 0;
        }

        if (smallIcon == 0){
            try {
                smallIcon = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).icon;
            } catch (PackageManager.NameNotFoundException e) {
                // use the ic_dialog_alert icon if the app's can not be found
            }
            if (0 == smallIcon) {
                smallIcon = android.R.drawable.ic_dialog_alert;
            }
        }
        return smallIcon;
    }

    static boolean isValidMPMessage(Bundle extras){
        return extras != null && extras.containsKey(ROOT_KEY);
    }

    static Notification buildNotification(Context context, Bundle extras) throws JSONException {
        JSONObject mpData = new JSONObject(extras.getString(ROOT_KEY));
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        int smallIconResId = 0;
        if (mpData.has(SMALL_ICON) &&
                (smallIconResId = context.getResources().getIdentifier(mpData.getString(SMALL_ICON), "drawable", context.getPackageName())) > 0) {
            notification.setSmallIcon(smallIconResId);
        } else {
            notification.setSmallIcon(MPCloudMessage.getFallbackIcon(context));
        }
        String fallbackTitle = MPCloudMessage.getFallbackTitle(context);
        notification.setContentTitle(mpData.optString(TITLE, fallbackTitle));
        notification.setContentText(mpData.optString(PRIMARY_MESSAGE, fallbackTitle));

        if (mpData.has(SECONDARY_MESSAGE)) {
            notification.setSubText(mpData.getString(SECONDARY_MESSAGE));
        }
            /*
              waiting for v21 of support library to be released.
            if (mpData.containsKey("g")){
               notification.setGroup(mpData.getString("g));
            }
            */
        if (mpData.has(LARGE_ICON)) {
            String largeIcon = mpData.getString(LARGE_ICON);
            if (largeIcon.contains("http")) {
                try {
                    InputStream in = new java.net.URL(largeIcon).openStream();
                    notification.setLargeIcon(BitmapFactory.decodeStream(in));
                } catch (Exception e) {


                }
            } else {
                int drawableResourceId = context.getResources().getIdentifier(largeIcon, "drawable", context.getPackageName());
                if (drawableResourceId > 0) {
                    Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                            drawableResourceId);
                    notification.setLargeIcon(icon);
                }
            }
        }
        if (mpData.has(LIGHTS_ROOT)) {
            JSONObject lightData = mpData.getJSONObject(LIGHTS_ROOT);
            int argbColor = lightData.getInt(LIGHTS_COLOR);
            int off = lightData.getInt(LIGHTS_OFF_MILLIS);
            int on = lightData.getInt(LIGHTS_ON_MILLIS);
            notification.setLights(argbColor, on, off);
        }
        if (mpData.has(NUMBER)) {
            notification.setNumber(mpData.getInt(NUMBER));
        }
        if (mpData.has(ALERT_ONCE)) {
            notification.setOnlyAlertOnce(mpData.getBoolean(ALERT_ONCE));
        }
        if (mpData.has(PRIORITY)) {
            notification.setPriority(mpData.getInt(PRIORITY));
        }
        if (mpData.has(SOUND)) {
            try {
                notification.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + mpData.getString(SOUND)));
            } catch (Exception e) {

            }
        }
        if (mpData.has(BIG_IMAGE)) {
            try {
                InputStream in = new java.net.URL(mpData.getString(BIG_IMAGE)).openStream();
                NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
                style.bigPicture(BitmapFactory.decodeStream(in));
                if (mpData.has(TITLE_EXPANDED)) {
                    style.setBigContentTitle(mpData.getString(TITLE_EXPANDED));
                }
                notification.setStyle(style);
            } catch (Exception e) {


            }
        } else if (mpData.has(BIG_TEXT)) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle().bigText(mpData.getString(BIG_TEXT));
            if (mpData.has(TITLE_EXPANDED)) {
                style.setBigContentTitle(mpData.getString(TITLE_EXPANDED));
            }
            notification.setStyle(style);
        } else if (mpData.has(INBOX_STYLE_ROOT)) {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            JSONArray lines = mpData.getJSONArray(INBOX_STYLE_ROOT);
            for (int i = 0; i < lines.length(); i++) {
                style.addLine(lines.getString(i));
            }
            if (mpData.has(TITLE_EXPANDED)) {
                style.setBigContentTitle(mpData.getString(TITLE_EXPANDED));
            }
            notification.setStyle(style);
        }
        if (mpData.has(VIBRATION_PATTERN)) {
            JSONArray pattern = mpData.getJSONArray(VIBRATION_PATTERN);
            List<Long> list = new ArrayList<Long>();
            for (int i = 0; i < pattern.length(); i++) {
                list.add(pattern.getLong(i));
            }
            Long[] vArray = new Long[list.size()];
            vArray = list.toArray(vArray);
            long[] primitives = new long[vArray.length];
            for (int i = 0; i < vArray.length; i++)
                primitives[i] = vArray[i];

            notification.setVibrate(primitives);
        }
        if (mpData.has(ACTION_1)) {
            addAction(context, notification, mpData.getJSONObject(ACTION_1));
        }
        if (mpData.has(ACTION_2)) {
            addAction(context, notification, mpData.getJSONObject(ACTION_2));
        }
        if (mpData.has(ACTION_3)) {
            addAction(context, notification, mpData.getJSONObject(ACTION_3));
        }

        notification.setContentIntent(MPCloudMessage.getDefaultOpenIntent(context, extras));
        notification.setAutoCancel(true);
        // broadcastNotificationReceived(newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD));
        return notification.build();
    }

    private static PendingIntent getDefaultOpenIntent(Context context, Bundle extras){
        Intent launchIntent = new Intent(context, MPService.class);
        launchIntent.setAction(MPService.MPARTICLE_NOTIFICATION_OPENED);
        launchIntent.putExtras(extras);
        return PendingIntent.getService(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

        private static void addAction(Context context, NotificationCompat.Builder notification, JSONObject actionData) {
            try {
                int drawableResourceId = context.getResources().getIdentifier(actionData.getString(ACTION_ICON), "drawable", context.getPackageName());
                notification.addAction(drawableResourceId, actionData.getString(ACTION_TITLE), null);
            }catch (JSONException e){}
        }


    }
