package com.mparticle

import org.junit.Assert
import org.junit.Test

class SessionTest {

    @Test
    fun equals() {
        var sessionA = Session(null, null)
        var sessionB = Session(null, null)
        Assert.assertEquals(sessionA, sessionB)
        val sessionC = Session(null, 123L)
        Assert.assertEquals(sessionA, sessionB)
        Assert.assertFalse(sessionA == sessionC)
        Assert.assertFalse(sessionC == sessionA)
        var sessionF = Session("foo", 123L)
        val sessionG = Session("foo", 456L)
        Assert.assertFalse(sessionF == sessionG)
        sessionF = Session("foo", null)
        Assert.assertFalse(sessionF == sessionG)
        sessionA = Session("foo", 456L)
        sessionB = Session("fOo", 456L)
        Assert.assertEquals(sessionA, sessionB)
    }
}
