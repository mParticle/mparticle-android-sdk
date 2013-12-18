package com.mparticle;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/* package-private */class MParticleDatabase extends SQLiteOpenHelper {

    private static final int DB_VERSION = 11;
    private static final String DB_NAME = "mparticle.db";

    public interface SessionTable10 {
        public final static String TABLE_NAME = "sessions";
        public final static String SESSION_ID = "session_id";
        public final static String API_KEY = "api_key";
        public final static String START_TIME = "start_time";
        public final static String END_TIME = "end_time";
        public final static String SESSION_LENGTH = "session_length";
        public final static String ATTRIBUTES = "attributes";
    }

    private static final String CREATE_SESSIONS_DDL10 =
            "CREATE TABLE " + SessionTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    SessionTable10.SESSION_ID + " STRING NOT NULL, " +
                    SessionTable10.API_KEY + " STRING NOT NULL, " +
                    SessionTable10.START_TIME + " INTEGER NOT NULL," +
                    SessionTable10.END_TIME + " INTEGER NOT NULL," +
                    SessionTable10.SESSION_LENGTH + " INTEGER NOT NULL," +
                    SessionTable10.ATTRIBUTES + " TEXT" +
                    ");";

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

    public interface MessageTable10 {
        public final static String TABLE_NAME = "messages";
        public final static String SESSION_ID = "session_id";
        public final static String API_KEY = "api_key";
        public final static String MESSAGE = "message";
        public final static String STATUS = "status";
        public final static String CREATED_AT = "created_at";
    }

    private static final String CREATE_MESSAGES_DDL10 =
            "CREATE TABLE " + MessageTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MessageTable10.SESSION_ID + " STRING NOT NULL, " +
                    MessageTable10.API_KEY + " STRING NOT NULL, " +
                    MessageTable10.MESSAGE + " TEXT," +
                    MessageTable10.STATUS + " INTEGER," +
                    MessageTable10.CREATED_AT + " INTEGER NOT NULL" +
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

    public interface UploadTable10 {
        public final static String TABLE_NAME = "uploads";
        public final static String API_KEY = "api_key";
        public final static String MESSAGE = "message";
        public final static String CREATED_AT = "created_at";
    }

    private static final String CREATE_UPLOADS_DDL10 =
            "CREATE TABLE " + UploadTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UploadTable10.API_KEY + " STRING NOT NULL, " +
                    UploadTable10.MESSAGE + " TEXT," +
                    UploadTable10.CREATED_AT + " INTEGER NOT NULL" +
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
                    UploadTable.CF_UUID + " STRING NOT NULL, " +
                    UploadTable.SESSION_ID + " STRING NOT NULL" +
                    ");";
    
    public interface CommandTable10 {
        public final static String TABLE_NAME = "commands";
        public final static String URL = "url";
        public final static String METHOD = "method";
        public final static String POST_DATA = "post_data";
        public final static String HEADERS = "headers";
        public final static String CREATED_AT = "created_at";
    }

    private static final String CREATE_COMMANDS_DDL10 =
            "CREATE TABLE " + CommandTable.TABLE_NAME + " (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    CommandTable10.URL + " STRING NOT NULL, " +
                    CommandTable10.METHOD + " STRING NOT NULL, " +
                    CommandTable10.POST_DATA + " TEXT, " +
                    CommandTable10.HEADERS + " TEXT, " +
                    CommandTable10.CREATED_AT + " INTEGER" +
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
                    CommandTable.SESSION_ID + " STRING NOT NULL, " +
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
    }

    private void copyDB(SQLiteDatabase db, String tablename, Cursor c, String[] columns10, String[] columns) {
		if ((c != null) && (c.getCount() > 0)) {
	        db.execSQL("DROP TABLE IF EXISTS " + tablename);
	        db.execSQL(CREATE_SESSIONS_DDL);
	        c.moveToFirst();
	        // add the rows from the cursor into the (now) new table
	        for (int i=0; i<c.getCount(); i++) {
	        	ContentValues cv = new ContentValues();
	        	
	        	for (int j=0; j<columns10.length; j++) {
	        		switch (c.getType(c.getColumnIndex(columns10[j]))) {
	        			case Cursor.FIELD_TYPE_NULL:
	    	        		cv.putNull(columns[j]);
	        				break;
	        			case Cursor.FIELD_TYPE_BLOB:
	    	        		cv.put(columns[j], c.getBlob(j));
	        				break;
	        			case Cursor.FIELD_TYPE_FLOAT:
	        				cv.put(columns[j], c.getFloat(j));
	        				break;
	        			case Cursor.FIELD_TYPE_INTEGER:
	        				cv.put(columns[j], c.getInt(j));
	        				break;
	        			case Cursor.FIELD_TYPE_STRING:
	        				cv.put(columns[j], c.getString(j));
	        				break;
	        		}
	        	}
	        	for (int j=columns10.length; j<columns.length; j++) {
	        		// create null entries for rest of missing fields
	        		cv.putNull(columns[j]);
	        	}
    	        db.insert( tablename, null, cv);
	        }
		}
		if (c != null) {
			c.close();
		}
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	if (oldVersion == 10) {
    		// read each table, write new table
    		Cursor c = db.rawQuery("SELECT * FROM " + SessionTable.TABLE_NAME, null);
        	String[] columnsSessionTable10 = { 
        			SessionTable10.SESSION_ID, 
        			SessionTable10.API_KEY,
        			SessionTable10.START_TIME,
        			SessionTable10.END_TIME,
        			SessionTable10.SESSION_LENGTH,
        			SessionTable10.ATTRIBUTES };
        	
        	String[] columnsSessionTable = { 
        			SessionTable.SESSION_ID,
        			SessionTable.API_KEY,
        			SessionTable.START_TIME,
        			SessionTable.END_TIME,
        			SessionTable.SESSION_LENGTH,
        			SessionTable.ATTRIBUTES,
        			SessionTable.CF_UUID };

    		copyDB(db, SessionTable.TABLE_NAME, c, columnsSessionTable10, columnsSessionTable);
    		c.close();
    		
    		c = db.rawQuery("SELECT * FROM " + MessageTable.TABLE_NAME, null);
    		String[] columnsMessageTable10 = {
    				MessageTable10.SESSION_ID,
    				MessageTable10.API_KEY,
    				MessageTable10.MESSAGE,
    				MessageTable10.STATUS,
    				MessageTable10.CREATED_AT };
        	
    		String[] columnsMessageTable = {    
    				MessageTable.SESSION_ID,
    				MessageTable.API_KEY,
    				MessageTable.MESSAGE,
    				MessageTable.STATUS,
    				MessageTable.CREATED_AT,
    				MessageTable.MESSAGE_TYPE,
    				MessageTable.SESSION_ID,
    				MessageTable.CF_UUID };

    		copyDB(db, MessageTable.TABLE_NAME, c, columnsMessageTable10, columnsMessageTable);
    		c.close();
    		
    		c = db.rawQuery("SELECT * FROM " + UploadTable.TABLE_NAME, null);
    		String[] columnsUploadTable10 = {
    				UploadTable10.API_KEY,
    				UploadTable10.MESSAGE,
    				UploadTable10.CREATED_AT };
        	
    		String[] columnsUploadTable = {  
    				UploadTable.API_KEY,
    				UploadTable.MESSAGE,
    				UploadTable.CREATED_AT,
    				UploadTable.CF_UUID,
    				UploadTable.SESSION_ID };

    		copyDB(db, UploadTable.TABLE_NAME, c, columnsUploadTable10, columnsUploadTable);
    		c.close();
    		
    		c = db.rawQuery("SELECT * FROM " + CommandTable.TABLE_NAME, null);
    		String[] columnsCommandTable10 = {     
    				CommandTable10.URL,
    				CommandTable10.METHOD,
    				CommandTable10.POST_DATA,
    				CommandTable10.HEADERS,
    				CommandTable10.CREATED_AT };
        	
    		String[] columnsCommandTable = {
    				CommandTable.URL,
    				CommandTable.METHOD,
    				CommandTable.POST_DATA,
    				CommandTable.HEADERS,
    				CommandTable.CREATED_AT,
                    CommandTable.API_KEY,
                    CommandTable.CF_UUID,
                    CommandTable.SESSION_ID };

    		copyDB(db, CommandTable.TABLE_NAME, c, columnsCommandTable10, columnsCommandTable);
    		c.close();
    		
    	} else {
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

}
