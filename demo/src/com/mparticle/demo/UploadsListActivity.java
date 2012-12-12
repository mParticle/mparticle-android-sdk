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

import com.mparticle.DemoMParticleDatabase;
import com.mparticle.DemoMParticleDatabase.UploadTable;

public class UploadsListActivity extends ListActivity {

    private static final SimpleDateFormat sFormatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoMParticleDatabase mmDB = new DemoMParticleDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query("uploads", null, null, null, null, null, UploadTable.CREATED_AT + " desc");

        String[] from = new String[] { UploadTable.MESSAGE, UploadTable.API_KEY, UploadTable.CREATED_AT, UploadTable.MESSAGE };
        int[] to = { R.id.uploadId, R.id.apiKey, R.id.msgTime, R.id.msgMsg };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.upload_list_entry, selectCursor,
                from, to);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch (view.getId()) {
                case R.id.msgTime:
                    ((TextView) view).setText(sFormatter.format(new Date(cursor.getLong(columnIndex))));
                    break;
                case R.id.msgMsg: {
                    String message = cursor.getString(columnIndex);
                    try {
                        JSONObject messageJSON = new JSONObject(message);
                        ((TextView) view).setText(messageJSON.toString(2));
                        break;
                    } catch (JSONException e) {
                        return false;
                    }
                }
                case R.id.uploadId: {
                    String message = cursor.getString(columnIndex);
                    try {
                        JSONObject messageJSON = new JSONObject(message);
                        ((TextView) view).setText(messageJSON.getString("id"));
                        break;
                    } catch (JSONException e) {
                        return false;
                    }
                }
                default:
                    return false;
                }
                return true;
            }
        });

        setListAdapter(adapter);
        db.close();

    }

}
