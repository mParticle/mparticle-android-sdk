package com.mparticle;

import junit.framework.Assert;

import org.junit.Test;

public class SessionTest {

    @Test
    public void getSessionUUID() {
        Session session = new Session(null);
        Assert.assertNull(session.getSessionUUID());
        session = new Session("foo-id");
        Assert.assertEquals("FOO-ID", session.getSessionUUID());
    }

    @Test
    public void getSessionID() {
        Session session = new Session(null);
        Assert.assertEquals(0, session.getSessionID());

        session = new Session(null);
        Assert.assertEquals(0, session.getSessionID());

        session = new Session("222F6BEA-F6A8-4DFC-A950-744EFD6FEC3D");
        Assert.assertEquals(7868951891731938297L, session.getSessionID());
    }

    @Test
    public void equals() {
        Session sessionA = new Session(null);
        Session sessionB = new Session(null);
        Assert.assertEquals(sessionA, sessionB);

        Session sessionC = new Session("foo");
        Assert.assertFalse(sessionA.equals(sessionC));
        Assert.assertFalse(sessionC.equals(sessionA));

        sessionA = new Session("foo");
        sessionB = new Session("fOo");
        Assert.assertEquals(sessionA, sessionB);
    }
}