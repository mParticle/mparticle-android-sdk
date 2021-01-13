package com.mparticle;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class SessionTest {

    @Test
    public void getSessionUUID() {
        Session session = new Session(null, null);
        Assert.assertNull(session.getSessionUUID());
        session = new Session("foo-id", null);
        Assert.assertEquals("FOO-ID", session.getSessionUUID());
    }

    @Test
    public void getSessionID() {
        Session session = new Session(null, null);
        Assert.assertEquals(0, session.getSessionID());

        session = new Session(null, null);
        Assert.assertEquals(0, session.getSessionID());

        session = new Session("222F6BEA-F6A8-4DFC-A950-744EFD6FEC3D", System.currentTimeMillis());
        Assert.assertEquals(7868951891731938297L, session.getSessionID());
    }

    @Test
    public void equals() {
        Session sessionA = new Session(null, null);
        Session sessionB = new Session(null, null);
        Assert.assertEquals(sessionA, sessionB);

        Session sessionC = new Session(null, 123L);
        Session sessionD = new Session(null, 123L);
        Assert.assertEquals(sessionA, sessionB);

        Session sessionE = new Session("foo", null);
        Assert.assertFalse(sessionA.equals(sessionC));
        Assert.assertFalse(sessionC.equals(sessionA));

        Session sessionF = new Session("foo", 123L);
        Session sessionG = new Session("foo", 456L);
        assertFalse(sessionF.equals(sessionG));

        sessionF = new Session("foo", null);
        assertFalse(sessionF.equals(sessionG));

        sessionA = new Session("foo", 456L);
        sessionB = new Session("fOo", 456L);
        Assert.assertEquals(sessionA, sessionB);
    }
}