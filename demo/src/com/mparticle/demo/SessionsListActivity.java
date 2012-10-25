package com.mparticle.demo;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import com.mparticle.MessageDatabase;
import com.mparticle.MessageDatabase.SessionTable;

public class SessionsListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MessageDatabase mmDB = new MessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query("sessions", null, null, null, null, null, SessionTable.START_TIME + " desc");

        String[] from = new String[] { SessionTable.SESSION_ID, SessionTable.START_TIME, SessionTable.END_TIME,
                SessionTable.UPLOAD_STATUS };
        int[] to = { R.id.sessionId, R.id.startTime, R.id.endTime, R.id.status };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.session_list_entry, selectCursor,
                from, to);

        setListAdapter(adapter);
        db.close();

    }

}
