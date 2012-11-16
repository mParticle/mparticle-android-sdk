package com.mparticle.demo;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mparticle.DemoMessageDatabase;
import com.mparticle.DemoMessageDatabase.CommandTable;

public class CommandsListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DemoMessageDatabase mmDB = new DemoMessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query(CommandTable.TABLE_NAME, null, null, null, null, null, "_id desc");

        String[] from = new String[] { CommandTable.COMMAND_ID, CommandTable.URL, CommandTable.METHOD,
                CommandTable.POST_DATA, CommandTable.HEADERS, CommandTable.STATUS };
        int[] to = { R.id.commandId, R.id.url, R.id.method, R.id.postData, R.id.headers, R.id.status };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.command_list_entry, selectCursor,
                from, to);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if(view.getId() == R.id.status) {
                    ((TextView) view).setText(cursor.getInt(columnIndex)==1?"Ready":"Unknown");
                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
        db.close();

    }

}
