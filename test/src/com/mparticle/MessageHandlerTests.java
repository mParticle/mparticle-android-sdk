package com.mparticle;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mparticle.Constants.Status;
import com.mparticle.MessageDatabase.CommandTable;
import com.mparticle.MessageDatabase.MessageTable;
import com.mparticle.MessageDatabase.SessionTable;
import com.mparticle.MessageDatabase.UploadTable;

public class MessageHandlerTests extends AndroidTestCase {

    private static MessageDatabase sDB;
    private static MessageHandler sMessageHandler1, sMessageHandler2;
    private static MessageManager sMessageManager1, sMessageManager2;

    private static final int SLEEP_DELAY = 200;
    private static int sSessionCounter = 0;

    private String mSessionId;

    private static final String SQL_WHERE_STATUS = MessageTable.STATUS+"=?";
    private static final String[] SQL_ARGS_READY = new String[] {Integer.toString(Status.READY)};
    private static final String[] SQL_ARGS_BATCH_READY = new String[] {Integer.toString(Status.BATCH_READY)};

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSessionId = "session-" + sSessionCounter++;
        if (null==sDB) {
            initalSetup();
        }
    }

    private void initalSetup() throws Exception {
        sDB = new MessageDatabase(getContext());

        sMessageHandler1 = new MessageHandler(getContext(), Looper.getMainLooper(), "test-api-1");
        sMessageManager1 = new MessageManager(sMessageHandler1,null);
        sMessageHandler2 = new MessageHandler(getContext(), Looper.getMainLooper(), "test-api-2");
        sMessageManager2 = new MessageManager(sMessageHandler2,null);

        clearDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        clearDatabase();
        sDB.close();
    }

    private void clearDatabase() {
        SQLiteDatabase db = sDB.getWritableDatabase();
        db.delete(SessionTable.TABLE_NAME, null,  null);
        db.delete(MessageTable.TABLE_NAME, null,  null);
        db.delete(UploadTable.TABLE_NAME, null,  null);
        db.delete(CommandTable.TABLE_NAME, null,  null);
    }

    // store a "ss" message and also create a session
    // - status should be READY
    public void testStoreSessionStart() throws InterruptedException  {

        sMessageManager1.startSession(mSessionId, 1000, null);

        Thread.sleep(SLEEP_DELAY);
        while (sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        assertEquals(mSessionId,  sessionsCursor.getString(0));

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, messagesCursor.getCount());
        while (messagesCursor.moveToNext()) {
            assertEquals(mSessionId,  messagesCursor.getString(0));
            assertEquals(Status.READY,  messagesCursor.getInt(1));
        }

    }

    // store an "e" message and also update the session with the "e" timestamp
    // - status should be READY
    public void testStoreSessionEvent() throws InterruptedException {

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 2000, "event1", null);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 3000, "event2", null);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 4000, "event3", null);

        while (sMessageHandler1.mIsProcessingMessage || sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID, SessionTable.END_TIME, SessionTable.SESSION_LENGTH };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        assertEquals(mSessionId,  sessionsCursor.getString(0));
        assertEquals(4000, sessionsCursor.getLong(1));
        assertEquals(0, sessionsCursor.getLong(2));

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(4, messagesCursor.getCount());
        while (messagesCursor.moveToNext()) {
            assertEquals(mSessionId,  messagesCursor.getString(0));
            assertEquals(Status.READY,  messagesCursor.getInt(1));
        }

    }

    // session attributes should be updated
    public void testUpdateSessionAttributes() throws JSONException, InterruptedException {

        JSONObject sessionAttrsJSON = new JSONObject("{key1:val1,key2:val2}");

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.setSessionAttributes(mSessionId, sessionAttrsJSON);

        while (sMessageHandler1.mIsProcessingMessage || sMessageHandler1.hasMessages(MessageHandler.UPDATE_SESSION_ATTRIBUTES)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID, SessionTable.SESSION_LENGTH, SessionTable.END_TIME, SessionTable.ATTRIBUTES };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        JSONObject storedAttrs = new JSONObject(sessionsCursor.getString(3));
        assertEquals(storedAttrs.toString(), sessionAttrsJSON.toString());

    }

    // session length and session end should be updated - no messages added
    public void testSessionStop() throws InterruptedException {

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 2000, "event1", null);
        sMessageManager1.stopSession(mSessionId, 3000, 250);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 4000, "event2", null);
        sMessageManager1.stopSession(mSessionId, 5000, 3500);

        while ( sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE) ||
                sMessageHandler1.hasMessages(MessageHandler.UPDATE_SESSION_END ) ) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID, SessionTable.END_TIME, SessionTable.SESSION_LENGTH };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        assertEquals(mSessionId,  sessionsCursor.getString(0));
        assertEquals(5000, sessionsCursor.getLong(1));
        assertEquals(3500, sessionsCursor.getLong(2));

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(3, messagesCursor.getCount());
    }

    // session-end message should be added, messages marked as batch-ready
    public void testSessionEnd() throws InterruptedException {
        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 2000, "event1", null);
        sMessageManager1.stopSession(mSessionId, 3000, 1500);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 4000, "event2", null);
        sMessageManager1.endSession(mSessionId, 5000, 2500);

        while ( sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE) ||
                sMessageHandler1.hasMessages(MessageHandler.UPDATE_SESSION_END ) ||
                sMessageHandler1.hasMessages(MessageHandler.CREATE_SESSION_END_MESSAGE ) ) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID, SessionTable.END_TIME, SessionTable.SESSION_LENGTH };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(0, sessionsCursor.getCount());

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(4, messagesCursor.getCount());
    }

    // un-ended sessions should be ended
    public void testEndOrphans() throws JSONException, InterruptedException {

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logCustomEvent(mSessionId, 1000, 2000, "event1", null);

        sMessageManager2.startSession(mSessionId+"-2", 1500, null);
        sMessageManager2.logCustomEvent(mSessionId+"-2", 1500, 2500, "event1", null);

        while ( sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE) ||
                sMessageHandler2.hasMessages(MessageHandler.STORE_MESSAGE) ) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        sMessageHandler1.sendEmptyMessage(MessageHandler.END_ORPHAN_SESSIONS);

        while ( sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.END_ORPHAN_SESSIONS) ||
                sMessageHandler1.hasMessages(MessageHandler.CREATE_SESSION_END_MESSAGE) ) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        SQLiteDatabase db = sDB.getReadableDatabase();

        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, null, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());

        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, null, null, null, null, null, null);
        assertEquals(5, messagesCursor.getCount());

        Cursor readyMessages = db.query(MessageTable.TABLE_NAME, null,
                SQL_WHERE_STATUS, SQL_ARGS_READY, null, null, null);
        assertEquals(2, readyMessages.getCount());

        Cursor batchReadyMessages = db.query(MessageTable.TABLE_NAME, null,
                SQL_WHERE_STATUS, SQL_ARGS_BATCH_READY, null, null, null);
        assertEquals(3, batchReadyMessages.getCount());

    }

}
