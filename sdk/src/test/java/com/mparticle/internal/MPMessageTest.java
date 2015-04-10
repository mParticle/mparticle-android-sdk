package com.mparticle.internal;

import com.mparticle.MPEvent;
import com.mparticle.MParticle;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;

public class MPMessageTest {

    @Test
    public void testEventLength() throws Exception {
        MPEvent event = new MPEvent.Builder("test name", MParticle.EventType.Navigation).build();
        Session session = new Session();
        MPMessage message = new MPMessage.Builder(Constants.MessageType.EVENT, session, null)
                .name(event.getEventName())
                .timestamp(1235)
                .length(event.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                .build();

        assertNull(message.opt("el"));
        assertNull(message.getAttributes());

        Map<String, String> info = new HashMap<String, String>(1);
        info.put("EventLength", "321");
        MPEvent event2 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).info(info).build();
        MPMessage message2 = new MPMessage.Builder(Constants.MessageType.EVENT, session, null)
                .name(event2.getEventName())
                .timestamp(1235)
                .length(event2.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event2.getInfo()))
                .build();

        assertEquals(message2.getAttributes().getString("EventLength"), "321");

        MPEvent event3 = new MPEvent.Builder("test name", MParticle.EventType.Navigation).duration(123).build();
        MPMessage message3 = new MPMessage.Builder(Constants.MessageType.EVENT, session, null)
                .name(event3.getEventName())
                .timestamp(1235)
                .length(event3.getLength())
                .attributes(MPUtility.enforceAttributeConstraints(event.getInfo()))
                .build();

        assertEquals(message3.getAttributes().getString("EventLength"), "123");

    }
}