package com.mparticle.demo;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import com.mparticle.MessageDatabase;
import com.mparticle.MessageDatabase.CommandTable;

public class CommandsListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MessageDatabase mmDB = new MessageDatabase(this);
        SQLiteDatabase db = mmDB.getReadableDatabase();

        Cursor selectCursor = db.query(CommandTable.TABLE_NAME, null, null, null, null, null, "_id desc");

        String[] from = new String[] { CommandTable.COMMAND_ID, CommandTable.URL, CommandTable.METHOD,
                CommandTable.POST_DATA, CommandTable.UPLOAD_STATUS };
        int[] to = { R.id.commandId, R.id.url, R.id.method, R.id.postData, R.id.uploadStatus };

        // NOTE: this Activity is doing SQL directly on the main UI thread,
        // which you would never do in production code
        @SuppressWarnings("deprecation")
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.command_list_entry, selectCursor,
                from, to);

        setListAdapter(adapter);
        db.close();

    }

}
