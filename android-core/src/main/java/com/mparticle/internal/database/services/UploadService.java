package com.mparticle.internal.database.services;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.style.UpdateLayout;

import com.mparticle.internal.Constants;
import com.mparticle.internal.Logger;
import com.mparticle.internal.database.MPDatabase;
import com.mparticle.internal.database.UploadSettings;
import com.mparticle.internal.database.tables.UploadTable;
import com.mparticle.internal.listeners.InternalListenerManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UploadService extends UploadTable {

    public static int cleanupUploadMessages(MPDatabase database) {
        return database.delete(UploadTableColumns.TABLE_NAME, "length(" + UploadTableColumns.MESSAGE + ") > " + Constants.LIMIT_MAX_UPLOAD_SIZE, null);
    }

    /**
     * Generic method to insert a new upload,
     * either a regular message batch, or a session history.
     *
     * @param message
     */
    public static void insertUpload(MPDatabase database, JSONObject message, UploadSettings uploadSettings) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTableColumns.API_KEY, uploadSettings.getApiKey());
        contentValues.put(UploadTableColumns.CREATED_AT, message.optLong(Constants.MessageKey.TIMESTAMP, System.currentTimeMillis()));
        contentValues.put(UploadTableColumns.MESSAGE, message.toString());
        contentValues.put(UploadTableColumns.REQUEST_TYPE, UploadTable.UPLOAD_REQUEST);
        contentValues.put(UploadTableColumns.UPLOAD_SETTINGS, uploadSettings.toJson());
        InternalListenerManager.getListener().onCompositeObjects(message, contentValues);
        database.insert(UploadTableColumns.TABLE_NAME, null, contentValues);
    }

    public static List<MParticleDBManager.ReadyUpload> getReadyUploads(MPDatabase database) {
        List<MParticleDBManager.ReadyUpload> readyUploads = new ArrayList<MParticleDBManager.ReadyUpload>();
        Cursor readyUploadsCursor = null;
        try {
            readyUploadsCursor = database.query(UploadTableColumns.TABLE_NAME, new String[]{"_id", UploadTableColumns.MESSAGE, UploadTableColumns.REQUEST_TYPE, UploadTableColumns.UPLOAD_SETTINGS},
                    null, null, null, null, UploadTableColumns.CREATED_AT);
            int messageIdIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns._ID);
            int messageIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns.MESSAGE);
            int requestTypeIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns.REQUEST_TYPE);
            int uploadSettingsIndex = readyUploadsCursor.getColumnIndexOrThrow(UploadTableColumns.UPLOAD_SETTINGS);
            while (readyUploadsCursor.moveToNext()) {
                MParticleDBManager.ReadyUpload readyUpload = new MParticleDBManager.ReadyUpload(readyUploadsCursor.getInt(messageIdIndex), UploadTable.ALIAS_REQUEST.equals(readyUploadsCursor.getString(requestTypeIndex)), readyUploadsCursor.getString(messageIndex), UploadSettings.withJson(readyUploadsCursor.getString(uploadSettingsIndex)));
                readyUploads.add(readyUpload);
                InternalListenerManager.getListener().onCompositeObjects(readyUploadsCursor, readyUpload);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to get ready uploads");
        } finally {
            if (readyUploadsCursor != null && !readyUploadsCursor.isClosed()) {
                readyUploadsCursor.close();
            }
        }
        return readyUploads;
    }

    /**
     * After an actually successful upload over the wire.
     *
     * @param id
     * @return number of rows deleted (should be 1)
     */
    public static int deleteUpload(MPDatabase database, int id) {
        String[] whereArgs = {Long.toString(id)};
        return database.delete(UploadTableColumns.TABLE_NAME, "_id=?", whereArgs);
    }

    public static long insertAliasRequest(MPDatabase database, JSONObject request, UploadSettings uploadSettings) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UploadTableColumns.API_KEY, uploadSettings.getApiKey());
        contentValues.put(UploadTableColumns.CREATED_AT, System.currentTimeMillis());
        contentValues.put(UploadTableColumns.MESSAGE, request.toString());
        contentValues.put(UploadTableColumns.REQUEST_TYPE, UploadTable.ALIAS_REQUEST);
        contentValues.put(UploadTableColumns.UPLOAD_SETTINGS, uploadSettings.toJson());
        InternalListenerManager.getListener().onCompositeObjects(request, contentValues);
        return database.insert(UploadTableColumns.TABLE_NAME, null, contentValues);
    }
}
