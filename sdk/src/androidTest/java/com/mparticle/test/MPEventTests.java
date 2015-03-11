package com.mparticle.test;

import android.test.AndroidTestCase;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;

/**
 * Created by sdozor on 3/4/15.
 */
public class MPEventTests extends AndroidTestCase {
    public void testBasicBuilder(){
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category").duration(1234).build();
        assertTrue(event.getEventName().equals("test name"));
        assertTrue(event.getEventType().equals(MParticle.EventType.Navigation));
        assertTrue(event.getCategory().equals("test category"));
        assertTrue(event.getLength() == 1234);
    }

    public void testSerialization(){
        String eventString = new MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category").duration(1234).toString();
        MPEvent event = MPEvent.Builder.parseString(eventString).build();
        assertTrue(event.getEventName().equals("test name"));
        assertTrue(event.getEventType().equals(MParticle.EventType.Navigation));
        assertTrue(event.getCategory().equals("test category"));
        assertTrue(event.getLength() == 1234);
    }

    public void testEventLength() {
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category").build();
        assertNull(event.getLength());
    }

    public void testTimer(){
        MPEvent.Builder eventBuilder = new MPEvent.Builder("test name", MParticle.EventType.Navigation);

        eventBuilder.startTime();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        eventBuilder.endTime();
        assertTrue(eventBuilder.build().getLength() >= 1000);
    }
}
