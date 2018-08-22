package com.mparticle.internal;

import com.mparticle.mock.MockContext;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class InternalSessionTest {

    @Test
    public void testSessionIdsAreCapitalized() {
        InternalSession session = new InternalSession();
        session.start(new MockContext());
        String sessionId = session.mSessionID;
        Assert.assertNotEquals(Constants.NO_SESSION_ID, sessionId);
        Assert.assertEquals(sessionId.toUpperCase(Locale.US), sessionId);
    }
}