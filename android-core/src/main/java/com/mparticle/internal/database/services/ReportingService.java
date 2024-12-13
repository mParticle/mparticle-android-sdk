package com.mparticle.internal.database.services;

import android.content.ContentValues;
import android.database.Cursor;

import com.mparticle.internal.Constants;
import com.mparticle.internal.JsonReportingMessage;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.tables.BreadcrumbTable;
import com.mparticle.internal.database.tables.ReportingTable;
import com.mparticle.internal.listeners.InternalListenerManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReportingService extends ReportingTable {

    public static void insertReportingMessage(MPDatabase db, JsonReportingMessage message, long mpId) {
        ContentValues values = new ContentValues();
        values.put(ReportingTableColumns.MP_ID, mpId);
        values.put(ReportingTableColumns.CREATED_AT, message.getTimestamp());
        values.put(ReportingTableColumns.MODULE_ID, message.getModuleId());
        values.put(ReportingTableColumns.MESSAGE, message.toJson().toString());
        values.put(ReportingTableColumns.SESSION_ID, message.getSessionId());
        db.insert(ReportingTableColumns.TABLE_NAME, null, values);
    }

    /**
     * Will return all ReportingMessages, except for those with MP_ID == Constants.TEMPORARY_MPID,
     * useful in non-testing context.
     */
    public static List<ReportingMessage> getReportingMessagesForUpload(MPDatabase database) throws JSONException {
        return getReportingMessagesForUpload(database, false, Constants.TEMPORARY_MPID);
    }

    static List<ReportingMessage> getReportingMessagesForUpload(MPDatabase database, boolean include, long mpid) throws JSONException {
        List<ReportingMessage> reportingMessages = new ArrayList<ReportingMessage>();
        Cursor reportingMessageCursor = null;
        try {
            reportingMessageCursor = database.query(
                    ReportingTableColumns.TABLE_NAME,
                    null,
                    ReportingTableColumns.MP_ID + (include ? " = ?" : " != ?"),
                    new String[]{String.valueOf(mpid)},
                    null,
                    null,
                    ReportingTableColumns._ID + " asc");
            int reportingMessageIdIndex = reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns._ID);
            while (reportingMessageCursor.moveToNext()) {
                JSONObject msgObject = new JSONObject(
                        reportingMessageCursor.getString(
                                reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns.MESSAGE)
                        )
                );
                String sessionId = reportingMessageCursor.getString(
                        reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns.SESSION_ID)
                );
                int reportingMessageId = reportingMessageCursor.getInt(reportingMessageIdIndex);
                long reportingMessageMpid = reportingMessageCursor.getLong(reportingMessageCursor.getColumnIndexOrThrow(ReportingTableColumns.MP_ID));
                ReportingMessage reportingMessage = new ReportingMessage(msgObject, sessionId, reportingMessageId, reportingMessageMpid);
                InternalListenerManager.getListener().onCompositeObjects(reportingMessageCursor, reportingMessage);
                reportingMessages.add(reportingMessage);
            }
        } finally {
            if (reportingMessageCursor != null && !reportingMessageCursor.isClosed()) {
                reportingMessageCursor.close();
            }
        }
        return reportingMessages;
    }

    /**
     * Delete reporting messages after they've been included in an upload message.
     */
    public static void deleteReportingMessage(MPDatabase database, int messageId) {
        String[] whereArgs = new String[]{Long.toString(messageId)};
        String whereClause = "_id =?";
        database.delete(ReportingTableColumns.TABLE_NAME, whereClause, whereArgs);
    }

    public static void deleteAll(MPDatabase db) {
        db.delete(ReportingTableColumns.TABLE_NAME, null, null);
    }

    public static class ReportingMessage {
        private long mpid;
        private JSONObject msgObject;
        private String sessionId;
        private int reportingMessageId;

        private ReportingMessage(JSONObject msgObject, String sessionId, int reportingMessageId, long mpid) {
            this.msgObject = msgObject;
            this.sessionId = sessionId;
            this.reportingMessageId = reportingMessageId;
            this.mpid = mpid;
        }

        public Long getMpid() {
            return mpid;
        }

        public JSONObject getMsgObject() {
            return msgObject;
        }

        public String getSessionId() {
            return sessionId;
        }

        public int getReportingMessageId() {
            return reportingMessageId;
        }
    }
}
