package com.mparticle.internal.database.services.mp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPMessage;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.database.tables.mp.MessageTable;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageService extends MessageTable {

    private final static String[] prepareSelection = new String[]{"_id", MessageTableColumns.MESSAGE, MessageTableColumns.CREATED_AT, MessageTableColumns.STATUS, MessageTableColumns.SESSION_ID};
    private final static String prepareOrderBy =  MessageTableColumns._ID + " asc";

    private static String sessionHistorySelection = String.format(
            "(%s = %d) and (%s != ?) and (%s = ?)",
            MessageTableColumns.STATUS,
            Constants.Status.UPLOADED,
            MessageTableColumns.SESSION_ID,
            MessageTableColumns.MP_ID);

    public static List<ReadyMessage> getSessionHistory(SQLiteDatabase database, String currentSessionId, long mpId){
        String[] selectionArgs = new String[]{currentSessionId, String.valueOf(mpId)};
        Cursor readyMessagesCursor = null;
        List<ReadyMessage> readyMessages = new ArrayList<ReadyMessage>();
        try {
            readyMessagesCursor = database.query(
                    MessageTableColumns.TABLE_NAME,
                    prepareSelection,
                    sessionHistorySelection,
                    selectionArgs,
                    null,
                    null,
                    prepareOrderBy, "100");
            int messageIdIndex = readyMessagesCursor.getColumnIndex(MessageTableColumns._ID);
            int messageIndex = readyMessagesCursor.getColumnIndex(MessageTableColumns.MESSAGE);
            int sessionIdIndex = readyMessagesCursor.getColumnIndex(MessageTableColumns.SESSION_ID);
            while (readyMessagesCursor.moveToNext()) {
                String sessionId = readyMessagesCursor.getString(sessionIdIndex);
                int messageId = readyMessagesCursor.getInt(messageIdIndex);
                String message = readyMessagesCursor.getString(messageIndex);
                readyMessages.add(new ReadyMessage(sessionId, messageId, message));
            }
        }
        finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()) {
                readyMessagesCursor.close();
            }
        }
        return readyMessages;
    }

    public static int deleteOldMessages(SQLiteDatabase database, String currentSessionId, long mpId){
        String[] selectionArgs = new String[]{currentSessionId, String.valueOf(mpId)};
        return database.delete(
                MessageTableColumns.TABLE_NAME,
                sessionHistorySelection,
                selectionArgs);
    }

    public static List<ReadyMessage> getMessagesForUpload(SQLiteDatabase database, long mpId){
        Cursor readyMessagesCursor = null;
        List<ReadyMessage> readyMessages = new ArrayList<ReadyMessage>();
        try {
            readyMessagesCursor = database.query(
                    MessageTableColumns.TABLE_NAME,
                    null,
                    MessageTableColumns.STATUS + " != ? and " + MessageTableColumns.CREATED_AT + " < " + System.currentTimeMillis() + " and " + MessageTableColumns.MP_ID + " = ?",
                    new String[]{Integer.toString(Constants.Status.UPLOADED), String.valueOf(mpId)},
                    null,
                    null,
                    prepareOrderBy, "100");
            int messageIdIndex = readyMessagesCursor.getColumnIndex(MessageTableColumns._ID);
            int messageIndex = readyMessagesCursor.getColumnIndex(MessageTableColumns.MESSAGE);
            int sessionIdIndex = readyMessagesCursor.getColumnIndex(MessageTableColumns.SESSION_ID);
            while (readyMessagesCursor.moveToNext()) {
                String sessionId = readyMessagesCursor.getString(sessionIdIndex);
                int messageId = readyMessagesCursor.getInt(messageIdIndex);
                String message = readyMessagesCursor.getString(messageIndex);
                readyMessages.add(new ReadyMessage(sessionId, messageId, message));
            }
        }
        finally {
            if (readyMessagesCursor != null && !readyMessagesCursor.isClosed()) {
                readyMessagesCursor.close();
            }
        }
        return readyMessages;
    }

    public static int cleanupMessages(SQLiteDatabase database, long mpId) {
        return database.delete(MessageTableColumns.TABLE_NAME, "length(" + MessageTableColumns.MESSAGE + ") > " + Constants.LIMIT_MAX_MESSAGE_SIZE + " and " + MessageTableColumns.MP_ID + " = ?", new String[]{String.valueOf(mpId)});
    }


    /**
     * The following get*Query methods were once static fields, but in order to save on app startup time, they're
     * now created as needed.
     */
    /**
     * The beginning of the delete query used to clear the uploads table after a successful upload.
     */
    private static String getDeletableMessagesQuery() {
        return String.format(
                "(%s='NO-SESSION')",
                MessageTableColumns.SESSION_ID);
    }

    public static int markMessagesAsUploaded(SQLiteDatabase database, int messageId, long mpId) {
        String[] whereArgs = new String[]{Integer.toString(messageId), String.valueOf(mpId)};
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTableColumns.STATUS, Constants.Status.UPLOADED);
        return database.update(MessageTableColumns.TABLE_NAME, contentValues, MessageTableColumns._ID + " <= ? and " + MessageTableColumns.MP_ID + " = ? ", whereArgs);
    }

    /**
     * Delete a message that has been uploaded in session history
     */
    public static int deleteMessages(SQLiteDatabase database, int messageId, long mpId) {
        String[] whereArgs = new String[]{Integer.toString(messageId), String.valueOf(mpId)};
        return database.delete(MessageTableColumns.TABLE_NAME, MessageTableColumns._ID + " <= ? and " + MessageTableColumns.MP_ID + " = ?", whereArgs);
    }

    public static void insertMessage(SQLiteDatabase db, String apiKey, MPMessage message, long mpId) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MessageTableColumns.API_KEY, apiKey);
        contentValues.put(MessageTableColumns.CREATED_AT, message.getLong(Constants.MessageKey.TIMESTAMP));
        String sessionID = message.getSessionId();
        contentValues.put(MessageTableColumns.SESSION_ID, sessionID);
        contentValues.put(MessageTableColumns.MP_ID, mpId);
        if (Constants.NO_SESSION_ID.equals(sessionID)) {
            message.remove(Constants.MessageKey.SESSION_ID);
        }
        String messageString = message.toString();
        if (messageString.length() > Constants.LIMIT_MAX_MESSAGE_SIZE) {
            Logger.error("Message logged of size " + messageString.length() + " that exceeds maximum safe size of " + Constants.LIMIT_MAX_MESSAGE_SIZE + " bytes.");
            return;
        }
        contentValues.put(MessageTableColumns.MESSAGE, messageString);

        if (message.getString(Constants.MessageKey.TYPE) == Constants.MessageType.FIRST_RUN) {
            // Force the first run message to be parsed immediately
            contentValues.put(MessageTableColumns.STATUS, Constants.Status.BATCH_READY);
        } else {
            contentValues.put(MessageTableColumns.STATUS, Constants.Status.READY);
        }

        db.insert(MessageTableColumns.TABLE_NAME, null, contentValues);
    }

    public static class ReadyMessage {
        private String sessionId;
        private int messageId;
        private String message;

        private ReadyMessage(String sessionId, int messageId, String message) {
            this.sessionId = sessionId;
            this.messageId = messageId;
            this.message = message;
        }

        public String getSessionId() {
            return sessionId;
        }

        public int getMessageId() {
            return messageId;
        }

        public String getMessage() {
            return message;
        }
    }
}
