package com.mparticle.demo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mparticle.DemoMessageDatabase;
import com.mparticle.DemoMessageDatabase.UploadTable;

public class UploadsListActivity extends ListActivity {

    private static final SimpleDateFormat sFormatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoMessageDatabase mmDB = new DemoMessageDatabase(this);
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
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(view.getId() == R.id.msgTime ) {
                    ((TextView) view).setText(sFormatter.format(new Date(cursor.getLong(columnIndex))));
                    return true;
                }
                if(view.getId() == R.id.msgStatus) {
                    ((TextView) view).setText(cursor.getInt(columnIndex)==1?"Ready":"Unknown");
                    return true;
                }
                if(view.getId() == R.id.msgMsg) {
                    String message = cursor.getString(columnIndex);
                    try {
                        JSONObject messageJSON = new JSONObject(message);
                        ((TextView) view).setText(messageJSON.toString(2));
                        return true;
                    } catch (JSONException e) {
                        // just print the string
                    }
                }
                return false;
            }
        });

        setListAdapter(adapter);
        db.close();

    }

}
