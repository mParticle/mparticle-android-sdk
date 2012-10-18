package com.mparticle.demo;

import org.json.JSONObject;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.mparticle.MessageManager;

public class PendingMessagesActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MessageManager mm=MessageManager.getInstance(this.getApplicationContext());

        ArrayAdapter<JSONObject> adapter = new ArrayAdapter<JSONObject>(this,
                  android.R.layout.simple_list_item_1, android.R.id.text1, mm.messages);
        setListAdapter(adapter);
    }

}
