package com.mparticle.internal


import com.mparticle.mock.MockContext
import org.junit.Assert
import org.junit.Test

class InternalSessionTest {
    @Test
    fun testSessionIdsAreCapitalized() {
        val session = InternalSession()
        session.start(MockContext())
        val sessionId = session.mSessionID
        Assert.assertNotEquals(Constants.NO_SESSION_ID, sessionId)
        Assert.assertEquals(sessionId.uppercase(), sessionId)
    }
}