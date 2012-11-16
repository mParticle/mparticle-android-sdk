package com.mparticle.demo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mparticle.DemoMessageDatabase;
import com.mparticle.DemoMessageDatabase.SessionTable;

public class SessionsListActivity extends ListActivity {

    private static final SimpleDateFormat sFormatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoMessageDatabase mmDB = new DemoMessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query("sessions", null, null, null, null, null, SessionTable.START_TIME + " desc");

        String[] from = new String[] { SessionTable.SESSION_ID, SessionTable.START_TIME, SessionTable.END_TIME,
                SessionTable.SESSION_LENGTH, SessionTable.STATUS, SessionTable.ATTRIBUTES };
        int[] to = { R.id.sessionId, R.id.startTime, R.id.endTime, R.id.sessionLength, R.id.status, R.id.attributes };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.session_list_entry, selectCursor,
                from, to);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(view.getId() == R.id.startTime || view.getId() == R.id.endTime ) {
                    long time = cursor.getLong(columnIndex);
                    if (time>0) {
                        ((TextView) view).setText(sFormatter.format(new Date(time)));
                        return true;
                    }
                }
                if(view.getId() == R.id.sessionLength ) {
                    ((TextView) view).setText(cursor.getLong(columnIndex)/1000 + " seconds");
                    return true;
                }
                if(view.getId() == R.id.status) {
                    ((TextView) view).setText(cursor.getInt(columnIndex)==1?"Active":"Ended");
                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
        db.close();

    }

}
