package com.mparticle;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.test.AndroidTestCase;
import android.util.Log;

import com.mparticle.Constants.MessageKey;
import com.mparticle.Constants.Status;
import com.mparticle.MParticleAPI.EventType;
import com.mparticle.MParticleAPI.IdentityType;
import com.mparticle.MParticleDatabase.CommandTable;
import com.mparticle.MParticleDatabase.MessageTable;
import com.mparticle.MParticleDatabase.SessionTable;
import com.mparticle.MParticleDatabase.UploadTable;

public class UploadHandlerTests extends AndroidTestCase {

    private static MParticleDatabase sDB;
    private static MessageHandler sMessageHandler1, sMessageHandler2;
    private static UploadHandler sUploadHandler1, sUploadHandler2;
    private static MessageManager sMessageManager1, sMessageManager2;
    private static MParticleAPI sMParticleAPI;

    private static final int SLEEP_DELAY = 200;
    private static int sSessionCounter = 0;

    private String mSessionId;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSessionId = "session-" + sSessionCounter++;
        if (null == sDB) {
            initalSetup();
        }
    }

    private void initalSetup() throws Exception {
        sDB = new MParticleDatabase(getContext());

        sMParticleAPI = new MParticleAPI(getContext(), "test-api-1", sMessageManager1);
        
        sMessageHandler1 = new MessageHandler(getContext(), Looper.getMainLooper(), "test-api-1");
        sUploadHandler1 = new UploadHandler(getContext(), Looper.getMainLooper(), "test-api-1", "secret1");
        sMessageManager1 = new MessageManager(sMessageHandler1, sUploadHandler1);

        sMessageHandler2 = new MessageHandler(getContext(), Looper.getMainLooper(), "test-api-2");
        sUploadHandler2 = new UploadHandler(getContext(), Looper.getMainLooper(), "test-api-2", "secret2");
        sMessageManager2 = new MessageManager(sMessageHandler2, sUploadHandler2);

        clearDatabase();
    }

    @Override
    protected void tearDown() throws Exception {
        clearDatabase();
        sDB.close();
    }

    private void clearDatabase() {
        SQLiteDatabase db = sDB.getWritableDatabase();
        db.delete(SessionTable.TABLE_NAME, null, null);
        db.delete(MessageTable.TABLE_NAME, null, null);
        db.delete(UploadTable.TABLE_NAME, null, null);
        db.delete(CommandTable.TABLE_NAME, null, null);
    }

    // only process messages from ended sessions
    public void testPrepareUploadsBatch() throws InterruptedException, JSONException {

    	sMParticleAPI.setUserIdentity("tbreffni@mparticle.com", IdentityType.MICROSOFT);
    	
        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logEvent(mSessionId, 1000, 2000, "event1", EventType.UserContent, null);
        sMessageManager1.endSession(mSessionId, 3000, 2000);
        sMessageManager1.startSession(mSessionId + "-2", 4000, null);
        sMessageManager1.logEvent(mSessionId + "-2", 4000, 5000, "event1", EventType.UserContent, null);

        while (sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        sUploadHandler1.setUploadMode(Status.BATCH_READY);
        sUploadHandler1.prepareUploads();

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        assertEquals(mSessionId + "-2", sessionsCursor.getString(0));

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(2, messagesCursor.getCount());
        while (messagesCursor.moveToNext()) {
            assertEquals(mSessionId + "-2", messagesCursor.getString(0));
            assertEquals(Status.READY, messagesCursor.getInt(1));
        }

        columns = new String[] { UploadTable.MESSAGE };
        Cursor uploadsCursor = db.query(UploadTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, uploadsCursor.getCount());
        uploadsCursor.moveToFirst();
        JSONObject uploadMessage = new JSONObject(uploadsCursor.getString(0));
        assertEquals(3, uploadMessage.getJSONArray(MessageKey.MESSAGES).length());
        assertEquals("tbreffni@mparticle.com", ((JSONObject)uploadMessage.getJSONArray(MessageKey.USER_IDENTITIES).get(0)).get(MessageKey.IDENTITY_VALUE));
    }

    // process all messages from active and ended sessions
    public void testPrepareUploadsStream() throws InterruptedException, JSONException {

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logEvent(mSessionId, 1000, 2000, "event1", EventType.UserContent, null);
        sMessageManager1.endSession(mSessionId, 3000, 2000);
        sMessageManager1.startSession(mSessionId + "-2", 4000, null);
        sMessageManager1.logEvent(mSessionId + "-2", 4000, 5000, "event1", EventType.UserContent, null);

        while (sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        sUploadHandler1.setUploadMode(Status.READY);
        sUploadHandler1.prepareUploads();

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        assertEquals(mSessionId + "-2", sessionsCursor.getString(0));

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(0, messagesCursor.getCount());

        columns = new String[] { UploadTable.MESSAGE };
        Cursor uploadsCursor = db.query(UploadTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, uploadsCursor.getCount());
        uploadsCursor.moveToFirst();
        JSONObject uploadMessage = new JSONObject(uploadsCursor.getString(0));
        assertEquals(5, uploadMessage.getJSONArray(MessageKey.MESSAGES).length());

    }

    // process messages without sessions immediately, even in batch mode
    public void testPrepareUploadsMixed() throws InterruptedException, JSONException {

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager1.logEvent(mSessionId, 1000, 2000, "event1", EventType.UserContent, null);
        sMessageManager1.setPushRegistrationId("token1", true);
        sMessageManager1.logEvent(mSessionId, 1000, 3000, "event2", EventType.UserContent, null);

        while (sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        sUploadHandler1.setUploadMode(Status.BATCH_READY);
        sUploadHandler1.prepareUploads();

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, sessionsCursor.getCount());
        sessionsCursor.moveToFirst();
        assertEquals(mSessionId, sessionsCursor.getString(0));

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(3, messagesCursor.getCount());
        while (messagesCursor.moveToNext()) {
            assertEquals(mSessionId, messagesCursor.getString(0));
            assertEquals(Status.READY, messagesCursor.getInt(1));
        }

        columns = new String[] { UploadTable.MESSAGE };
        Cursor uploadsCursor = db.query(UploadTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(1, uploadsCursor.getCount());
        uploadsCursor.moveToFirst();
        JSONObject uploadMessage = new JSONObject(uploadsCursor.getString(0));
        assertEquals(1, uploadMessage.getJSONArray(MessageKey.MESSAGES).length());

    }

    // ensure inter-mixed messages between two SDK instances get processed
    // correctly
    public void testPrepareUploadsMultipleInstances() throws InterruptedException, JSONException {

        sMessageManager1.startSession(mSessionId, 1000, null);
        sMessageManager2.startSession(mSessionId + "-2", 1500, null);

        sMessageManager1.logEvent(mSessionId, 1000, 2000, "event1", EventType.UserContent, null);
        sMessageManager2.logEvent(mSessionId + "-2", 1500, 2500, "event1", EventType.UserContent, null);

        sMessageManager1.endSession(mSessionId, 3000, 2000);
        sMessageManager2.endSession(mSessionId + "-2", 3500, 2000);

        sMessageManager1.startSession(mSessionId + "-B", 4000, null);
        sMessageManager2.startSession(mSessionId + "-2B", 4500, null);

        sMessageManager1.logEvent(mSessionId + "-B", 4000, 5000, "event1", EventType.UserContent, null);
        sMessageManager2.logEvent(mSessionId + "-2B", 4500, 5500, "event1", EventType.UserContent, null);

        while (sMessageHandler1.mIsProcessingMessage ||
                sMessageHandler1.hasMessages(MessageHandler.STORE_MESSAGE) ||
                sMessageHandler2.mIsProcessingMessage ||
                sMessageHandler2.hasMessages(MessageHandler.STORE_MESSAGE)) {
            Log.d(Constants.LOG_TAG, "Still processing messages...");
            Thread.sleep(SLEEP_DELAY);
        }

        sUploadHandler1.setUploadMode(Status.BATCH_READY);
        sUploadHandler2.setUploadMode(Status.READY);
        sUploadHandler1.prepareUploads();
        sUploadHandler2.prepareUploads();

        SQLiteDatabase db = sDB.getReadableDatabase();

        String[] columns = new String[] { SessionTable.SESSION_ID };
        Cursor sessionsCursor = db.query(SessionTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(2, sessionsCursor.getCount());

        columns = new String[] { MessageTable.SESSION_ID, MessageTable.STATUS };
        Cursor messagesCursor = db.query(MessageTable.TABLE_NAME, columns, null, null, null, null, null);
        assertEquals(2, messagesCursor.getCount());
        while (messagesCursor.moveToNext()) {
            assertEquals(mSessionId + "-B", messagesCursor.getString(0));
            assertEquals(Status.READY, messagesCursor.getInt(1));
        }

        columns = new String[] { UploadTable.MESSAGE };
        String whereClause = UploadTable.API_KEY + "=?";
        Cursor uploadsCursor = db.query(UploadTable.TABLE_NAME, columns, whereClause, new String[] { "test-api-1" },
                null, null, null);
        assertEquals(1, uploadsCursor.getCount());
        uploadsCursor.moveToFirst();
        JSONObject uploadMessage = new JSONObject(uploadsCursor.getString(0));
        assertEquals(3, uploadMessage.getJSONArray(MessageKey.MESSAGES).length());

        uploadsCursor = db.query(UploadTable.TABLE_NAME, columns, whereClause, new String[] { "test-api-2" }, null,
                null, null);
        assertEquals(1, uploadsCursor.getCount());
        uploadsCursor.moveToFirst();
        uploadMessage = new JSONObject(uploadsCursor.getString(0));
        assertEquals(5, uploadMessage.getJSONArray(MessageKey.MESSAGES).length());
    }

}
