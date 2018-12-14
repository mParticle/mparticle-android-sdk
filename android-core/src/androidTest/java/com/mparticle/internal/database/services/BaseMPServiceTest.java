package com.mparticle.internal.database.services;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.support.test.InstrumentationRegistry;

import com.mparticle.internal.InternalSession;
import com.mparticle.internal.MessageManager;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.testutils.BaseCleanInstallEachTest;

import org.json.JSONException;
import org.junit.Before;

import java.util.UUID;

abstract public class BaseMPServiceTest extends BaseCleanInstallEachTest {
    protected static SQLiteDatabase database;

    @Before
    public final void beforeBaseMPService() throws Exception {
        SQLiteOpenHelper openHelper = new BaseDatabase(new MParticleDatabaseHelper(InstrumentationRegistry.getContext()), MParticleDatabaseHelper.DB_NAME);
        database = openHelper.getWritableDatabase();
    }

    MessageManager.BaseMPMessage getMpMessage() throws JSONException {
        return getMpMessage(UUID.randomUUID().toString());
    }

    MessageManager.BaseMPMessage getMpMessage(String sessionId) throws JSONException {
        return getMpMessage(sessionId, mRandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    MessageManager.BaseMPMessage getMpMessage(String sessionId, long mpid) throws JSONException {
        InternalSession session = new InternalSession();
        session.mSessionID = sessionId;
        return new MessageManager.BaseMPMessage.Builder(mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(20, 48)), session, new Location(mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55))), mpid).build();
    }
}
