package com.mparticle.internal.database.services;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mparticle.internal.Constants;
import com.mparticle.internal.MessageBatch;
import com.mparticle.internal.database.services.MParticleDBManager;
import com.mparticle.internal.database.tables.UploadTable;

import java.util.ArrayList;
import java.util.List;

public class UploadService extends UploadTable {
    public static int cleanupUploadMessages(SQLiteDatabase database) {
        return database.delete(UploadTableColumns.TABLE_NAME, "length(" + UploadTableColumns.MESSAGE + ") > " + Constants.LIMIT_MAX_UPLOAD_SIZE, null);
    }

    /**
     * Generic method to insert a new upload,
     * either a regular message batch, or a session history.
     *
     * @param message
     */
    public static void insertUpload(SQLiteDatabase database, MessageBatch message, String apiKey) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTableColumns.API_KEY, apiKey);
        contentValues.put(UploadTableColumns.CREATED_AT, message.optLong(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis()));
        contentValues.put(UploadTableColumns.MESSAGE, message.toString());
        database.insert(UploadTableColumns.TABLE_NAME, null, contentValues);
    }

    public static List<MParticleDBManager.ReadyUpload> getReadyUploads(SQLiteDatabase database) {
        List<MParticleDBManager.ReadyUpload> readyUploads = new ArrayList<MParticleDBManager.ReadyUpload>();
        Cursor readyUploadsCursor = null;
        try {
            readyUploadsCursor = database.query(UploadTableColumns.TABLE_NAME, new String[]{"_id", UploadTableColumns.MESSAGE},
                    null, null, null, null, UploadTableColumns.CREATED_AT);
            int messageIdIndex = readyUploadsCursor.getColumnIndex(UploadTableColumns._ID);
            int messageIndex = readyUploadsCursor.getColumnIndex(UploadTableColumns.MESSAGE);
            while (readyUploadsCursor.moveToNext()) {
                readyUploads.add(new MParticleDBManager.ReadyUpload(readyUploadsCursor.getInt(messageIdIndex), readyUploadsCursor.getString(messageIndex)));
                readyUploadsCursor.moveToNext();
            }
        }
        finally {
            if (readyUploadsCursor != null && !readyUploadsCursor.isClosed()) {
                readyUploadsCursor.close();
            }
            return readyUploads;
        }

    }

    /**
     * After an actually successful upload over the wire.
     *
     * @param id
     * @return number of rows deleted (should be 1)
     */
    public static int deleteUpload(SQLiteDatabase database, int id) {
        String[] whereArgs = {Long.toString(id)};
        return database.delete(UploadTableColumns.TABLE_NAME, "_id=?", whereArgs);
    }
}
