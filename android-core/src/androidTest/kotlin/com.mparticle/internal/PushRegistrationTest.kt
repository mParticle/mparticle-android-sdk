package com.mparticle.internal

import com.google.firebase.iid.FirebaseInstanceIdToken
import com.google.firebase.messaging.FirebaseMessagingServiceTestContext
import com.mparticle.MParticle
import com.mparticle.messaging.InstanceIdService
import com.mparticle.testutils.BaseCleanStartedEachTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertNotNull

class PushRegistrationTest : BaseCleanStartedEachTest() {

    @Test
    fun deferDuplicatePushRegistrationFromFirebase() {
        val configManager = MParticle.getInstance()?.Internal()?.configManager

        assertNull(MParticle.getInstance()?.currentSession)

        // configure the _next_ token Firebase will return when we fetch it (during test)
        FirebaseInstanceIdToken.token = "token1"
        FirebaseMessagingServiceTestContext.appContext = mContext.applicationContext

        // set the current sender/instanceId in the SDK
        configManager?.pushSenderId = "sender1"
        configManager?.pushInstanceId = "token1"

        // kick off a token fetch
        InstanceIdService().onNewToken("")

        // test that 1) the token was fetched and 2) we did not start a session based off of it because it was a duplicate
        assertNull(MParticle.getInstance()?.currentSession)
        assertEquals("token1", configManager?.pushInstanceIdBackground)

        // set the current senderId and remove the current instanceId in the SDK
        configManager?.clearPushRegistration()
        configManager?.pushSenderId = "sender1"

        // kick off a token fetch
        InstanceIdService().onNewToken("")

        // test that 1) the token was fetched and 2) we DID start a session because it was a new token
        assertNotNull(MParticle.getInstance()?.currentSession)
        assertEquals("token1", configManager?.pushInstanceId)
    }
}
