package com.mparticle.internal

import com.mparticle.InstallReferrerHelper
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions
import com.mparticle.networking.Matcher
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BatchSessionInfoTest : BaseCleanStartedEachTest() {

    override fun useInMemoryDatabase() = true

    override fun transformMParticleOptions(builder: MParticleOptions.Builder): MParticleOptions.Builder {
        return builder.logLevel(MParticle.LogLevel.INFO)
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
        InstallReferrerHelper.setInstallReferrer(mContext, "111")
        (0..150).forEach {
            MParticle.getInstance()?.logEvent(MPEvent.Builder(it.toString()).build())
        }

        AccessUtils.awaitMessageHandler()
        MParticle.getInstance()?.Internal()?.apply {
            val sessionId = appStateManager.fetchSession().mSessionID
            appStateManager.endSession()
            appStateManager.ensureActiveSession()
            InstallReferrerHelper.setInstallReferrer(mContext, "222")
            assertNotEquals(sessionId, appStateManager.fetchSession().mSessionID)
        }

        var messageCount = 0
        MParticle.getInstance()?.upload()
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch {
                val version =
                    it.getJSONObject("ai").getString(Constants.MessageKey.INSTALL_REFERRER)
                if (it.has("msgs")) {
                    var messages = it.getJSONArray("msgs")
                    for (i in 0 until messages.length()) {
                        if (messages.getJSONObject(i).getString("dt") == "e") {
                            messageCount++
                        }
                    }
                }
                assertEquals("111", version)
                MParticle.getInstance()?.upload()
                messageCount >= 150
            }
        )

        MParticle.getInstance()?.apply {
            logEvent(MPEvent.Builder("1").build())
            upload()
        }
        mServer.waitForVerify(
            Matcher(mServer.Endpoints().getEventsUrl()).bodyMatch {
                val version =
                    it.getJSONObject("ai").getString(Constants.MessageKey.INSTALL_REFERRER)
                assertEquals("222", version)
                true
            }
        )
    }
}
