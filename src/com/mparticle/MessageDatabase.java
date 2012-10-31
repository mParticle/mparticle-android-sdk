package com.mparticle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// TODO: this should be package-private but is accessed from the demo
@SuppressWarnings("javadoc")
public class MessageDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 7;
    private static final String DB_NAME = "mparticle.db";

    public interface MessageTable {
        public final static String TABLE_NAME = "messages";
        public final static String MESSAGE_TYPE = "message_type";
        public final static String SESSION_ID = "session_id";
        public final static String UUID = "uuid";
        public final static String MESSAGE_TIME = "message_time";
        public final static String MESSAGE = "message";
        public final static String STATUS = "status";
    }
    private static final String CREATE_MESSAGES_DDL =
               "CREATE TABLE "+ MessageTable.TABLE_NAME+" (" +
                  "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                  MessageTable.MESSAGE_TYPE + " STRING NOT NULL," +
                  MessageTable.SESSION_ID + " STRING NOT NULL, " +
                  MessageTable.UUID + " STRING NOT NULL, " +
                  MessageTable.MESSAGE_TIME + " INTEGER NOT NULL," +
                  MessageTable.MESSAGE + " TEXT," +
                  MessageTable.STATUS + " INTEGER" +
                ");";

    public interface SessionTable {
        public final static String TABLE_NAME = "sessions";
        public final static String SESSION_ID = "session_id";
        public final static String START_TIME = "start_time";
        public final static String END_TIME = "end_time";
        public final static String SESSION_LENGTH = "session_length";
        public final static String ATTRIBUTES = "attributes";
        public final static String STATUS = "status";
    }
    private static final String CREATE_SESSIONS_DDL =
            "CREATE TABLE "+ SessionTable.TABLE_NAME+" (" +
               "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
               SessionTable.SESSION_ID + " STRING NOT NULL, " +
               SessionTable.START_TIME + " INTEGER NOT NULL," +
               SessionTable.END_TIME + " INTEGER NOT NULL," +
               SessionTable.SESSION_LENGTH + " INTEGER NOT NULL," +
               SessionTable.ATTRIBUTES + " TEXT," +
               SessionTable.STATUS + " INTEGER" +
             ");";

    public interface UploadTable {
        public final static String TABLE_NAME = "uploads";
        public final static String UPLOAD_ID = "upload_id";
        public final static String MESSAGE = "message";
        public final static String MESSAGE_TIME = "message_time";
        public final static String STATUS = "status";
    }
    private static final String CREATE_UPLOADS_DDL =
            "CREATE TABLE "+ UploadTable.TABLE_NAME+" (" +
               "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
               UploadTable.UPLOAD_ID + " STRING NOT NULL, " +
               UploadTable.MESSAGE + " TEXT," +
               UploadTable.MESSAGE_TIME + " INTEGER NOT NULL," +
               UploadTable.STATUS + " INTEGER" +
             ");";

    public interface CommandTable {
        public final static String TABLE_NAME = "commands";
        public final static String COMMAND_ID = "command_id";
        public final static String URL = "url";
        public final static String METHOD = "method";
        public final static String POST_DATA = "post_data";
        public final static String CLEAR_HEADERS = "clear_headers";
        public final static String HEADERS = "headers";
        public final static String STATUS = "status";
    }
    private static final String CREATE_COMMANDS_DDL =
            "CREATE TABLE "+ CommandTable.TABLE_NAME+" (" +
               "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
               CommandTable.COMMAND_ID + " STRING NOT NULL, " +
               CommandTable.URL + " STRING NOT NULL, " +
               CommandTable.METHOD + " STRING NOT NULL, " +
               CommandTable.POST_DATA + " TEXT, " +
               CommandTable.CLEAR_HEADERS + " INTEGER NOT NULL, " +
               CommandTable.HEADERS + " TEXT, " +
               CommandTable.STATUS + " INTEGER" +
             ");";

    public MessageDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MESSAGES_DDL);
        db.execSQL(CREATE_SESSIONS_DDL);
        db.execSQL(CREATE_UPLOADS_DDL);
        db.execSQL(CREATE_COMMANDS_DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MessageTable.TABLE_NAME);
        db.execSQL(CREATE_MESSAGES_DDL);
        db.execSQL("DROP TABLE IF EXISTS " + SessionTable.TABLE_NAME);
        db.execSQL(CREATE_SESSIONS_DDL);
        db.execSQL("DROP TABLE IF EXISTS " + UploadTable.TABLE_NAME);
        db.execSQL(CREATE_UPLOADS_DDL);
        db.execSQL("DROP TABLE IF EXISTS " + CommandTable.TABLE_NAME);
        db.execSQL(CREATE_COMMANDS_DDL);
    }

}
