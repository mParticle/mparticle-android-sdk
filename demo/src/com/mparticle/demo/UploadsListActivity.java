package com.mparticle.demo;

import android.app.ListActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import com.mparticle.MessageDatabase;
import com.mparticle.MessageDatabase.UploadTable;

public class UploadsListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MessageDatabase mmDB = new MessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query("uploads", null, null, null, null, null, UploadTable.MESSAGE_TIME + " desc");

        String[] from = new String[] { UploadTable.UPLOAD_ID, UploadTable.MESSAGE_TIME,
                UploadTable.STATUS, UploadTable.MESSAGE };
        int[] to = { R.id.uploadId, R.id.msgTime, R.id.msgStatus, R.id.msgMsg };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.upload_list_entry, selectCursor,
                from, to);

        setListAdapter(adapter);
        db.close();

    }

}
