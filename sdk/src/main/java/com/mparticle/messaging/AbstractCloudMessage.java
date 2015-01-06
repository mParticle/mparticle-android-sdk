package com.mparticle.messaging;

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

import com.mparticle.MPService;
import com.mparticle.MParticlePushUtility;
import com.mparticle.internal.ConfigManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * Base class for the class representation of a GCM/push
 *
 * This class and it's children implement parcelable so that they can be passed around as extras
 * of an Intent.
 *
 */
public abstract class AbstractCloudMessage implements Parcelable {

    public static final int FLAG_RECEIVED = 1 << 0;
    public static final int FLAG_DIRECT_OPEN = 1 << 1;
    public static final int FLAG_READ = 1 << 2;
    public static final int FLAG_INFLUENCE_OPEN = 1 << 3;
    public static final int FLAG_DISPLAYED = 1 << 4;

    private long mActualDeliveryTime = 0;
    protected Bundle mExtras;

    public AbstractCloudMessage(Parcel pc) {
        mExtras = pc.readBundle();
        mActualDeliveryTime = pc.readLong();
    }

    public AbstractCloudMessage(Bundle extras){
        mExtras = extras;
    }

    protected abstract CloudAction getDefaultAction();

    public Notification buildNotification(Context context, long time){
        setActualDeliveryTime(time);
        return buildNotification(context);
    }

    protected abstract Notification buildNotification(Context context);

    protected Intent getDefaultOpenIntent(Context context, AbstractCloudMessage message) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        intent.putExtra(MessagingUtils.CLOUD_MESSAGE_EXTRA, message);
        return intent;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    public static AbstractCloudMessage createMessage(Intent intent, JSONArray keys) throws InvalidGcmMessageException {
        if (MPCloudNotificationMessage.isValid(intent.getExtras())){
            return new MPCloudNotificationMessage(intent.getExtras());
        }else{
            return new ProviderCloudMessage(intent.getExtras(), keys);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(mExtras);
        dest.writeLong(mActualDeliveryTime);
    }

    public abstract int getId();

    public abstract String getPrimaryMessage(Context context);

    public abstract JSONObject getRedactedJsonPayload();

    protected static PendingIntent getLoopbackIntent(Context context, AbstractCloudMessage message, CloudAction action){
        Intent intent = new Intent(MPService.INTERNAL_NOTIFICATION_TAP + action.getActionId());
        intent.setClass(context, MPService.class);
        intent.putExtra(MessagingUtils.CLOUD_MESSAGE_EXTRA, message);
        intent.putExtra(MessagingUtils.CLOUD_ACTION_EXTRA, action);

        return PendingIntent.getService(context, action.getActionIdentifier().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public boolean shouldDisplay(){
        return true;
    }

    public long getActualDeliveryTime() {
        return mActualDeliveryTime;
    }

    public void setActualDeliveryTime(long time){
        mActualDeliveryTime = time;
    }

    /**
     * @hide
     */
    public static class InvalidGcmMessageException extends Exception {
        public InvalidGcmMessageException(String detailMessage) {
            super(detailMessage);
        }
    }

    protected static String getFallbackTitle(Context context){
        String fallbackTitle = null;
        int titleResId = ConfigManager.getPushTitle(context);
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

    protected static int getFallbackIcon(Context context){
        int smallIcon = ConfigManager.getPushIcon(context);
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
}
