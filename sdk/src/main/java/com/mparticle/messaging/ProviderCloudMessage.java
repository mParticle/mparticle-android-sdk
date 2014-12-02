package com.mparticle.messaging;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

import com.mparticle.MParticlePushUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * Representation of a GCM/push sent by a 3rd party such as Urban Airship or Mixpanel.
 */
public class ProviderCloudMessage extends AbstractCloudMessage {
    private final String mPrimaryText;

    public ProviderCloudMessage(Bundle extras, JSONArray pushKeys) {
        super(extras);
        mPrimaryText = findProviderMessage(pushKeys);
    }

    public ProviderCloudMessage(Parcel pc) {
        super(pc);
        mPrimaryText = pc.readString();
    }

    @Override
    protected CloudAction getDefaultAction() {
        return new CloudAction(0, null, mPrimaryText, null);
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


    @Override
    public int getId() {
        return mPrimaryText.hashCode();
    }

    @Override
    public JSONObject getJsonPayload() {
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

    @Override
    public Notification buildNotification(Context context) {

        Notification notification = new NotificationCompat.Builder(context)
                .setContentIntent(getLoopbackIntent(context, this, getDefaultAction()))
                .setSmallIcon(MParticlePushUtility.getFallbackIcon(context))
                .setTicker(mPrimaryText)
                .setContentTitle(MParticlePushUtility.getFallbackTitle(context))
                .setContentText(mPrimaryText).build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        return notification;
    }

    private String findProviderMessage(JSONArray possibleKeys){
        if (possibleKeys != null) {
            for (int i = 0; i < possibleKeys.length(); i++){
                try {
                    String message = mExtras.getString(possibleKeys.getString(i));
                    if (message != null && message.length() > 0) {
                        mExtras.remove(possibleKeys.getString(i));
                        return message;
                    }
                }catch (JSONException jse){

                }
            }
        }
        return "";
    }
}
