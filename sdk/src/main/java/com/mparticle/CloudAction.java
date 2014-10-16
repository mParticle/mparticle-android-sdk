package com.mparticle;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by sdozor on 10/16/14.
 */
public class CloudAction implements Parcelable {

    private final String mActionId;
    private final String mActionIcon;
    private final String mActionTitle;
    private final String mActionActivity;

    public CloudAction(String actionId, String actionIcon, String actionTitle, String actionActivity) {
        mActionId = actionId;
        mActionIcon = actionIcon;
        mActionTitle = actionTitle;
        mActionActivity = actionActivity;
    }

    public CloudAction(Parcel source) {
        mActionId = source.readString();
        mActionIcon = source.readString();
        mActionTitle = source.readString();
        mActionActivity = source.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mActionId);
        dest.writeString(mActionIcon);
        dest.writeString(mActionTitle);
        dest.writeString(mActionActivity);
    }

    public PendingIntent getIntent(Context context, Bundle extras) {
        if (mActionActivity != null){
            try {
                Intent intent = new Intent();
                intent.setClass(context, Class.forName(mActionActivity));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtras(extras);
                return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }catch (Exception e){

            }
        }
        return null;
    }

    public static final Parcelable.Creator<CloudAction> CREATOR = new Parcelable.Creator<CloudAction>() {

        @Override
        public CloudAction createFromParcel(Parcel source) {
            return new CloudAction(source);
        }

        @Override
        public CloudAction[] newArray(int size) {
            return new CloudAction[size];
        }
    };

    public int getIconId(Context context) {
        if (!TextUtils.isEmpty(mActionId)) {
            int id = context.getResources().getIdentifier(mActionId, "drawable", context.getPackageName());
            if (id > 0){
                return id;
            }
        }
        return 0;
    }

    public CharSequence getTitle() {
        return mActionTitle;
    }
}
