package com.mparticle

import android.os.Handler
import android.os.Looper
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.AppStateManager
import com.mparticle.messages.events.SessionStartMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.Mutable
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.orThrow
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionMessagesTest : BaseStartedTest() {
    lateinit var mAppStateManager: AppStateManager
    lateinit var mHandler: Handler

    @Before
    fun before() {
        mAppStateManager = MParticle.getInstance().orThrow().Internal().getAppStateManager()
        mHandler = Handler(Looper.getMainLooper())
    }

    @Test
    @Throws(Exception::class)
    fun testSessionStartMessage() {
        Assert.assertFalse(mAppStateManager.getSession().isActive())
        val sessionId: Mutable<String?> = Mutable(null)

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.messages
                    .filterIsInstance<SessionStartMessage>()
                    .let {
                        assertEquals(1, it.size)
                        assertEquals(mAppStateManager.session.mSessionStartTime.toDouble(), it[0].timeStamp?.toDouble() ?: 0.0, 1000.0)
                        true
                    }
            }
            .after {
                mAppStateManager.ensureActiveSession()
                sessionId.value = mAppStateManager.getSession().mSessionID
                AccessUtils.awaitMessageHandler()
                MParticle.getInstance()!!.upload()
            }
            .blockUntilFinished()
    }
}
