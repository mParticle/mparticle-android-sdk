package com.mparticle.internal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.os.Message;

import com.mparticle.*;
import com.mparticle.AccessUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MParticleDatabaseTest extends BaseCleanInstallEachTest {
    private static String mApiKey = "apiKeyTest";
    private MessageManager mMessageManager;

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {
        //set database instance with a v6 version
        MParticle.start(mContext, mApiKey, "secret");
        mMessageManager = AccessUtils.getMessageManager();
    }



    /**
     * this had to be hacked together, with the custom UploadHandler instance that will insert
     * a corrupted message into the Upload table. The test is to see if the MParticleApiImpl will
     * detect and migrate the good data
     * @throws Exception
     */
    @Test
    public void testDetectCorruptUploadsTable() throws Exception {
        //replace the MessageHandler and the UploadHandler with our legacy, subclassed versions, which adds "api_key" to stay in line with the old database requirements
        mMessageManager.mUploadHandler = new NaughtyUploadHandler(mContext, MParticle.getInstance().getConfigManager(), MParticle.getInstance().getAppStateManager(), mMessageManager);
        mMessageManager.mUploadHandler.setApiClient(new MParticleApiClientImpl(MParticle.getInstance().getConfigManager(), mContext.getSharedPreferences("",Context.MODE_PRIVATE), mContext) {
            @Override
            public void fetchConfig() throws IOException, MPConfigException {
                //do nothing
            }

            @Override
            public int sendMessageBatch(String message) throws IOException, MPThrottleException, MPRampException {
                return 429;
            }
        });
        assertEquals(Utils.getTableEntries(MParticleDatabase.UploadTable.TABLE_NAME).size(), 0);
        assertFalse(MParticle.getInstance().getConfigManager().hasRebuiltUploadsTable());

        //load test data into the database, wait for it all to go through the MessageHandler (and into the "messages" database),
        // then call the method in the UploadHandler which will convert the "messages" to "uploads". after this we should have a number
        //
        for (int i = 0; i < 10; i++) {
            for (int j =0; j < Utils.getInstance().randomInt(1,5); j++) {
                MParticle.getInstance().logEvent(Utils.getInstance().getRandomMPEventRich());
            }
            Utils.awaitStoreMessage();
            //hackto instantiate database in UploadHandler.. see LegacyUploadHandler
            mMessageManager.mUploadHandler.handleMessage(mMessageManager.mUploadHandler.obtainMessage(-1, -1000, -1000));
            mMessageManager.mUploadHandler.prepareMessageUploads(false);
        }

        //instread of having the uploadHandler send a message, just simulate a bad message being sent
        //this message should be rejected and trigger the rebuilding of the database
        mMessageManager.mUploadHandler.upload(false);

        //this copy of the uploads table should basically include no
        List<JSONObject> uploadsTableEntries = Utils.getTableEntries(MParticleDatabase.UploadTable.TABLE_NAME);

        assertTrue(MParticle.getInstance().getConfigManager().hasRebuiltUploadsTable());

        assertEquals(Utils.getTableEntries(MParticleDatabase.UploadTable.TABLE_NAME).size(), 0);

        //load more data, just make sure the database is writeable
        for (int i = 0; i < 10; i++) {
            for (int j =0; j < Utils.getInstance().randomInt(1,5); j++) {
                MParticle.getInstance().logEvent(Utils.getInstance().getRandomMPEventRich());
            }
            Utils.awaitStoreMessage();
            //hackto instantiate database in UploadHandler.. see LegacyUploadHandler
            mMessageManager.mUploadHandler.handleMessage(mMessageManager.mUploadHandler.obtainMessage(-1, -1000, -1000));
            mMessageManager.mUploadHandler.prepareMessageUploads(false);
        }

        assertEquals(Utils.getTableEntries(MParticleDatabase.UploadTable.TABLE_NAME).size(), 10);
        assertTrue(MParticle.getInstance().getConfigManager().hasRebuiltUploadsTable());
    }

    class NaughtyUploadHandler extends UploadHandler {
        SQLiteDatabase db;

        NaughtyUploadHandler(Context context, ConfigManager configManager, AppStateManager appStateManager, MessageManager messageManager) {
            super(context, Looper.getMainLooper(), configManager, mMessageManager.mUploadHandler.mDbHelper, appStateManager, messageManager);
            db = mMessageManager.mUploadHandler.mDbHelper.getWritableDatabase();
        }

        @Override
        void dbInsertUpload(MessageBatch message) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MParticleDatabase.UploadTable.API_KEY, mApiKey);
            contentValues.put(MParticleDatabase.UploadTable.CREATED_AT, message.optLong(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis()));
            //randomly, ~20% of the time, put the API key as the message instead of the actual message
            if (new Random().nextBoolean()) {
                contentValues.put(MParticleDatabase.UploadTable.MESSAGE, message.toString());
            } else {
                contentValues.put(MParticleDatabase.UploadTable.MESSAGE, mApiKey);
            }
            db.insert(MParticleDatabase.UploadTable.TABLE_NAME, null, contentValues);
        }

        @Override
        public void handleMessage(Message msg) {
            //this is a hack to populate the "db" field un UploadHandler..essentially a message that will do nothing
            if (msg.what == -1 && msg.arg1 == -1000 && msg.arg2 == -1000) {
                super.handleMessage(msg);
            }
            //do nothing
            // in this limited test, it is better if we just don't handle any messages, and
            // make the db calls explicitly
        }
    }
}
