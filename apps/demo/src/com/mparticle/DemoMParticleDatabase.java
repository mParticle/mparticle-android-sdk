package com.mparticle;

import android.content.Context;

public class DemoMParticleDatabase extends MParticleDatabase {

    public DemoMParticleDatabase(Context context) {
        super(context);
    }

    public interface MessageTable extends MParticleDatabase.MessageTable {
    };

    public interface CommandTable extends MParticleDatabase.CommandTable {
    };

    public interface SessionTable extends MParticleDatabase.SessionTable {
    };

    public interface UploadTable extends MParticleDatabase.UploadTable {
    };

}
