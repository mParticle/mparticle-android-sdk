package com.mparticle.internal.database.services.mp;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;

import com.mparticle.BaseCleanInstallEachTest;
import com.mparticle.internal.networking.BaseMPMessage;
import com.mparticle.internal.Session;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.tables.mp.MParticleDatabaseHelper;

import org.json.JSONException;

import java.util.UUID;

abstract public class BaseMPServiceTest extends BaseCleanInstallEachTest {
    protected static SQLiteDatabase database;

    @CallSuper
    @Override
    protected void before() throws Exception {
        SQLiteOpenHelper openHelper = new BaseDatabase(new MParticleDatabaseHelper(InstrumentationRegistry.getContext()), MParticleDatabaseHelper.DB_NAME);
        database = openHelper.getWritableDatabase();
    }

    @Override
    protected void beforeClass() throws Exception {

    }

    BaseMPMessage getMpMessage() throws JSONException {
        return getMpMessage(UUID.randomUUID().toString());
    }

    BaseMPMessage getMpMessage(String sessionId) throws JSONException {
        Session session = new Session();
        session.mSessionID = sessionId;
        return new BaseMPMessage.Builder("test", session, new Location("New York City"), 1).build();
    }
}
