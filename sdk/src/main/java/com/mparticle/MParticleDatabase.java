package com.mparticle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* package-private */class MParticleDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "mparticle.db";

    public interface SessionTable {
        public final static String TABLE_NAME = "sessions";
        public final static String SESSION_ID = "session_id";
        public final static String API_KEY = "api_key";
        public final static String START_TIME = "start_time";
        public final static String END_TIME = "end_time";
        public final static String SESSION_LENGTH = "session_length";
        public final static String ATTRIBUTES = "attributes";
        public final static String CF_UUID = "cfuuid";
    }

    public interface BreadcrumbTable {
        public final static String TABLE_NAME = "breadcrumbs";
        public final static String SESSION_ID = "session_id";
        public final static String API_KEY = "api_key";
        public final static String MESSAGE = "message";
        public final static String CREATED_AT = "breadcrumb_time";
        public final static String CF_UUID = "cfuuid";
    }

    private static final String CREATE_BREADCRUMBS_DDL =
            "CREATE TABLE " + BreadcrumbTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    BreadcrumbTable.SESSION_ID + " STRING NOT NULL, " +
                    BreadcrumbTable.API_KEY + " STRING NOT NULL, " +
                    BreadcrumbTable.MESSAGE + " TEXT, " +
                    BreadcrumbTable.CREATED_AT + " INTEGER NOT NULL, " +
                    BreadcrumbTable.CF_UUID + " TEXT" +
                    ");";


    private static final String CREATE_SESSIONS_DDL =
            "CREATE TABLE " + SessionTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SessionTable.SESSION_ID + " STRING NOT NULL, " +
                    SessionTable.API_KEY + " STRING NOT NULL, " +
                    SessionTable.START_TIME + " INTEGER NOT NULL," +
                    SessionTable.END_TIME + " INTEGER NOT NULL," +
                    SessionTable.SESSION_LENGTH + " INTEGER NOT NULL," +
                    SessionTable.ATTRIBUTES + " TEXT, " +
                    SessionTable.CF_UUID + " TEXT" +
                    ");";


    public interface MessageTable {
        public final static String TABLE_NAME = "messages";
        public final static String SESSION_ID = "session_id";
        public final static String API_KEY = "api_key";
        public final static String MESSAGE = "message";
        public final static String STATUS = "upload_status";
        public final static String CREATED_AT = "message_time";
        public final static String MESSAGE_TYPE = "message_type";
        public final static String CF_UUID = "cfuuid";
    }

    private static final String CREATE_MESSAGES_DDL =
            "CREATE TABLE " + MessageTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MessageTable.SESSION_ID + " STRING NOT NULL, " +
                    MessageTable.API_KEY + " STRING NOT NULL, " +
                    MessageTable.MESSAGE + " TEXT, " +
                    MessageTable.STATUS + " INTEGER, " +
                    MessageTable.CREATED_AT + " INTEGER NOT NULL, " +
                    MessageTable.MESSAGE_TYPE + " TEXT, " +
                    MessageTable.CF_UUID + " TEXT" +
                    ");";

    public interface UploadTable {
        public final static String TABLE_NAME = "uploads";
        public final static String API_KEY = "api_key";
        public final static String MESSAGE = "message";
        public final static String CREATED_AT = "message_time";
        public final static String CF_UUID = "cfuuid";
        public final static String SESSION_ID = "session_id";
    }

    private static final String CREATE_UPLOADS_DDL =
            "CREATE TABLE " + UploadTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UploadTable.API_KEY + " STRING NOT NULL, " +
                    UploadTable.MESSAGE + " TEXT, " +
                    UploadTable.CREATED_AT + " INTEGER NOT NULL, " +
                    UploadTable.CF_UUID + " TEXT, " +
                    UploadTable.SESSION_ID + " TEXT" +
                    ");";

    public interface CommandTable {
        public final static String TABLE_NAME = "commands";
        public final static String URL = "url";
        public final static String METHOD = "method";
        public final static String POST_DATA = "post_data";
        public final static String HEADERS = "headers";
        public final static String CREATED_AT = "timestamp";
        public final static String SESSION_ID = "session_id";
        public final static String API_KEY = "api_key";
        public final static String CF_UUID = "cfuuid";
    }

    private static final String CREATE_COMMANDS_DDL =
            "CREATE TABLE " + CommandTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    CommandTable.URL + " STRING NOT NULL, " +
                    CommandTable.METHOD + " STRING NOT NULL, " +
                    CommandTable.POST_DATA + " TEXT, " +
                    CommandTable.HEADERS + " TEXT, " +
                    CommandTable.CREATED_AT + " INTEGER, " +
                    CommandTable.SESSION_ID + " TEXT, " +
                    CommandTable.API_KEY + " STRING NOT NULL, " +
                    CommandTable.CF_UUID + " TEXT" +
                    ");";

    public MParticleDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SESSIONS_DDL);
        db.execSQL(CREATE_MESSAGES_DDL);
        db.execSQL(CREATE_UPLOADS_DDL);
        db.execSQL(CREATE_COMMANDS_DDL);
        db.execSQL(CREATE_BREADCRUMBS_DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // just blow away the old tables
        db.execSQL("DROP TABLE IF EXISTS " + SessionTable.TABLE_NAME);
        db.execSQL(CREATE_SESSIONS_DDL);
        db.execSQL("DROP TABLE IF EXISTS " + MessageTable.TABLE_NAME);
        db.execSQL(CREATE_MESSAGES_DDL);
        db.execSQL("DROP TABLE IF EXISTS " + UploadTable.TABLE_NAME);
        db.execSQL(CREATE_UPLOADS_DDL);
        db.execSQL("DROP TABLE IF EXISTS " + CommandTable.TABLE_NAME);
        db.execSQL(CREATE_COMMANDS_DDL);
    }

}
