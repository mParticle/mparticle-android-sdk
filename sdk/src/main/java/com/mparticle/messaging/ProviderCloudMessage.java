package com.mparticle.messaging;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.support.v4.app.NotificationCompat;

import com.mparticle.MParticlePushUtility;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Representation of a GCM/push sent by a 3rd party such as Urban Airship or Mixpanel.
 */
public class ProviderCloudMessage extends AbstractCloudMessage {
    private final String mPrimaryText;

    public ProviderCloudMessage(Bundle extras, JSONArray pushKeys) {
        super(extras);
        mPrimaryText = findProviderMessage(extras, pushKeys);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(mPrimaryText);
    }

    @Override
    public int getId() {
        return mPrimaryText.hashCode();
    }

    @Override
    public Notification buildNotification(Context context) {
        Notification notification = new NotificationCompat.Builder(context)
                .setContentIntent(getDefaultOpenIntent(context, this))
                .setSmallIcon(MParticlePushUtility.getFallbackIcon(context))
                .setTicker(mPrimaryText)
                .setContentTitle(MParticlePushUtility.getFallbackTitle(context))
                .setContentText(mPrimaryText).build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        return notification;
    }

    private String findProviderMessage(Bundle extras, JSONArray possibleKeys){
        if (possibleKeys != null) {
            for (int i = 0; i < possibleKeys.length(); i++){
                try {
                    String message = extras.getString(possibleKeys.getString(i));
                    if (message != null && message.length() > 0) {
                        extras.remove(possibleKeys.getString(i));
                        return message;
                    }
                }catch (JSONException jse){

                }
            }
        }
        return "";
    }
}
