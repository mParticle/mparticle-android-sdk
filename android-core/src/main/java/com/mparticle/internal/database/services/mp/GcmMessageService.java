package com.mparticle.internal.database.services.mp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.Logger;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.tables.mp.GcmMessageTable;

import com.mparticle.internal.dto.GcmMessageDTO;
import com.mparticle.messaging.AbstractCloudMessage;
import com.mparticle.messaging.MPCloudNotificationMessage;

import java.util.ArrayList;
import java.util.List;

public class GcmMessageService extends GcmMessageTable {
    private static String[] gcmColumns = {GcmMessageTableColumns.CONTENT_ID, GcmMessageTableColumns.CAMPAIGN_ID, GcmMessageTableColumns.EXPIRATION, GcmMessageTableColumns.DISPLAYED_AT};
    private static String gcmDeleteWhere = GcmMessageTableColumns.EXPIRATION + " < ? and " + GcmMessageTableColumns.DISPLAYED_AT + " > 0 and " + GcmMessageTableColumns.MP_ID + " = ? ";

    public static int deleteExpiredGcmMessages(SQLiteDatabase database, long mpId) {
        String[] deleteWhereArgs = {Long.toString(System.currentTimeMillis()), String.valueOf(mpId)};
        return database.delete(GcmMessageTableColumns.TABLE_NAME, gcmDeleteWhere, deleteWhereArgs);
    }

    public static List<GcmHistory> getGcmHistory(SQLiteDatabase database, long mpId) {
        Cursor gcmHistory = null;
        List<GcmHistory> histories = new ArrayList<GcmHistory>();
        try {
            gcmHistory = database.query(GcmMessageTableColumns.TABLE_NAME,
                    gcmColumns,
                    GcmMessageTableColumns.MP_ID + " = ? ",
                    new String[]{String.valueOf(mpId)},
                    null,
                    null,
                    GcmMessageTableColumns.EXPIRATION + " desc");
            if (gcmHistory.getCount() > 0) {
                while (gcmHistory.moveToNext()) {
                    int contentId = gcmHistory.getInt(gcmHistory.getColumnIndex(GcmMessageTableColumns.CONTENT_ID));
                    if (contentId != GcmMessageTableColumns.PROVIDER_CONTENT_ID) {
                        int campaignId = gcmHistory.getInt(gcmHistory.getColumnIndex(GcmMessageTableColumns.CAMPAIGN_ID));
                        String campaignIdString = Integer.toString(campaignId);
                        long displayedDate = gcmHistory.getLong(gcmHistory.getColumnIndex(GcmMessageTableColumns.DISPLAYED_AT));
                        histories.add(new GcmHistory(contentId, campaignIdString, displayedDate));
                    }
                }
            }
        } finally {
            if (gcmHistory != null && !gcmHistory.isClosed()) {
                gcmHistory.close();
            }
        }
        return histories;
    }

    public static void clearOldProviderGcm(SQLiteDatabase db, long mpId) {
        String[] deleteWhereArgs = {Integer.toString(GcmMessageTableColumns.PROVIDER_CONTENT_ID), String.valueOf(mpId)};
        db.delete(GcmMessageTableColumns.TABLE_NAME, GcmMessageTableColumns.CONTENT_ID + " = ?", deleteWhereArgs);
    }

    public static void insertGcmMessage(SQLiteDatabase db, AbstractCloudMessage message, String appState, long mpId) {
        ContentValues contentValues = new ContentValues();
        if (message instanceof MPCloudNotificationMessage) {
            contentValues.put(GcmMessageTableColumns.CONTENT_ID, ((MPCloudNotificationMessage) message).getContentId());
            contentValues.put(GcmMessageTableColumns.CAMPAIGN_ID, ((MPCloudNotificationMessage) message).getCampaignId());
            contentValues.put(GcmMessageTableColumns.EXPIRATION, ((MPCloudNotificationMessage) message).getExpiration());
            contentValues.put(GcmMessageTableColumns.DISPLAYED_AT, message.getActualDeliveryTime());
        } else {
            contentValues.put(GcmMessageTableColumns.CONTENT_ID, GcmMessageTableColumns.PROVIDER_CONTENT_ID);
            contentValues.put(GcmMessageTableColumns.CAMPAIGN_ID, 0);
            contentValues.put(GcmMessageTableColumns.EXPIRATION, System.currentTimeMillis() + (24 * 60 * 60 * 1000));
            contentValues.put(GcmMessageTableColumns.DISPLAYED_AT, System.currentTimeMillis());
        }
        contentValues.put(GcmMessageTableColumns.PAYLOAD, message.getRedactedJsonPayload().toString());
        contentValues.put(GcmMessageTableColumns.BEHAVIOR, 0);
        contentValues.put(GcmMessageTableColumns.CREATED_AT, System.currentTimeMillis());
        contentValues.put(GcmMessageTableColumns.MP_ID, String.valueOf(mpId));
        contentValues.put(GcmMessageTableColumns.APPSTATE, appState);

        db.replace(GcmMessageTableColumns.TABLE_NAME, null, contentValues);
    }

    public static List<GcmMessageDTO> logInfluenceOpenGcmMessages(SQLiteDatabase db, MessageManager.InfluenceOpenMessage message, long mpId) {
        Cursor gcmCursor = null;
        List<GcmMessageDTO> gcmMessages = new ArrayList<GcmMessageDTO>();
        try {

            gcmCursor = db.query(GcmMessageTableColumns.TABLE_NAME,
                    null,
                    GcmMessageTableColumns.CONTENT_ID + " != " + GcmMessageTableColumns.PROVIDER_CONTENT_ID + " and " +
                            GcmMessageTableColumns.DISPLAYED_AT +
                            " > 0 and " +
                            GcmMessageTableColumns.DISPLAYED_AT +
                            " > " + (message.mTimeStamp - message.mTimeout) +
                            " and ((" + GcmMessageTableColumns.BEHAVIOR + " & " + AbstractCloudMessage.FLAG_INFLUENCE_OPEN + "" + ") != " + AbstractCloudMessage.FLAG_INFLUENCE_OPEN + ") and " + GcmMessageTableColumns.MP_ID + " = ?",
                    new String[]{String.valueOf(mpId)},
                    null,
                    null,
                    null);
            while (gcmCursor.moveToNext()) {
                new GcmMessageDTO(gcmCursor.getInt(gcmCursor.getColumnIndex(GcmMessageTableColumns.CONTENT_ID)),
                        gcmCursor.getString(gcmCursor.getColumnIndex(GcmMessageTableColumns.PAYLOAD)),
                        gcmCursor.getString(gcmCursor.getColumnIndex(GcmMessageTableColumns.APPSTATE)),
                        AbstractCloudMessage.FLAG_INFLUENCE_OPEN);
            }
        } catch (Exception e) {
            Logger.error(e, "Error logging influence-open message to mParticle DB ", e.toString());
        } finally {
            if (gcmCursor != null && !gcmCursor.isClosed()) {
                gcmCursor.close();
            }
        }
        return gcmMessages;
    }

    public static int getCurrentBehaviors(SQLiteDatabase db, String contentId, long mpId) {
        Cursor gcmCursor = null;
        int currentBehaviors = -1;
        try {
            String[] args = {contentId, String.valueOf(mpId)};
            gcmCursor = db.query(GcmMessageTableColumns.TABLE_NAME,
                    null,
                    GcmMessageTableColumns.CONTENT_ID + " = ? and " + GcmMessageTableColumns.MP_ID + " = ?",
                    args,
                    null,
                    null,
                    null);
            if (gcmCursor.moveToFirst()) {
                currentBehaviors = gcmCursor.getInt(gcmCursor.getColumnIndex(GcmMessageTableColumns.BEHAVIOR));
            }
        } finally {
            if (gcmCursor != null && !gcmCursor.isClosed()) {
                gcmCursor.close();
            }
        }
        return currentBehaviors;
    }

    /**
     * TODO
     * check if this one needs to be stored by queried by MPID, or is contentId a unique identifier..it seems like it is unique
     */
    public static int updateGcmBehavior(SQLiteDatabase db, int newBehavior, long timestamp, String contentId) {
        ContentValues values = new ContentValues();
        String[] args = {contentId};

        values.put(GcmMessageTableColumns.BEHAVIOR, newBehavior);
        if (timestamp > 0) {
            values.put(GcmMessageTableColumns.DISPLAYED_AT, timestamp);
        }
        return db.update(GcmMessageTableColumns.TABLE_NAME, values, GcmMessageTableColumns.CONTENT_ID + " =?", args);
    }

    public static String getPayload(SQLiteDatabase db, long mpId) {
        Cursor pushCursor = null;
        String payload = null;
        try {
            pushCursor = db.query(GcmMessageTableColumns.TABLE_NAME,
                    null,
                    GcmMessageTableColumns.DISPLAYED_AT + " > 0 and " + GcmMessageTableColumns.MP_ID,
                    new String[]{String.valueOf(mpId)},
                    null,
                    null,
                    GcmMessageTableColumns.DISPLAYED_AT + " desc limit 1");
            if (pushCursor.moveToFirst()) {
                payload = pushCursor.getString(pushCursor.getColumnIndex(GcmMessageTableColumns.PAYLOAD));
            }
        } finally {
            if (pushCursor != null && !pushCursor.isClosed()) {
                pushCursor.close();
            }
        }
        return payload;
    }

    public static class GcmHistory {
        private int contentId;
        private String campaignIdString;
        private long displayDate;

        private GcmHistory(int contentId, String campaignIdString, long displayDate) {
            this.contentId = contentId;
            this.campaignIdString = campaignIdString;
            this.displayDate = displayDate;
        }

        public int getContentId() {
            return contentId;
        }

        public String getCampaignIdString() {
            return campaignIdString;
        }

        public long getDisplayDate() {
            return displayDate;
        }
    }
}
