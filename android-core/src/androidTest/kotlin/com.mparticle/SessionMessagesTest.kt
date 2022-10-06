package com.mparticle

import android.os.Handler
import android.os.Looper
import com.mparticle.internal.AccessUtils
import com.mparticle.internal.AppStateManager
import com.mparticle.internal.Constants
import com.mparticle.networking.Matcher
import com.mparticle.networking.MockServer.JSONMatch
import com.mparticle.testutils.AndroidUtils
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SessionMessagesTest : BaseCleanStartedEachTest() {
    private  lateinit var mAppStateManager: AppStateManager
    private lateinit var mHandler: Handler

    @Before
    fun before() {
        mAppStateManager = MParticle.getInstance()?.mInternal?.appStateManager!!
        mHandler = Handler(Looper.getMainLooper())
    }

    @Test
    @Throws(Exception::class)
    fun testSessionStartMessage() {
        val sessionStartReceived = BooleanArray(1)
        sessionStartReceived[0] = false
        Assert.assertFalse(mAppStateManager.session.isActive)
        val sessionId = AndroidUtils.Mutable<String?>(null)
        mAppStateManager.ensureActiveSession()
        sessionId.value = mAppStateManager.session.mSessionID
        AccessUtils.awaitMessageHandler()
        MParticle.getInstance()?.upload()
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().eventsUrl).bodyMatch(
                JSONMatch { jsonObject ->
                    try {
                        val jsonArray = jsonObject.optJSONArray(Constants.MessageKey.MESSAGES)
                            ?: return@JSONMatch false
                        for (i in 0 until jsonArray.length()) {
                            val eventObject = jsonArray.getJSONObject(i)
                            if (eventObject.getString("dt") == Constants.MessageType.SESSION_START) {
                                Assert.assertEquals(
                                    eventObject.getLong("ct").toFloat(),
                                    mAppStateManager.session.mSessionStartTime.toFloat(),
                                    1000f
                                )
                                Assert.assertEquals(
                                    """started sessionID = ${sessionId.value} 
current sessionId = ${mAppStateManager.session.mSessionID} 
sent sessionId = ${eventObject.getString("id")}""",
                                    mAppStateManager.session.mSessionID,
                                    eventObject.getString("id")
                                )
                                sessionStartReceived[0] = true
                                return@JSONMatch true
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Assert.fail(e.message)
                    }
                    false
                })
        )
        Assert.assertTrue(sessionStartReceived[0])
    }
}