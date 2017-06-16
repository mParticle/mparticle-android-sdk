package com.mparticle.internal.database.services.mp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.JsonReportingMessage;
import com.mparticle.internal.database.tables.mp.ReportingTable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ReportingService extends ReportingTable {

    public static void insertReportingMessage(SQLiteDatabase db, JsonReportingMessage message) {
        ContentValues values = new ContentValues();
        values.put(ReportingTableColumns.CREATED_AT, message.getTimestamp());
        values.put(ReportingTableColumns.MODULE_ID, message.getModuleId());
        values.put(ReportingTableColumns.MESSAGE, message.toJson().toString());
        values.put(ReportingTableColumns.SESSION_ID, message.getSessionId());
        db.insert(ReportingTableColumns.TABLE_NAME, null, values);
    }

    public static List<ReportingMessage> getReportingMessagesForUpload(SQLiteDatabase database) throws JSONException {
        List<ReportingMessage> reportingMessages = new ArrayList<ReportingMessage>();
        Cursor reportingMessageCursor = null;
        try {
            reportingMessageCursor = database.query(
                    ReportingTableColumns.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    ReportingTableColumns._ID + " asc");
            int reportingMessageIdIndex = reportingMessageCursor.getColumnIndex(ReportingTableColumns._ID);
            while (reportingMessageCursor.moveToNext()) {
                JSONObject msgObject = new JSONObject(
                        reportingMessageCursor.getString(
                                reportingMessageCursor.getColumnIndex(ReportingTableColumns.MESSAGE)
                        )
                );
                String sessionId = reportingMessageCursor.getString(
                        reportingMessageCursor.getColumnIndex(ReportingTableColumns.SESSION_ID)
                );
                int reportingMessageId = reportingMessageCursor.getInt(reportingMessageIdIndex);
                reportingMessages.add(new ReportingMessage(msgObject, sessionId, reportingMessageId));
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
    public static void dbMarkAsUploadedReportingMessage(SQLiteDatabase database, int lastMessageId) {
        String messageId = Long.toString(lastMessageId);
        String[] whereArgs = new String[]{messageId};
        String whereClause = "_id =?";
        database.delete(ReportingTableColumns.TABLE_NAME, whereClause, whereArgs);
    }

    public static class ReportingMessage {
        private JSONObject msgObject;
        private String sessionId;
        private int reportingMessageId;

        private ReportingMessage(JSONObject msgObject, String sessionId, int reportingMessageId) {
            this.msgObject = msgObject;
            this.sessionId = sessionId;
            this.reportingMessageId = reportingMessageId;
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
