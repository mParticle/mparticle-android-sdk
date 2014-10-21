package com.mparticle.messaging;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.mparticle.MParticlePushUtility;

import org.json.JSONArray;

/**
 * Base class for the class representation of a GCM/push
 *
 * This class and it's children implement parcelable so that they can be passed around as extras
 * of an Intent.
 *
 */
public abstract class AbstractCloudMessage implements Parcelable {
    private String appState;
    protected Bundle mExtras;

    public AbstractCloudMessage(Parcel pc) {
        mExtras = pc.readBundle();
        appState = pc.readString();
    }

    public AbstractCloudMessage(Bundle extras){
        mExtras = extras;
    }

    public abstract Notification buildNotification(Context context);

    public void setAppState(String appState) {
        this.appState = appState;
    }

    public String getAppState() {
        return appState;
    }

    protected PendingIntent getDefaultOpenIntent(Context context, AbstractCloudMessage message) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        dest.writeString(appState);
    }

    public abstract int getId();
}
