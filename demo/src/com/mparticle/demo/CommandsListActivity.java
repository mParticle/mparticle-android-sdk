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

import com.mparticle.DemoMParticleDatabase;
import com.mparticle.DemoMParticleDatabase.CommandTable;

public class CommandsListActivity extends ListActivity {

    private static final SimpleDateFormat sFormatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoMParticleDatabase mmDB = new DemoMParticleDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query(CommandTable.TABLE_NAME, null, null, null, null, null, "_id desc");

        String[] from = new String[] { CommandTable.URL, CommandTable.METHOD,
                CommandTable.POST_DATA, CommandTable.HEADERS, CommandTable.CREATED_AT };
        int[] to = { R.id.url, R.id.method, R.id.postData, R.id.headers, R.id.msgTime };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.command_list_entry, selectCursor,
                from, to);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch (view.getId()) {
                case R.id.msgTime:
                    ((TextView) view).setText(sFormatter.format(new Date(cursor.getLong(columnIndex))));
                    break;
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
