package com.mparticle.messaging;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.mparticle.MPService;
import com.mparticle.MParticlePushUtility;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * The class representation of a GCM/push sent by mParticle. Allows for very granular customization
 * based on a series of key/value pairs in the GCM data payload.
 */
public class MPCloudNotificationMessage extends AbstractCloudMessage {
    public final static String COMMAND = "m_cmd";
    private final static String CAMPAIGN_ID = "m_cid";
    private final static String CONTENT_ID = "m_cntid";
    private final static String SMALL_ICON = "m_si";
    private final static String SHOW_INAPP_MESSAGE = "m_sia";
    private final static String DEFAULT_ACTIVITY = "m_dact";
    private final static String TITLE = "m_t";
    private final static String PRIMARY_MESSAGE = "m_m";
    private final static String SECONDARY_MESSAGE = "m_sm";
    private final static String LARGE_ICON = "m_li";
    private final static String LIGHTS_COLOR = "m_l_c";
    private final static String LIGHTS_OFF_MILLIS = "m_l_off";
    private final static String LIGHTS_ON_MILLIS = "m__on";
    private final static String NUMBER = "m_n";
    private final static String ALERT_ONCE = "m_ao";
    private final static String PRIORITY = "m_p";
    private final static String SOUND = "m_s";
    private final static String BIG_IMAGE = "m_bi";
    private final static String TITLE_EXPANDED = "m_xt";

    private final static String INBOX_TEXT_1 = "m_ib_1";
    private final static String INBOX_TEXT_2 = "m_ib_2";
    private final static String INBOX_TEXT_3 = "m_ib_3";
    private final static String INBOX_TEXT_4 = "m_ib_4";
    private final static String INBOX_TEXT_5 = "m_ib_5";

    private final static String BIG_TEXT = "m_bt";
    private final static String VIBRATION_PATTERN = "m_v";

    private final static String ACTION_1_ID = "m_a1_aid";
    private final static String ACTION_1_ICON = "m_a1_ai";
    private final static String ACTION_1_TITLE = "m_a1_at";
    private final static String ACTION_1_ACTIVITY = "m_a1_act";

    private final static String ACTION_2_ID = "m_a2_aid";
    private final static String ACTION_2_ICON = "m_a2_ai";
    private final static String ACTION_2_TITLE = "m_a2_at";
    private final static String ACTION_2_ACTIVITY = "m_a2_act";

    private final static String ACTION_3_ID = "m_a3_aid";
    private final static String ACTION_3_ICON = "m_a3_ai";
    private final static String ACTION_3_TITLE = "m_a3_at";
    private final static String ACTION_3_ACTIVITY = "m_a3_act";

    private final static String GROUP = "m_g";
    private final static String INAPP_MESSAGE_THEME = "m_iamt";

    private CloudAction[] mActions;
    private final static String EXPIRATION = "m_expy";

    public MPCloudNotificationMessage(Parcel pc){
        super(pc);
        mActions = new CloudAction[3];
        pc.readTypedArray(mActions, CloudAction.CREATOR);
    }

    public MPCloudNotificationMessage(Bundle extras) {
        super(extras);

        mActions = new CloudAction[3];
        if (mExtras.containsKey(ACTION_1_ID)){
            mActions[0] = new CloudAction(
                    Integer.parseInt(mExtras.getString(ACTION_1_ID)),
                    mExtras.getString(ACTION_1_ICON),
                    mExtras.getString(ACTION_1_TITLE),
                    mExtras.getString(ACTION_1_ACTIVITY));
        }
        if (mExtras.containsKey(ACTION_2_ID)){
            mActions[1] = new CloudAction(
                    Integer.parseInt(mExtras.getString(ACTION_2_ID)),
                    mExtras.getString(ACTION_2_ICON),
                    mExtras.getString(ACTION_2_TITLE),
                    mExtras.getString(ACTION_2_ACTIVITY));
        }
        if (mExtras.containsKey(ACTION_3_ID)){
            mActions[2] = new CloudAction(
                    Integer.parseInt(mExtras.getString(ACTION_3_ID)),
                    mExtras.getString(ACTION_3_ICON),
                    mExtras.getString(ACTION_3_TITLE),
                    mExtras.getString(ACTION_3_ACTIVITY));
        }


    }

    private String getDefaultActivity(){
        return mExtras.getString(DEFAULT_ACTIVITY);
    }

    @Override
    protected CloudAction getDefaultAction() {
        return new CloudAction(getContentId(), null, null, getDefaultActivity());
    }

    public static final Parcelable.Creator<MPCloudNotificationMessage> CREATOR = new Parcelable.Creator<MPCloudNotificationMessage>() {

        @Override
        public MPCloudNotificationMessage createFromParcel(Parcel source) {
            return new MPCloudNotificationMessage(source);
        }

        @Override
        public MPCloudNotificationMessage[] newArray(int size) {
            return new MPCloudNotificationMessage[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeTypedArray(mActions, 0);
    }

    @Override
    public int getId() {
        return getContentId();
    }


    public int getSmallIconResourceId(Context context){
        String resourceName = mExtras.getString(SMALL_ICON);
        if (!TextUtils.isEmpty(resourceName)) {
            int id = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
            if (id > 0){
                return id;
            }
        }
        return MParticlePushUtility.getFallbackIcon(context);
    }

    public String getContentTitle(Context context){
        String title = mExtras.getString(TITLE);
        if (TextUtils.isEmpty(title)){
            return MParticlePushUtility.getFallbackTitle(context);
        }else{
            return title;
        }
    }

    public String getPrimaryText(Context context){
        String text = mExtras.getString(PRIMARY_MESSAGE);
        if (TextUtils.isEmpty(text)){
            return MParticlePushUtility.getFallbackTitle(context);
        }else{
            return text;
        }
    }

    public boolean getShowInApp() {
        return Boolean.parseBoolean(mExtras.getString(SHOW_INAPP_MESSAGE));
    }

    public String getInAppTheme() {
        return mExtras.getString(INAPP_MESSAGE_THEME);
    }

    public String getSubText(){
        return mExtras.getString(SECONDARY_MESSAGE);
    }

    public Bitmap getLargeIcon(Context context){
        String largeIconUri = mExtras.getString(LARGE_ICON);
        if (!TextUtils.isEmpty(largeIconUri)) {
            if (largeIconUri.contains("http")) {
                try {
                    InputStream in = new java.net.URL(largeIconUri).openStream();
                    return BitmapFactory.decodeStream(in);
                } catch (Exception e) {


                }
            } else {
                int drawableResourceId = context.getResources().getIdentifier(largeIconUri, "drawable", context.getPackageName());
                if (drawableResourceId > 0) {
                    return BitmapFactory.decodeResource(context.getResources(),
                            drawableResourceId);
                }
            }
        }
        return null;
    }

    public int getLightColorArgb(){
        try {
        return Integer.parseInt(mExtras.getString(LIGHTS_COLOR));
        }catch (NumberFormatException nfe){
            return 0;
        }
    }

    public int getLightOffMillis(){
        try {
        return Integer.parseInt(mExtras.getString(LIGHTS_OFF_MILLIS));
        }catch (NumberFormatException nfe){
            return 0;
        }
    }

    public int getLightOnMillis(){
        try {
        return Integer.parseInt(mExtras.getString(LIGHTS_ON_MILLIS));
        }catch (NumberFormatException nfe){
            return 0;
        }
    }

    public int getNumber(){
        try {
            return Integer.parseInt(mExtras.getString(NUMBER));
        }catch (NumberFormatException nfe){
            return 0;
        }
    }

    public boolean getAlertOnlyOnce(){
        if (mExtras.containsKey(ALERT_ONCE)){
            return Boolean.parseBoolean(mExtras.getString(ALERT_ONCE));
        }
        return true;
    }

    public Integer getPriority(){
        if (mExtras.containsKey(PRIORITY)){
            try {
                return Integer.parseInt(mExtras.getString(PRIORITY));
            }catch (NumberFormatException nfe){
                return null;
            }
        }else{
            return null;
        }
    }

    public Uri getSound(Context context){
        String sound = mExtras.getString(SOUND);
        if (!TextUtils.isEmpty(sound)) {
            return Uri.parse("android.resource://" + context.getPackageName() + "/raw/" + mExtras.getString(SOUND));
        }else{
            return null;
        }
    }

    public Bitmap getBigPicture(Context context){
        String image = mExtras.getString(BIG_IMAGE);
        if (!TextUtils.isEmpty(image)){
            try {
                InputStream in = new java.net.URL(image).openStream();
                return BitmapFactory.decodeStream(in);
            }catch (Exception e){

            }
        }
        return null;
    }

    public String getExpandedTitle(){
        return mExtras.getString(TITLE_EXPANDED);
    }

    public String getBigText(){
        return mExtras.getString(BIG_TEXT);
    }

    public ArrayList<String> getInboxLines(){
        ArrayList<String> lines = new ArrayList<String>();
        String line_1 = mExtras.getString(INBOX_TEXT_1);
        if (!TextUtils.isEmpty(line_1)){
            lines.add(line_1);
        }
        String line_2 = mExtras.getString(INBOX_TEXT_2);
        if (!TextUtils.isEmpty(line_2)){
            lines.add(line_2);
        }
        String line_3 = mExtras.getString(INBOX_TEXT_3);
        if (!TextUtils.isEmpty(line_3)){
            lines.add(line_3);
        }
        String line_4 = mExtras.getString(INBOX_TEXT_4);
        if (!TextUtils.isEmpty(line_4)){
            lines.add(line_4);
        }
        String line_5 = mExtras.getString(INBOX_TEXT_5);
        if (!TextUtils.isEmpty(line_5)){
            lines.add(line_5);
        }
        return lines;
    }

    public long[] getVibrationPattern(){
        String patternString = mExtras.getString(VIBRATION_PATTERN);
        if (!TextUtils.isEmpty(patternString)){
            try {
                String[] patterns = patternString.split(",");
                long[] primPatterns = new long[patterns.length];
                int i = 0;
                for (String pattern : patterns) {
                    primPatterns[i] = Long.parseLong(pattern.trim());
                    i++;
                }
                return primPatterns;
            }catch (Exception e){

            }
        }
        return null;
    }

    public Notification buildNotification(Context context) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        notification.setSmallIcon(getSmallIconResourceId(context));
        notification.setContentTitle(getContentTitle(context));
        notification.setContentText(getPrimaryText(context));
        notification.setSubText(getSubText());
        notification.setLargeIcon(getLargeIcon(context));
        int lightColor = getLightColorArgb();
        if (lightColor > 0) {
            notification.setLights(lightColor, getLightOnMillis(), getLightOffMillis());
        }
        int number = getNumber();
        if (number > 0) {
            notification.setNumber(number);
        }
        notification.setOnlyAlertOnce(getAlertOnlyOnce());
        Integer priority = getPriority();
        if (priority != null) {
            notification.setPriority(priority);
        }

        /*
          waiting for v21 of support library to be released.
        if (mpData.containsKey("g")){
           notification.setGroup(mpData.getString("g));
        }
        */

        Uri sound = getSound(context);
        if (sound != null){
            notification.setSound(sound);
        }
        Bitmap bigImage = getBigPicture(context);
        ArrayList<String> inboxLines = getInboxLines();
        String bigContentTitle = getExpandedTitle();
        if (bigImage != null) {
            NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
            style.bigPicture(bigImage);

            if (!TextUtils.isEmpty(bigContentTitle)) {
                style.setBigContentTitle(bigContentTitle);
            }
            notification.setStyle(style);
        } else if (!TextUtils.isEmpty(getBigText())) {
            NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle().bigText(getBigText());

            if (!TextUtils.isEmpty(bigContentTitle)) {
                style.setBigContentTitle(bigContentTitle);
            }
            notification.setStyle(style);
        } else if (inboxLines != null && inboxLines.size() > 0) {
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            for (String line : inboxLines){
                style.addLine(line);
            }

            if (!TextUtils.isEmpty(bigContentTitle)) {
                style.setBigContentTitle(bigContentTitle);
            }
            notification.setStyle(style);
        }
        long[] pattern = getVibrationPattern();
        if (pattern != null && pattern.length > 0) {
            notification.setVibrate(pattern);
        }

        if (mActions != null) {
            for (CloudAction action : mActions) {
                if (action != null) {
                    notification.addAction(action.getIconId(context), action.getTitle(), getLoopbackIntent(context, this, action));
                }
            }
        }

        notification.setContentIntent(getLoopbackIntent(context, this, getDefaultAction()));
        notification.setAutoCancel(true);
        return notification.build();
    }



    public static boolean isValid(Bundle extras) {
        return extras.containsKey(CAMPAIGN_ID);
    }

    public CloudAction[] getActions() {
        return mActions;
    }


    public int getContentId() {
        return Integer.parseInt(mExtras.getString(CONTENT_ID));
    }

    public int getCampaignId() {
        return Integer.parseInt(mExtras.getString(CAMPAIGN_ID));
    }

    public long getExpiration() {
        return Long.parseLong(mExtras.getString(EXPIRATION));
    }
}
