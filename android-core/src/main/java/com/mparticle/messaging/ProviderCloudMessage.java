package com.mparticle.messaging;

import static com.mparticle.MPServiceUtil.NOTIFICATION_CHANNEL;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.mparticle.MPService;
import com.mparticle.MPServiceUtil;
import com.mparticle.internal.ConfigManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * Representation of an FCM/push sent by a 3rd party such as Urban Airship or Mixpanel.
 */
public class ProviderCloudMessage implements Parcelable {
    public static final int FLAG_RECEIVED = 1;
    public static final int FLAG_DIRECT_OPEN = 1 << 1;
    public static final int FLAG_READ = 1 << 2;
    public static final int FLAG_INFLUENCE_OPEN = 1 << 3;
    public static final int FLAG_DISPLAYED = 1 << 4;

    private final String mPrimaryText;
    private long mActualDeliveryTime = 0;
    protected Bundle mExtras;
    private boolean mDisplayed;


    public ProviderCloudMessage(@Nullable Bundle extras, @Nullable JSONArray pushKeys) {
        if (extras != null) {
            mExtras = new Bundle(extras);
        }
        mPrimaryText = findProviderMessage(pushKeys);
    }

    public ProviderCloudMessage(@NonNull Parcel pc) {
        mExtras = pc.readBundle();
        mActualDeliveryTime = pc.readLong();
        mDisplayed = pc.readInt() == 1;
        mPrimaryText = pc.readString();
    }

    @NonNull
    public Notification buildNotification(@NonNull Context context, long time) {
        setActualDeliveryTime(time);
        return buildNotification(context);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(mExtras);
        dest.writeLong(mActualDeliveryTime);
        dest.writeInt(mDisplayed ? 1 : 0);
        dest.writeString(mPrimaryText);
    }

    public static final Parcelable.Creator<ProviderCloudMessage> CREATOR = new Parcelable.Creator<ProviderCloudMessage>() {

        @Override
        public ProviderCloudMessage createFromParcel(Parcel source) {
            return new ProviderCloudMessage(source);
        }

        @Override
        public ProviderCloudMessage[] newArray(int size) {
            return new ProviderCloudMessage[size];
        }
    };

    @NonNull
    public int getId() {
        return mPrimaryText.hashCode();
    }

    @NonNull
    public String getPrimaryMessage(@NonNull Context context) {
        return mPrimaryText;
    }

    /**
     * Note that the actual message is stripped from the extras bundle in findProviderMessage().
     *
     * @return
     */
    @NonNull
    public JSONObject getRedactedJsonPayload() {
        JSONObject json = new JSONObject();
        Set<String> keys = mExtras.keySet();
        for (String key : keys) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    json.put(key, JSONObject.wrap(mExtras.get(key)));
                } else {
                    json.put(key, mExtras.get(key));
                }
            } catch (JSONException e) {
            }
        }
        return json;
    }

    @NonNull
    public Notification buildNotification(@NonNull Context context) {
        NotificationCompat.Builder builder;
        if (oreoNotificationCompatAvailable()) {
            builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
        Notification notification = builder.setContentIntent(getLoopbackIntent(context, this, String.valueOf(getId())))
                .setSmallIcon(getFallbackIcon(context))
                .setTicker(mPrimaryText)
                .setContentTitle(getFallbackTitle(context))
                .setContentText(mPrimaryText)
                .build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        return notification;
    }

    @NonNull
    public static ProviderCloudMessage createMessage(@NonNull Intent intent, @Nullable JSONArray keys) {
        return new ProviderCloudMessage(intent.getExtras(), keys);
    }


    protected static PendingIntent getLoopbackIntent(Context context, ProviderCloudMessage message, String id) {
        Intent intent = new Intent(MPServiceUtil.INTERNAL_NOTIFICATION_TAP);
        intent.setClass(context, MPService.class);
        intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);

        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getService
                    (context, id.hashCode(), intent, PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getService(context, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return pendingIntent;
    }

    @Nullable
    protected static String getFallbackTitle(@NonNull Context context) {
        String fallbackTitle = null;
        int titleResId = ConfigManager.getPushTitle(context);
        if (titleResId > 0) {
            try {
                fallbackTitle = context.getString(titleResId);
            } catch (Resources.NotFoundException e) {

            }
        } else {
            try {
                int stringId = context.getApplicationInfo().labelRes;
                fallbackTitle = context.getResources().getString(stringId);
            } catch (Resources.NotFoundException ex) {

            }
        }
        return fallbackTitle;
    }

    protected static int getFallbackIcon(@NonNull Context context) {
        int smallIcon = ConfigManager.getPushIcon(context);
        try {
            Drawable draw = context.getResources().getDrawable(smallIcon);
        } catch (Resources.NotFoundException nfe) {
            smallIcon = 0;
        }

        if (smallIcon == 0) {
            try {
                smallIcon = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).icon;
            } catch (PackageManager.NameNotFoundException e) {
                // use the ic_dialog_alert icon if the app's can not be found
            }
            if (0 == smallIcon) {
                smallIcon = context.getResources().getIdentifier("ic_dialog_alert", "drawable", "android");
            }
        }
        return smallIcon;
    }

    public boolean shouldDisplay() {
        return true;
    }

    public long getActualDeliveryTime() {
        return mActualDeliveryTime;
    }

    public void setActualDeliveryTime(long time) {
        mActualDeliveryTime = time;
    }

    public void setDisplayed(boolean displayed) {
        mDisplayed = displayed;
    }

    public boolean getDisplayed() {
        return mDisplayed;
    }

    @NonNull
    public Intent getDefaultOpenIntent(Context context, ProviderCloudMessage message) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
        return intent;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    /**
     * Find the intended message, and also remove it from the extras for privacy.
     *
     * @param possibleKeys
     * @return
     */
    private String findProviderMessage(JSONArray possibleKeys) {
        if (possibleKeys != null) {
            for (int i = 0; i < possibleKeys.length(); i++) {
                try {
                    String message = mExtras.getString(possibleKeys.getString(i));
                    if (message != null && message.length() > 0) {
                        mExtras.remove(possibleKeys.getString(i));
                        return message;
                    }
                } catch (JSONException jse) {

                }
            }
        }
        return "";
    }

    private boolean oreoNotificationCompatAvailable() {
        try {
            NotificationCompat.Builder.class.getMethod("setChannelId", String.class);
            return true;
        } catch (NoSuchMethodException ignore) {
            return false;
        }
    }
}
