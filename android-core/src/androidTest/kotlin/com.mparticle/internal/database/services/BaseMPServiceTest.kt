package com.mparticle.internal.database.services

import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import com.mparticle.internal.InternalSession
import com.mparticle.internal.database.MPDatabaseImpl
import com.mparticle.internal.database.TestSQLiteOpenHelper
import com.mparticle.internal.database.tables.MParticleDatabaseHelper
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.testutils.BaseCleanInstallEachTest
import org.json.JSONException
import org.junit.Before
import java.util.UUID

abstract class BaseMPServiceTest : BaseCleanInstallEachTest() {
    @Before
    @Throws(Exception::class)
    fun beforeBaseMPService() {
        val openHelper: SQLiteOpenHelper = TestSQLiteOpenHelper(
            MParticleDatabaseHelper(mContext),
            MParticleDatabaseHelper.getDbName()
        )
        database = MPDatabaseImpl(openHelper.writableDatabase)
    }

    @get:Throws(JSONException::class)
    val mpMessage: BaseMPMessage
        get() = getMpMessage(UUID.randomUUID().toString())

    @Throws(JSONException::class)
    fun getMpMessage(sessionId: String?): BaseMPMessage {
        return getMpMessage(sessionId, mRandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE))
    }

    @Throws(JSONException::class)
    fun getMpMessage(sessionId: String?, mpid: Long): BaseMPMessage {
        val session = InternalSession()
        session.mSessionID = sessionId
        return BaseMPMessage.Builder(
            mRandomUtils.getAlphaNumericString(
                mRandomUtils.randomInt(
                    20,
                    48
                )
            )
        ).build(
            session,
            Location(mRandomUtils.getAlphaNumericString(mRandomUtils.randomInt(1, 55))),
            mpid
        )
    }

    companion object {
        @JvmField
        var database: MPDatabaseImpl? = null
    }
}
