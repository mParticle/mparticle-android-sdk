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

    private String mAppState;
    protected Bundle mExtras;
    private int mBehavior;

    public static final int FLAG_RECEIVED = 0x00000001;
    public static final int FLAG_DIRECT_OPEN = 0x00000010;
    public static final int FLAG_READ = 0x00001000;
    public static final int FLAG_INFLUENCE_OPEN = 0x00010000;

    public AbstractCloudMessage(Parcel pc) {
        mExtras = pc.readBundle();
        mAppState = pc.readString();
        mBehavior = pc.readInt();
    }

    public AbstractCloudMessage(Bundle extras){
        mExtras = extras;
    }

    protected abstract CloudAction getDefaultAction();

    public abstract Notification buildNotification(Context context);

    public void setAppState(String appState) {
        this.mAppState = appState;
    }

    public void setBehavior(int behavior){
        mBehavior = behavior;
    }

    public void addBehavior(int behavior){
        mBehavior |= behavior;
    }

    public int getBehavior() {
        return mBehavior;
    }

    public String getAppState() {
        return mAppState;
    }

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
        dest.writeString(mAppState);
        dest.writeInt(mBehavior);
    }

    public abstract int getId();

    public JSONObject getJsonPayload(){
        JSONObject json = new JSONObject();
        Set<String> keys = mExtras.keySet();
        for (String key : keys) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    json.put(key, JSONObject.wrap(mExtras.get(key)));
                }else{
                    json.put(key, mExtras.get(key));
                }
            } catch(JSONException e) {
            }
        }
        return json;
    }

    protected static PendingIntent getLoopbackIntent(Context context, AbstractCloudMessage message, CloudAction action){
        Intent intent = new Intent(MPService.INTERNAL_NOTIFICATION_TAP + action.getActionId());
        intent.setClass(context, MPService.class);
        intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        intent.putExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA, action);

        return PendingIntent.getService(context, action.getActionId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
