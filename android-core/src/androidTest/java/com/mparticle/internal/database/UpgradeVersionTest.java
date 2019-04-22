package com.mparticle.internal.database;

import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.internal.database.tables.BaseTableTest;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;

import org.junit.Test;

public class UpgradeVersionTest extends BaseTableTest {
    private MParticleDatabaseHelper helper = new MParticleDatabaseHelper(InstrumentationRegistry.getContext());

    protected interface FcmMessageTableColumns {
        String CONTENT_ID = "content_id";
        String CAMPAIGN_ID = "campaign_id";
        String TABLE_NAME = "gcm_messages";
        String PAYLOAD = "payload";
        String CREATED_AT = "message_time";
        String DISPLAYED_AT = "displayed_time";
        String EXPIRATION = "expiration";
        String BEHAVIOR = "behavior";
        String APPSTATE = "appstate";
    }

    static final String CREATE_GCM_MSG_DDL =
            "CREATE TABLE IF NOT EXISTS " + FcmMessageTableColumns.TABLE_NAME + " (" + FcmMessageTableColumns.CONTENT_ID +
                    " INTEGER PRIMARY KEY, " +
                    FcmMessageTableColumns.PAYLOAD + " TEXT NOT NULL, " +
                    FcmMessageTableColumns.APPSTATE + " TEXT NOT NULL, " +
                    FcmMessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    FcmMessageTableColumns.EXPIRATION + " INTEGER NOT NULL, " +
                    FcmMessageTableColumns.BEHAVIOR + " INTEGER NOT NULL," +
                    FcmMessageTableColumns.CAMPAIGN_ID + " TEXT NOT NULL, " +
                    FcmMessageTableColumns.DISPLAYED_AT + " INTEGER NOT NULL " +
                    ");";

    @Test
    public void testDropFcmMessageTable() throws InterruptedException {
        //test to make sure it doesn't crash when there is an FcmMessages table to delete
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(CREATE_GCM_MSG_DDL);
                helper.onCreate(database);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                helper.onUpgrade(database, oldVersion, newVersion);
            }
        }, 7);
        deleteTestingDatabase();

        //test to make sure it doesn't crash when there is NO FcmMessages table to delete
        runTest(helper, 7);
    }
}
