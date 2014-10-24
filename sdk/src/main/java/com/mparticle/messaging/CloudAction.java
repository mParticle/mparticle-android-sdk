package com.mparticle.messaging;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.mparticle.MPService;
import com.mparticle.MParticlePushUtility;

/**
 * Represents an tappable action to be associated with an {@link MPCloudNotificationMessage}
 */
public class CloudAction implements Parcelable {

    private final int mActionId;
    private final String mActionIcon;
    private final String mActionTitle;
    private final String mActionActivity;

    public CloudAction(int actionId, String actionIcon, String actionTitle, String actionActivity) {
        mActionId = actionId;
        mActionIcon = actionIcon;
        mActionTitle = actionTitle;
        mActionActivity = actionActivity;
    }

    public CloudAction(Parcel source) {
        mActionId = source.readInt();
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
        dest.writeInt(mActionId);
        dest.writeString(mActionIcon);
        dest.writeString(mActionTitle);
        dest.writeString(mActionActivity);
    }

    public PendingIntent getIntent(Context context, AbstractCloudMessage message, CloudAction action) {
        PendingIntent activityIntent = null;
        if (mActionActivity != null){
            try {
                Intent intent = new Intent(MPService.INTERNAL_NOTIFICATION_TAP);
                intent.setClass(context, Class.forName(mActionActivity));
                intent.putExtra(MParticlePushUtility.CLOUD_MESSAGE_EXTRA, message);
                intent.putExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA, action);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                activityIntent = PendingIntent.getActivity(context, action.getActionId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }catch (Exception e){

            }
        }
        if (activityIntent == null){
            Intent intent = message.getDefaultOpenIntent(context, message);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(MParticlePushUtility.CLOUD_ACTION_EXTRA, action);
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

    public int getIconId(Context context) {
        if (!TextUtils.isEmpty(mActionIcon)) {
            int id = context.getResources().getIdentifier(mActionIcon, "drawable", context.getPackageName());
            if (id > 0){
                return id;
            }
        }
        return 0;
    }

    public CharSequence getTitle() {
        return mActionTitle;
    }

    public int getActionId() {
        return mActionId;
    }
}
