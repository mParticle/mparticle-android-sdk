package com.mparticle;

import android.content.Context;

public class DemoMessageDatabase extends MessageDatabase {

    public DemoMessageDatabase(Context context) {
        super(context);
    }

    public interface MessageTable extends MessageDatabase.MessageTable {};
    public interface CommandTable extends MessageDatabase.CommandTable {};
    public interface SessionTable extends MessageDatabase.SessionTable {};
    public interface UploadTable extends MessageDatabase.UploadTable {};

}
