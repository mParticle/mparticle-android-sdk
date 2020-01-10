package com.mparticle.internal.database;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.internal.database.tables.BaseTableTest;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.internal.database.tables.MessageTable;

import org.junit.Test;

public class UpgradeMessageTableTest extends BaseTableTest {
    private MParticleDatabaseHelper helper = new MParticleDatabaseHelper(InstrumentationRegistry.getContext());

    static final String CREATE_MESSAGES_DDL =
            "CREATE TABLE IF NOT EXISTS " + MessageTable.MessageTableColumns.TABLE_NAME + " (" + BaseColumns._ID +
                    " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MessageTable.MessageTableColumns.SESSION_ID + " STRING NOT NULL, " +
                    MessageTable.MessageTableColumns.API_KEY + " STRING NOT NULL, " +
                    MessageTable.MessageTableColumns.MESSAGE + " TEXT, " +
                    MessageTable.MessageTableColumns.STATUS + " INTEGER, " +
                    MessageTable.MessageTableColumns.CREATED_AT + " INTEGER NOT NULL, " +
                    MessageTable.MessageTableColumns.MESSAGE_TYPE + " TEXT, " +
                    MessageTable.MessageTableColumns.CF_UUID + " TEXT, " +
                    MessageTable.MessageTableColumns.MP_ID + " INTEGER" +
                    ");";

    @Test
    public void addMessageTableTest() throws InterruptedException {
        //test to make sure it doesn't crash when there is an FcmMessages table to delete
        runTest(new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL(CREATE_MESSAGES_DDL);
                helper.onCreate(database);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                helper.onUpgrade(database, oldVersion, newVersion);
            }

            @Override
            public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                helper.onDowngrade(database, oldVersion, newVersion);
            }
        }, 9);
        deleteTestingDatabase();

        //test to make sure it doesn't crash when there is NO FcmMessages table to delete
        runTest(helper, 8);
    }
}
