package com.mparticle.internal.database;

import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import com.mparticle.internal.InternalSession;
import com.mparticle.internal.database.services.BreadcrumbService;
import com.mparticle.internal.database.services.MessageService;
import com.mparticle.internal.database.services.ReportingService;
import com.mparticle.internal.database.services.SQLiteOpenHelperWrapper;
import com.mparticle.internal.database.services.SessionService;
import com.mparticle.internal.database.services.UploadService;
import com.mparticle.internal.database.services.UserAttributesService;
import com.mparticle.internal.database.tables.BaseTableTest;
import com.mparticle.internal.database.tables.BreadcrumbTableTest;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.internal.database.tables.MessageTableTest;
import com.mparticle.internal.database.tables.MpIdDependentTable;
import com.mparticle.internal.database.tables.ReportingTableTest;
import com.mparticle.internal.database.tables.SessionTableTest;
import com.mparticle.internal.database.tables.UploadTable;
import com.mparticle.internal.database.tables.UserAttributeTableTest;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.testutils.MPLatch;
import com.mparticle.testutils.TestingUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class UpgradeVersionTest extends BaseTableTest {
    private MParticleDatabaseHelper helper = new MParticleDatabaseHelper(mContext);

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

            @Override
            public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                helper.onDowngrade(database, oldVersion, newVersion);
            }
        }, 7);
        deleteTestingDatabase();

        //test to make sure it doesn't crash when there is NO FcmMessages table to delete
        runTest(helper, 7);
    }

    @Test
    public void testDowngradeTable() throws InterruptedException, JSONException {

        //Open database and insert some values
        TestSQLiteOpenHelper baseDatabase = new TestSQLiteOpenHelper(helper, DB_NAME, Integer.MAX_VALUE);
        baseDatabase.onCreateLatch.await();
        MPDatabase db = new MPDatabaseImpl(baseDatabase.getWritableDatabase());

        BaseMPMessage message = new BaseMPMessage.Builder("test").build(new InternalSession(), new Location("New York City"), 1);

        BreadcrumbService.insertBreadcrumb(db, mContext, message, "key", 1l);
        MessageService.insertMessage(db, "key", message, 1L, "id", 1);
        ReportingService.insertReportingMessage(db, TestingUtils.getInstance().getRandomReportingMessage("123"), 1L);
        SessionService.insertSession(db, message, "key", "", "", 1L);
        UploadService.insertAliasRequest(db, "key", new JSONObject().put("key", "value"));
        UserAttributesService.insertAttribute(db, "key", "value", 1L, false, 1L);

        //test to make sure there are values in the database
        JSONObject databaseJSON = getDatabaseContents(db);
        assertEquals(6, databaseJSON.length());
        Iterator<String> databaseTables = databaseJSON.keys();
        while(databaseTables.hasNext()) {
            String tableName = databaseTables.next();
            assertEquals(tableName,1, databaseJSON.getJSONArray(tableName).length());
        }

        //reopen the database, make sure nothing happens on a normal install
        baseDatabase = new TestSQLiteOpenHelper(helper, DB_NAME, Integer.MAX_VALUE);
        db = new MPDatabaseImpl(baseDatabase.getWritableDatabase());

        //test to make sure the values are still in the database
        databaseJSON = getDatabaseContents(db);
        assertEquals(6, databaseJSON.length());
        databaseTables = databaseJSON.keys();
        while(databaseTables.hasNext()) {
            String tableName = databaseTables.next();
            assertEquals(tableName,1, databaseJSON.getJSONArray(tableName).length());
        }

        //downgrade the database
        baseDatabase = new TestSQLiteOpenHelper(helper, DB_NAME, 1);
        baseDatabase.getWritableDatabase();
        baseDatabase.onDowngradeLatch.await();
        db = new MPDatabaseImpl(baseDatabase.getWritableDatabase());

        //test to make sure the values where delete and the database is empty
        databaseJSON = getDatabaseContents(db);
        assertEquals(6, databaseJSON.length());
        databaseTables = databaseJSON.keys();
        while(databaseTables.hasNext()) {
            String tableName = databaseTables.next();
            assertEquals(tableName, 0, databaseJSON.getJSONArray(tableName).length());
        }
    }

    @Test
    public void testAddMpidColumns() throws InterruptedException, JSONException {
        SQLiteOpenHelperWrapper sqLiteOpenHelperWrapper  = new SQLiteOpenHelperWrapper() {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(SessionTableTest.old_CREATE_SESSION_DDL);
                db.execSQL(MessageTableTest.old_no_mpid_CREATE_MESSAGES_DDL);
                db.execSQL(BreadcrumbTableTest.old_CREATE_BREADCRUMBS_DDL);
                db.execSQL(ReportingTableTest.old_CREATE_REPORTING_DDL);
                db.execSQL(UserAttributeTableTest.old_CREATE_USER_ATTRIBUTES_DDL);
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                helper.onUpgrade(database, oldVersion, newVersion);
            }

            @Override
            public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
                helper.onDowngrade(database, oldVersion, newVersion);
            }
        };
        TestSQLiteOpenHelper sqLiteOpenHelper = new TestSQLiteOpenHelper(sqLiteOpenHelperWrapper, DB_NAME, 6);
        sqLiteOpenHelper.onCreateLatch.await();

        JSONObject databaseContents = getDatabaseSchema(sqLiteOpenHelper.getWritableDatabase());
        Iterator<String> keys = databaseContents.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            assertFalse(databaseContents.getJSONObject(key).has(MpIdDependentTable.MP_ID));
        }
        sqLiteOpenHelper = new TestSQLiteOpenHelper(sqLiteOpenHelperWrapper, DB_NAME, 8);
        sqLiteOpenHelper.getWritableDatabase();
        sqLiteOpenHelper.onUpgradeLatch.await();

        databaseContents = getDatabaseSchema(sqLiteOpenHelper.getWritableDatabase());
        keys = databaseContents.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            if (!key.equals("uploads")) {
                assertTrue(databaseContents.getJSONObject(key).has(MpIdDependentTable.MP_ID));
            } else {
                assertFalse(databaseContents.getJSONObject(key).has(MpIdDependentTable.MP_ID));
            }
        }
    }
}
