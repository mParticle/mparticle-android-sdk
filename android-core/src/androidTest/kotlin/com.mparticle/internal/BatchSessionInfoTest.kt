package com.mparticle.internal

import com.mparticle.InstallReferrerHelper
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.internal.database.tables.MParticleDatabaseHelper
import com.mparticle.messages.ConfigResponseMessage
import com.mparticle.messages.events.MPEventMessage
import com.mparticle.testing.BaseStartedTest
import com.mparticle.testing.context
import com.mparticle.testing.equals
import com.mparticle.testing.mockserver.EndpointType
import com.mparticle.testing.mockserver.Server
import com.mparticle.testing.mustEqual
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNotEquals

class BatchSessionInfoTest : BaseStartedTest() {

    override fun afterBeforeAll() {
        MParticleDatabaseHelper.setDbName(null)
        super.afterBeforeAll()
    }

    @Before
    fun before() {
        initialConfigResponse(ConfigResponseMessage(includeSessionHistory = false))
    }

    /**
     * This test is in response to a bug where, when many messages (> 1 batch worth)
     * are uploaded with for a Session other than the current Session, batches after the first
     * one were being sent with the current Session's Application/Device Info rather
     * that the Application/Device Info of the Session they where logged under
     *
     * This makes sure that all messages logged under a Session are uploaded with that Session's
     * Application/Device Info
     */
    @Test
    fun testProperSessionAttachedToBatch() {
        var messageCount = 0
        MParticle.getInstance()?.upload()
        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.appInfo?.installReferrer mustEqual "111"
                messageCount += it.body.messages.filterIsInstance<MPEventMessage>().size
                MParticle.getInstance()?.upload()
                messageCount >= 150
            }
            .after {
                InstallReferrerHelper.setInstallReferrer(context, "111")
                (0..150).forEach { MParticle.getInstance()!!.logEvent(MPEvent.Builder(it.toString()).build()) }

                AccessUtils.awaitMessageHandler()
                MParticle.getInstance()?.apply {
                    Internal().apply {
                        val sessionId = appStateManager.session.mSessionID
                        appStateManager.endSession()
                        appStateManager.ensureActiveSession()
                        InstallReferrerHelper.setInstallReferrer(context, "222")
                        assertNotEquals(sessionId, appStateManager.session.mSessionID)
                    }
                    upload()
                }
            }
            .blockUntilFinished()

        Server
            .endpoint(EndpointType.Events)
            .assertWillReceive {
                it.body.appInfo?.installReferrer equals "222"
            }
            .after {
                MParticle.getInstance()?.apply {
                    logEvent(MPEvent.Builder("1").build())
                    upload()
                }
            }
    }
}
