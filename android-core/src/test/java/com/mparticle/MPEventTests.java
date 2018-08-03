package com.mparticle;



import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class MPEventTests  {

    @Test
    public void testBasicBuilder(){
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category").duration(1234).build();
        assertTrue(event.getEventName().equals("test name"));
        assertTrue(event.getEventType().equals(MParticle.EventType.Navigation));
        assertTrue(event.getCategory().equals("test category"));
        assertTrue(event.getLength() == 1234);
    }

    @Test
    public void testScreenBuilder(){
        MPEvent event = new MPEvent.Builder("test name").category("test category").duration(1234).build();
        assertTrue(event.getEventName().equals("test name"));
        assertTrue(event.getEventType().equals(MParticle.EventType.Other));
        assertTrue(event.getCategory().equals("test category"));
        assertTrue(event.getLength() == 1234);
    }

    @Test
    public void testSerialization(){
        String eventString = new MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category").duration(1234).toString();
        MPEvent event = MPEvent.Builder.parseString(eventString).build();
        assertTrue(event.getEventName().equals("test name"));
        assertTrue(event.getEventType().equals(MParticle.EventType.Navigation));
        assertTrue(event.getCategory().equals("test category"));
        assertTrue(event.getLength() == 1234);
    }

    @Test
    public void testEventLength() {
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).category("test category").build();
        assertNull(event.getLength());
    }

    @Test
    public void testTimer(){
        MPEvent.Builder eventBuilder = new MPEvent.Builder("test name", MParticle.EventType.Navigation);

        eventBuilder.startTime();
        Long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            fail(e.toString());
        }
        eventBuilder.endTime();
        Long duration = System.currentTimeMillis() - startTime;
        assertEquals(duration, eventBuilder.build().getLength(), 5);
    }

    @Test
    public void testCopyConstructor () {
        //test the most basic event - there was a bug when there were no attributes.
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Other).build();
        MPEvent copiedEvent = new MPEvent(event);
        assertEquals(event.getEventName(), copiedEvent.getEventName());
        assertEquals(event.getEventType(), copiedEvent.getEventType());

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("key 1", "value 1");
        attributes.put("key 2", "value 2");

        event = new MPEvent.Builder("another name", MParticle.EventType.Social)
                .category("category")
                .duration(12345)
                .info(attributes)
                .addCustomFlag("cool flag key", "flag 1 value 1")
                .addCustomFlag("cool flag key", "flag 1 value 2")
                .addCustomFlag("cool flag key 2", "flag 2 value 1")
                .build();

        copiedEvent = new MPEvent(event);
        assertEquals("another name", copiedEvent.getEventName());
        assertEquals(MParticle.EventType.Social, copiedEvent.getEventType());
        assertEquals("category", copiedEvent.getCategory());
        assertEquals("value 1", copiedEvent.getInfo().get("key 1"));
        assertEquals("value 2", copiedEvent.getInfo().get("key 2"));
        Map<String, List<String>> flags = copiedEvent.getCustomFlags();
        assertEquals(flags.get("cool flag key").size(), 2);
        assertEquals(flags.get("cool flag key 2").size(), 1);
        assertEquals(flags.get("cool flag key").get(0), "flag 1 value 1");
        assertEquals(flags.get("cool flag key").get(1), "flag 1 value 2");
        assertEquals(flags.get("cool flag key 2").get(0), "flag 2 value 1");

    }
}
