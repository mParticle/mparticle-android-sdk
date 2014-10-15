package com.mparticle;

import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by sdozor on 9/11/14.
 */
public class MPCloudMessage extends AbstractCloudMessage {
    private final static String COMMAND = "m_cmd";
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

    private final static String ACTION_ICON = "ai";
    private final static String ACTION_TITLE = "at";
    private final static String GROUP = "m_g";
    private final static String INAPP_MESSAGE_THEME = "m_iamt";



    private String mSmallIcon;

    private String mSecondayText;
    private String mLargeIconUri;
    private int mLightColor = -1;
    private int mLightOffMillis = -1;
    private int mLightOnMillis = -1;
    private int mNumber = -1;
    private boolean mAlertOnce;
    private int mPriority = NotificationCompat.PRIORITY_DEFAULT;
    private String mSoundUri;
    private String mBigImageUri;
    private String mTitleExpanded;
    private String mBigText;
    private String[] mInboxText;
    private long[] mVibrationPattern;
    private CloudAction[] mActions;

    public MPCloudMessage(){}

    public MPCloudMessage(Parcel pc){
        mSmallIcon = pc.readString();
        mTitle = pc.readString();
        mPrimaryText = pc.readString();
        mSecondayText = pc.readString();
        mLargeIconUri = pc.readString();
        mLightColor = pc.readInt();
        mLightOffMillis = pc.readInt();
        mLightOnMillis = pc.readInt();
        mNumber = pc.readInt();
        mAlertOnce = (pc.readInt() == 1);
        mPriority = pc.readInt();
        mSoundUri = pc.readString();
        mBigImageUri = pc.readString();
        mTitleExpanded = pc.readString();
        mBigText = pc.readString();
        pc.readStringArray(mInboxText);
        pc.readLongArray(mVibrationPattern);
        mActions = (CloudAction[]) pc.readParcelableArray(CloudAction.class.getClassLoader());
        mExtras = pc.readBundle();
    }

    MPCloudMessage(Bundle extras) throws JSONException{


    }

    public static final Parcelable.Creator<MPCloudMessage> CREATOR = new Parcelable.Creator<MPCloudMessage>() {

        @Override
        public MPCloudMessage createFromParcel(Parcel source) {
            return new MPCloudMessage(source);
        }

        @Override
        public MPCloudMessage[] newArray(int size) {
            return new MPCloudMessage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public Notification buildNotification(Context context) {

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        int smallIconResId = 0;
        if (!TextUtils.isEmpty(mSmallIcon) && (smallIconResId = context.getResources().getIdentifier(mSmallIcon, "drawable", context.getPackageName())) > 0) {
            notification.setSmallIcon(smallIconResId);
        } else {
            notification.setSmallIcon(MParticlePushUtility.getFallbackIcon(context));
        }
        String fallbackTitle = MParticlePushUtility.getFallbackTitle(context);
        notification.setContentTitle(mTitle == null ? fallbackTitle : mTitle);
        notification.setContentText(mPrimaryText == null ? fallbackTitle : mPrimaryText);

        if (!TextUtils.isEmpty(mSecondayText))
            notification.setSubText(mSecondayText);

        /*
          waiting for v21 of support library to be released.
        if (mpData.containsKey("g")){
           notification.setGroup(mpData.getString("g));
        }
        */

        if (!TextUtils.isEmpty(mLargeIconUri)) {
            if (mLargeIconUri.contains("http")) {
                try {
                    InputStream in = new java.net.URL(mLargeIconUri).openStream();
                    notification.setLargeIcon(BitmapFactory.decodeStream(in));
                } catch (Exception e) {


                }
            } else {
                int drawableResourceId = context.getResources().getIdentifier(mLargeIconUri, "drawable", context.getPackageName());
                if (drawableResourceId > 0) {
                    Bitmap icon = BitmapFactory.decodeResource(context.getResources(),
                            drawableResourceId);
                    notification.setLargeIcon(icon);
                }
            }
        }
        if (mLightColor > 0) {
            notification.setLights(mLightColor, mLightOnMillis, mLightOffMillis);
        }
        if (mNumber > 0) {
            notification.setNumber(mNumber);
        }

        notification.setOnlyAlertOnce(mAlertOnce);
        notification.setPriority(mPriority);

      /*  if (mpData.has(SOUND)) {
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

        notification.setContentIntent(MPCloudMessage.getDefaultOpenIntent(context, extras));*/
        notification.setAutoCancel(true);
        // broadcastNotificationReceived(newExtras.getBundle(PUSH_ORIGINAL_PAYLOAD));
        return notification.build();
    }



    private static void addAction(Context context, NotificationCompat.Builder notification, JSONObject actionData) {
        try {
            int drawableResourceId = context.getResources().getIdentifier(actionData.getString(ACTION_ICON), "drawable", context.getPackageName());
            notification.addAction(drawableResourceId, actionData.getString(ACTION_TITLE), null);
        }catch (JSONException e){}
    }


}
