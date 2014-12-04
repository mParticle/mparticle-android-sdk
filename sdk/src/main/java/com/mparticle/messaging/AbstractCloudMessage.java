package com.mparticle.messaging;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.mparticle.MPService;
import com.mparticle.MParticlePushUtility;

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

    public static final int FLAG_RECEIVED = 0x00000001;
    public static final int FLAG_DIRECT_OPEN = 0x00000010;
    public static final int FLAG_READ = 0x00001000;
    public static final int FLAG_INFLUENCE_OPEN = 0x00010000;
    public static final int FLAG_DISPLAYED = 0x00100000;

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
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        return intent;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    public static AbstractCloudMessage createMessage(Intent intent, JSONArray keys) {
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

    public abstract String getId();

    public abstract JSONObject getRedactedJsonPayload();

    protected static PendingIntent getLoopbackIntent(Context context, AbstractCloudMessage message, CloudAction action){
        Intent intent = new Intent(MPService.INTERNAL_NOTIFICATION_TAP + action.getActionId());
        intent.setClass(context, MPService.class);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        intent.putExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA, action);

        return PendingIntent.getService(context, action.getActionId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
}
