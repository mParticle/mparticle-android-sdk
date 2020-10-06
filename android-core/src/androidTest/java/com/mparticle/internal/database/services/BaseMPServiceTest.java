package com.mparticle.internal.database.services;

import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import androidx.test.platform.app.InstrumentationRegistry;

import com.mparticle.internal.InternalSession;
import com.mparticle.internal.database.BaseDatabase;
import com.mparticle.internal.database.MPDatabaseImpl;
import com.mparticle.internal.database.tables.MParticleDatabaseHelper;
import com.mparticle.internal.messages.BaseMPMessage;
import com.mparticle.testutils.BaseCleanInstallEachTest;

import org.json.JSONException;
import org.junit.Before;

import java.util.UUID;

abstract public class BaseMPServiceTest extends BaseCleanInstallEachTest {
    protected static MPDatabaseImpl database;

    @Before
    public final void beforeBaseMPService() throws Exception {
        SQLiteOpenHelper openHelper = new BaseDatabase(new MParticleDatabaseHelper(mContext), MParticleDatabaseHelper.DB_NAME);
        database = new MPDatabaseImpl(openHelper.getWritableDatabase());
    }

    BaseMPMessage getMpMessage() throws JSONException {
        return getMpMessage(UUID.randomUUID().toString());
    }

    BaseMPMessage getMpMessage(String sessionId) throws JSONException {
        return getMpMessage(sessionId, mRandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    BaseMPMessage getMpMessage(String sessionId, long mpid) throws JSONException {
        InternalSession session = new InternalSession();
        session.mSessionID = sessionId;
        return new BaseMPMessage.Builder(mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(20, 48))).build(session, new Location(mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55))), mpid);
    }
}
