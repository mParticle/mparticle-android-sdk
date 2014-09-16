package com.mparticle;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

/**
 * Created by sdozor on 9/15/14.
 */
public abstract class AbstractCloudMessage implements Parcelable{
    private String appState;
    protected String mTitle;
    protected String mPrimaryText;
    protected Bundle mExtras;
    public abstract Notification buildNotification(Context context);

    public void setAppState(String appState) {
        this.appState = appState;
    }

    public String getAppState() {
        return appState;
    }

    protected static PendingIntent getDefaultOpenIntent(Context context, AbstractCloudMessage message){
        Intent launchIntent = new Intent(context, MPService.class);
        launchIntent.setAction(MPService.MPARTICLE_NOTIFICATION_OPENED);
        launchIntent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
        return PendingIntent.getService(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public Bundle getExtras() {
        return mExtras;
    }
}
