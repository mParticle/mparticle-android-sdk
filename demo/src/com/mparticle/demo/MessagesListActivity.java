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
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mparticle.DemoMessageDatabase;
import com.mparticle.DemoMessageDatabase.MessageTable;

public class MessagesListActivity extends ListActivity {

    private static final String TAG = "mParticleDemo";

    private static final SimpleDateFormat sFormatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoMessageDatabase mmDB = new DemoMessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query("messages", null, null, null, null, null, MessageTable.CREATED_AT
                + " desc, _id desc");

        String[] from = new String[] { MessageTable.SESSION_ID, MessageTable.API_KEY, MessageTable.CREATED_AT, MessageTable.MESSAGE,
                MessageTable.MESSAGE, MessageTable.MESSAGE, MessageTable.STATUS };
        int[] to = { R.id.sessionId, R.id.apiKey, R.id.msgTime, R.id.msgType, R.id.msgMsg, R.id.msgId, R.id.msgStatus };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.message_list_entry, selectCursor,
                from, to);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                try {
                    switch (view.getId()) {
                    case R.id.msgTime:
                        ((TextView) view).setText(sFormatter.format(new Date(cursor.getLong(columnIndex))));
                        break;
                    case R.id.msgStatus:
                        ((TextView) view).setText(cursor.getInt(columnIndex)==1?"Ready":"Batch-ready");
                        break;
                    case R.id.msgId: {
                        JSONObject msgJSON = new JSONObject(cursor.getString(columnIndex));
                        ((TextView) view).setText(msgJSON.optString("id"));
                        break;
                    }
                    case R.id.msgType: {
                        JSONObject msgJSON = new JSONObject(cursor.getString(columnIndex));
                        ((TextView) view).setText(msgJSON.optString("dt"));
                        break;
                    }
                    default:
                        return false;
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "Failed to parse JSON", e);
                }
                return true;
            }
        });
        setListAdapter(adapter);
        db.close();

    }

}
