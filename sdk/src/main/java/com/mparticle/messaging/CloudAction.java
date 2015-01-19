package com.mparticle.messaging;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mparticle.MPService;

import java.util.UUID;

/**
 * Represents a tappable action to be associated with an {@link MPCloudNotificationMessage}
 */
public class CloudAction implements Parcelable {

    private final String mActionId;
    private final String mActionIcon;
    private final String mActionTitle;
    private final String mActionActivity;
    private final String mActionIdentifier;

    public CloudAction(String actionId, String actionIcon, String actionTitle, String actionActivity) {

        mActionIcon = actionIcon;
        mActionTitle = actionTitle;
        mActionActivity = actionActivity;
        mActionId = actionId;

        if (TextUtils.isEmpty(mActionId)){
            if (!TextUtils.isEmpty(mActionTitle)){
                mActionIdentifier = mActionTitle;
            }else if (!TextUtils.isEmpty(mActionIcon)){
                mActionIdentifier = mActionIcon;
            }else{
                mActionIdentifier = UUID.randomUUID().toString();
            }
        }else{
            mActionIdentifier = mActionId;
        }
    }

    public CloudAction(Parcel source) {
        mActionId = source.readString();
        mActionIcon = source.readString();
        mActionTitle = source.readString();
        mActionActivity = source.readString();
        mActionIdentifier = source.readString();
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
        dest.writeString(mActionIdentifier);
    }

    public PendingIntent getIntent(Context context, AbstractCloudMessage message, CloudAction action) {
        PendingIntent activityIntent = null;
        if (mActionActivity != null){
            try {
                Intent intent = new Intent(MPService.INTERNAL_NOTIFICATION_TAP);
                intent.setClass(context, Class.forName(mActionActivity));
                intent.putExtra(MPMessagingAPI.CLOUD_MESSAGE_EXTRA, message);
                intent.putExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA, action);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                activityIntent = PendingIntent.getActivity(context, action.getActionIdentifier().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }catch (Exception e){

            }
        }
        if (activityIntent == null){
            Intent intent = message.getDefaultOpenIntent(context, message);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(MPMessagingAPI.CLOUD_ACTION_EXTRA, action);
            activityIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return activityIntent;
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

    public String getIconId(){
        return mActionIcon;
    }

    public int getIconId(Context context) {
        if (!TextUtils.isEmpty(mActionIcon)) {
            int id = context.getResources().getIdentifier(mActionIcon, "drawable", context.getPackageName());
            if (id > 0){
                return id;
            }
        }
        return 0;
    }

    public String getTitle() {
        return mActionTitle;
    }

    public String getActionIdentifier() {
        return mActionIdentifier;
    }

    public int getActionIdInt() {
        try {
            return Integer.parseInt(getActionIdentifier());
        }catch (Exception e){

        }
        return -1;
    }
}
