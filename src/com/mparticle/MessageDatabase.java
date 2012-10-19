package com.mparticle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MessageDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 2;
    private static final String DB_NAME = "mparticle.db";

    public interface MessageTable {
        public final static String MESSAGE_TYPE = "message_type";
        public final static String SESSION_ID = "session_id";
        public final static String UUID = "uuid";
        public final static String MESSAGE_TIME = "message_time";
        public final static String MESSAGE = "message";
        public final static String UPLOAD_STATUS = "upload_status";
    }
    private static final String CREATE_MESSAGES_DDL =
               "CREATE TABLE messages (" +
                  "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                  MessageTable.MESSAGE_TYPE + " STRING NOT NULL," +
                  MessageTable.SESSION_ID + " STRING NOT NULL, " +
                  MessageTable.UUID + " STRING NOT NULL, " +
                  MessageTable.MESSAGE_TIME + " INTEGER NOT NULL," +
                  MessageTable.MESSAGE + " TEXT," +
                  MessageTable.UPLOAD_STATUS + " INTEGER" +
                ");";

    public interface SessionTable {
        public final static String SESSION_ID = "session_id";
        public final static String START_TIME = "start_time";
        public final static String END_TIME = "start_time";
        public final static String ATTRIBUTES = "attributes";
        public final static String UPLOAD_STATUS = "upload_status";
    }
    private static final String CREATE_SESSIONS_DDL =
            "CREATE TABLE sessions (" +
               "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
               SessionTable.SESSION_ID + " STRING NOT NULL, " +
               SessionTable.START_TIME + " INTEGER NOT NULL," +
               SessionTable.END_TIME + " INTEGER NOT NULL," +
               SessionTable.ATTRIBUTES + " TEXT," +
               SessionTable.UPLOAD_STATUS + " INTEGER" +
             ");";

    public MessageDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MESSAGES_DDL);
        db.execSQL(CREATE_SESSIONS_DDL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL(CREATE_MESSAGES_DDL);
        db.execSQL("DROP TABLE IF EXISTS sessions");
        db.execSQL(CREATE_SESSIONS_DDL);
    }

}
