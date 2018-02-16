package com.mparticle.internal;

import com.mparticle.BaseCleanStartedEachTest;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.junit.Test;

import java.util.Hashtable;

public class MPEventTest extends BaseCleanStartedEachTest {

    @Override
    protected void beforeClass() throws Exception {

    }

    @Override
    protected void before() throws Exception {

    }

    @Test
    public void testDangerousMapImplementations() {
        MPEvent event = new MPEvent.Builder("randomEvent", MParticle.EventType.Other).info(new Hashtable<String, String>()).build();
    }
}
