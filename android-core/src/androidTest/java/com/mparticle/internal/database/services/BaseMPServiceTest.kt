package com.mparticle.internal.database.services

import android.database.sqlite.SQLiteOpenHelper
import android.location.Location
import com.mparticle.internal.InternalSession
import com.mparticle.internal.database.MPDatabaseImpl
import com.mparticle.internal.database.TestSQLiteOpenHelper
import com.mparticle.internal.database.tables.MParticleDatabaseHelper
import com.mparticle.internal.messages.BaseMPMessage
import com.mparticle.testing.BaseTest
import com.mparticle.testing.RandomUtils
import com.mparticle.testing.context
import org.json.JSONException
import org.junit.Before
import java.util.UUID

abstract class BaseMPServiceTest : BaseTest() {
    @Before
    @Throws(Exception::class)
    fun beforeBaseMPService() {
        val openHelper: SQLiteOpenHelper = TestSQLiteOpenHelper(
            MParticleDatabaseHelper(context),
            MParticleDatabaseHelper.getDbName()
        )
        database = MPDatabaseImpl(openHelper.writableDatabase)
    }

    @get:Throws(JSONException::class)
    val mpMessage: BaseMPMessage
        get() = getMpMessage(UUID.randomUUID().toString())

    @Throws(JSONException::class)
    fun getMpMessage(sessionId: String): BaseMPMessage {
        return getMpMessage(sessionId, RandomUtils.randomLong(Long.MIN_VALUE, Long.MAX_VALUE))
    }

    @Throws(JSONException::class)
    fun getMpMessage(sessionId: String, mpid: Long): BaseMPMessage {
        val session = InternalSession()
        session.mSessionID = sessionId
        return BaseMPMessage.Builder(
            RandomUtils.getAlphaNumericString(
                RandomUtils.randomInt(
                    20,
                    48
                )
            )
        ).build(
            session,
            Location(RandomUtils.getAlphaNumericString(RandomUtils.randomInt(1, 55))),
            mpid
        )
    }

    companion object {
        @JvmStatic
        protected var database: MPDatabaseImpl? = null
    }
}
