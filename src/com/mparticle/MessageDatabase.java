package com.mparticle;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MessageDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "mparticle.db";

    public interface MessageTable {
    	public final static String MESSAGE_TYPE = "message_type";
    	public final static String UUID = "uuid";
    	public final static String MESSAGE_TIME = "message_time";
    	public final static String MESSAGE = "message";
    }
    private static final String CREATE_MESSAGES_DDL =
    	       "CREATE TABLE messages (" +
    	          "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
    	          MessageTable.MESSAGE_TYPE + " STRING NOT NULL," +
    	          MessageTable.UUID + " STRING NOT NULL, " +
    	          MessageTable.MESSAGE_TIME + " INTEGER NOT NULL," +
    	          MessageTable.MESSAGE + " TEXT" +
    	        ");";

    public MessageDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MESSAGES_DDL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS messages");
        db.execSQL(CREATE_MESSAGES_DDL);
	}

}
